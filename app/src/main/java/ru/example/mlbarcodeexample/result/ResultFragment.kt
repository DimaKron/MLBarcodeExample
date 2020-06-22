package ru.example.mlbarcodeexample.result

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.dialog_result.*
import ru.example.mlbarcodeexample.R
import ru.example.mlbarcodeexample.Workflow
import ru.example.mlbarcodeexample.WorkflowState

class ResultFragment: BottomSheetDialogFragment(){

    companion object{
        fun newInstance(barcode: String?) = ResultFragment().apply {
            arguments = Bundle().apply {
                putString("barcode", barcode)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_result, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        titleTextView.text = "Товар найден"
        subtitleTextView.text = "Код: ${arguments?.getString("barcode")}"
    }

    override fun onDismiss(dialog: DialogInterface) {
        (activity as? Workflow)?.onWorkflowStateChange(WorkflowState.DETECTING)
        super.onDismiss(dialog)
    }

}