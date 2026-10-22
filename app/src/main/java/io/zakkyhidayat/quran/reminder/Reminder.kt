package io.zakkyhidayat.quran.reminder

import android.annotation.SuppressLint
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.zakkyhidayat.quran.MainActivity
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.app
import io.zakkyhidayat.quran.data.Collections
import io.zakkyhidayat.quran.data.DivisionMath
import io.zakkyhidayat.quran.home.plainTranslation
import io.zakkyhidayat.quran.data.MarkerKind
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.settings.ReminderUnit
import io.zakkyhidayat.quran.data.SunnahReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Calendar

/**
 * Pengingat membaca harian. AlarmManager (tidak eksak, jadi tanpa izin alarm tepat) memicu [ReminderReceiver] di jam
 * pilihan pengguna; notifikasinya menyebut bagian (juz/hizb/manzil) yang dibaca hari ini beserta rentang ayatnya (bagian
 * tempat posisi baca terakhir berada, atau bagian berikutnya bila sudah sampai ujungnya), dan ketukan membuka awal
 * bagian itu. Jadwal dipasang ulang setiap kali notifikasi tampil, setelah perangkat menyala ulang, dan saat
 * jam/zona waktu berubah.
 */
object Reminder {
    private const val CHANNEL = "reading_reminder"

