package org.thequestionmark.questionmark

import android.os.Bundle
import androidx.activity.ComponentActivity
/*
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
*/

class ScanGmsActivity : ComponentActivity() {
/*
    private lateinit var scanner: GmsBarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .enableAutoZoom()
            .build()
        this.scanner = GmsBarcodeScanning.getClient(this, options)
    }

    override fun onStart() {
        super.onStart()
        this.scanner.startScan()
            .addOnSuccessListener { barcode ->
                println("Scanned: " + barcode.rawValue)
            }
            .addOnCanceledListener {
                println("Scan cancelled")
            }
            .addOnFailureListener { e ->
                println("Scan failure: $e")
            }
    }
    */
}