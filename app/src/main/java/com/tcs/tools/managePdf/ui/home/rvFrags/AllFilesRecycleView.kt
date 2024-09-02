package com.tcs.tools.managePdf.ui.home.rvFrags

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.DialogRenameFileBinding
import com.tcs.tools.managePdf.databinding.FragmentAllFilesRecycleViewBinding
import com.tcs.tools.managePdf.ui.home.Home.Companion.TAG
import com.tcs.tools.managePdf.ui.home.adapter.HomeFileListItemAdapter
import com.tcs.tools.managePdf.ui.home.rvFrags.rvViewModels.RvViewModel
import com.tcs.tools.managePdf.utils.InputManager
import data.sharedPrefs.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AllFilesRecycleView : Fragment() {
    private var _binding:FragmentAllFilesRecycleViewBinding?=null
    val binding:FragmentAllFilesRecycleViewBinding
        get()=_binding!!
    private val rvViewModel: RvViewModel by viewModels(ownerProducer = {requireParentFragment()})
    private var mInterstitialAd: InterstitialAd? = null
    private val preferenceManager:PreferenceManager by lazy {
        PreferenceManager(requireContext())
    }
    private lateinit var adapter: HomeFileListItemAdapter
    private val dialogView:View by lazy{LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading,binding.root,false)}
    val dialog: AlertDialog by lazy {
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(requireActivity())
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding=FragmentAllFilesRecycleViewBinding.inflate(layoutInflater,container,false)
        observeState()
        return binding.root
    }
    private fun observeState(){
        lifecycleScope.launch {
            rvViewModel.state.collect{
                when(it){
                    is RvState.AllFiles->{
                        if(::adapter.isInitialized) {
                            val files = rvViewModel.getAllFilesList(false)
                            adapter.updateItems(files)
                            Log.d("states", "state is RvState.AllFiles in All Files")
                        }else
                            Log.d("states", "state is RvState.AllFiles in All Files and adapter not initialized")
                        rvViewModel.state.value = RvState.Idle
                    }
                    is RvState.FavFiles->{
                        Log.d("states","state is RvState.FavFiles in All Files")
                    }
                    is RvState.Idle->{
                        Log.d("states","state is RvState.Idle in All Files")
                    }
                }
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAll.layoutManager=LinearLayoutManager(requireContext())
        lifecycleScope.launch {
            rvViewModel.getAllFilesList()
            val id=preferenceManager.currFileId
            rvViewModel.files.observe(viewLifecycleOwner) { files ->
                Log.d("reload", "files changed")
                if(files.isEmpty()) {
                    binding.rvAll.visibility = View.GONE
                    binding.rvText.visibility = View.VISIBLE
                }else {
                    binding.rvText.visibility=View.GONE
                    adapter=HomeFileListItemAdapter(files, id) { file, type, position ->
                        val bundle = Bundle().apply {
                                putLong("file_id", file.id)
                        }
                        if (type == 0) {
                            Log.d("AdDelay", "Type 0")
                            dialog.show()
                            val currentTimeMillis: Long = System.currentTimeMillis()
                            val lastTime = rvViewModel.lastAdShown
                            Log.d("AdDelay", "Fetched last Ad show")
                            //TODO remove false condition
                            if (currentTimeMillis - lastTime > SECONDS) {
                                Log.d("AdDelay", "Ad request Start")
                                val adRequest = AdRequest.Builder().build()
                                InterstitialAd.load(
                                    requireContext(),
                                    "ca-app-pub-3906662861593519/7554687549",
                                    adRequest,
                                    object : InterstitialAdLoadCallback() {
                                        override fun onAdFailedToLoad(adError: LoadAdError) {
                                            Log.d(TAG, adError.toString())
                                            dialog.dismiss()
                                            Log.d("AdDelay", "Ad failed to load")
                                            mInterstitialAd = null
                                            findNavController().navigate(R.id.sortPdf, bundle)
                                        }

                                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                                            dialog.dismiss()
                                            mInterstitialAd = interstitialAd
                                            rvViewModel.lastAdShown = currentTimeMillis
                                            findNavController().navigate(R.id.sortPdf, bundle)
                                            Log.d(TAG, "Ad was loaded.")
                                            Log.d("AdDelay", "Ad Loaded")

                                            mInterstitialAd?.fullScreenContentCallback =
                                                object : FullScreenContentCallback() {
                                                    override fun onAdDismissedFullScreenContent() {
                                                        Log.d(
                                                            TAG,
                                                            "Ad dismissed fullscreen content."
                                                        )
                                                        mInterstitialAd = null
                                                    }
                                                }
                                            mInterstitialAd?.show(requireActivity())
                                        }
                                    }
                                )
                            } else {
                                dialog.dismiss()
                                findNavController().navigate(R.id.sortPdf, bundle)
                            }
                        }else if(type == 2) {
                            val alertView=DialogRenameFileBinding.inflate(layoutInflater,binding.root,false)
                            val alertDialog=AlertDialog.Builder(requireContext())
                                .setView(alertView.root)
                                .setCancelable(false)
                                .create()
                            alertView.reName.setText(file.fileName)
                            alertView.reName.requestFocus()
                            InputManager.showKeyboard(requireContext(),alertView.reName)
                            alertView.dialogSave.setOnClickListener{
                                val name=alertView.reName.text.toString()
                                if(name.isNotEmpty()){
                                    if(name != file.fileName){
                                        lifecycleScope.launch {
                                            rvViewModel.renameFile(
                                                requireContext(),
                                                file.id,
                                                name
                                            )
                                        }
                                        alertView.reName.clearFocus()
                                        InputManager.hideKeyboard(requireContext(),alertView.root)
                                        alertDialog.cancel()
                                        adapter.nameChanged(position,name)
                                        showToast("File renamed")
                                    }else
                                        showToast("File name is not changed",false)
                                }else
                                    showToast("File name can not be empty",false)
                            }
                            alertView.dialogClose.setOnClickListener{
                                alertView.reName.clearFocus()
                                InputManager.hideKeyboard(requireContext(),alertView.root)
                                alertDialog.cancel()
                            }
                            alertDialog.show()
                        } else if(type == 3) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Delete Current File")
                                .setMessage("Are you sure you want to delete this file?")
                                .setCancelable(false)
                                .setPositiveButton("Delete") { dialog, which ->
                                    lifecycleScope.launch {
                                        rvViewModel.deleteFile(requireContext(),file){deleted->
                                            if(deleted){
                                                lifecycleScope.launch {
                                                    withContext(Dispatchers.Main) {
                                                        dialog.dismiss()
                                                        adapter.removeItem(position)
                                                        showToast("File Deleted")
                                                    }
                                                }
                                            }else{
                                                lifecycleScope.launch {
                                                    withContext(Dispatchers.Main) {
                                                        dialog.dismiss()
                                                        showToast("Failed to delete file! Retry")
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
                        } else if(type == 4) {
                            lifecycleScope.launch {
                                withContext(Dispatchers.Main) {
                                    rvViewModel.addPdfToFavorites(file.id, true)
                                    rvViewModel.favFilesChanged=true
                                    adapter.removeItem(position)
                                }
                            }
                        } else if(type==5){
                            lifecycleScope.launch {
                                rvViewModel.removeFile(file)
                                withContext(Dispatchers.Main){
                                    adapter.removeItem(position)
                                }
                            }
                        } else {
                            findNavController().navigate(R.id.show_pdf, bundle)
                        }
                        Log.d(TAG, file.toString())
                    }
                    binding.rvAll.adapter =adapter
                }

            }
        }
    }
    private fun showToast(text:String,long:Boolean=true){
        if(long) {
            Toast.makeText(requireContext(),text,Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(requireContext(),text,Toast.LENGTH_SHORT).show()
        }
    }
    companion object{
        private const val SECONDS=1800000L
    }

}