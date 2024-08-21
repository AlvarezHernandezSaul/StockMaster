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

/**
 * LoginActivity maneja el inicio de sesión del usuario.
 * Verifica las credenciales del usuario contra la base de datos de Firebase
 * y redirige al usuario a la actividad correspondiente basada en su rol.
 */
class LoginActivity : AppCompatActivity() {

    // Referencia a la base de datos de Firebase
    private lateinit var database: DatabaseReference

    /**
     * Método onCreate se llama cuando la actividad es creada.
     * @param savedInstanceState Contiene el estado previamente guardado de la actividad, si existe.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializa la referencia a la base de datos
        database = FirebaseDatabase.getInstance("https://proyect-a4a4e-default-rtdb.firebaseio.com/").reference

        // Obtiene las vistas de la actividad
        val emailField: EditText = findViewById(R.id.login_email)
        val passwordField: EditText = findViewById(R.id.login_password)
        val loginButton: Button = findViewById(R.id.login_button)
        val signUpRedirect: TextView = findViewById(R.id.signupRedirectText)
        val emailError: TextView = findViewById(R.id.login_email_error)
        val passwordError: TextView = findViewById(R.id.login_password_error)

        // Configura el botón de inicio de sesión
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            var valid = true

            // Validación de campo de email
            if (email.isEmpty()) {
                emailError.text = "El email no puede estar vacío"
                emailError.visibility = TextView.VISIBLE
                valid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError.text = "Correo electrónico inválido"
                emailError.visibility = TextView.VISIBLE
                valid = false
            } else {
                emailError.visibility = TextView.GONE
            }

            // Validación de campo de contraseña
            if (password.isEmpty()) {
                passwordError.text = "La contraseña no puede estar vacía"
                passwordError.visibility = TextView.VISIBLE
                valid = false
            } else if (password.length < 8) {
                passwordError.text = "La contraseña debe tener al menos 8 caracteres"
                passwordError.visibility = TextView.VISIBLE
                valid = false
            } else {
                passwordError.visibility = TextView.GONE
            }

            // Si la validación es exitosa, procede con la autenticación en Firebase
            if (valid) {
                // Consulta la base de datos para verificar las credenciales
                database.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var userFound = false

                        // Recorre los usuarios en la base de datos
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)

                            // Verifica las credenciales del usuario
                            if (user != null && user.email == email && user.password == hashPassword(password)) {
                                userFound = true
                                Log.d("LoginActivity", "User found: ${user.email}, Role: ${user.role}")

                                // Guarda el ID del usuario en SharedPreferences
                                saveUserId(userSnapshot.key ?: "", user)

                                // Guarda datos en UserSession
                                UserSession.userId = user.id
                                UserSession.userRole = user.role

                                // Redirige al usuario según su rol
                                val intent = when (user.role) {
                                    "Admin" -> Intent(this@LoginActivity, AdminActivity::class.java)
                                    else -> {
                                        // Redirige a MainActivity y pasa el nombre de usuario
                                        val mainActivityIntent = Intent(this@LoginActivity, MainActivity::class.java)
                                        mainActivityIntent.putExtra("username", user.username)
                                        mainActivityIntent
                                    }
                                }
                                startActivity(intent)
                                finish() // Finaliza la actividad actual
                                break
                            }
                        }

                        // Muestra un mensaje si el usuario no se encuentra
                        if (!userFound) {
                            Toast.makeText(this@LoginActivity, "Correo electrónico o contraseña inválidos", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Maneja errores en la consulta a la base de datos
                        Toast.makeText(this@LoginActivity, "Error en la base de datos: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }

        // Configura el enlace para la actividad de registro
        signUpRedirect.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
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
        editor.putString("userImageUrl", user.imageUrl) // Almacena imageUrl aquí
        editor.apply()

        // Verifica si los datos se guardaron correctamente
        val storedUserId = sharedPreferences.getString("userId", null)
        if (storedUserId == userId) {
            Log.d("LoginActivity", "User data saved in SharedPreferences: $storedUserId")
        } else {
            Log.e("LoginActivity", "Failed to save user data in SharedPreferences")
        }

        // Llama al método para registrar los datos en el log
        logSharedPreferences()
    }

    /**
     * Genera un hash de la contraseña usando SHA-256.
     * @param password Contraseña a hashear.
     * @return Hash de la contraseña.
     */
    private fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun logSharedPreferences() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "N/A")
        val userName = sharedPreferences.getString("userName", "N/A")
        val userEmail = sharedPreferences.getString("userEmail", "N/A")
        val userUsername = sharedPreferences.getString("userUsername", "N/A")
        val userRole = sharedPreferences.getString("userRole", "N/A")
        val userImageUrl = sharedPreferences.getString("userImageUrl", "N/A") // Recupera imageUrl aquí

        Log.d("LoginActivity", "SharedPreferences - userId: $userId")
        Log.d("LoginActivity", "SharedPreferences - userName: $userName")
        Log.d("LoginActivity", "SharedPreferences - userEmail: $userEmail")
        Log.d("LoginActivity", "SharedPreferences - userUsername: $userUsername")
        Log.d("LoginActivity", "SharedPreferences - userRole: $userRole")
        Log.d("LoginActivity", "SharedPreferences - userImageUrl: $userImageUrl") // Registra imageUrl aquí
    }

}
