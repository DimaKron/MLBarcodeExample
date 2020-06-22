package ru.example.mlbarcodeexample

import androidx.annotation.MainThread
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode

interface Workflow {

    var isCameraLive: Boolean

    var workflowState: WorkflowState?

    fun onBarcodeDetected(barcode: FirebaseVisionBarcode)

    @MainThread
    fun onWorkflowStateChange(workflowState: WorkflowState)

}

enum class WorkflowState {
    NOT_STARTED,
    DETECTING,
    DETECTED,
    SEARCHING,
    SEARCHED
}