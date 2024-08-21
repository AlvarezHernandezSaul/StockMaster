package com.utvt.stockyng

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

/**
 * Adaptador para el RecyclerView que muestra una lista de usuarios.
 *
 * @property userList Lista mutable de usuarios que se mostrarán en el RecyclerView.
 * @property onEditClick Función que se ejecuta cuando se hace clic en el botón de editar.
 * @property onDeleteClick Función que se ejecuta cuando se hace clic en el botón de eliminar.
 */
class UserAdapter(
    private var userList: MutableList<User>,
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    /**
     * ViewHolder que contiene las vistas para cada ítem de la lista de usuarios.
     *
     * @property userName Vista de texto para mostrar el nombre del usuario.
     * @property userEmail Vista de texto para mostrar el correo electrónico del usuario.
     * @property userRole Vista de texto para mostrar el rol del usuario.
     * @property editButton Botón de imagen para editar el usuario.
     * @property deleteButton Botón de imagen para eliminar el usuario.
     */
    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userImage: ImageView = view.findViewById(R.id.user_image)
        val userName: TextView = view.findViewById(R.id.user_name)
        val userUsername: TextView = view.findViewById(R.id.user_username)
        val userEmail: TextView = view.findViewById(R.id.user_email)
        val userRole: TextView = view.findViewById(R.id.user_role)
        val editButton: ImageButton = view.findViewById(R.id.edit_button)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)

        init {
            // Configura los listeners para los botones de editar y eliminar
            editButton.setOnClickListener {
                onEditClick(userList[adapterPosition])
            }
            deleteButton.setOnClickListener {
                onDeleteClick(userList[adapterPosition])
            }

            // Configura el listener para mostrar el diálogo de imagen
            userImage.setOnClickListener {
                val user = userList[adapterPosition]
                if (user.imageUrl?.isNotEmpty() == true) {
                    ImageDialogFragment.newInstance(user.imageUrl).show(
                        (it.context as FragmentActivity).supportFragmentManager,
                        "ImageDialog"
                    )
                }
            }
        }
    }

    /**
     * Crea nuevas vistas y las envuelve en un UserViewHolder.
     *
     * @param parent Vista del grupo donde se inflará la nueva vista.
     * @param viewType Tipo de vista que se va a inflar.
     * @return Un nuevo UserViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    /**
     * Asocia los datos del usuario con las vistas del ViewHolder.
     *
     * @param holder ViewHolder que contiene las vistas para mostrar los datos del usuario.
     * @param position Posición del usuario en la lista.
     */
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.userName.text = user.name
        holder.userEmail.text = user.email
        holder.userUsername.text = user.username
        holder.userRole.text = "Rol: ${user.role}"

        // Carga la imagen del usuario usando Picasso
        if (user.imageUrl?.isNotEmpty() == true) {
            Picasso.get().load(user.imageUrl).into(holder.userImage)
        } else {
            holder.userImage.setImageResource(R.drawable.baseline_account_circle_24) // Imagen por defecto si la URL está vacía
        }
    }

    /**
     * Devuelve el número total de ítems en la lista de usuarios.
     *
     * @return El tamaño de la lista de usuarios.
     */
    override fun getItemCount(): Int {
        return userList.size
    }

    /**
     * Actualiza la lista de usuarios y notifica al RecyclerView que los datos han cambiado.
     *
     * @param filteredUsers Lista de usuarios actualizada.
     */
    fun updateList(filteredUsers: List<User>) {
        userList.clear()
        userList.addAll(filteredUsers)
        notifyDataSetChanged()
    }
}
