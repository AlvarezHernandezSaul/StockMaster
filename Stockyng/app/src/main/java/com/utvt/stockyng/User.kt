package com.utvt.stockyng

/**
 * Representa un usuario en la aplicación.
 *
 * @property id Identificador único del usuario. Este campo es autogenerado en Firebase.
 * @property name Nombre del usuario.
 * @property email Correo electrónico del usuario.
 * @property username Nombre de usuario.
 * @property password Contraseña del usuario en formato hasheado.
 * @property role Rol del usuario en la aplicación. Por defecto es "user", pero puede ser "admin".
 */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val username: String = "",
    val role: String = "user",
    val password: String = "",
    val imageUrl: String? = null
)
