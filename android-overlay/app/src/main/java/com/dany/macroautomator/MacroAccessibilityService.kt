package com.dany.macroautomator

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * Servicio de accesibilidad propio.
 *
 * Modo GRABAR: escucha clicks y cambios de texto reales en pantalla
 * (los que tú haces con el dedo) y los guarda con su delay relativo.
 *
 * Modo REPRODUCIR: recorre la lista de pasos guardada y la ejecuta
 * sola, usando dispatchGesture() para toques y performAction() para
 * escribir texto en el campo que esté enfocado.
 */
class MacroAccessibilityService : AccessibilityService() {

    companion object {
        // Referencia estática para que el plugin de Capacitor pueda
        // hablarle al servicio sin necesidad de bind/unbind manual.
        var instance: MacroAccessibilityService? = null
    }

    private var isRecording = false
    private var recordedSteps = mutableListOf<JSONObject>()
    private var lastEventTime = 0L
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRecording || event == null) return

        val now = System.currentTimeMillis()
        val delay = if (lastEventTime == 0L) 0 else (now - lastEventTime)

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val node = event.source ?: return
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                val x = bounds.centerX()
                val y = bounds.centerY()

                val step = JSONObject()
                step.put("type", "tap")
                step.put("x", x)
                step.put("y", y)
                step.put("delay", delay)
                recordedSteps.add(step)
                lastEventTime = now
                node.recycle()
            }

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text?.joinToString(" ") ?: return
                if (text.isBlank()) return

                val step = JSONObject()
                step.put("type", "text")
                step.put("text", text)
                step.put("delay", delay)
                recordedSteps.add(step)
                lastEventTime = now
            }
        }
    }

    fun startRecording() {
        recordedSteps = mutableListOf()
        lastEventTime = 0L
        isRecording = true
    }

    fun stopRecording(): JSONArray {
        isRecording = false
        val arr = JSONArray()
        recordedSteps.forEach { arr.put(it) }
        return arr
    }

    /**
     * Reproduce una secuencia de pasos respetando los delays grabados.
     */
    fun playSequence(steps: JSONArray) {
        playStepAt(steps, 0)
    }

    private fun playStepAt(steps: JSONArray, index: Int) {
        if (index >= steps.length()) return
        val step = steps.getJSONObject(index)
        val delay = step.optLong("delay", 300L)

        handler.postDelayed({
            when (step.optString("type")) {
                "tap" -> {
                    val x = step.optInt("x")
                    val y = step.optInt("y")
                    dispatchTap(x, y)
                }
                "text" -> {
                    val text = step.optString("text")
                    typeIntoFocusedField(text)
                }
            }
            playStepAt(steps, index + 1)
        }, delay)
    }

    private fun dispatchTap(x: Int, y: Int) {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        val stroke = GestureDescription.StrokeDescription(path, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    /**
     * En vez de simular tecla por tecla, se escribe directamente en el
     * campo de texto que tenga el foco, usando la propia API de
     * accesibilidad (ACTION_SET_TEXT). Es más confiable que simular
     * pulsaciones de teclado.
     */
    private fun typeIntoFocusedField(text: String) {
        val root = rootInActiveWindow ?: return
        val focused = findFocusedEditable(root) ?: return
        val args = Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        focused.recycle()
    }

    private fun findFocusedEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFocusedEditable(child)
            if (result != null) return result
        }
        return null
    }
}
