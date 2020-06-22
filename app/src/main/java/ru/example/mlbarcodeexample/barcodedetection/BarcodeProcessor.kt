/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.example.mlbarcodeexample.barcodedetection

import android.animation.ValueAnimator
import android.util.Log
import androidx.annotation.MainThread
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import ru.example.mlbarcodeexample.PreferenceUtils
import ru.example.mlbarcodeexample.Workflow
import ru.example.mlbarcodeexample.WorkflowState
import ru.example.mlbarcodeexample.camera.FrameProcessorBase
import ru.example.mlbarcodeexample.camera.GraphicOverlay
import java.io.IOException

/** A processor to run the barcode detector.  */
class BarcodeProcessor(private val workflow: Workflow) : FrameProcessorBase<List<FirebaseVisionBarcode>>() {

    private val detector = FirebaseVision.getInstance().visionBarcodeDetector

    override fun detectInImage(image: FirebaseVisionImage): Task<List<FirebaseVisionBarcode>> = detector.detectInImage(image)

    @MainThread
    override fun onSuccess(
        image: FirebaseVisionImage,
        results: List<FirebaseVisionBarcode>,
        graphicOverlay: GraphicOverlay
    ) {

        if (!workflow.isCameraLive) return

        Log.d(TAG, "Barcode result size: ${results.size}")

        // Picks the barcode, if exists, that covers the center of graphic overlay.

        val barcodeInCenter = results.firstOrNull { barcode ->
            val boundingBox = barcode.boundingBox ?: return@firstOrNull false
            val box = graphicOverlay.translateRect(boundingBox)
            box.contains(graphicOverlay.width / 2f, graphicOverlay.height / 2f)
        }

        graphicOverlay.clear()
        if (barcodeInCenter == null) {
            workflow.onWorkflowStateChange(WorkflowState.DETECTING)
        } else {
            if (PreferenceUtils.shouldDelayLoadingBarcodeResult(graphicOverlay.context)) {
                val loadingAnimator = createLoadingAnimator(graphicOverlay, barcodeInCenter)
                loadingAnimator.start()
                workflow.onWorkflowStateChange(WorkflowState.SEARCHING)
            } else {
                workflow.onWorkflowStateChange(WorkflowState.DETECTED)
                workflow.onBarcodeDetected(barcodeInCenter)
            }
        }
        graphicOverlay.invalidate()
    }

    private fun createLoadingAnimator(graphicOverlay: GraphicOverlay, barcode: FirebaseVisionBarcode): ValueAnimator {
        val endProgress = 1.1f
        return ValueAnimator.ofFloat(0f, endProgress).apply {
            addUpdateListener {
                if ((animatedValue as Float).compareTo(endProgress) >= 0) {
                    graphicOverlay.clear()
                    workflow.onWorkflowStateChange(WorkflowState.SEARCHED)
                    workflow.onBarcodeDetected(barcode)
                } else {
                    graphicOverlay.invalidate()
                }
            }
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Barcode detection failed!", e)
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close barcode detector!", e)
        }
    }

    companion object {
        private const val TAG = "BarcodeProcessor"
    }
}
