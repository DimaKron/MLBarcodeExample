package ru.example.mlbarcodeexample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import kotlinx.android.synthetic.main.activity_main.*
import ru.example.mlbarcodeexample.barcodedetection.BarcodeProcessor
import ru.example.mlbarcodeexample.camera.CameraSource
import ru.example.mlbarcodeexample.result.ResultFragment
import java.io.IOException

//TODO: разрешение
//TODO:  вкл / выкл вспышки:
//  cameraSource!!.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
//  cameraSource?.updateFlashMode(Camera.Parameters.FLASH_MODE_OFF)
class MainActivity : AppCompatActivity(), Workflow {

    private var cameraSource: CameraSource? = null

    override var isCameraLive = false

    override var workflowState: WorkflowState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        cameraSource = CameraSource(cameraGraphicOverlay)
    }

    override fun onResume() {
        super.onResume()

        isCameraLive = false
        workflowState = WorkflowState.NOT_STARTED
        cameraSource?.setFrameProcessor(BarcodeProcessor(this))
        onWorkflowStateChange(WorkflowState.DETECTING)
    }

    override fun onPostResume() {
        super.onPostResume()
        (supportFragmentManager.findFragmentByTag(ResultFragment::class.java.simpleName) as? ResultFragment)?.dismiss()
    }

    override fun onPause() {
        super.onPause()
        workflowState = WorkflowState.NOT_STARTED
        stopCameraPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
        cameraSource = null
    }

    override fun onBarcodeDetected(barcode: FirebaseVisionBarcode) {
        ResultFragment.newInstance(barcode.rawValue)
            .show(supportFragmentManager, ResultFragment::class.java.simpleName)
    }

    override fun onWorkflowStateChange(workflowState: WorkflowState) {
        if (this.workflowState == workflowState) return

        this.workflowState = workflowState

        when (workflowState) {
            WorkflowState.DETECTING -> {
                startCameraPreview()
            }
            WorkflowState.SEARCHING, WorkflowState.DETECTED, WorkflowState.SEARCHED -> {
                stopCameraPreview()
            }
            else -> {}
        }
    }

    private fun startCameraPreview() {
        val cameraSource = this.cameraSource?: return

        if (!isCameraLive) {
            try {
                isCameraLive = true
                cameraPreview?.start(cameraSource)
            } catch (e: IOException) {
                cameraSource.release()
                this.cameraSource = null
            }
        }
    }

    private fun stopCameraPreview() {
        if (isCameraLive) {
            isCameraLive = false
            cameraPreview?.stop()
        }
    }
}