package com.tcs.tools.managePdf.ui.home


import android.content.ContentResolver
import android.content.Context
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
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

class HomeViewModel() : ViewModel() {
    private lateinit var repository: Repository
    fun init(context: Context){
        val fileDao = AppDatabase.getDatabase(context).fileDao()
       repository=Repository(context,fileDao,PreferenceManager(context))
    }
    fun loadPdfFilesFromUri(contentResolver: ContentResolver,onComplete:(Boolean)->Unit) {
        viewModelScope.launch {
            val uri=repository.currentUri
            if(uri!="null") {
                val result = withContext(Dispatchers.IO) {
                    repository.loadPdfFilesFromUri(contentResolver, uri.toUri())
                }
                onComplete(result)
            }
            else{
                onComplete(false)
            }
        }
    }

    var permissionGranted: Boolean
        get() = repository.permissionGranted
        set(value) {
            repository.permissionGranted = value
        }

    var fistStart:Boolean
        get()=repository.firstStart
        set(value){
            repository.firstStart=value
        }
//    // Getter and setter for currFileIndex
//    var currFileIndex: Int
//        get() = preferenceManager.currFileIndex
//        set(value) {
//            preferenceManager.currFileIndex = value
//        }

    var pagesPerFile:Int
        get()=repository.pagesPerFile
        set(value){
            repository.pagesPerFile=value
        }
}
