package com.utvt.stockyng

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.security.MessageDigest

/**
 * Actividad para el registro de nuevos usuarios.
 */
class SignUpActivity : AppCompatActivity() {

    // Referencia a la base de datos de Firebase
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Inicializar la referencia a la base de datos
        database = FirebaseDatabase.getInstance("https://proyect-a4a4e-default-rtdb.firebaseio.com/").reference

        // Inicializar los campos de entrada
        val nameField: EditText = findViewById(R.id.signup_name)
        val emailField: EditText = findViewById(R.id.signup_email)
        val usernameField: EditText = findViewById(R.id.signup_username)
        val passwordField: EditText = findViewById(R.id.signup_password)
        val confirmPasswordField: EditText = findViewById(R.id.signup_confirm_password)
        val signUpButton: Button = findViewById(R.id.signup_button)
        val loginRedirect: TextView = findViewById(R.id.loginRedirectText)

        // Configurar la acción del botón de registro
        signUpButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            // Validar los campos de entrada
            if (name.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Correo electrónico inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar si el nombre de usuario ya existe en la base de datos
            database.child("users").orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Nombre de usuario ya existe
                            Toast.makeText(this@SignUpActivity, "El nombre de usuario ya está en uso", Toast.LENGTH_SHORT).show()
                        } else {
                            // Nombre de usuario disponible, proceder con el registro
                            registerUser(name, email, username, password)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(this@SignUpActivity, "Error en la base de datos: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // Configurar la redirección al inicio de sesión
        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    /**
     * Registra un nuevo usuario en Firebase.
     */
    private fun registerUser(name: String, email: String, username: String, password: String) {
        // Generar un ID único para el usuario
        val userId = database.child("users").push().key ?: return

        // Crear una instancia de User con el rol predeterminado 'user' y un imageUrl nulo
        val hashedPassword = hashPassword(password)
        val user = User(
            id = userId,
            name = name,
            email = email,
            username = username,
            password = hashedPassword, // Contraseña hasheada
            role = "user", // Rol predeterminado
            imageUrl = null // URL de la imagen puede ser nulo al inicio
        )

        // Guardar el usuario en la base de datos
        database.child("users").child(userId).setValue(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Guardar el ID del usuario en SharedPreferences
                    saveUserId(userId, user)

                    // Mostrar un mensaje de éxito
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()

                    // Redirigir a la actividad principal
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("username", username)
                    startActivity(intent)
                    finish() // Finalizar la actividad actual
                } else {
                    // Mostrar un mensaje de error
                    Toast.makeText(this, "Error al registrar el usuario", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Guarda el ID del usuario y otros datos en SharedPreferences.
     * @param userId ID del usuario a guardar.
     * @param user Datos del usuario a guardar.
     */
    private fun saveUserId(userId: String, user: User) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("userId", userId)
        editor.putString("userName", user.name)
        editor.putString("userEmail", user.email)
        editor.putString("userUsername", user.username)
        editor.putString("userRole", user.role)
        editor.apply()

        // Verifica si los datos se guardaron correctamente
        val storedUserId = sharedPreferences.getString("userId", null)
        if (storedUserId == userId) {
            Log.d("SignUpActivity", "User data saved in SharedPreferences: $storedUserId")
        } else {
            Log.e("SignUpActivity", "Failed to save user data in SharedPreferences")
        }
    }

    /**
     * Genera un hash de la contraseña usando SHA-256.
     * @param password Contraseña a hashear.
     * @return Hash de la contraseña.
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
