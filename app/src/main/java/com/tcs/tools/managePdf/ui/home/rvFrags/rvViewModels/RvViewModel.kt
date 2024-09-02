package com.tcs.tools.managePdf.ui.home.rvFrags.rvViewModels

import android.content.Context
import android.provider.DocumentsContract
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcs.tools.managePdf.ui.home.rvFrags.RvState
import data.repository.Repository
import data.room.AppDatabase
import data.room.FileEntity
import data.sharedPrefs.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class RvViewModel:ViewModel() {
    private lateinit var repository: Repository
    val favFiles=MutableLiveData<MutableList<FileEntity>>()
    val files=MutableLiveData<MutableList<FileEntity>>()
    var state=MutableStateFlow<RvState>(RvState.Idle)
    var filesChanged=false
    var favFilesChanged=false
    fun init(context: Context){
        val fileDao = AppDatabase.getDatabase(context).fileDao()
        repository= Repository(context,fileDao, PreferenceManager(context))
    }
    suspend fun getAllFilesList(setLiveData:Boolean=true):MutableList<FileEntity> {
        return withContext(Dispatchers.IO){
            val data=repository.getAllFiles()
            if(setLiveData){
                withContext(Dispatchers.Main){
                    files.value=data
                }
            }
            Log.d("state","all file size+${data.size}")
            data
        }
    }
    var lastAdShown:Long
        get()=repository.lastAdShown
        set(value){
            repository.lastAdShown=value
        }
    suspend fun getFavFilesList(setLiveData: Boolean = true): MutableList<FileEntity> {
        return withContext(Dispatchers.IO) {
            val data = repository.getFavoriteFiles()
            if (setLiveData) {
                withContext(Dispatchers.Main) {
                    favFiles.value = data
                }
            }
            Log.d("state", "all fav file size ${data.size}")
            data
        }
    }
    suspend fun renameFile(context: Context, id:Long, newName: String) {
        Log.d("uri","trying to rename file")
        return withContext(Dispatchers.IO) {
            val file=repository.getFileById(id)
            try {
                val fileUri=file.uri
                val renamedUri = DocumentsContract.renameDocument(context.contentResolver, fileUri, "$newName.pdf")
                Log.d("uri","$fileUri to $renamedUri")
                renamedUri?: throw IOException("Failed to rename file")
                file.uri=renamedUri
                file.fileName=newName
                repository.updateFile(file)
            }
            catch(e:IllegalStateException) {
                repository.deleteFile(file)
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun removeFile(file:FileEntity){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                repository.deleteFile(file)
            }
        }
    }
    fun deleteFile(context: Context, file:FileEntity,onComplete:(Boolean)->Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val fileUri = file.uri
                    val deleted = DocumentsContract.deleteDocument(context.contentResolver, fileUri)

                    if (deleted) {
                        Log.d("uri", "File deleted successfully: $fileUri")
                        repository.deleteFile(file)
                        onComplete(true)
                    } else {
                        Log.d("uri", "Failed to delete: $fileUri")
                        onComplete(false)
                        throw IOException("Failed to delete file")
                    }
                }catch(e:IllegalArgumentException){
                    repository.deleteFile(file)
                    onComplete(true)
                }
                catch (e: Exception) {
                    Log.d("uri", "File exception: ${e.message}")
                    onComplete(false)
                    e.printStackTrace()
                }
            }
        }
    }
    suspend fun addPdfToFavorites(id:Long,flag:Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.setFavoriteFile(id, flag)
            }
        }
    }
}