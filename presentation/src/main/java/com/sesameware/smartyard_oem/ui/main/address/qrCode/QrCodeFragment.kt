package com.sesameware.smartyard_oem.ui.main.address.qrCode

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentQrCodeBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.math.max

class QrCodeFragment :
    Fragment(),
    OnSuccessListener<List<Barcode>>,
    OnFailureListener {
    private var _binding: FragmentQrCodeBinding? = null
    private val binding get() = _binding!!

    private var camera: Camera? = null
    private lateinit var imageAnalysis: ImageAnalysis
    private val mViewModel by viewModel<QrCodeViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.hideSystemUI()
        if (ContextCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            bindCamera()
        } else {
            requestCameraPermission()
        }
        binding.switchFlash.setOnCheckedChangeListener { _, isChecked ->
            enableFlashlight(isChecked)
        }
        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }
        mViewModel.navigationToDialog.observe(
            viewLifecycleOwner,
            EventObserver {
                showDialog(it)
            }
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    bindCamera()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.qr_code_error_permission_title),
                        Toast.LENGTH_LONG
                    ).show()
                    this.findNavController().popBackStack()
                }
            }
        }
    }

    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    @OptIn(ExperimentalGetImage::class) private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )
            barcodeScanner.process(inputImage)
                .addOnSuccessListener(this)
                .addOnFailureListener(this)
                .addOnCompleteListener {
                    imageProxy.image?.close()
                    imageProxy.close()
                }
        }
    }

    private fun bindCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // setting up the preview use case
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraTextureView.surfaceProvider)
                }

            // configure to use the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_QR_CODE
            ).build()
            val scanner = BarcodeScanning.getClient(options)

            imageAnalysis = ImageAnalysis.Builder()
                .build()
            imageAnalysis.setAnalyzer(
                Executors.newSingleThreadExecutor()
            ) { imageProxy ->
                processImageProxy(scanner, imageProxy)
            }

            try {
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    previewUseCase,
                    imageAnalysis
                )
            } catch (e: IllegalStateException) {
                // If the use case has already been bound to another lifecycle or method is not called on main thread.
                Timber.e("debug_dmm    ${e.message.orEmpty()}")
            } catch (e: IllegalArgumentException) {
                // If the provided camera selector is unable to resolve a camera to be used for the given use cases.
                Timber.e("debug_dmm    ${e.message.orEmpty()}")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun enableFlashlight(enabled: Boolean) {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            camera?.cameraControl?.enableTorch(enabled)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.showSystemUI()
    }

    override fun onFailure(p0: Exception) {
        context?.run {
            Toast.makeText(this, "Failure: $p0", Toast.LENGTH_LONG).show()
            Timber.d("__Q__    ${(p0 as MlKitException).message}    error code: ${(p0 as MlKitException).errorCode}.")
            imageAnalysis.clearAnalyzer()
        }
    }

    override fun onSuccess(result: List<Barcode>) {
        if (result.isNotEmpty()) {
            val position = IntArray(2)
            binding.croppedView.getLocationOnScreen(position)
            val cx = (imageAnalysis.resolutionInfo?.resolution?.width ?: 0).toFloat() / 2.0f
            val cy = (imageAnalysis.resolutionInfo?.resolution?.height ?: 0).toFloat() / 2.0f
            val m = Matrix()
            m.postRotate(imageAnalysis.resolutionInfo?.rotationDegrees?.toFloat() ?: 0.0f, 0.0f, 0.0f)
            val sourceRect = RectF(-cx, -cy, cx, cy)
            val destRect = RectF()
            m.mapRect(destRect, sourceRect)
            if (destRect.width() > 0 && destRect.height() > 0) {
                val scaleX = requireContext().resources.displayMetrics.widthPixels / destRect.width()
                val scaleY = requireContext().resources.displayMetrics.heightPixels / destRect.height()
                val scale = max(scaleX, scaleY)
                val tx = (requireContext().resources.displayMetrics.widthPixels - destRect.width() * scale) / 2
                val ty = (requireContext().resources.displayMetrics.heightPixels - destRect.height() * scale) / 2
                val croppedRect = Rect(
                    position[0], position[1],
                    position[0] + binding.croppedView.width - 1,
                    position[1] + binding.croppedView.height - 1)
                result.first().boundingBox?.let { bBoxRect ->
                    bBoxRect.left = Math.round(bBoxRect.left * scale + tx)
                    bBoxRect.right = Math.round(bBoxRect.right * scale + tx)
                    bBoxRect.top = Math.round(bBoxRect.top * scale + ty)
                    bBoxRect.bottom = Math.round(bBoxRect.bottom * scale + ty)

                    // bBoxRect must be inside croppedRect
                    if (croppedRect.contains(bBoxRect)) {
                        result.first().rawValue?.let {
                            mViewModel.registerQR(it)
                            imageAnalysis.clearAnalyzer()
                        }
                    }
                }
            }
        }
    }

    private fun showDialog(message: String) {
        NavHostFragment.findNavController(this)
            .navigate(R.id.action_global_addressFragment2)
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)
        builder
            .setMessage(message)
            .setPositiveButton(resources.getString(R.string.qr_code_dialog_ok)) { _, _ -> }.show()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
    }
}
