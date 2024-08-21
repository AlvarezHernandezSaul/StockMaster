package com.utvt.stockyng

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var addProductButton: ImageButton
    private lateinit var searchProductEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var productRecyclerView: RecyclerView
    private lateinit var usernameTextView: TextView
    private lateinit var productCountTextView: TextView
    private lateinit var logoutButton: ImageButton
    private lateinit var perfilButton: ImageButton

    private var imageUri: Uri? = null
    private lateinit var selectedImageButton: ImageView

    // Referencias a Firebase database y storage
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference

    private var productList: MutableList<Product> = mutableListOf()
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa los elementos de la interfaz de usuario
        addProductButton = findViewById(R.id.add_product_button)
        searchProductEditText = findViewById(R.id.search_product_edit_text)
        searchButton = findViewById(R.id.search_button)
        productRecyclerView = findViewById(R.id.product_recycler_view)
        usernameTextView = findViewById(R.id.username_text_view) // Asegúrate de que el ID sea correcto
        productCountTextView = findViewById(R.id.product_count_text_view)
        logoutButton = findViewById(R.id.logout_button)
        perfilButton = findViewById(R.id.perfil_button)

        // Obtiene el nombre de usuario del Intent
        val username = intent.getStringExtra("username")

        // Muestra el nombre de usuario en la interfaz
        usernameTextView.text = "Bienvenido, $username"


        // Configura el RecyclerView para mostrar la lista de productos
        productRecyclerView.layoutManager = LinearLayoutManager(this)
        productAdapter = ProductAdapter(productList,
            onEditClick = { product -> openEditProductDialog(product) },
            onDeleteClick = { product -> deleteProduct(product) }
        )
        productRecyclerView.adapter = productAdapter

        // Inicializa las referencias a la base de datos y almacenamiento en Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("Products")
        storageReference = FirebaseStorage.getInstance().getReference("Products")

        // Configura el botón de agregar producto
        addProductButton.setOnClickListener { openAddProductDialog() }
        //botn userperfil
        val backButton: ImageButton = findViewById(R.id.perfil_button)
        perfilButton.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
        // Configura el botón de cierre de sesión
        logoutButton.setOnClickListener {
            logoutUser()
        }

        // Configura el TextWatcher para la búsqueda automática
        searchProductEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchProducts()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Carga los productos y configura el nombre de usuario y el conteo de productos
        loadProducts()
        setUserNameAndProductCount()
    }
    /**
     * Maneja el cierre de sesión del usuario.
     */
    private fun logoutUser() {
        // Limpia los datos de la sesión
        UserSession.userId = null
        UserSession.userRole = null

        // Limpia SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        // Redirige al usuario a la pantalla de login
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Abre el diálogo para agregar un nuevo producto.
     */
    private fun openAddProductDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_product)
        dialog.setTitle("Agregar Producto")

        dialog.window?.setBackgroundDrawableResource(R.drawable.lavender_border)

        val layoutParams = dialog.window?.attributes
        layoutParams?.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        dialog.window?.attributes = layoutParams

        selectedImageButton = dialog.findViewById(R.id.image_button)
        val productNameEditText = dialog.findViewById<EditText>(R.id.product_name)
        val productInventoryEditText = dialog.findViewById<EditText>(R.id.product_inventory)
        val productPurchasePriceEditText = dialog.findViewById<EditText>(R.id.product_purchase_price)
        val productSalePriceEditText = dialog.findViewById<EditText>(R.id.product_sale_price)
        val dialogButton = dialog.findViewById<Button>(R.id.add_product_dialog_button)

        selectedImageButton.setOnClickListener { showImagePickerDialog(null) }

        dialogButton.setOnClickListener {
            val name = productNameEditText.text.toString().trim()
            val inventory = productInventoryEditText.text.toString().trim()
            val purchasePrice = productPurchasePriceEditText.text.toString().trim()
            val salePrice = productSalePriceEditText.text.toString().trim()
            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(inventory)
                && !TextUtils.isEmpty(purchasePrice) && !TextUtils.isEmpty(salePrice)) {
                uploadProduct(name, inventory, purchasePrice, salePrice)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    /**
     * Abre el diálogo para editar un producto existente.
     * @param product El producto a editar.
     */
    private fun openEditProductDialog(product: Product) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_product)
        dialog.setTitle("Editar Producto")

        dialog.window?.setBackgroundDrawableResource(R.drawable.lavender_border)

        val layoutParams = dialog.window?.attributes
        layoutParams?.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        dialog.window?.attributes = layoutParams

        selectedImageButton = dialog.findViewById(R.id.image_button)
        val productNameEditText = dialog.findViewById<EditText>(R.id.product_name)
        val productInventoryEditText = dialog.findViewById<EditText>(R.id.product_inventory)
        val productPurchasePriceEditText = dialog.findViewById<EditText>(R.id.product_purchase_price)
        val productSalePriceEditText = dialog.findViewById<EditText>(R.id.product_sale_price)
        val dialogButton = dialog.findViewById<Button>(R.id.update_product_dialog_button)
        val lastEditedDateTextView = dialog.findViewById<TextView>(R.id.last_edited_date)

        selectedImageButton.setOnClickListener { showImagePickerDialog(product) }

        // Rellena el formulario con los datos del producto existente
        productNameEditText.setText(product.name)
        productInventoryEditText.setText(product.inventory)
        productPurchasePriceEditText.setText(product.purchasePrice)
        productSalePriceEditText.setText(product.salePrice)
        lastEditedDateTextView.text = "Última edición: ${product.lastEditedDate?.split(" ")?.first() ?: "No disponible"}"

        // Carga la imagen del producto si está disponible
        if (product.imageUrl != null) {
            Glide.with(this)
                .load(product.imageUrl)
                .placeholder(R.drawable.baseline_add_photo_alternate_24) // Imagen por defecto mientras se carga
                .into(selectedImageButton)
            imageUri = Uri.parse(product.imageUrl)
        } else {
            selectedImageButton.setImageResource(R.drawable.baseline_add_photo_alternate_24) // Imagen por defecto
        }

        dialogButton.setOnClickListener {
            val name = productNameEditText.text.toString().trim()
            val inventory = productInventoryEditText.text.toString().trim()
            val purchasePrice = productPurchasePriceEditText.text.toString().trim()
            val salePrice = productSalePriceEditText.text.toString().trim()

            // Solo actualiza los campos que han cambiado y añade la fecha actual
            val updatedProduct = product.copy(
                name = if (name.isNotEmpty()) name else product.name,
                inventory = if (inventory.isNotEmpty()) inventory else product.inventory,
                purchasePrice = if (purchasePrice.isNotEmpty()) purchasePrice else product.purchasePrice,
                salePrice = if (salePrice.isNotEmpty()) salePrice else product.salePrice,
                imageUrl = imageUri?.toString() ?: product.imageUrl,
                lastEditedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
            updateProduct(product.id, updatedProduct)
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Muestra un diálogo para seleccionar la imagen del producto.
     * @param product El producto cuya imagen se va a editar (opcional).
     */
    private fun showImagePickerDialog(product: Product?) {
        val options = arrayOf("Abrir cámara", "Elegir de la galería")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    /**
     * Abre la cámara del dispositivo para tomar una foto.
     */
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    /**
     * Abre la galería del dispositivo para seleccionar una imagen.
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    /**
     * Maneja los resultados de las actividades para seleccionar imágenes.
     * @param requestCode El código de solicitud de la actividad.
     * @param resultCode El código de resultado de la actividad.
     * @param data Los datos devueltos por la actividad.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                val bitmap = data?.extras?.get("data") as Bitmap
                selectedImageButton.setImageBitmap(bitmap)
                imageUri = getImageUriFromBitmap(bitmap)
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                imageUri = data?.data
                selectedImageButton.setImageURI(imageUri)
            }
        }
    }

    /**
     * Convierte un Bitmap en una URI.
     * @param bitmap El Bitmap que se convertirá.
     * @return La URI del Bitmap.
     */
    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    /**
     * Sube un nuevo producto a Firebase.
     * @param name Nombre del producto.
     * @param inventory Inventario del producto.
     * @param purchasePrice Precio de compra del producto.
     * @param salePrice Precio de venta del producto.
     */
    private fun uploadProduct(name: String, inventory: String, purchasePrice: String, salePrice: String) {
        val id = databaseReference.push().key ?: return
        val product = Product(id, name, inventory, purchasePrice, salePrice, null, null)

        if (imageUri != null) {
            val imageRef = storageReference.child("$id.jpg")
            imageRef.putFile(imageUri!!).addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val updatedProduct = product.copy(imageUrl = uri.toString())
                    saveProductToDatabase(id, updatedProduct)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
        } else {
            saveProductToDatabase(id, product)
        }
    }

    /**
     * Guarda un producto en la base de datos de Firebase.
     * @param id ID del producto.
     * @param product Producto a guardar.
     */
    private fun saveProductToDatabase(id: String, product: Product) {
        databaseReference.child(id).setValue(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto agregado", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al agregar el producto", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Actualiza un producto en Firebase.
     * @param id ID del producto.
     * @param updatedProduct Producto actualizado.
     */
    private fun updateProduct(id: String, updatedProduct: Product) {
        databaseReference.child(id).setValue(updatedProduct)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar el producto", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Elimina un producto de Firebase.
     * @param product Producto a eliminar.
     */
    private fun deleteProduct(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de que deseas eliminar este producto?")
            .setPositiveButton("Sí") { _, _ ->
                databaseReference.child(product.id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                        loadProducts()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar el producto", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    /**
     * Carga la lista de productos desde Firebase y actualiza el RecyclerView.
     */
    private fun loadProducts() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let { productList.add(it) }
                }
                productAdapter.notifyDataSetChanged()
                updateProductCount()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Error al cargar los productos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Actualiza el conteo de productos en la interfaz de usuario.
     */
    private fun updateProductCount() {
        val productCount = productList.size
        productCountTextView.text = "Total productos: $productCount"
    }

    /**
     * Configura el nombre de usuario y el conteo de productos.
     */
    private fun setUserNameAndProductCount() {
        val username = intent.getStringExtra("username") ?: "Usuario"
        usernameTextView.text = "Bienvenido $username"
        updateProductCount()
    }

    /**
     * Busca productos en Firebase según el texto introducido en el campo de búsqueda.
     */
    private fun searchProducts() {
        val query = searchProductEditText.text.toString().trim()
        if (query.isEmpty()) {
            loadProducts()
        } else {
            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    productList.clear()
                    for (productSnapshot in snapshot.children) {
                        val product = productSnapshot.getValue(Product::class.java)
                        product?.let { productList.add(it) }
                    }
                    filterProducts(query)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error al buscar productos", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    /**
     * Filtra la lista de productos localmente según la consulta.
     *
     * @param query La consulta de búsqueda introducida por el usuario.
     */
    private fun filterProducts(query: String) {
        val filteredProducts = productList.filter { product ->
            product.name?.contains(query, ignoreCase = true) == true
        }
        productAdapter.updateList(filteredProducts)
    }


    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
    }
}
