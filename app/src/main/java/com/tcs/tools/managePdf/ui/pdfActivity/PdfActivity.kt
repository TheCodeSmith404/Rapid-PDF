package com.tcs.tools.managePdf.ui.pdfActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.perf.FirebasePerformance
import com.rajat.pdfviewer.PdfRendererView
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.ActivityPdfBinding
import com.tcs.tools.managePdf.ui.baseActivity.MainActivity
import kotlinx.coroutines.launch


class PdfActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfBinding
    private val viewModel:PdfActivityViewModel by viewModels()
    private val trace=FirebasePerformance.getInstance().newTrace("pdf_activity_load_time")
    private var scrolling=false
    private var currentPageGlobal=0
    override fun onCreate(savedInstanceState: Bundle?) {
        trace.start()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.title="Rapid PDF"
        val intent = intent
        viewModel.init(this)
        if (Intent.ACTION_VIEW == intent.action) {
            val pdfUri = intent.data
            viewModel.fileUri=pdfUri
            if (pdfUri != null) {
                try {
                    val takeFlags =
                        intent.flags.and(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    this.contentResolver.takePersistableUriPermission(pdfUri, takeFlags ?: 0)
                }catch (e:SecurityException){
                    binding.favImageButton.visibility=View.GONE
                }
                openPdf(pdfUri)
            }
        }
        setUpScrollBar()
        trace.stop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.share_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.shareFileActivity->{
                if(viewModel.fileUri!=null) {
                    val intent = Intent()
                    intent.setAction(Intent.ACTION_SEND)
                    intent.setType("application/pdf")
                    intent.putExtra(Intent.EXTRA_STREAM, viewModel.fileUri)
                    startActivity(Intent.createChooser(intent,"Share File"))
                }
                else{
                    Toast.makeText(baseContext,"Unable to share file",Toast.LENGTH_SHORT).show()
                }
                true

            }

            else->{
                false
            }
        }
    }
    private fun setUpScrollBar(){
        binding.pdfRenderer.statusListener=object : PdfRendererView.StatusCallBack{
            override fun onPageChanged(currentPage: Int, totalPage: Int) {
                if (!scrolling) {
                    scrolling=true
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.verticalSliderActivity.post{
                            binding.verticalSliderActivity.updateProgress(currentPageGlobal)
                            scrolling=false
                        }
                    },500)
                }else{
                    currentPageGlobal=currentPage+1
                }
            }
        }
        binding.verticalSliderActivity.updateMaxValue(binding.pdfRenderer.totalPageCount)
        binding.verticalSliderActivity.setOnProgressChangeListener { page->
            binding.pdfRenderer.jumpToPage(page-1)
        }
    }
    private fun openPdf(pdfUri: Uri) {
        val pdfView = binding.pdfRenderer
        try {
            pdfView.initWithUri(pdfUri)
        }catch (_:SecurityException){
            Toast.makeText(this,"Password protected file can not be opened",Toast.LENGTH_LONG).show()
            val intent=Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        val intent=Intent(this,MainActivity::class.java)
        startActivity(intent)
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}