package com.tcs.tools.managePdf.ui.home.adapter

import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.FavFileItemHorizontalBinding
import data.room.FileEntity
import android.os.Handler
import android.util.Log


class HomeFavFileListItemAdapter(private val pdfFiles: MutableList<FileEntity>, private val onItemClick: (FileEntity,Int,Int) -> Unit) : RecyclerView.Adapter<HomeFavFileListItemAdapter.HomeFavFileListItemViewHolder>() {

    inner class HomeFavFileListItemViewHolder(binding: FavFileItemHorizontalBinding) : RecyclerView.ViewHolder(binding.root) {
        val name = binding.itemName
        val size = binding.itemSize
        val time = binding.itemTime
        val option=binding.imageButtonOptions
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeFavFileListItemViewHolder {
        val binding = FavFileItemHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeFavFileListItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeFavFileListItemViewHolder, position: Int) {
        val file = pdfFiles[position]
        holder.name.text=file.fileName
        holder.time.text=file.date
        holder.size.text=file.size
        holder.root.setOnClickListener{
            onItemClick(file,0,position)
        }
        holder.option.setOnClickListener{view->
            showMenu(view,file,position)
        }
    }

    override fun getItemCount(): Int = pdfFiles.size
    private fun showMenu(view: View,file:FileEntity,position: Int){
        val popupMenu=PopupMenu(view.context,view)
        popupMenu.inflate(R.menu.fav_file_item_menu)
        popupMenu.setOnMenuItemClickListener { menuItem->
            when (menuItem.itemId) {
                R.id.fav_file_item_menu_rename -> {
                    onItemClick(file,1,position)
                    true
                }
                R.id.fav_file_item_menu_delete -> {
                    onItemClick(file,2,position)
                    true
                }
                R.id.fav_file_item_menu_fav->{
                    onItemClick(file,3,position)
                    true
                }
                else->{
                    false
                }
            }
        }
        popupMenu.show()
    }
    fun nameChanged(position: Int,newName:String){
        pdfFiles[position].fileName=newName
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
        Log.d("state","Fav Files Update Item Triggered")
        val diffCallback=PdfListDiffCallback(pdfFiles,newItems)
        val diffResult=DiffUtil.calculateDiff(diffCallback)
        pdfFiles.clear()
        pdfFiles.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }
    inner class PdfListDiffCallback(val oldItems:MutableList<FileEntity>,val newItems: MutableList<FileEntity>):DiffUtil.Callback(){
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