package com.tcs.tools.managePdf.ui.showPdf

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.repository.Repository
import data.room.AppDatabase
import data.room.FileEntity
import data.sharedPrefs.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShowPdfViewModel:ViewModel() {
    private lateinit var repository: Repository
    private lateinit var contentResolver: ContentResolver
    lateinit var fileUri:Uri
    fun init(context: Context){
        val fileDao = AppDatabase.getDatabase(context).fileDao()
        repository=Repository(context,fileDao, PreferenceManager(context))
        contentResolver=context.contentResolver
    }
    suspend fun getFile(id:Long): FileEntity {
        return withContext(Dispatchers.IO){
           val file=repository.getFileById(id)
            fileUri=file.uri
            file
        }
    }
    suspend fun setHasFavFile(){
        withContext(Dispatchers.IO){
            repository.hasFavList=true
        }
    }
    suspend fun setFavFile(id:Long,boolean: Boolean){
        withContext(Dispatchers.IO){
            repository.setFavoriteFile(id,boolean)
        }
    }
    fun deleteFile(file:FileEntity){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                repository.deleteFile(file)
            }
        }
    }
}