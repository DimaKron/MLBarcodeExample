package ru.example.mlbarcodeexample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.common.base.Objects
import kotlinx.android.synthetic.main.activity_main.*
import ru.example.mlbarcodeexample.barcodedetection.BarcodeProcessor
import ru.example.mlbarcodeexample.camera.CameraSource
import ru.example.mlbarcodeexample.camera.WorkflowModel
import java.io.IOException

//TODO: разрешение
//TODO:  вкл / выкл вспышки:
//  cameraSource!!.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
//  cameraSource?.updateFlashMode(Camera.Parameters.FLASH_MODE_OFF)
class MainActivity : AppCompatActivity() {

    private var cameraSource: CameraSource? = null
    private var workflowModel: WorkflowModel? = null
    private var currentWorkflowState: WorkflowModel.WorkflowState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        cameraSource = CameraSource(cameraGraphicOverlay)

        setUpWorkflowModel()
    }

    override fun onResume() {
        super.onResume()

        workflowModel?.markCameraFrozen()
        currentWorkflowState = WorkflowModel.WorkflowState.NOT_STARTED
        cameraSource?.setFrameProcessor(BarcodeProcessor(cameraGraphicOverlay, workflowModel!!))
        workflowModel?.setWorkflowState(WorkflowModel.WorkflowState.DETECTING)
    }

    override fun onPostResume() {
        super.onPostResume()
        //BarcodeResultFragment.dismiss(supportFragmentManager)
    }

    override fun onPause() {
        super.onPause()
        currentWorkflowState = WorkflowModel.WorkflowState.NOT_STARTED
        stopCameraPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
        cameraSource = null
    }

    private fun startCameraPreview() {
        val workflowModel = this.workflowModel ?: return
        val cameraSource = this.cameraSource ?: return
        if (!workflowModel.isCameraLive) {
            try {
                workflowModel.markCameraLive()
                cameraPreview?.start(cameraSource)
            } catch (e: IOException) {
                Log.e("MainActivity", "Failed to start camera preview!", e)
                cameraSource.release()
                this.cameraSource = null
            }
        }
    }

    private fun stopCameraPreview() {
        val workflowModel = this.workflowModel ?: return
        if (workflowModel.isCameraLive) {
            workflowModel.markCameraFrozen()
            cameraPreview?.stop()
        }
    }

    private fun setUpWorkflowModel() {
        workflowModel = ViewModelProviders.of(this).get(WorkflowModel::class.java)

        // Observes the workflow state changes, if happens, update the overlay view indicators and
        // camera preview state.
        workflowModel!!.workflowState.observe(this, Observer { workflowState ->
            if (workflowState == null || Objects.equal(currentWorkflowState, workflowState)) {
                return@Observer
            }

            currentWorkflowState = workflowState
            Log.d("MainActivity", "Current workflow state: ${currentWorkflowState!!.name}")

            when (workflowState) {
                WorkflowModel.WorkflowState.DETECTING -> {
                    startCameraPreview()
                }
                WorkflowModel.WorkflowState.SEARCHING -> {
                    stopCameraPreview()
                }
                WorkflowModel.WorkflowState.DETECTED, WorkflowModel.WorkflowState.SEARCHED -> {
                    stopCameraPreview()
                }
                else -> {
                }
            }
        })

        workflowModel?.detectedBarcode?.observe(this, Observer { barcode ->
            if (barcode != null) {
                //BarcodeResultFragment.show(supportFragmentManager, barcodeFieldList)
                Toast.makeText(this, "${barcode.rawValue}", Toast.LENGTH_LONG).show()
            }
        })
    }

}