package com.tcs.tools.managePdf.ui.baseActivity

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.ActivityMainBinding
import com.tcs.tools.managePdf.ui.home.Home.Companion.TAG
import data.sharedPrefs.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    private val viewModel:MainActivityViewModel by viewModels()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager(this)
    }
    private val trace=FirebasePerformance.getInstance().newTrace("main_activity_start_up")
    private val dialogView:View by lazy{LayoutInflater.from(this).inflate(R.layout.dialog_loading,null)}
    private val dialog:AlertDialog by lazy {
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }
    private val requestStorageAccessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                val takeFlags = result.data?.flags?.and(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION )
                this.contentResolver.takePersistableUriPermission(uri, takeFlags ?: 0)
                preferenceManager.currentUri=uri.toString()
                preferenceManager.permissionGranted = true
                loadPdfFilesFromUri()
            }
        }else{
            hideLoadingDialog()
            //Todo Implement error handling if there is an issue when getting uri
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        trace.start()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            initializeServices()
        }
        installSplashScreen().apply {
            this.setKeepOnScreenCondition{
                viewModel.loading
            }
        }
        window.statusBarColor=ContextCompat.getColor(this,R.color.white)
        viewModel.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.appBarMain.toolbar)
        setOnClickListners()
        setUpDrawerAndNavigation()
        trace.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    private fun initializeServices(){
        MobileAds.initialize(this)
    }
    private fun setUpDrawerAndNavigation() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, binding.appBarMain.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.home, R.id.sortPdf
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateActionBar(destination.id)
        }
        setupNavigationView()
    }
    private fun loadPdfFilesFromUri() {
        Log.d(TAG,"Loading files")
        //TODO set a loading animation or bar to load files
        viewModel.loadPdfFilesFromUri(this.contentResolver) { result ->
            if(true){
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        Log.d("files","Sending Intent")
                        viewModel.channel.send(HomeStateIntent.TriggerReload)
                        hideLoadingDialog()
                    }
                }

            }else{
                Log.d(TAG," Files Not loaded Failure")
            }
        }
    }
    private fun updateActionBar(destinationId: Int) {
        val actionBar = supportActionBar
        if (actionBar != null) {
            when(destinationId) {
                R.id.home,R.id.manage_dialog-> {
                    // Show hamburger menu icon
                    binding.appBarMain.fab.visibility = View.VISIBLE
                    binding.appBarMain.appBarLayout.visibility = View.VISIBLE
                    drawerToggle.isDrawerIndicatorEnabled = true
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    drawerToggle.syncState()
                }
                R.id.on_boarding->{
                    hideFab()
                    binding.appBarMain.appBarLayout.visibility = View.GONE
                    binding.appBarMain.fab.visibility = View.GONE
                    drawerToggle.isDrawerIndicatorEnabled = false
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    drawerToggle.syncState()
                }
                else-> {
                    hideFab()
                    binding.appBarMain.appBarLayout.visibility = View.VISIBLE
                    binding.appBarMain.fab.visibility = View.GONE
                    val icon=ContextCompat.getDrawable(this,R.drawable.baseline_arrow_back_24)
                    drawerToggle.setHomeAsUpIndicator(icon)
                    drawerToggle.setToolbarNavigationClickListener {
                        navHostFragment.navController.navigateUp()
                    }
                    drawerToggle.isDrawerIndicatorEnabled = false
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    drawerToggle.syncState()
                }
            }
        }

    }
    private fun openMail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // Only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf("contact.thecodesmith@gmail.com")) // Recipient email address
            putExtra(Intent.EXTRA_SUBJECT, "Query regarding Rapid PDF") // Optional: pre-filled body
        }
        startActivity(Intent.createChooser(intent,"Contact Us"))
    }
    override fun onSupportNavigateUp(): Boolean {
        return (navHostFragment.navController.navigateUp()
                || super.onSupportNavigateUp())
    }
    private fun setOnClickListners(){
        binding.appBarMain.fab.setOnClickListener { view ->
            Log.d("fab","fab clicked")
            if(binding.appBarMain.fab1.visibility==View.GONE) {
                Log.d("fab","fab show")
                showFab()
            }
            else {
                Log.d("fab","fab hide")
                hideFab()
            }
        }
        binding.appBarMain.fab2.setOnClickListener{
            hideFab()
            optionAddFiles()
        }
        binding.appBarMain.fab1.setOnClickListener{
            hideFab()
            optionReloadFiles()
        }
        binding.appBarMain.fab3.setOnClickListener{
            hideFab()
            optionChangeDirectory()
        }
    }
    private fun showFab() {
        Log.d("fab","fab show fn")
        binding.appBarMain.fab.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.baseline_remove_24))
        animateFabShow(binding.appBarMain.fab1)
        animateFabShow(binding.appBarMain.fab2)
        animateFabShow(binding.appBarMain.fab3)
    }
    private fun animateFabShow(floatingActionButton: FloatingActionButton){
        Log.d("fab","fab animate fn")
        floatingActionButton.animate().withStartAction { floatingActionButton.visibility=View.VISIBLE }.translationY(0f).alpha(1f).setDuration(300).start()
    }
    private fun hideFab() {
        Log.d("fab","fab hide fn")
        binding.appBarMain.fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.baseline_add_24))
        animateFabHide(binding.appBarMain.fab1)
        animateFabHide(binding.appBarMain.fab2)
        animateFabHide(binding.appBarMain.fab3)
    }
    private fun animateFabHide(floatingActionButton: FloatingActionButton){
        Log.d("fab","fab hide fn animate")
        floatingActionButton.animate().translationY(200f).alpha(0f).setDuration(300).withEndAction{floatingActionButton.visibility=View.GONE}.start()
    }


    private fun setupNavigationView() {
        val context=this
        val navigationView: NavigationView = binding.navView
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.itemRemoveAds -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Upgrade to Premium")
                        .setMessage("Currently we do not offer premium services.\nThanks for showing interest!")
                        .setCancelable(false)
                        .setPositiveButton("OK"){dialog,_->
                            dialog.dismiss()
                        }
                        .show()
                }
                R.id.optionManageCategories -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        navHostFragment.navController.navigate(R.id.action_home_to_manage_categories) }, 220)
                }
                R.id.optionPagesPerFile -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        showPagesDialog() }, 220)
                }
                R.id.optionReloadFiles -> {
                   optionReloadFiles()
                }
                R.id.optionAddFiles -> {
                    optionAddFiles()
                }
                R.id.optionChangeDirectory -> {
                    optionChangeDirectory()
                }
                R.id.optionContactUs -> {
                    openMail()
                }
                R.id.optionWriteReview -> {
                    Toast.makeText(this,"Thanks :)",Toast.LENGTH_LONG).show()
                    optionOpenAppRating()
                }
                R.id.optionOpenTutorial->{
                    navHostFragment.navController.navigate(R.id.on_boarding)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

    }
    private fun optionOpenAppRating(){
        val packageName = this.packageName
        val uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)

        // To count with Play market backstack, after pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        try {
            this.startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            this.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
        }
    }
    private fun optionChangeDirectory(){
        showDialogWithActions("Change Folder", "Load files from new folder?", "Change")
        {dialogInterface ->
            showLoadingDialog()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    Uri.parse("content://com.android.externalstorage.documents/document/primary:")
                )
            }
            dialogInterface.dismiss()
            viewModel.deleteAllFiles()
            requestStorageAccessLauncher.launch(intent)
        }
    }
    private fun optionReloadFiles(){
        showDialogWithActions("Reload Files","Update files from last loaded folder?","Reload")
        {dialogInterface ->
            showLoadingDialog()
            viewModel.deleteAllFiles()
            dialogInterface.dismiss()
            loadPdfFilesFromUri()
        }
    }
    private fun optionAddFiles(){
        showDialogWithActions("Add Files", "Add new files from different folder?", "Add files"){
                dialogInterface ->
            showLoadingDialog()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    Uri.parse("content://com.android.externalstorage.documents/document/primary:")
                )
            }
            dialogInterface.dismiss()
            requestStorageAccessLauncher.launch(intent)
        }
    }
    private fun showDialogWithActions(title:String,message:String,positiveText:String,positiveAction:(DialogInterface)->Unit){
        if(viewModel.getCurrentUri()!="null"){
            MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(positiveText) { dialog, _ ->
                    showLoadingDialog()
                    positiveAction(dialog)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }else{
            Toast.makeText(this,"Please select files to Begin",Toast.LENGTH_LONG).show()
        }
    }
    private fun showPagesDialog() {
        // Inflate the custom layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pages_per_pdf, null)

        // Create the AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        val pages=viewModel.getPagesPerFile()
        val id=when(pages) {
            1->R.id.radioPage1
            3->R.id.radioPage3
            else->R.id.radioPage2
        }
        dialogView.findViewById<RadioButton>(id).isChecked=true
        val dialogCloseButton = dialogView.findViewById<Button>(R.id.dialogClose)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroup)
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioPage1 -> {
                    viewModel.setPagesPerFile(1)
                    dialog.dismiss()
                }
                R.id.radioPage2 -> {
                    // Update the ViewModel or handle the checked change for 2 pages
                    viewModel.setPagesPerFile(2)
                    dialog.dismiss()
                }
                R.id.radioPage3 -> {
                    // Update the ViewModel or handle the checked change for 3 pages
                    viewModel.setPagesPerFile(3)
                    dialog.dismiss()
                }
            }
        }
        // Set click listener for the close button
        dialogCloseButton.setOnClickListener {
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }
    private fun showLoadingDialog(){
        dialog.show()

    }
    private fun hideLoadingDialog(){
        dialog.hide()
    }
}
