package io.zakkyhidayat.quran.reminder

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
import io.zakkyhidayat.quran.data.DivisionMath
import io.zakkyhidayat.quran.data.MarkerKind
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.settings.ReminderUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private const val NOTIFICATION_ID = 1

    fun schedule(context: Context, settings: AppSettings) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = alarmIntent(context)
        alarms.cancel(pending)
        if (!settings.reminderEnabled) return
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.reminderMinutes / 60)
            set(Calendar.MINUTE, settings.reminderMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        // Jendela 10 menit: cukup tepat untuk pengingat, tanpa izin SCHEDULE_EXACT_ALARM.
        alarms.setWindow(AlarmManager.RTC_WAKEUP, next.timeInMillis, 10 * 60 * 1000L, pending)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal suspend fun notify(context: Context) {
        val app = context.app
        val settings = app.settings.settings.first()
        if (!settings.reminderEnabled || !canNotify(context)) return
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

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = PendingIntent.getActivity(
            context,
            0,
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
        manager.notify(NOTIFICATION_ID, notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (intent.action == ACTION_REMIND) Reminder.notify(context)
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
    }
}
