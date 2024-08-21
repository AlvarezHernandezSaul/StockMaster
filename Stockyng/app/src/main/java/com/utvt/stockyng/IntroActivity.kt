package com.utvt.stockyng

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * IntroActivity es la actividad de introducción que se muestra al iniciar la aplicación.
 * Muestra una pantalla de presentación durante 3 segundos antes de redirigir a la actividad de inicio de sesión.
 */
class IntroActivity : AppCompatActivity() {

    /**
     * Método onCreate se llama cuando la actividad es creada.
     * @param savedInstanceState Contiene el estado previamente guardado de la actividad, si existe.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        // Handler para retrasar la ejecución de una acción en el hilo principal
        Handler(Looper.getMainLooper()).postDelayed({
            // Redirigir a la actividad de inicio de sesión
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Finaliza la actividad de introducción
        }, 3000) // 3000 milisegundos = 3 segundos
    }
}
