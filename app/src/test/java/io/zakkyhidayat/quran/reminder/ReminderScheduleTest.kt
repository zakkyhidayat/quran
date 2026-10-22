package io.zakkyhidayat.quran.reminder

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderScheduleTest {
    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, second)
        }.timeInMillis

    // 14 Oktober 2026 hari Rabu, 15 Oktober Kamis, 22 Oktober Kamis berikutnya.
    @Test
    fun `harian hari ini bila jamnya belum lewat dan besok bila sudah`() {
        val minutes = Reminder.BEDTIME_MINUTES
        assertEquals(at(2026, Calendar.OCTOBER, 14, 20, 30), Reminder.nextTriggerMillis(at(2026, Calendar.OCTOBER, 14, 12, 0), minutes))
        assertEquals(at(2026, Calendar.OCTOBER, 15, 20, 30), Reminder.nextTriggerMillis(at(2026, Calendar.OCTOBER, 14, 21, 0), minutes))
    }

    @Test
    fun `tepat pada jam pemicu dianggap sudah lewat`() {
        val now = at(2026, Calendar.OCTOBER, 14, 20, 30)
        assertEquals(at(2026, Calendar.OCTOBER, 15, 20, 30), Reminder.nextTriggerMillis(now, Reminder.BEDTIME_MINUTES))
    }

    @Test
    fun `ayat hari ini tiga kali sehari pukul 07-00, 13-00, dan 20-00`() {
        assertEquals(listOf(6 * 60, 13 * 60, 20 * 60), Reminder.VOTD_MINUTES)
        assertEquals(io.zakkyhidayat.quran.data.Collections.DAILY_SLOTS, Reminder.VOTD_MINUTES.size)
        // Notifikasi muncul setelah kartunya terbuka di beranda (04.00, 12.00, 20.00).
        val unlockHours = listOf(4, 12, 20)
        Reminder.VOTD_MINUTES.forEachIndexed { slot, minutes -> assertEquals(true, minutes >= unlockHours[slot] * 60) }
    }

    @Test
    fun `pengingat malam pukul 20-30 dan Al-Kahfi pukul 18-00`() {
        assertEquals(20 * 60 + 30, Reminder.BEDTIME_MINUTES)
        assertEquals(18 * 60, Reminder.KAHF_MINUTES)
    }

    @Test
    fun `mingguan Kamis 18-00 dari Rabu, Kamis sebelum jam, Kamis setelah jam, dan akhir pekan`() {
        val kahf = Reminder.KAHF_MINUTES
        val thursday = at(2026, Calendar.OCTOBER, 15, 18, 0)
        val nextThursday = at(2026, Calendar.OCTOBER, 22, 18, 0)
        assertEquals(thursday, Reminder.nextTriggerMillis(at(2026, Calendar.OCTOBER, 14, 9, 0), kahf, Calendar.THURSDAY))
        assertEquals(thursday, Reminder.nextTriggerMillis(at(2026, Calendar.OCTOBER, 15, 17, 59), kahf, Calendar.THURSDAY))
        assertEquals(nextThursday, Reminder.nextTriggerMillis(at(2026, Calendar.OCTOBER, 15, 18, 0), kahf, Calendar.THURSDAY))
        assertEquals(nextThursday, Reminder.nextTriggerMillis(at(2026, Calendar.OCTOBER, 15, 18, 30), kahf, Calendar.THURSDAY))
        // Jumat, Sabtu, dan Minggu: Kamis minggu ini sudah lewat, jadi Kamis berikutnya (tidak bergantung hari awal pekan).
        for (day in 16..18) {
            assertEquals(nextThursday, Reminder.nextTriggerMillis(at(2026, Calendar.OCTOBER, day, 10, 0), kahf, Calendar.THURSDAY))
        }
        // Senin dan Selasa: Kamis terdekat di depan.
        for (day in 19..20) {
            assertEquals(nextThursday, Reminder.nextTriggerMillis(at(2026, Calendar.OCTOBER, day, 10, 0), kahf, Calendar.THURSDAY))
        }
    }
}
