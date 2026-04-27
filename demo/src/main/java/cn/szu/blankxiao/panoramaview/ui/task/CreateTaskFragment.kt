package cn.szu.blankxiao.panoramaview.ui.task

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.szu.blankxiao.panoramaview.R
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaInputImageAssetDto
import cn.szu.blankxiao.panoramaview.viewmodel.PanoramaViewModel
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CreateTaskFragment : Fragment() {

    private val vm: PanoramaViewModel by activityViewModels()

    private val reminderPrefs by lazy {
        requireContext().getSharedPreferences(OUTPAINT_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val pickInputImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            vm.uploadInputImage(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_create, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_mode)
        val layoutInputImage = view.findViewById<View>(R.id.layout_input_image)
        val etName = view.findViewById<TextInputEditText>(R.id.et_task_name)
        val etPrompt = view.findViewById<TextInputEditText>(R.id.et_prompt)
        val tvSelectedInputImage = view.findViewById<TextView>(R.id.tv_selected_input_image)
        val ivSelectedInputImage = view.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.iv_selected_input_image)
        val btnPickUploadInputImage = view.findViewById<MaterialButton>(R.id.btn_pick_upload_input_image)
        val btnChooseExistingInputImage = view.findViewById<MaterialButton>(R.id.btn_choose_existing_input_image)
        val tvError = view.findViewById<TextView>(R.id.tv_error)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btn_create)
        val progress = view.findViewById<LinearProgressIndicator>(R.id.progress)

        var createErrorMsg: String? = null
        var inputErrorMsg: String? = null

        fun renderError() {
            val msg = createErrorMsg ?: inputErrorMsg
            if (msg.isNullOrBlank()) {
                tvError.visibility = View.GONE
            } else {
                tvError.text = msg
                tvError.visibility = View.VISIBLE
            }
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val isOutpaint = checkedIds.contains(R.id.chip_outpaint)
            layoutInputImage.visibility = if (isOutpaint) View.VISIBLE else View.GONE
            if (isOutpaint) {
                vm.ensureInputImagesLoaded()
            }
        }

        btnPickUploadInputImage.setOnClickListener {
            val isOutpaint = chipGroup.checkedChipId == R.id.chip_outpaint
            if (isOutpaint) {
                showOutpaintCropNoticeIfNeeded { pickInputImageLauncher.launch("image/*") }
            } else {
                pickInputImageLauncher.launch("image/*")
            }
        }

        btnChooseExistingInputImage.setOnClickListener {
            val assets = vm.inputImageState.value.assets
            if (assets.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.create_input_image_empty_list), Toast.LENGTH_SHORT).show()
                vm.loadInputImageAssets()
                return@setOnClickListener
            }
            showAssetSelectDialog(assets)
        }

        btnCreate.setOnClickListener {
            val mode = if (chipGroup.checkedChipId == R.id.chip_outpaint) "outpaint" else "text2pano"
            vm.createTask(
                prompt = etPrompt.text?.toString().orEmpty(),
                name = etName.text?.toString(),
                mode = mode,
                inputImageUrl = if (mode == "outpaint") vm.inputImageState.value.selectedImageUrl else null
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.createState.collect { state ->
                        progress.visibility = if (state.loading || vm.inputImageState.value.uploading) View.VISIBLE else View.GONE
                        btnCreate.isEnabled = !state.loading && !vm.inputImageState.value.uploading

                        createErrorMsg = state.errorMsg
                        renderError()

                        if (state.success) {
                            Toast.makeText(requireContext(), getString(R.string.create_success, state.taskId), Toast.LENGTH_SHORT).show()
                            etPrompt.text?.clear()
                            etName.text?.clear()
                            vm.clearCreateState()
                        }
                    }
                }

                launch {
                    vm.inputImageState.collect { state ->
                        progress.visibility = if (state.uploading || vm.createState.value.loading) View.VISIBLE else View.GONE
                        btnCreate.isEnabled = !state.uploading && !vm.createState.value.loading
                        btnPickUploadInputImage.isEnabled = !state.uploading
                        btnChooseExistingInputImage.isEnabled = !state.uploading

                        inputErrorMsg = state.errorMsg
                        renderError()

                        if (state.uploading) {
                            tvSelectedInputImage.text = getString(R.string.create_input_image_uploading)
                        } else {
                            val selectedText = state.selectedImageUrl?.let {
                                getString(R.string.create_input_image_selected, it)
                            } ?: getString(R.string.create_input_image_not_selected)
                            tvSelectedInputImage.text = selectedText
                        }

                        val preview = state.selectedPreviewUrl
                        if (preview.isNullOrBlank()) {
                            ivSelectedInputImage.visibility = View.GONE
                        } else {
                            ivSelectedInputImage.visibility = View.VISIBLE
                            ivSelectedInputImage.load(preview)
                        }
                    }
                }
            }
        }
    }

    private fun showAssetSelectDialog(assets: List<PanoramaInputImageAssetDto>) {
        val labels = assets.mapIndexed { index, item ->
            val url = item.ossUrl ?: ""
            val suffix = if (url.length > 40) "...${url.takeLast(40)}" else url
            "${index + 1}. $suffix"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.create_btn_choose_existing_image))
            .setItems(labels) { _, which ->
                vm.selectInputImage(assets[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showOutpaintCropNoticeIfNeeded(onConfirmed: () -> Unit) {
        val alreadyShown = reminderPrefs.getBoolean(KEY_OUTPAINT_CROP_NOTICE_SHOWN, false)
        if (alreadyShown) {
            onConfirmed()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.outpaint_crop_notice_title))
            .setMessage(getString(R.string.outpaint_crop_notice_message))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.outpaint_crop_notice_confirm) { _, _ ->
                reminderPrefs.edit().putBoolean(KEY_OUTPAINT_CROP_NOTICE_SHOWN, true).apply()
                onConfirmed()
            }
            .show()
    }

    companion object {
        private const val OUTPAINT_PREFS_NAME = "create_task_prefs"
        private const val KEY_OUTPAINT_CROP_NOTICE_SHOWN = "outpaint_crop_notice_shown"
    }
}
