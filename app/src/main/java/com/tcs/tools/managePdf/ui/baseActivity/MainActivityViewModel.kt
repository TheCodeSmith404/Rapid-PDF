package com.tcs.tools.managePdf.ui.baseActivity

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.repository.Repository
import data.room.AppDatabase
import data.room.FileEntity
import data.sharedPrefs.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivityViewModel:ViewModel() {
    private lateinit var repository: Repository
    private val _pdfFilesFlow: MutableStateFlow<HomeState> = MutableStateFlow(HomeState.Idle)
    val pdfFilesFlow: StateFlow<HomeState> get() = _pdfFilesFlow
    val channel= Channel<HomeStateIntent>(Channel.UNLIMITED)
    init{
        observeChannel()
    }
    fun init(context: Context){
        val fileDao = AppDatabase.getDatabase(context).fileDao()
        repository= Repository(context,fileDao, PreferenceManager(context))
    }
    var loading:Boolean=true
    fun deleteAllFiles(){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                repository.deleteAllFiles()
            }
        }

    }
    fun observeChannel(){
        viewModelScope.launch {
            channel.consumeAsFlow().collect{
                when(it){
                    is HomeStateIntent.TriggerReload->{
                        Log.d("files","Channel received")
                        _pdfFilesFlow.value=HomeState.Reload
                    }
                    is HomeStateIntent.TriggerIdle->{
                        _pdfFilesFlow.value=HomeState.Idle
                    }
                }
            }
        }
    }
    fun getPagesPerFile():Int{
        return repository.pagesPerFile
    }
    fun setPagesPerFile(value:Int){
        repository.pagesPerFile=value
    }
    fun getCurrentUri():String{
        return repository.currentUri
    }
    fun loadPdfFilesFromUri(contentResolver: ContentResolver, onComplete:(Boolean)->Unit) {
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
    suspend fun getAllFilesList(): List<FileEntity> {
        return withContext(Dispatchers.IO) {
            repository.getAllFiles()
        }
    }
}