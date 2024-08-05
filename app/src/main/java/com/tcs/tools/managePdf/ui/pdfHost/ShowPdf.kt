package com.tcs.tools.managePdf.ui.pdfHost

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rajat.pdfviewer.PdfRendererView
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.FragmentShowPdfBinding
import data.room.FileEntity
import kotlinx.coroutines.launch
import java.io.File

class ShowPdf : Fragment() {
    private var _binding:FragmentShowPdfBinding?=null
    private val binding:FragmentShowPdfBinding
        get()=_binding?: FragmentShowPdfBinding.inflate(layoutInflater)
    private val  viewModel:ShowPdfViewModel by viewModels()
    private var scrolling=false
    private var currentPageGlobal=0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.init(requireContext())
        _binding= FragmentShowPdfBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val id = arguments?.getLong("file_id") ?: throw IllegalArgumentException("File ID is required")
        addMenu()
        lifecycleScope.launch {
            val file=viewModel.getFile(id)
            try {
                if (file.isFavorite)
                    binding.favImageButton.isSelected = true
                binding.pdfRenderer.initWithUri(file.uri)
                binding.pdfRenderer.statusListener=object :PdfRendererView.StatusCallBack{
                    override fun onPageChanged(currentPage: Int, totalPage: Int) {
                        if (!scrolling) {
                            scrolling=true
                            Handler(Looper.getMainLooper()).postDelayed({
                                binding.verticalSlider.post{
                                    binding.verticalSlider.updateProgress(currentPageGlobal)
                                    scrolling=false
                                }
                            },500)
                        }else{
                            currentPageGlobal=currentPage
                        }
                    }
                }
                binding.verticalSlider.updateMaxValue(binding.pdfRenderer.totalPageCount)
                binding.verticalSlider.setOnProgressChangeListener { page->
                    if(page!=1)
                        binding.pdfRenderer.jumpToPage(page)
                }
                binding.showPdfProgressBar.visibility = View.GONE
                binding.favImageButton.setOnClickListener {
                    if (file.isFavorite) {
                        Log.d(
                            "favFiles",
                            "Is file Fav:${file.isFavorite} and Is button Pressed:${binding.favImageButton.isPressed}"
                        )
                        binding.favImageButton.isSelected = false
                        file.isFavorite = false
                        lifecycleScope.launch {
                            viewModel.setFavFile(id, false)
                        }
                        Log.d(
                            "favFiles",
                            "Is file Fav:${file.isFavorite} and Is button Pressed:${binding.favImageButton.isPressed}"
                        )
                    } else {
                        Log.d(
                            "favFiles",
                            "Is file Fav:${file.isFavorite} and Is button Pressed:${binding.favImageButton.isPressed}"
                        )
                        binding.favImageButton.isSelected = true
                        file.isFavorite = true
                        lifecycleScope.launch {
                            viewModel.setFavFile(id, true)
                            viewModel.setHasFavFile()
                        }
                        Log.d(
                            "favFiles",
                            "Is file Fav:${file.isFavorite} and Is button Pressed:${binding.favImageButton.isPressed}"
                        )
                    }
                }
            }catch (_:IllegalArgumentException){
                binding.fileNotFoundError.visibility=View.VISIBLE
                binding.showPdfGroup.visibility=View.GONE
                binding.deleteFile.setOnClickListener{
                    viewModel.deleteFile(file)
                    findNavController().navigateUp()
                }
            }catch (_:SecurityException){
                Toast.makeText(requireContext(),"Password protected file cannot be opened",Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            } catch(e:Exception){
                Toast.makeText(requireContext(),"An unknown error occured",Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            }

        }


    }
    private fun addMenu(){
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_fav, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                Log.d("menu","Item Selected ${menuItem.title}")
                when (menuItem.itemId) {
                    R.id.optionManageCategoriesMenuFav -> {
                        Log.d("menu","Item Selected categories")
                        findNavController().navigate(R.id.show_pdf_to_manage_categories)
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

}