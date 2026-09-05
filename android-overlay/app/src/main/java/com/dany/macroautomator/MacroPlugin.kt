package com.dany.macroautomator

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import org.json.JSONArray
import java.util.Calendar

@CapacitorPlugin(name = "MacroPlugin")
class MacroPlugin : Plugin() {

    @PluginMethod
    fun isAccessibilityEnabled(call: PluginCall) {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val serviceName = "${context.packageName}/${MacroAccessibilityService::class.java.name}"
        val enabled = enabledServices != null &&
                enabledServices.split(":").any { it.equals(serviceName, ignoreCase = true) }

        val result = JSObject()
        result.put("enabled", enabled)
        call.resolve(result)
    }

    @PluginMethod
    fun openAccessibilitySettings(call: PluginCall) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        call.resolve()
    }

    @PluginMethod
    fun startRecording(call: PluginCall) {
        MacroAccessibilityService.instance?.startRecording()
        call.resolve()
    }

    @PluginMethod
    fun stopRecording(call: PluginCall) {
        val steps = MacroAccessibilityService.instance?.stopRecording() ?: JSONArray()
        val result = JSObject()
        result.put("steps", steps)
        call.resolve(result)
    }

    @PluginMethod
    fun playSequence(call: PluginCall) {
        val stepsRaw = call.getString("steps") ?: "[]"
        val steps = JSONArray(stepsRaw)
        MacroAccessibilityService.instance?.playSequence(steps)
        call.resolve()
    }

    @PluginMethod
    fun scheduleSequence(call: PluginCall) {
        val name = call.getString("name") ?: return call.reject("Falta el nombre")
        val stepsRaw = call.getString("steps") ?: "[]"
        val hour = call.getInt("hour") ?: 0
        val minute = call.getInt("minute") ?: 0
        val daysCsv = call.getString("days") ?: ""

        // Guarda los pasos para que el receiver los pueda leer al dispararse la alarma.
        val prefs = context.getSharedPreferences("macro_schedules", Context.MODE_PRIVATE)
        prefs.edit().putString("seq_$name", stepsRaw).apply()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, MacroAlarmReceiver::class.java)
        intent.putExtra("sequenceName", name)

        val requestCode = name.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (TextUtils.isEmpty(daysCsv)) {
            // Una sola vez
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
        } else {
            // Repetir diario a esa hora; el filtro de día exacto se
            // resuelve dentro del receiver comparando con "days".
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            prefs.edit().putString("days_$name", daysCsv).apply()
        }

        call.resolve()
    }
}
