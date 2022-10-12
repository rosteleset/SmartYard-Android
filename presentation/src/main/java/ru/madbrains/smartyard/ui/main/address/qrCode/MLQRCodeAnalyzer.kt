package ru.madbrains.smartyard.ui.main.address.qrCode

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import timber.log.Timber

/**
 * @author Nail Shakurov
 * Created on 24/03/2020.
 */
class MLQRCodeAnalyzer(
    private var onSuccessListener: OnSuccessListener<List<FirebaseVisionBarcode>>,
    private var onFailureListener: OnFailureListener
) : ImageAnalysis.Analyzer {
    private var pendingTask: Task<out Any>? = null

    private val detector: FirebaseVisionBarcodeDetector by lazy {
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                FirebaseVisionBarcode.FORMAT_QR_CODE
            )
            .build()
        FirebaseVision.getInstance().getVisionBarcodeDetector(options)
    }

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        // Throttle calls to the detector.
        if (pendingTask != null && !pendingTask!!.isComplete) {
            Timber.d("MLQRCodeAnalyzer Throttle calls to the detector")
            return
        }
        // YUV_420 is normally the input type here
        var rotation = rotationDegrees % 360
        if (rotation < 0) {
            rotation += 360
        }
        val mediaImage = FirebaseVisionImage.fromMediaImage(
            image.image!!,
            when (rotation) {
                0 -> FirebaseVisionImageMetadata.ROTATION_0
                90 -> FirebaseVisionImageMetadata.ROTATION_90
                180 -> FirebaseVisionImageMetadata.ROTATION_180
                270 -> FirebaseVisionImageMetadata.ROTATION_270
                else -> {
                    Timber.e("unexpected rotation: $rotationDegrees")
                    FirebaseVisionImageMetadata.ROTATION_0
                }
            }
        )
        pendingTask = detector.detectInImage(mediaImage).also {
            it.addOnSuccessListener(onSuccessListener)
            it.addOnFailureListener(onFailureListener)
        }
    }
}
