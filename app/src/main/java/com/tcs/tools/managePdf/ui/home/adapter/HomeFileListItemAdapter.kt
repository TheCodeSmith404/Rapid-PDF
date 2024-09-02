package com.tcs.tools.managePdf.ui.home.adapter

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.FileItemHorizontalBinding
import com.tcs.tools.managePdf.databinding.FileItemHorizontalStrokeBinding
import data.room.FileEntity

class HomeFileListItemAdapter(private val pdfFiles: MutableList<FileEntity>,private val currId:Long,private val onItemClick: (FileEntity, Int, Int) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_HIGHLIGHTED = 1
    }

    inner class HomeFileListItemViewHolder(binding: FileItemHorizontalBinding) : RecyclerView.ViewHolder(binding.root) {
        val name = binding.itemName
        val size = binding.itemSize
        val time = binding.itemTime
        val option=binding.imageButtonOptions
        val root = binding.root
    }

    inner class HomeFileHighlightedItemViewHolder(binding: FileItemHorizontalStrokeBinding) : RecyclerView.ViewHolder(binding.root) {
        val name = binding.itemName
        val size = binding.itemSize
        val option=binding.imageButtonOptions
        val time = binding.itemTime
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HIGHLIGHTED) {
            val binding = FileItemHorizontalStrokeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HomeFileHighlightedItemViewHolder(binding)
        } else {
            val binding = FileItemHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HomeFileListItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val file = pdfFiles[position]

        if (holder is HomeFileListItemViewHolder) {
            holder.time.text = file.date
            holder.name.text = file.fileName
            holder.size.text = file.size
            holder.option.setOnClickListener{view->
                showPopupMenu(view,file,position)
            }
            holder.root.setOnClickListener {
                onItemClick(file,0,position)
            }
            holder.root.setOnLongClickListener{
                onItemClick(file,1,position)
                true
            }
        } else if (holder is HomeFileHighlightedItemViewHolder) {
            holder.time.text = file.date
            holder.name.text = file.fileName
            holder.size.text = file.size
            holder.option.setOnClickListener{view->
                showPopupMenu(view,file,position)
            }
            holder.root.setOnClickListener {
                onItemClick(file,0,position)
            }
            holder.root.setOnLongClickListener {
                onItemClick(file,1,position)
                true
            }
        }
    }
    private fun showPopupMenu(view: View,file:FileEntity,position: Int){
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.inflate(R.menu.file_item_menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.file_item_menu_rename -> {
                    onItemClick(file,2,position)
                    true
                }
                R.id.file_item_menu_delete -> {
                    onItemClick(file,3,position)
                    true
                }
                R.id.file_item_menu_fav->{
                    onItemClick(file,4,position)
                    true
                }
                R.id.file_item_remove->{
                    onItemClick(file,5,position)
                    true
                }
                else->{
                    false
                }
            }
        }
        popupMenu.show()
    }

    override fun getItemCount(): Int = pdfFiles.size

    override fun getItemViewType(position: Int): Int {
        return if (currId != -1L && currId == pdfFiles[position].id) {
            VIEW_TYPE_HIGHLIGHTED
        } else {
            VIEW_TYPE_NORMAL
        }
    }
    fun nameChanged(position: Int,name:String){
        pdfFiles[position].fileName=name
        notifyItemChanged(position)
    }
    fun removeItem(position: Int){
        pdfFiles.removeAt(position)
        notifyItemRemoved(position)
        Handler(Looper.getMainLooper()).postDelayed({
            notifyItemRangeChanged(position,itemCount)
        },200)
    }
    fun updateItems(newItems:MutableList<FileEntity>){
        Log.d("state","Home Files Update Item Triggered")
        val diffCallback=PdfListDiffCallback(pdfFiles,newItems)
        val diffResult= DiffUtil.calculateDiff(diffCallback)
        pdfFiles.clear()
        pdfFiles.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }
    inner class PdfListDiffCallback(val oldItems:MutableList<FileEntity>,val newItems: MutableList<FileEntity>):
        DiffUtil.Callback(){
        override fun getOldListSize(): Int {
            Log.d("state","oldListSize is ${oldItems.size}")
            return oldItems.size
        }

        override fun getNewListSize(): Int {
            Log.d("state","newListSize is ${newItems.size}")
            return newItems.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].id==newItems[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition]==newItems[newItemPosition]
        }
    }
}

