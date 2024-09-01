package com.tcs.tools.managePdf.ui.sortPdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.SortPdfItemBinding
import data.room.FileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext


class SortPdfAdapter(
    private val context: Context,
    private val numPages:Int,
    private val totalPdfFiles: List<FileEntity>
) : RecyclerView.Adapter<SortPdfAdapter.SortPdfViewHolder>() {
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var uri:Uri

    inner class SortPdfViewHolder(binding: SortPdfItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val viewBinding=binding
        val pdfName=binding.pdfName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SortPdfViewHolder {
        val binding = SortPdfItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SortPdfViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SortPdfViewHolder, position: Int) {
        val file=totalPdfFiles[position]
        uri=file.uri
        if(file.isDeleted){
            holder.viewBinding.fileDeleted.visibility=View.VISIBLE
            if(holder.viewBinding.pdfViewContainer.visibility!=View.GONE)
                holder.viewBinding.pdfViewContainer.visibility=View.GONE
            if(holder.viewBinding.uriError.visibility!=View.GONE)
                holder.viewBinding.uriError.visibility=View.GONE
            if(holder.viewBinding.securityError.visibility!=View.GONE)
                holder.viewBinding.securityError.visibility=View.GONE
        }else {
            if(holder.viewBinding.fileDeleted.visibility!=View.GONE)
                holder.viewBinding.fileDeleted.visibility=View.GONE
            if(holder.viewBinding.pdfViewContainer.visibility!=View.VISIBLE)
                holder.viewBinding.pdfViewContainer.visibility=View.VISIBLE
            holder.pdfName.text=file.fileName
            adapterScope.launch {
                renderPdfPages(uri, holder.viewBinding)
            }
        }
    }

    override fun getItemCount(): Int = totalPdfFiles.size
    fun setFileDeleted(position: Int){
        totalPdfFiles[position].isDeleted=true
        notifyItemChanged(position)
    }
    fun setFileName(position: Int,name:String){
        totalPdfFiles[position].fileName=name
    }

    private suspend fun renderPdfPages(pdfUri: Uri, binding: SortPdfItemBinding) {
        val linearLayoutPdfContainer=binding.pdfViewContainer
        val pdfView=binding.pdfView
        val fileDeleted=binding.fileDeleted
        binding.scrollView.scrollY=0
        val securityError=binding.securityError
        val uriError=binding.uriError
        var pdfRenderer: PdfRenderer? = null
        var fileDescriptor: ParcelFileDescriptor? = null
        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(pdfUri, "r")
            if (fileDescriptor != null) {
                pdfRenderer = PdfRenderer(fileDescriptor)

                // Remove any previously added views to avoid duplication
                withContext(Dispatchers.Main) {
                    if(linearLayoutPdfContainer.visibility!=View.VISIBLE)
                        linearLayoutPdfContainer.visibility=View.VISIBLE
                    pdfView.removeAllViews()
                    if(securityError.visibility!=View.GONE)
                        securityError.visibility=View.GONE
                    if(uriError.visibility!=View.GONE)
                        uriError.visibility=View.GONE
                }

                val pageCount = pdfRenderer.pageCount
                val pagesToRender = minOf(pageCount, numPages) // Ensure we do not exceed the available pages
                withContext(Dispatchers.Main){
                    binding.pageCount.text="Pages: $pageCount"
                }

                for (i in 0 until pagesToRender) {
                    val imageView = ImageView(context)
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
//                        val dimen = context.resources.getDimensionPixelSize(R.dimen.four_dp);
//                        layoutParams.setMargins(0, 0, 0, dimen)
                    imageView.layoutParams = layoutParams
                    imageView.adjustViewBounds = true
                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    imageView.setBackgroundColor(context.getColor(R.color.white))
                    imageView.isForceDarkAllowed = false
                    renderPageToImageView(pdfRenderer, i, imageView)
                    withContext(Dispatchers.Main) {
                        pdfView.addView(imageView)
                        // Create the divider view
                        val divider = View(context).apply {
                            id = View.generateViewId() // Generate a unique ID for the view
                            val layoutParamsDiv = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                resources.getDimensionPixelSize(R.dimen.divider_height) // Replace with your desired height
                            )
                            setBackgroundColor(ContextCompat.getColor(context,R.color.grey)) // Set background color to black
                            this.layoutParams=layoutParamsDiv
                        }
                        pdfView.addView(divider)
                    }
                }
            }else{
                Log.d("pdf","Descriptor is null")
            }
        } catch (e: SecurityException) {
            pdfRenderer?.close()
            fileDescriptor?.close()
            withContext(Dispatchers.Main) {
                linearLayoutPdfContainer.visibility=View.GONE
                securityError.visibility=View.VISIBLE
                if(fileDeleted.visibility!=View.GONE)
                    fileDeleted.visibility=View.GONE
                if(uriError.visibility!=View.GONE)
                    uriError.visibility=View.GONE
            }
        }catch(e:IllegalArgumentException){
            pdfRenderer?.close()
            fileDescriptor?.close()
            withContext(Dispatchers.Main){
                if(securityError.visibility!=View.GONE)
                    securityError.visibility=View.GONE
                if(fileDeleted.visibility!=View.GONE)
                    fileDeleted.visibility=View.GONE
                linearLayoutPdfContainer.visibility=View.GONE
                uriError.visibility=View.VISIBLE
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun renderPageToImageView(
        pdfRenderer: PdfRenderer,
        pageIndex: Int,
        imageView: ImageView
    ) {
        val page = pdfRenderer.openPage(pageIndex)
        val width = page.width
        val height = page.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        imageView.setImageBitmap(bitmap)
        page.close()
    }
    fun clearResources(){
        adapterScope.cancel()
    }
}

