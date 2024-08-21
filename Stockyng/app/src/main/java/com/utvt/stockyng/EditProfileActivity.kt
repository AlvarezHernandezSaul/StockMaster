package com.utvt.stockyng

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

class   EditProfileActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var profileImageView: ShapeableImageView
    private lateinit var userNameEditText: EditText
    private lateinit var userEmailEditText: EditText
    private lateinit var userUsernameEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmNewPasswordEditText: EditText
    private lateinit var updateUserButton: Button

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private var currentUserID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Inicializar vistas
        backButton = findViewById(R.id.back_button)
        profileImageView = findViewById(R.id.profile_image)
        userNameEditText = findViewById(R.id.user_name)
        userEmailEditText = findViewById(R.id.user_email)
        userUsernameEditText = findViewById(R.id.user_username)
        newPasswordEditText = findViewById(R.id.new_password)
        confirmNewPasswordEditText = findViewById(R.id.confirm_new_password)
        updateUserButton = findViewById(R.id.update_user_button)

        // Inicializa SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        currentUserID = sharedPreferences.getString("userId", "").orEmpty()

        // Inicializa Firebase
        databaseReference = FirebaseDatabase.getInstance("https://proyect-a4a4e-default-rtdb.firebaseio.com").getReference("users")
        storageReference = FirebaseStorage.getInstance("gs://proyect-a4a4e.appspot.com").reference.child("profile_images")

        // Cargar información del usuario actual
        loadUserInfo()

        // Listeners
        backButton.setOnClickListener { onBackPressed() }
        profileImageView.setOnClickListener { showImagePickerDialog() }
        updateUserButton.setOnClickListener { updateUserProfile() }
    }

    private fun loadUserInfo() {
        val userName = sharedPreferences.getString("userName", "").orEmpty()
        val userEmail = sharedPreferences.getString("userEmail", "").orEmpty()
        val userUsername = sharedPreferences.getString("userUsername", "").orEmpty()
        val userImageUrl = sharedPreferences.getString("userImageUrl", null)

        Log.d("EditProfileActivity", "Cargando datos de usuario: $userName, $userEmail, $userUsername")

        userNameEditText.setText(userName)
        userEmailEditText.setText(userEmail)
        userUsernameEditText.setText(userUsername)

        if (userImageUrl != null && userImageUrl != "N/A") {
            // Cargar imagen desde la URL almacenada
            Picasso.get().load(userImageUrl).into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.baseline_account_circle_24)
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Elegir de la galería")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecciona una imagen de perfil")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> takePhotoFromCamera()
                1 -> pickPhotoFromGallery()
            }
        }
        builder.show()
    }

    private fun takePhotoFromCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
    }

    private fun pickPhotoFromGallery() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    profileImageView.setImageBitmap(imageBitmap)
                    saveProfileImage(imageBitmap)
                }
                REQUEST_IMAGE_PICK -> {
                    val selectedImageUri: Uri? = data?.data
                    profileImageView.setImageURI(selectedImageUri)
                    selectedImageUri?.let { saveProfileImage(it) }
                }
            }
        }
    }

    private fun saveProfileImage(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        // Subir imagen a Firebase Storage
        val imageRef = storageReference.child("$currentUserID.jpg")
        val uploadTask = imageRef.putBytes(data)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val profileImageUrl = uri.toString()
                sharedPreferences.edit().putString("userImageUrl", profileImageUrl).apply()
                updateUserProfile() // Actualizar datos de perfil después de subir la imagen
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileImage(uri: Uri) {
        val imageRef = storageReference.child("$currentUserID.jpg")
        val uploadTask = imageRef.putFile(uri)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val profileImageUrl = downloadUri.toString()
                sharedPreferences.edit().putString("userImageUrl", profileImageUrl).apply()
                updateUserProfile() // Actualizar datos de perfil después de subir la imagen
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserProfile() {
        val name = userNameEditText.text.toString().trim()
        val email = userEmailEditText.text.toString().trim()
        val username = userUsernameEditText.text.toString().trim()
        val newPassword = newPasswordEditText.text.toString().trim()
        val confirmNewPassword = confirmNewPasswordEditText.text.toString().trim()

        if (newPassword != confirmNewPassword) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        // Actualizar los datos del usuario en SharedPreferences
        sharedPreferences.edit()
            .putString("userName", name)
            .putString("userEmail", email)
            .putString("userUsername", username)
            .apply()

        // Crear mapa para actualizar los datos del usuario en Firebase
        val userUpdates = mutableMapOf<String, Any>(
            "name" to name,
            "email" to email,
            "username" to username,
            "imageUrl" to (sharedPreferences.getString("userImageUrl", "") ?: "")
        )

        if (newPassword.isNotEmpty()) {
            val hashedPassword = hashPassword(newPassword)
            userUpdates["password"] = hashedPassword
        }

        // Actualizar los datos del usuario en Firebase Realtime Database
        databaseReference.child(currentUserID).updateChildren(userUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(password.toByteArray())
        val bytes = md.digest()
        val sb = StringBuilder()
        for (byte in bytes) {
            sb.append(String.format("%02x", byte))
        }
        return sb.toString()
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
    }
}
