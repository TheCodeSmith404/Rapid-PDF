package com.tcs.tools.managePdf.ui.sortPdf

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.FragmentPdfsortBinding
import com.tcs.tools.managePdf.ui.home.Home
import com.tcs.tools.managePdf.utils.InputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SortPdf: Fragment() {
    private var _binding: FragmentPdfsortBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SortPdfViewModel by viewModels()
    private lateinit var adapter: SortPdfAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Disable force dark mode for this fragment
        val themedInflater = inflater.cloneInContext(context)
        // Inflate the binding layout
        _binding = FragmentPdfsortBinding.inflate(themedInflater, container, false)
        viewModel.init(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pos=arguments?.getLong("file_id",0L)?:0L
        lifecycleScope.launch {
            val numPages=viewModel.getPages()
            viewModel.getAllFilesAfterId(pos).observe(viewLifecycleOwner, Observer { pdfFiles ->
                viewModel.sizePdf=pdfFiles.size
                adapter = SortPdfAdapter(requireContext(),numPages, pdfFiles)
                binding.viewpager.adapter = adapter
                binding.viewpager.registerOnPageChangeCallback(object :
                    ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        val file = pdfFiles[position]
                        binding.radioGroupSortFiles.clearCheck()
                        viewModel.currentPosition = position
                        viewModel.currentId = file.id
                        viewModel.currentUri = file.uri
                        viewModel.currentName = file.fileName
                        binding.changeName.setText(file.fileName)
                        val drawableEnd = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.close_circle_outline
                        )
                        binding.changeName.setCompoundDrawablesWithIntrinsicBounds(
                            null,
                            null,
                            drawableEnd,
                            null
                        )
                        if(position==adapter.itemCount-1){
                            Toast.makeText(requireContext(),"No more Files Left",Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            })
        }
        setUpRadioGroup()
        setOnClickListeners()
        addMenu()
    }
    private fun addMenu(){
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.sort_pdf_menu, menu)
                menu.findItem(R.id.optionLeftRight).isChecked=viewModel.hideLeftRight
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                Log.d("menu","Item Selected ${menuItem.title}")
                when (menuItem.itemId) {
                    R.id.optionRapidMode -> {
                        val isChecked = !menuItem.isChecked
                        menuItem.isChecked = isChecked
                        viewModel.rapidMode = isChecked
                        if(viewModel.rapidMode){
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Enabled Rapid Mode")
                                .setMessage("Rapid mode allows instant deletion of files which might cause accidents.\nHowever It allows to rapidly rename and access files hence saves time.")
                                .setPositiveButton("OK"){dialog,_->
                                    dialog.dismiss()
                                }
                                .show()
                        }
                        return true
                    }
                    R.id.optionViewFile->{
                        findNavController().navigate(R.id.action_sortPdf_to_show_pdf, bundleOf("file_id" to viewModel.currentId))
                        return true
                    }
                    R.id.optionLeftRight->{
                        val isChecked=menuItem.isChecked
                        Log.d("leftRight",isChecked.toString())
                        menuItem.isChecked=!isChecked
                        viewModel.hideLeftRight=!isChecked
                        if (!isChecked)
                            binding.leftRightGroup.visibility=View.GONE
                        else
                            binding.leftRightGroup.visibility=View.VISIBLE
                        return true
                    }
                    R.id.shareFile->{
                        val intent= Intent()
                        intent.setAction(Intent.ACTION_SEND)
                        intent.setType("application/pdf")
                        intent.putExtra(Intent.EXTRA_STREAM,viewModel.currentUri)
                        startActivity(Intent.createChooser(intent,"Share File"))
                        return true
                    }
                    else -> {
                        Log.d("menu","No Item Selected ${menuItem.itemId}")
                        return false
                    }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setOnClickListeners(){
        if(viewModel.hideLeftRight){
            binding.leftRightGroup.visibility=View.GONE
        }else {
            binding.left.setOnClickListener {
                val pos = viewModel.currentPosition
                if (pos > 0)
                    binding.viewpager.currentItem = pos - 1
            }
            binding.right.setOnClickListener {
                val pos = viewModel.currentPosition
                if (pos < viewModel.sizePdf - 1)
                    binding.viewpager.currentItem = pos + 1
            }
        }
        binding.imageButtonSave.setOnClickListener{
            renameFile()
            Log.d(Home.TAG,"File Renamed")
        }
        binding.changeName.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                renameFile()
                true
            } else {
                false
            }
        }

        binding.changeName.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Check if the touch was within the bounds of drawableEnd
                val width=binding.changeName.compoundDrawables[2]?.bounds?.width()?:0
                if (event.rawX >= ((binding.changeName.right - width))
                ) {
                    binding.changeName.text?.clear()
                    binding.changeName.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
        binding.delete.setOnClickListener{
            if(viewModel.rapidMode){
                lifecycleScope.launch {
                    deleteFile()
                }
            }else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Current File")
                    .setMessage("Are you sure you want to delete this file?")
                    .setCancelable(false)
                    .setPositiveButton("Delete") { dialog, which ->
                        // User clicked Delete button
                        lifecycleScope.launch {
                            deleteFile()
                            Toast.makeText(requireContext(), "File Deleted", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, which ->
                        dialog.dismiss() // Dismiss the dialog
                    }
                    .show()
            }
        }
        binding.favorite.setOnClickListener{
            lifecycleScope.launch {
                viewModel.addPdfToFavorites(viewModel.currentId)
                Toast.makeText(requireContext(),"File added to favorites",Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setUpRadioGroup(){
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),  // Disabled
                intArrayOf(android.R.attr.state_enabled) // Enabled
            ),
            intArrayOf(
                Color.GRAY,  // disabled
                Color.parseColor("#6200EE") // enabled
            )
        )
        val set= mutableSetOf<String>()
        val radioGroup=binding.radioGroupSortFiles
        lifecycleScope.launch {
            set.addAll(viewModel.getAllCategories())
            if(set.isEmpty()){
                set.addAll(requireContext().resources.getStringArray(R.array.my_category_array))
            }
            for(str in set){
                addRadioButton(str,radioGroup,colorStateList)
            }
        }
        radioGroup.setOnCheckedChangeListener { group, id ->
            //TODO find button
            val selectedRadioButton = group.findViewById<RadioButton>(id)

            // Check if selectedRadioButton is null (happens when no RadioButton is selected)
            if (selectedRadioButton != null && selectedRadioButton.isChecked) {
                // Get the text of the selected RadioButton
                val selectedText = selectedRadioButton.text.toString()
                val et=binding.changeName
                et.setText("${et.text}_$selectedText")

            }

        }
    }
    private fun addRadioButton(text: String, radioGroup: RadioGroup,colorStateList: ColorStateList) {
        // Create a new RadioButton
        val radioButton = RadioButton(radioGroup.context).apply {
            this.id = View.generateViewId() // Generate unique ID for RadioButton
            this.text = text
            buttonTintList=colorStateList
            // Set layout params with margins
            val params = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            )
            val dim = requireContext().resources.getDimensionPixelSize(R.dimen.four_dp)
            params.setMargins(dim, 0, 0, 0) // Set left margin here
            layoutParams = params
            setPadding(dim,dim,dim,dim)


            // Set background drawable
            background = ContextCompat.getDrawable(context, R.drawable.background_stroked_curved_rectangle)
        }

        // Add the RadioButton to the RadioGroup
        radioGroup.addView(radioButton)
    }
    private suspend fun deleteFile(){
        viewModel.deleteFile(requireContext(), viewModel.currentId)
        adapter.setFileDeleted(viewModel.currentPosition)
        binding.viewpager.setCurrentItem(viewModel.currentPosition + 1, true)
    }
    private fun renameFile(){
        val name=binding.changeName.text.toString()
        Log.d("uri","Save Button Pressed")
        if(name.isNotEmpty()){
            lifecycleScope.launch {
                adapter.setFileName(viewModel.currentPosition,name)
                viewModel.renameFile(requireContext(),viewModel.currentId,name)
            }
            if(!viewModel.rapidMode) {
                InputManager.hideKeyboard(requireContext(), binding.root)
                Toast.makeText(requireContext(), "File Name changed", Toast.LENGTH_SHORT).show()
            }
            binding.viewpager.setCurrentItem(viewModel.currentPosition+1,true)
        }
    }

    override fun onPause() {
        lifecycleScope.launch {
            Log.d("file_id","paused")
            withContext(Dispatchers.IO){
                viewModel.saveLastViewedFileId(viewModel.currentId)
            }
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(::adapter.isInitialized)
            adapter.clearResources()
    }
}