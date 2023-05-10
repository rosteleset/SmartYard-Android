package ru.madbrains.smartyard.ui.main.address.qrCode

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysisConfig
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
//import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentQrCodeBinding
import ru.madbrains.smartyard.ui.main.MainActivity

class QrCodeFragment :
    Fragment(),
//    OnSuccessListener<List<FirebaseVisionBarcode>>,
    OnFailureListener {
    private var _binding: FragmentQrCodeBinding? = null
    private val binding get() = _binding!!

    private lateinit var preview: Preview
    private lateinit var cameraTextureView: TextureView
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
        cameraTextureView = view.findViewById(R.id.cameraTextureView)
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
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

    private fun bindCamera() {
        val metrics = DisplayMetrics().also { cameraTextureView.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(screenSize)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(cameraTextureView.display.rotation)
        }.build()
        preview = AutoFitPreviewBuilder.build(previewConfig, cameraTextureView)
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setTargetResolution(screenSize)
            setTargetAspectRatio(screenAspectRatio)
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_NEXT_IMAGE)
            setImageQueueDepth(40)
        }.build()
        imageAnalysis = ImageAnalysis(analyzerConfig)
//        imageAnalysis.analyzer = MLQRcodeAnalyzer(this, this)
        CameraX.bindToLifecycle(this, preview, imageAnalysis)
    }

    private fun enableFlashlight(enabled: Boolean) {
        preview.enableTorch(enabled)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.showSystemUI()
        CameraX.unbindAll()
    }

    override fun onFailure(p0: Exception) {
        context?.run {
            Toast.makeText(this, "Failure: $p0", Toast.LENGTH_LONG).show()
        }
    }

//    override fun onSuccess(result: List<FirebaseVisionBarcode>) {
//        if (result.isNotEmpty()) {
//            result.first().rawValue?.let {
//                mViewModel.registerQR(it)
//                imageAnalysis.removeAnalyzer()
//                CameraX.unbindAll()
//            }
//        }
//    }

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
