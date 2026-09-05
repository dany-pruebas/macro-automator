package com.dany.macroautomator

import android.os.Bundle
import com.getcapacitor.BridgeActivity

class MainActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // El plugin debe registrarse ANTES de super.onCreate(),
        // que es cuando Capacitor arma el bridge con la WebView.
        // Registrarlo en un bloque init (como estaba antes) se
        // ejecuta demasiado pronto y provoca el cierre inesperado.
        registerPlugin(MacroPlugin::class.java)
        super.onCreate(savedInstanceState)
    }
}
