package com.saber.myapp

data class Product(
    val id: Int = 0,

    val barcode: String,

    val name: String,

    val expiryDate: String,

    // =========================
    // الكميات
    // =========================

    // عدد الكراتين الموجودة
    val cartons: Int = 0,

    // عدد الباكت داخل الكرتون الواحد
    val packsPerCarton: Int = 0,

    // عدد الحبات داخل الباكت الواحد
    val piecesPerPack: Int = 0,

    // =========================
    // الأسعار
    // =========================

    // سعر شراء الكرتون
    val cartonPurchasePrice: Double = 0.0,

    // سعر بيع الحبة
    val pieceSalePrice: Double = 0.0,

    // =========================
    // الصورة والتصنيف
    // =========================

    val imagePath: String,

    val category: String = ""
) {

    // =====================================================
    // حساب إجمالي الباكت
    // =====================================================

    val totalPacks: Int
        get() = cartons * packsPerCarton

    // =====================================================
    // عدد الحبات في الكرتون الواحد
    // =====================================================

    val piecesPerCarton: Int
        get() = packsPerCarton * piecesPerPack

    // =====================================================
    // إجمالي الحبات الموجودة
    // =====================================================

    val totalPieces: Int
        get() = cartons * packsPerCarton * piecesPerPack

    // =====================================================
    // سعر شراء الباكت
    // =====================================================

    val packPurchasePrice: Double
        get() =
            if (packsPerCarton > 0)
                cartonPurchasePrice / packsPerCarton
            else
                0.0

    // =====================================================
    // سعر شراء الحبة
    // =====================================================

    val piecePurchasePrice: Double
        get() =
            if (piecesPerPack > 0)
                packPurchasePrice / piecesPerPack
            else
                0.0

    // =====================================================
    // سعر بيع الباكت
    // =====================================================

    val packSalePrice: Double
        get() =
            pieceSalePrice * piecesPerPack

    // =====================================================
    // سعر بيع الكرتون
    // =====================================================

    val cartonSalePrice: Double
        get() =
            pieceSalePrice * piecesPerCarton

    // =====================================================
    // صافي ربح الحبة
    // =====================================================

    val pieceProfit: Double
        get() =
            pieceSalePrice - piecePurchasePrice

    // =====================================================
    // صافي ربح الباكت
    // =====================================================

    val packProfit: Double
        get() =
            packSalePrice - packPurchasePrice

    // =====================================================
    // صافي ربح الكرتون
    // =====================================================

    val cartonProfit: Double
        get() =
            cartonSalePrice - cartonPurchasePrice
}
