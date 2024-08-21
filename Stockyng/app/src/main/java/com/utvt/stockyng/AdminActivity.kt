package com.utvt.stockyng

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

/**
 * AdminActivity es la actividad principal para los administradores de la aplicación Stockyng.
 * Proporciona opciones para gestionar usuarios y productos, así como la posibilidad de cerrar sesión.
 */
class AdminActivity : AppCompatActivity() {

    /**
     * Método onCreate se llama cuando la actividad es creada.
     * @param savedInstanceState Contiene el estado previamente guardado de la actividad, si existe.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin)

        // Verifica si los datos del usuario están guardados en SharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)
        val userName = sharedPreferences.getString("userName", null)
        val userEmail = sharedPreferences.getString("userEmail", null)
        val userUsername = sharedPreferences.getString("userUsername", null)

        if (userId.isNullOrEmpty() || userName.isNullOrEmpty() || userEmail.isNullOrEmpty() || userUsername.isNullOrEmpty()) {
            // Los datos no están guardados, redirige al usuario a la pantalla de inicio de sesión
            Toast.makeText(this, "Error: Datos de usuario no encontrados. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Finaliza la actividad actual para que no quede en la pila de actividades
            return
        } else {
            // Los datos están presentes, continúa con la inicialización de la actividad
            Log.d("AdminActivity", "Datos de usuario cargados correctamente: userId=$userId, userName=$userName, userEmail=$userEmail, userUsername=$userUsername")
        }

        // Inicializa y configura el botón de logout
        val logoutButton: ImageButton = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            Log.d("AdminActivity", "Logout button clicked")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Finaliza la actividad actual para que no quede en la pila de actividades
        }

        // Inicializa y configura el botón para ir a UsersAdminActivity
        val usersButton: Button = findViewById(R.id.usersButton)
        usersButton.setOnClickListener {
            val intent = Intent(this, UsersAdminActivity::class.java)
            startActivity(intent)
        }

        // Inicializa y configura el botón para ir a ProductAdminActivity
        val productsButton: Button = findViewById(R.id.productsButton)
        productsButton.setOnClickListener {
            Log.d("AdminActivity", "Products button clicked")
            val intent = Intent(this, ProductAdmin::class.java)
            startActivity(intent)
        }
    }
}
