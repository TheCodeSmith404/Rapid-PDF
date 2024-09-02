package com.tcs.tools.managePdf.ui.baseActivity

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.updateMargins
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.DialogManageCategoriesBinding
import com.tcs.tools.managePdf.utils.InputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageCategories:DialogFragment() {
    private var _binding: DialogManageCategoriesBinding?= null
    val binding: DialogManageCategoriesBinding
        get()=_binding?:DialogManageCategoriesBinding.inflate(layoutInflater)
    private val viewmodel:ManageCategoriesViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isCancelable=false
        viewmodel.init(requireContext())
        _binding=DialogManageCategoriesBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()
    }
    private fun setUpViews(){
        var set= mutableSetOf<String>()
        val chipGroup=binding.categoryChipGroup
        lifecycleScope.launch {
            viewmodel.getCategories()
            viewmodel.liveSet.observe(viewLifecycleOwner,Observer{values->
                set=values
                binding.count.text=set.size.toString()
            })
            if(set.isEmpty())
                set.addAll(requireContext().resources.getStringArray(R.array.my_category_array))
            for(element in set){
                createChips(element,chipGroup)
            }
        }
        chipGroup.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                // Handle addition of a child view if necessary
            }

            override fun onChildViewRemoved(parent: View?, child: View?) {
                // Handle removal of a child view
                if (child is Chip) {
                    val chipText = child.text.toString()
                    // Perform any action you need when a chip is removed
                    set.remove(chipText)
                    viewmodel.liveSet.value=set
                }
            }
        })
        val editText=binding.addCategories
        editText.setOnEditorActionListener { textView, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                // Add the entered text to your set
                val enteredText = editText.text.toString().trim()
                val count=set.size
                if (enteredText.isNotEmpty()&&set.size<=6) {
                    set.add(enteredText)
                    editText.text?.clear()
                    if(count!=set.size) {
                        viewmodel.liveSet.value=set
                        createChips(enteredText, chipGroup)
                    }
                }
                true // Consume the event
            } else {
                false // Continue listening for other events
            }
        }
        binding.dialogClose.setOnClickListener{
            InputManager.hideKeyboard(requireContext(),requireView())
            dismiss()
        }
        binding.dialogSave.setOnClickListener{
            val setCategories= mutableSetOf<String>()
            for (i in 0 until binding.categoryChipGroup.childCount) {
                val chip = binding.categoryChipGroup.getChildAt(i) as Chip
                setCategories.add(chip.text.toString())
            }
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    viewmodel.setCategories(set)
                }
            }
            InputManager.hideKeyboard(requireContext(),requireView())
            dismiss()
        }

    }
    private fun createChips(category:String, chipGroup: ChipGroup){
        val chip = Chip(requireContext()).apply {
            text = category
            isCloseIconVisible = true
            setCloseIconResource(R.drawable.outline_close_24)
            setOnCloseIconClickListener {
                chipGroup.removeView(this)
            }
            setEnsureMinTouchTargetSize(false)
        }
        val params = ChipGroup.LayoutParams(
            ChipGroup.LayoutParams.WRAP_CONTENT,
            ChipGroup.LayoutParams.WRAP_CONTENT
        )
        val v=resources.getDimensionPixelSize(R.dimen.chip_margin_vertical)
        params.updateMargins(v,v,v,v)
        chip.layoutParams = params


        chipGroup.addView(chip)
    }
}