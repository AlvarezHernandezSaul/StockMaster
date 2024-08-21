package com.utvt.stockyng

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.security.MessageDigest

/**
 * Actividad para la administración de usuarios.
 * Permite a los administradores agregar, editar, eliminar y buscar usuarios.
 */
class UsersAdminActivity : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var userAdapter: UserAdapter
    private lateinit var userList: MutableList<User>
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchUserEditText: EditText
    private lateinit var userCountTextView: TextView
    private lateinit var addUserButton: ImageButton
    private lateinit var logoutButton: ImageButton
    private lateinit var usernameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users_admin)

        // Inicializar componentes de la interfaz
        recyclerView = findViewById(R.id.user_recycler_view)
        searchUserEditText = findViewById(R.id.search_user_edit_text)
        userCountTextView = findViewById(R.id.user_count_text_view)
        addUserButton = findViewById(R.id.add_user_button)
        logoutButton = findViewById(R.id.logout_button)
        usernameTextView = findViewById(R.id.username_text_view)
        // Configurar RecyclerView
        userList = mutableListOf()
        userAdapter = UserAdapter(userList, ::onEditUser, ::onDeleteUser)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = userAdapter

        // Configurar referencia a Firebase
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")

        // Cargar usuarios iniciales
        loadUsers()

        // Agregar listener para la búsqueda de usuarios
        searchUserEditText.addTextChangedListener { searchUsers() }

        // Configurar botón de agregar usuario
        addUserButton.setOnClickListener { showAddUserDialog() }

        // Configurar botón de logout
        logoutButton.setOnClickListener {
            logout()
        }
        // Obtener el nombre de usuario desde SharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userUsername = sharedPreferences.getString("userUsername", null)

        // Verificar si el nombre de usuario existe
        if (userUsername != null) {
            // Actualizar el mensaje de bienvenida con el nombre de usuario
            usernameTextView.text = "Bienvenido $userUsername"
        } else {
            // Manejar el caso donde no hay nombre de usuario almacenado
            usernameTextView.text = "Bienvenido Usuario"
        }
        // Configurar botón de regreso
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            val intent = Intent(this, AdminActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    /**
     * Muestra un diálogo para agregar un nuevo usuario.
     */
    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.user_name)
        val emailEditText = dialogView.findViewById<EditText>(R.id.user_email)
        val usernameEditText = dialogView.findViewById<EditText>(R.id.user_username)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.user_password)
        val roleEditText = dialogView.findViewById<EditText>(R.id.user_role)
        val addUserButton = dialogView.findViewById<Button>(R.id.add_user_dialog_button)

        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setTitle("Agregar Usuario")

        dialog.window?.setBackgroundDrawableResource(R.drawable.lavender_border)

        val layoutParams = dialog.window?.attributes
        layoutParams?.width = (resources.displayMetrics.widthPixels * 0.8).toInt()
        dialog.window?.attributes = layoutParams

        addUserButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val role = roleEditText.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty() && role.isNotEmpty()) {
                // Validación adicional y agregar usuario
                val userId = databaseReference.push().key ?: run {
                    Log.e("UsersAdminActivity", "Error al obtener el ID del usuario")
                    return@setOnClickListener
                }
                val user = User(userId, name, email, username, hashPassword(password), role)
                databaseReference.child(userId).setValue(user)
                    .addOnSuccessListener {
                        Log.d("UsersAdminActivity", "Usuario agregado exitosamente")
                    }
                    .addOnFailureListener { e ->
                        Log.e("UsersAdminActivity", "Error al agregar el usuario", e)
                    }
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Muestra un diálogo para editar un usuario existente.
     *
     * @param user El usuario que se va a editar.
     */
    private fun showEditUserDialog(user: User) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_user, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.user_name)
        val emailEditText = dialogView.findViewById<EditText>(R.id.user_email)
        val usernameEditText = dialogView.findViewById<EditText>(R.id.user_username)
        val roleEditText = dialogView.findViewById<EditText>(R.id.user_role)
        val updateButton = dialogView.findViewById<Button>(R.id.update_user_dialog_button)

        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setTitle("Editar Usuario")

        dialog.window?.setBackgroundDrawableResource(R.drawable.lavender_border)

        val layoutParams = dialog.window?.attributes
        layoutParams?.width = (resources.displayMetrics.widthPixels * 0.8).toInt()
        dialog.window?.attributes = layoutParams
        // Pre-cargar datos del usuario en los campos del diálogo
        nameEditText.setText(user.name)
        emailEditText.setText(user.email)
        usernameEditText.setText(user.username)
        roleEditText.setText(user.role)

        updateButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val role = roleEditText.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && username.isNotEmpty()  && role.isNotEmpty()) {
                val updatedUser = user.copy(name = name, email = email, username = username, role = role)
                databaseReference.child(user.id).setValue(updatedUser)
                    .addOnSuccessListener {
                        Log.d("UsersAdminActivity", "Usuario actualizado exitosamente")
                    }
                    .addOnFailureListener { e ->
                        Log.e("UsersAdminActivity", "Error al actualizar el usuario", e)
                    }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    /**
     * Muestra un mensaje de confirmación para eliminar un usuario.
     *
     * @param user El usuario que se va a eliminar.
     */
    private fun onDeleteUser(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Usuario")
            .setMessage("¿Está seguro de que desea eliminar a ${user.name}?")
            .setPositiveButton("Sí") { _, _ ->
                databaseReference.child(user.id).removeValue()
                    .addOnSuccessListener {
                        Log.d("UsersAdminActivity", "Usuario eliminado exitosamente")
                    }
                    .addOnFailureListener { e ->
                        Log.e("UsersAdminActivity", "Error al eliminar el usuario", e)
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    /**
     * Muestra un diálogo de edición para un usuario.
     *
     * @param user El usuario que se va a editar.
     */
    private fun onEditUser(user: User) {
        showEditUserDialog(user)
    }

    /**
     * Carga los usuarios desde Firebase y actualiza la lista en el RecyclerView.
     */
    private fun loadUsers() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userList.clear()
                for (snapshot in dataSnapshot.children) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let { userList.add(it) }
                }
                userAdapter.notifyDataSetChanged()
                userCountTextView.text = "Número de usuarios: ${userList.size}"
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("UsersAdminActivity", "Error al cargar usuarios", databaseError.toException())
                Toast.makeText(this@UsersAdminActivity, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Busca usuarios que coincidan con el término de búsqueda ingresado.
     */
    private fun searchUsers() {
        val searchTerm = searchUserEditText.text.toString().trim().lowercase()
        val filteredList = userList.filter { user ->
            user.name.lowercase().contains(searchTerm) ||
                    user.email.lowercase().contains(searchTerm) ||
                    user.username.lowercase().contains(searchTerm)
        }
        userAdapter.updateList(filteredList)
        userCountTextView.text = "Número de usuarios: ${filteredList.size}"
    }

    /**
     * Cierra la sesión del usuario actual y regresa a la pantalla de login.
     */
    private fun logout() {
        // Implementar lógica de cierre de sesión si es necesario
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }



    /**
     * Hashea la contraseña del usuario usando SHA-256.
     *
     * @param password La contraseña en texto claro.
     * @return La contraseña hasheada.
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { String.format("%02x", it) }
    }
}
