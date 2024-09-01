package com.tcs.tools.managePdf.ui.sortPdf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.repository.Repository
import data.room.AppDatabase
import data.room.FileEntity
import data.sharedPrefs.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


class SortPdfViewModel:ViewModel() {
    private lateinit var repository: Repository
    var rapidMode=false
    fun init(context: Context){
        val fileDao = AppDatabase.getDatabase(context).fileDao()
        repository= Repository(context,fileDao,PreferenceManager(context))
    }
    private lateinit var allPdfFiles:List<FileEntity>
    var sizePdf=0
    var currentPosition=0
    var currentId=0L
    var currentUri:Uri?=null
    var currentName:String=""
    var pagerChangeState =false

    suspend fun getAllFilesAfterId(id:Long):MutableLiveData<List<FileEntity>>{
        val files=MutableLiveData<List<FileEntity>>()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                allPdfFiles=repository.getFilesAfterId(id)
                withContext(Dispatchers.Main) {
                    files.value = allPdfFiles
                }
            }
        }
        return files
    }
    suspend fun getAllCategories():MutableSet<String>{
        return withContext(Dispatchers.IO){
            repository.getCategories()
        }
    }

    suspend fun renameFile(context: Context, id:Long, newName: String) {
        Log.d("uri","trying to rename file")
        return withContext(Dispatchers.IO) {
            try {
                val file=repository.getFileById(id)
                val fileUri=file.uri
                val renamedUri = DocumentsContract.renameDocument(context.contentResolver, fileUri, "$newName.pdf")
                Log.d("uri","$fileUri to $renamedUri")
                renamedUri?: throw IOException("Failed to rename file")
                file.uri=renamedUri
                file.fileName=newName
                repository.updateFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    // Method to delete the PDF file
    suspend fun deleteFile(context: Context, id: Long) {
        withContext(Dispatchers.IO) {
            try {
                val file = repository.getFileById(id)
                val fileUri = file.uri
                val deleted = DocumentsContract.deleteDocument(context.contentResolver, fileUri)

                if (deleted) {
                    Log.d("uri", "File deleted successfully: $fileUri")
                    repository.deleteFile(file)
                } else {
                    throw IOException("Failed to delete file")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    var hideLeftRight:Boolean
        get(){
            pagerChangeState=repository.hideLeftRight
            return pagerChangeState
        }
        set(value){
            pagerChangeState=value
            repository.hideLeftRight=value
        }

    // Method to add the PDF file to favorites
    suspend fun addPdfToFavorites(id:Long) {
        withContext(Dispatchers.IO) {
            repository.hasFavList=true
            repository.setFavoriteFile(id,true)
        }
    }
    suspend fun saveLastViewedFileId(id:Long){
        withContext(Dispatchers.IO){
            repository.currFileIndex=id
        }
    }
    fun getPages():Int{
        return repository.pagesPerFile
    }
}