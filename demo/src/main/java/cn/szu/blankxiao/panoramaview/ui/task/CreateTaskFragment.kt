package cn.szu.blankxiao.panoramaview.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.szu.blankxiao.panoramaview.R
import cn.szu.blankxiao.panoramaview.viewmodel.PanoramaViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class CreateTaskFragment : Fragment() {

    private val vm: PanoramaViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_create, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_mode)
        val layoutInputImage = view.findViewById<TextInputLayout>(R.id.layout_input_image)
        val etName = view.findViewById<TextInputEditText>(R.id.et_task_name)
        val etPrompt = view.findViewById<TextInputEditText>(R.id.et_prompt)
        val etInputImageUrl = view.findViewById<TextInputEditText>(R.id.et_input_image_url)
        val tvError = view.findViewById<TextView>(R.id.tv_error)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btn_create)
        val progress = view.findViewById<LinearProgressIndicator>(R.id.progress)

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val isOutpaint = checkedIds.contains(R.id.chip_outpaint)
            layoutInputImage.visibility = if (isOutpaint) View.VISIBLE else View.GONE
        }

        btnCreate.setOnClickListener {
            val mode = if (chipGroup.checkedChipId == R.id.chip_outpaint) "outpaint" else "text2pano"
            vm.createTask(
                prompt = etPrompt.text?.toString().orEmpty(),
                name = etName.text?.toString(),
                mode = mode,
                inputImageUrl = etInputImageUrl.text?.toString()
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.createState.collect { state ->
                    progress.visibility = if (state.loading) View.VISIBLE else View.GONE
                    btnCreate.isEnabled = !state.loading

                    if (state.errorMsg != null) {
                        tvError.text = state.errorMsg
                        tvError.visibility = View.VISIBLE
                    } else {
                        tvError.visibility = View.GONE
                    }

                    if (state.success) {
                        Toast.makeText(requireContext(), getString(R.string.create_success, state.taskId), Toast.LENGTH_SHORT).show()
                        etPrompt.text?.clear()
                        etName.text?.clear()
                        etInputImageUrl.text?.clear()
                        vm.clearCreateState()
                    }
                }
            }
        }
    }
}
