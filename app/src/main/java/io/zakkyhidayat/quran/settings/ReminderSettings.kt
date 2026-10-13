package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.ui.AppIcons
import kotlinx.coroutines.launch

/** Pengingat membaca harian: sakelar, jam, dan satuan bagian. Izin dikelola dari Settings > Notifications. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderControls(vm: AppViewModel, settings: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = vm.settingsRepository
    var showTime by rememberSaveable { mutableStateOf(false) }
    val timeText = remember(settings.reminderMinutes) { reminderTimeText(context, settings.reminderMinutes) }
    Group {
        item(
            title = stringResource(R.string.reminder_daily),
            subtitle = if (settings.reminderEnabled) stringResource(R.string.reminder_on_sub, timeText) else stringResource(R.string.reminder_off_sub),
            onClick = { scope.launch { repo.setReminder(!settings.reminderEnabled) } },
            leading = { Icon(AppIcons.Alarm, contentDescription = null) },
            trailing = { IconSwitch(settings.reminderEnabled) },
        )
        item(
            title = stringResource(R.string.reminder_bedtime_sunnah),
            subtitle = stringResource(R.string.reminder_bedtime_sunnah_sub),
            onClick = { scope.launch { repo.setBedtimeSunnahReminderEnabled(!settings.bedtimeSunnahReminderEnabled) } },
            leading = { Icon(AppIcons.DarkMode, contentDescription = null) },
            trailing = { IconSwitch(settings.bedtimeSunnahReminderEnabled) },
        )
        // Build debug saja: tampilkan pengingat sekarang untuk menguji isi dan ketukannya.
        if (settings.reminderEnabled && io.zakkyhidayat.quran.BuildConfig.DEBUG) {
            item(
                title = stringResource(R.string.reminder_send_test),
                onClick = { scope.launch { io.zakkyhidayat.quran.reminder.Reminder.notify(context) } },
            )
        }
        if (settings.reminderEnabled) {
            item(
                title = stringResource(R.string.reminder_time),
                subtitle = timeText,
                onClick = { showTime = true },
                leading = { Icon(AppIcons.Schedule, contentDescription = null) },
            )
        }
    }
    if (settings.reminderEnabled) {
        Labeled(stringResource(R.string.reminder_unit)) {
            val options = listOf(
                ReminderUnit.Juz to stringResource(R.string.tab_juz),
                ReminderUnit.Hizb to stringResource(R.string.tab_hizb),
                ReminderUnit.Manzil to stringResource(R.string.tab_manzil),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, (unit, label) ->
                    SegmentedButton(
                        selected = settings.reminderUnit == unit,
                        onClick = { scope.launch { repo.setReminderUnit(unit) } },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(label) }
                }
            }
        }
    }
    if (showTime) {
        val state = androidx.compose.material3.rememberTimePickerState(
            initialHour = settings.reminderMinutes / 60,
            initialMinute = settings.reminderMinutes % 60,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text(stringResource(R.string.reminder_time)) },
            text = { androidx.compose.material3.TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.setReminderMinutes(state.hour * 60 + state.minute) }
                    showTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Jam pengingat ([minutes] sejak tengah malam) dalam format jam perangkat. */
internal fun reminderTimeText(context: android.content.Context, minutes: Int): String {
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, minutes / 60)
        set(java.util.Calendar.MINUTE, minutes % 60)
    }
    return android.text.format.DateFormat.getTimeFormat(context).format(cal.time)
}
