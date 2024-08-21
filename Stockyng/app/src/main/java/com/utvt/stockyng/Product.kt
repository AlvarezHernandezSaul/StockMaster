package com.utvt.stockyng

/**
 * Representa como se almacena el producto del inventario en firebase.
 *
 * @property id Identificador único del producto.
 * @property name Nombre del producto.
 * @property inventory Cantidad del producto en inventario.
 * @property purchasePrice Precio de compra del producto.
 * @property salePrice Precio de venta del producto.
 * @property imageUrl URL de la imagen del producto.
 */
data class Product(
    var id: String = "",
    var name: String = "",
    var inventory: String = "",
    var purchasePrice: String = "",
    var salePrice: String = "",
    var imageUrl: String? = null,
    val lastEditedDate: String? = null
)
