package com.utvt.stockyng

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

/**
 * Adaptador para mostrar productos en un RecyclerView.
 *
 * @property productList Lista mutable de productos a mostrar.
 * @property onEditClick Función que se llama cuando se hace clic en el botón de editar de un producto.
 * @property onDeleteClick Función que se llama cuando se hace clic en el botón de eliminar de un producto.
 */
class ProductAdapter(
    private var productList: MutableList<Product>,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    /**
     * ViewHolder que mantiene referencias a las vistas de un elemento de producto.
     *
     * @property productImage Imagen del producto.
     * @property productName Nombre del producto.
     * @property productInventory Inventario del producto.
     * @property productPrice Precio de venta del producto.
     * @property editButton Botón para editar el producto.
     * @property deleteButton Botón para eliminar el producto.
     */
    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.product_image)
        val productName: TextView = view.findViewById(R.id.product_name)
        val productInventory: TextView = view.findViewById(R.id.product_inventory)
        val productPrice: TextView = view.findViewById(R.id.product_price)
        val editButton: ImageButton = view.findViewById(R.id.edit_button)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)

        init {
            // Configura los listeners para los botones de editar y eliminar
            editButton.setOnClickListener {
                onEditClick(productList[adapterPosition])
            }
            deleteButton.setOnClickListener {
                onDeleteClick(productList[adapterPosition])
            }

            // Configura el listener para la imagen del producto
            productImage.setOnClickListener {
                val context = itemView.context
                val product = productList[adapterPosition]
                val dialogFragment = ImageDialogFragment.newInstance(product.imageUrl ?: "")
                val activity = context as AppCompatActivity
                dialogFragment.show(activity.supportFragmentManager, "image_dialog")
            }
        }
    }

    /**
     * Crea un nuevo ViewHolder para un elemento de producto.
     *
     * @param parent El grupo de vistas donde se debe agregar el nuevo ViewHolder.
     * @param viewType Tipo de vista (no usado aquí).
     * @return Un nuevo ProductViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    /**
     * Asocia un producto con un ViewHolder.
     *
     * @param holder El ViewHolder al que se asociará el producto.
     * @param position La posición del producto en la lista.
     */
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.productName.text = product.name
        holder.productInventory.text = "Inventario: ${product.inventory}"
        holder.productPrice.text = "Precio: ${product.salePrice}"

        // Carga la imagen del producto usando Picasso
        if (product.imageUrl?.isNotEmpty() == true) {
            Picasso.get().load(product.imageUrl).into(holder.productImage)
        } else {
            holder.productImage.setImageResource(R.drawable.baseline_add_photo_alternate_24) // Imagen por defecto si la URL está vacía
        }
    }

    /**
     * Devuelve el número total de productos en la lista.
     *
     * @return Tamaño de la lista de productos.
     */
    override fun getItemCount(): Int {
        return productList.size
    }

    /**
     * Actualiza la lista de productos y notifica al adaptador sobre los cambios.
     *
     * @param filteredProducts Lista de productos actualizada.
     */
    fun updateList(filteredProducts: List<Product>) {
        productList.clear()
        productList.addAll(filteredProducts)
        notifyDataSetChanged()
    }
}
