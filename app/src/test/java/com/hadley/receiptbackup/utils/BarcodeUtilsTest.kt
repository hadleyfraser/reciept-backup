package com.hadley.receiptbackup.utils

import com.google.zxing.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeUtilsTest {

    @Test
    fun createBarcodeDimensions_returnsSquareFor2d() {
        val (width, height) = createBarcodeDimensions(true)
        assertEquals(width, height)
        assertTrue(width > 0)
    }

    @Test
    fun createBarcodeDimensions_returnsWideFor1d() {
        val (width, height) = createBarcodeDimensions(false)
        assertTrue(width > height)
    }

    @Test
    fun barcodeFormatFromName_returnsQrCode() {
        val format = barcodeFormatFromName("QR_CODE")
        assertEquals(BarcodeFormat.QR_CODE, format)
    }
}

