package com.tcs.tools.managePdf.ui.home.rvFrags

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.DialogRenameFileBinding
import com.tcs.tools.managePdf.databinding.FragmentAllFilesRecycleViewBinding
import com.tcs.tools.managePdf.databinding.FragmentFavoriteFilesRecycleViewBinding
import com.tcs.tools.managePdf.ui.home.Home
import com.tcs.tools.managePdf.ui.home.HomeViewModel
import com.tcs.tools.managePdf.ui.home.adapter.HomeFavFileListItemAdapter
import com.tcs.tools.managePdf.ui.home.rvFrags.rvViewModels.RvViewModel
import data.room.FileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FavoriteFilesRecycleView : Fragment() {
    private var _binding: FragmentFavoriteFilesRecycleViewBinding?=null
    val binding:FragmentFavoriteFilesRecycleViewBinding
        get()=_binding!!
    private val rvViewModel: RvViewModel by viewModels(ownerProducer = {requireParentFragment()})
    private lateinit var adapter: HomeFavFileListItemAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding=FragmentFavoriteFilesRecycleViewBinding.inflate(inflater,container, false)
        observeState()
        return binding.root
    }
    private fun observeState(){
        lifecycleScope.launch {
            rvViewModel.state.collect {
                when (it) {
                    is RvState.FavFiles -> {
                        if (::adapter.isInitialized) {
                            lifecycleScope.launch {
                                val data = rvViewModel.getFavFilesList(false) // Assuming getAllFilesList returns a List<FileEntity>
                                adapter.updateItems(data)
                                Log.d("states", "state is RvState.FavFiles in Fav Files with data size: ${data.size}")
                            }
                        } else
                            Log.d("states", "state is RvState.FavFiles in Fav Files and adapter is not initialized")
                        rvViewModel.state.value = RvState.Idle
                    }
                    is RvState.AllFiles -> {
                        Log.d("states","state is RvState.AllFiles in Fav Files")

                    }
                    is RvState.Idle->{
                        Log.d("states","state is RvState.Idle in Fav Files")

                    }
                }
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvFav.layoutManager=LinearLayoutManager(requireContext())
        lifecycleScope.launch {
            rvViewModel.getFavFilesList()
            rvViewModel.favFiles.observe(viewLifecycleOwner) { files ->
                if (files.isEmpty()) {
                    binding.rvFav.visibility=View.GONE
                    binding.rvText.visibility=View.VISIBLE
                } else {
                    binding.rvText.visibility=View.GONE
                    val onClick:(FileEntity,Int,Int)->Unit= { file, type, position ->
                        when (type) {
                            0 -> {
                                findNavController().navigate(
                                    R.id.action_home_to_showPdf,
                                    bundleOf("file_id" to file.id)
                                )
                            }
                            1 -> {
                                val alertView =
                                    DialogRenameFileBinding.inflate(
                                        layoutInflater,
                                        binding.root,
                                        false
                                    )
                                val alertDialog = AlertDialog.Builder(requireContext())
                                    .setView(alertView.root)
                                    .setCancelable(false)
                                    .create()
                                alertView.dialogSave.setOnClickListener {
                                    val name = alertView.reName.text.toString()
                                    if (name.isNotEmpty()) {
                                        if (name != file.fileName) {
                                            lifecycleScope.launch {
                                                rvViewModel.renameFile(
                                                    requireContext(),
                                                    file.id,
                                                    name
                                                )
                                            }
                                            alertDialog.cancel()
                                            files[position].fileName = name
                                            adapter.nameChanged(position,name)
                                        } else
                                            Toast.makeText(
                                                alertView.root.context,
                                                "File name is not changed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                    } else
                                        Toast.makeText(
                                            alertView.root.context,
                                            "File name can not be empty",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                }
                                alertView.dialogClose.setOnClickListener {
                                    alertDialog.cancel()
                                }
                                alertDialog.show()
                            }

                            2 -> {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Delete Current File")
                                    .setMessage("Are you sure you want to delete this file?")
                                    .setCancelable(false)
                                    .setPositiveButton("Delete") { dialog, which ->
                                        lifecycleScope.launch {
                                            rvViewModel.deleteFile(
                                                requireContext(),
                                                file.id
                                            ) { deleted ->
                                                if (deleted) {
                                                    lifecycleScope.launch {
                                                        withContext(Dispatchers.Main) {
                                                            files.removeAt(position)
                                                            adapter.removeItem(position)
                                                        }
                                                    }

                                                } else {
                                                    lifecycleScope.launch {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                requireContext(),
                                                                "Failed to delete file! Retry",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            }

                                        }
                                    }
                                    .setNegativeButton("Cancel") { dialog, which ->
                                        dialog.dismiss() // Dismiss the dialog
                                    }
                                    .show()
                            }

                            else -> {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.Main) {
                                        rvViewModel.addPdfToFavorites(file.id, false)
                                        rvViewModel.filesChanged=true
                                        adapter.removeItem(position)
                                    }
                                }
                            }
                        }
                    }
                    adapter=HomeFavFileListItemAdapter(files,onClick)
                    binding.rvFav.adapter =adapter
                }
            }
        }
    }
}