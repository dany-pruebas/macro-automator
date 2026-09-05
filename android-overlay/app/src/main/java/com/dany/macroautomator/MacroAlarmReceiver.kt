package com.dany.macroautomator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import java.util.Calendar

class MacroAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("sequenceName") ?: return
        val prefs = context.getSharedPreferences("macro_schedules", Context.MODE_PRIVATE)

        // Si la secuencia tiene días específicos configurados, se
        // valida que hoy sea uno de ellos antes de ejecutar.
        val daysCsv = prefs.getString("days_$name", null)
        if (daysCsv != null) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toString()
            val allowedDays = daysCsv.split(",")
            if (!allowedDays.contains(today)) return
        }

        val stepsRaw = prefs.getString("seq_$name", null) ?: return
        val steps = JSONArray(stepsRaw)

        // El servicio de accesibilidad debe estar activo (el usuario ya
        // dio el permiso una vez) para poder reproducir la secuencia.
        MacroAccessibilityService.instance?.playSequence(steps)
    }
}