    fun schedule(context: Context, settings: AppSettings) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        scheduleAlarm(
            alarms,
            context,
            settings.reminderEnabled,
            settings.reminderMinutes,
            null,
            REQUEST_DAILY,
            ReminderReceiver.ACTION_REMIND,
        )
        scheduleAlarm(
            alarms,
            context,
            settings.bedtimeSunnahReminderEnabled,
            BEDTIME_MINUTES,
            null,
            REQUEST_BEDTIME,
            ReminderReceiver.ACTION_BEDTIME,
        )
        scheduleAlarm(
            alarms,
            context,
            settings.kahfThursdayReminderEnabled,
            KAHF_MINUTES,
            Calendar.THURSDAY,
            REQUEST_KAHF,
            ReminderReceiver.ACTION_KAHF,
        )
        VOTD_MINUTES.forEachIndexed { slot, minutes ->
            scheduleAlarm(
                alarms,
                context,
                settings.votdReminderEnabled,
                minutes,
                null,
                REQUEST_VOTD + slot,
                ReminderReceiver.ACTION_VOTD[slot],
            )
        }
    }

    private fun scheduleAlarm(
        alarms: AlarmManager,
        context: Context,
        enabled: Boolean,
        minutes: Int,
        dayOfWeek: Int?,
        requestCode: Int,
        action: String,
    ) {
        val pending = alarmIntent(context, requestCode, action)
        alarms.cancel(pending)
        if (!enabled) return
        // Jendela 10 menit: cukup tepat untuk pengingat, tanpa izin SCHEDULE_EXACT_ALARM.
        alarms.setWindow(AlarmManager.RTC_WAKEUP, nextTriggerMillis(System.currentTimeMillis(), minutes, dayOfWeek), 10 * 60 * 1000L, pending)
    }

    /**
     * Waktu pemicu berikutnya setelah [now]: pukul [minutes] (menit sejak tengah malam) setiap hari, atau setiap
     * [dayOfWeek] ([Calendar.THURSDAY] dan seterusnya) bila diberikan. Tepat pada [now] dianggap sudah lewat.
     */
    internal fun nextTriggerMillis(now: Long, minutes: Int, dayOfWeek: Int? = null): Long {
        val next = Calendar.getInstance().apply {
            timeInMillis = now
            if (dayOfWeek != null) set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, if (dayOfWeek != null) 7 else 1)
        }
        return next.timeInMillis
    }

    private fun alarmIntent(context: Context, requestCode: Int, action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, ReminderReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("NotificationPermission")
    internal suspend fun notify(context: Context, kind: ReminderKind = ReminderKind.Daily) {
        val app = context.app
        val settings = app.settings.settings.first()
        val enabled = when (kind) {
            ReminderKind.Daily -> settings.reminderEnabled
            ReminderKind.Bedtime -> settings.bedtimeSunnahReminderEnabled
            ReminderKind.KahfThursday -> settings.kahfThursdayReminderEnabled
            ReminderKind.VotdMorning, ReminderKind.VotdAfternoon, ReminderKind.VotdNight -> settings.votdReminderEnabled
        }
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        when (kind) {
            ReminderKind.Bedtime -> {
                notifySunnah(
                    context, manager, SunnahReading.MulkSajdah, app.mushaf.ayahPage(32, 1),
                    REQUEST_BEDTIME, BEDTIME_NOTIFICATION_ID,
                    R.string.sunnah_reminder_notification_title, R.string.sunnah_reminder_notification_text,
                )
                return
            }
            ReminderKind.KahfThursday -> {
                notifySunnah(
                    context, manager, SunnahReading.KahfFriday, app.mushaf.ayahPage(18, 1),
                    REQUEST_KAHF, KAHF_NOTIFICATION_ID,
                    R.string.kahf_reminder_notification_title, R.string.kahf_reminder_notification_text,
                )
                return
            }
            ReminderKind.VotdMorning, ReminderKind.VotdAfternoon, ReminderKind.VotdNight -> {
                notifyVotd(context, manager, kind.ordinal - ReminderKind.VotdMorning.ordinal, settings.translationIds)
                return
            }
            ReminderKind.Daily -> Unit
        }
        val kind = when (settings.reminderUnit) {
            ReminderUnit.Juz -> MarkerKind.Juz
            ReminderUnit.Hizb -> MarkerKind.Hizb
            ReminderUnit.Manzil -> MarkerKind.Manzil
        }
        // Bagian hari ini dihitung dari posisi baca terakhir (nomor urut ayat di seluruh mushaf).
        val surahs = app.mushaf.surahs()
        val counts = surahs.values.map { it.ayahCount }
        val starts = app.mushaf.markers(kind).map { DivisionMath.ordinal(counts, it.surah, it.ayah) }
        val read = DivisionMath.ordinal(counts, settings.lastSurah.takeIf { it > 0 } ?: 1, settings.lastAyah.takeIf { it > 0 } ?: 1)
        val portion = DailyPortionMath.next(starts, counts.sum(), read)
        val (fromSurah, fromAyah) = DailyPortionMath.fromOrdinal(counts, portion.start)
        val (toSurah, toAyah) = DailyPortionMath.fromOrdinal(counts, portion.end)
        val unitText = context.getString(
            when (settings.reminderUnit) {
                ReminderUnit.Juz -> R.string.juz_n
                ReminderUnit.Hizb -> R.string.hizb_n
                ReminderUnit.Manzil -> R.string.manzil_n
            },
            portion.number,
        )
        val fromName = surahs[fromSurah]?.nameLatin.orEmpty()
        val range = if (fromSurah == toSurah) {
            context.getString(R.string.reminder_text_same, fromName, fromAyah, toAyah)
        } else {
            context.getString(R.string.reminder_text, fromName, fromAyah, surahs[toSurah]?.nameLatin.orEmpty(), toAyah)
        }
        val page = app.mushaf.ayahPage(fromSurah, fromAyah)

        val open = PendingIntent.getActivity(
            context,
            REQUEST_DAILY,
            Intent(context, MainActivity::class.java).putExtra("page", page).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title, unitText))
            .setContentText(range)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(DAILY_NOTIFICATION_ID, notification)
    }

    /**
     * "Ayat hari ini" untuk potongan [slot] (0 pagi, 1 siang, 2 malam): potongan yang sama dengan kartu di beranda
     * ([Collections.picksForDay] pada hari potongan). Isinya terjemahan aktif, atau teks Arab bila belum ada. Ketukan membuka
     * halaman ayatnya.
     */
    @SuppressLint("NotificationPermission")
    private suspend fun notifyVotd(context: Context, manager: NotificationManager, slot: Int, translationIds: List<String>) {
        val app = context.app
        val day = Collections.slotDay(LocalDateTime.now())
        val range = Collections.picksForDay(app.collections.dailyPool(), day).getOrNull(slot) ?: return
        val ayahs = (range.from..range.to).map { app.mushaf.ayahDetail(range.surah, it, translationIds.take(1)) }
        val translation = ayahs
            .mapNotNull { a -> a.translations.firstOrNull()?.text?.let(::plainTranslation)?.takeIf { it.isNotBlank() } }
            .joinToString(" ")
        val body = translation.ifBlank { ayahs.joinToString(" ") { it.arabic } }
        val reference = "${app.mushaf.surahs()[range.surah]?.nameLatin.orEmpty()} $range"
        val open = PendingIntent.getActivity(
            context,
            REQUEST_VOTD + slot,
            Intent(context, MainActivity::class.java)
                .putExtra("page", app.mushaf.ayahPage(range.surah, range.from))
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            VOTD_NOTIFICATION_ID + slot,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("${context.getString(R.string.home_votd)} · ${context.getString(VOTD_SLOT_LABELS[slot])}")
                .setContentText(reference)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$reference\n$body"))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    /** Notifikasi bacaan Sunnah: ketukan membuka bacaannya dari awal di pembaca. */
    @SuppressLint("NotificationPermission")
    private fun notifySunnah(
        context: Context,
        manager: NotificationManager,
        reading: SunnahReading,
        page: Int,
        requestCode: Int,
        notificationId: Int,
        title: Int,
        text: Int,
    ) {
        val open = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_SUNNAH_READING, reading.name)
                .putExtra("page", page)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            notificationId,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(title))
                .setContentText(context.getString(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    private const val REQUEST_DAILY = 0
    private const val REQUEST_BEDTIME = 1
    private const val REQUEST_KAHF = 2
    private const val REQUEST_VOTD = 3 // 3, 4, 5: pagi, siang, malam
    private const val DAILY_NOTIFICATION_ID = 1
    private const val BEDTIME_NOTIFICATION_ID = 2
    private const val KAHF_NOTIFICATION_ID = 3
    private const val VOTD_NOTIFICATION_ID = 10 // 10, 11, 12: pagi, siang, malam
    private val VOTD_SLOT_LABELS = listOf(R.string.home_votd_morning, R.string.home_votd_afternoon, R.string.home_votd_night)

    /** Bacaan Sunnah malam hari: pukul 20.30. */
    internal const val BEDTIME_MINUTES = 20 * 60 + 30

    /** "Ayat hari ini" (pagi, siang, malam): 06.00, 13.00, dan 20.00; kartunya di beranda sudah terbuka sejak 04.00, 12.00, dan 20.00. */
    internal val VOTD_MINUTES = listOf(6 * 60, 13 * 60, 20 * 60)

    /** Al-Kahfi: setiap Kamis pukul 18.00 (pendekatan awal malam Jumat, sama dengan [SunnahTime.Friday]). */
    internal const val KAHF_MINUTES = 18 * 60
    const val EXTRA_SUNNAH_READING = "sunnah_reading"
}

/** Jenis pengingat; tiap jenis punya sakelar, alarm, dan notifikasinya sendiri. */
internal enum class ReminderKind { Daily, Bedtime, KahfThursday, VotdMorning, VotdAfternoon, VotdNight }

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_REMIND -> Reminder.notify(context)
                    ACTION_BEDTIME -> Reminder.notify(context, ReminderKind.Bedtime)
                    ACTION_KAHF -> Reminder.notify(context, ReminderKind.KahfThursday)
                    ACTION_VOTD[0] -> Reminder.notify(context, ReminderKind.VotdMorning)
                    ACTION_VOTD[1] -> Reminder.notify(context, ReminderKind.VotdAfternoon)
                    ACTION_VOTD[2] -> Reminder.notify(context, ReminderKind.VotdNight)
                }
                // Selain itu: boot, pembaruan aplikasi, atau jam/zona waktu berubah; cukup jadwalkan ulang.
                // Jadwalkan kemunculan berikutnya (hanya bila pengingat menyala; schedule() membatalkan alarm bila mati).
                Reminder.schedule(context, context.app.settings.settings.first())
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "io.zakkyhidayat.quran.REMIND"
        const val ACTION_BEDTIME = "io.zakkyhidayat.quran.REMIND_BEDTIME_SUNNAH"
        const val ACTION_KAHF = "io.zakkyhidayat.quran.REMIND_KAHF_THURSDAY"
        val ACTION_VOTD = listOf(
            "io.zakkyhidayat.quran.REMIND_VOTD_MORNING",
            "io.zakkyhidayat.quran.REMIND_VOTD_AFTERNOON",
            "io.zakkyhidayat.quran.REMIND_VOTD_NIGHT",
        )
    }
}
