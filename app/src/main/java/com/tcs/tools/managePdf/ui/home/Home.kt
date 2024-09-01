package com.tcs.tools.managePdf.ui.home

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.perf.FirebasePerformance
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.FragmentHomeBinding
import com.tcs.tools.managePdf.ui.baseActivity.HomeState
import com.tcs.tools.managePdf.ui.baseActivity.HomeStateIntent
import com.tcs.tools.managePdf.ui.baseActivity.MainActivityViewModel
import com.tcs.tools.managePdf.ui.home.adapter.HomeFavFileListItemAdapter
import com.tcs.tools.managePdf.ui.home.adapter.HomeFileListItemAdapter
import com.tcs.tools.managePdf.ui.home.adapter.ViewPagerAdapter
import com.tcs.tools.managePdf.ui.home.rvFrags.RvState
import com.tcs.tools.managePdf.ui.home.rvFrags.rvViewModels.RvViewModel
import data.sharedPrefs.PreferenceManager
import kotlinx.coroutines.launch


class Home : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private val viewModel: MainActivityViewModel by activityViewModels()
    private val rvViewModel:RvViewModel by viewModels()
    private lateinit var fab:FloatingActionButton
    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager(context ?: requireContext())
    }
    private val requestStorageAccessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                val takeFlags = result.data?.flags?.and(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION )
                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags ?: 0)
                preferenceManager.currentUri=uri.toString()
                preferenceManager.permissionGranted = true
                loadPdfFilesFromUri()
            }
        }else{
            //Todo Implement error handling if there is an issue when getting uri
        }
    }
    private val trace=FirebasePerformance.getInstance().newTrace("home_start_time")
    private val dialogView:View by lazy{LayoutInflater.from(context).inflate(R.layout.dialog_loading,null)}
    private val dialog:AlertDialog by lazy {
        AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        trace.start()
        super.onCreate(savedInstanceState)
        MobileAds.initialize(requireActivity())
        homeViewModel.init(requireContext())
        rvViewModel.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf("62535BA0602010401DBCC361FA4C5551")) // Replace with your actual test device ID
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fab=requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        if(homeViewModel.fistStart){
            viewModel.loading=false
            findNavController().navigate(R.id.action_home_to_on_boarding)
        } else if (homeViewModel.permissionGranted) {
            loadAds()
            binding.requestStorageAccessContainer.visibility=View.GONE
            binding.filesContainer.visibility=View.VISIBLE
            setUpAdapter()
            if(fab.visibility==View.GONE)
                fab.visibility=View.VISIBLE
            viewModel.loading=false
        } else {
            fab.visibility=View.GONE
            showRequestStorageAccess()
        }
        addMenu()
        setOnClickListeners()
        observeState()
        trace.stop()
    }
    private fun observeState(){
        lifecycleScope.launch {
            viewModel.pdfFilesFlow.collect{
                when(it){
                    is HomeState.Idle->{}
                    is HomeState.Reload->{
                        Log.d("files","Triggered")
                        setUpAdapter()
                        viewModel.channel.send(HomeStateIntent.TriggerIdle)
                    }
                }
            }
        }
    }
    private fun loadAds(){
        val adView= AdView(requireContext())
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId="ca-app-pub-3906662861593519/5336869694"
        binding.adContainerView.removeAllViews()
        binding.adContainerView.visibility=View.VISIBLE
        binding.adContainerView.addView(adView)
        val adRequest = AdRequest.Builder().build()
        adView.adListener=object : AdListener(){
            override fun onAdLoaded() {
                super.onAdLoaded()
                Log.d("ad","Ad Loaded")
            }
        }
        adView.loadAd(adRequest)
    }
    private fun setUpAdapter(){
        binding.viewpager.adapter=ViewPagerAdapter(this)
        dialog.hide()
        binding.viewpager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                Log.d("states","Page Changed to $position")
                super.onPageSelected(position)
                if(rvViewModel.filesChanged){
                    rvViewModel.state.value=RvState.AllFiles
                    rvViewModel.filesChanged=false
                    Log.d("states","files are changed")
                }
                if(rvViewModel.favFilesChanged){
                    rvViewModel.state.value=RvState.FavFiles
                    rvViewModel.favFilesChanged=false
                    Log.d("states","fav files are changed")
                }
            }
        })
        TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            tab.text = when (position) {
                0 -> "All Files"
                1 -> "Favorites"
                else -> null
            }
        }.attach()
    }
    private fun addMenu(){
        viewModel.init(requireContext())
        var menuLocal:Menu?=null
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_home, menu)
                val num=homeViewModel.pagesPerFile
                menuLocal=menu
                val id=when(num){
                    1->R.id.optionPages1
                    3->R.id.optionPages3
                    else->R.id.optionPages2
                }
                menu.findItem(id).isChecked=true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                Log.d("menu","Item Selected ${menuItem.title}")
                when (menuItem.itemId) {
                    R.id.optionManageCategoriesMenu -> {
                        Log.d("menu","Item Selected categories")
                        findNavController().navigate(R.id.action_home_to_manage_categories)

                        return true
                    }
                    R.id.optionPages1->{
                        menuItem.isChecked=true
                        homeViewModel.pagesPerFile=1
                        menuLocal?.findItem(R.id.optionPages2)?.isChecked=false
                        menuLocal?.findItem(R.id.optionPages3)?.isChecked=false
                        return true
                    }
                    R.id.optionPages2->{
                        menuItem.isChecked=true
                        homeViewModel.pagesPerFile=2
                        menuLocal?.findItem(R.id.optionPages1)?.isChecked=false
                        menuLocal?.findItem(R.id.optionPages3)?.isChecked=false
                        return true
                    }
                    R.id.optionPages3->{
                        menuItem.isChecked=true
                        homeViewModel.pagesPerFile=3
                        menuLocal?.findItem(R.id.optionPages1)?.isChecked=false
                        menuLocal?.findItem(R.id.optionPages2)?.isChecked=false
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



    private fun showRequestStorageAccess() {
        viewModel.loading = false
        binding.requestStorageAccessContainer.visibility = View.VISIBLE
        binding.filesContainer.visibility = View.GONE
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.requestAccess.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/document/primary:"))
            }
            requestStorageAccessLauncher.launch(intent)
            dialog.show()
        }
    }
    private fun loadPdfFilesFromUri() {
        if(!dialog.isShowing){
            dialog.show()
        }
        Log.d(TAG,"Loading files")
        binding.requestStorageAccessContainer.visibility = View.GONE
        //TODO set a loading animation or bar to load files
        homeViewModel.loadPdfFilesFromUri(requireContext().contentResolver) { result ->
           if(result){
               binding.filesContainer.visibility=View.VISIBLE
               fab.visibility=View.VISIBLE
               setUpAdapter()
               Log.d(TAG,"Files Loaded Set Adapter")
           }else{
               val snackBar=Snackbar.make(binding.root,"Unable to load files from folder",Snackbar.LENGTH_LONG)
               snackBar.setAction("Retry"){
                   loadPdfFilesFromUri()
               }
               snackBar.show()
               Log.d(TAG," Files Not loaded Failure")
           }
        }
    }

    companion object{
        const val TAG="Home.kt"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
