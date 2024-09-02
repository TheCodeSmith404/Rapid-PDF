package data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import data.room.FileDao
import data.room.FileEntity
import data.sharedPrefs.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Repository(
    private val context:Context,
    private val fileDao: FileDao,
    private val preferenceManager: PreferenceManager,
) {
    suspend fun loadPdfFilesFromUri(contentResolver: ContentResolver, uri: Uri): Boolean {
        Log.d("PDF","function called with uri: $uri")
        return try {
            Log.d("PDF", "trying to get child uri")
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            Log.d("PDF","Success : child uri: $childrenUri")
            contentResolver.query(childrenUri, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ), null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                    val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE))
                    Log.d("PDF","mimetype is : $mimeType")
                    if ("application/pdf"==mimeType) {
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                        val size = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE))
                        val date = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED))
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)

                        val dateModified: String = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(
                            Date(date)
                        )


                        val sizeModified: String = formatFileSize(size)
                        val file = FileEntity(uri = documentUri, fileName = displayName.substringBefore('.'), size = sizeModified, date = dateModified, isFavorite = false)
                        Log.d("PDF", "file type is PDF File: $file")
                        fileDao.insert(file)
                    }else if(DocumentsContract.Document.MIME_TYPE_DIR==mimeType){
                        val folderUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
                        if(!isDirectoryEmpty(contentResolver,folderUri)) {
                            Log.d("PDF", "file type is dire -> Entering directory: $folderUri")
                            this.loadPdfFilesFromDirectory(contentResolver, folderUri)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.d("PDF", "failure : catch ${e.message}")
            e.printStackTrace()
            false
        }
    }
    private suspend fun isDirectoryEmpty(contentResolver: ContentResolver, directoryUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            contentResolver.query(
                DocumentsContract.buildChildDocumentsUriUsingTree(directoryUri, DocumentsContract.getDocumentId(directoryUri)),
                projection, null, null, null
            )?.use { cursor ->
                return@withContext cursor.count == 0
            }
            return@withContext false
        }
    }
    private suspend fun loadPdfFilesFromDirectory(contentResolver: ContentResolver, uri: Uri): Boolean {
        Log.d("PDF","function called with uri: $uri")
        return try {
            Log.d("PDF", "trying to get child uri")
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))
            Log.d("PDF","Success : child uri: $childrenUri")
            contentResolver.query(childrenUri, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ), null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                    val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE))
                    Log.d("PDF","mimetype is : $mimeType")
                    if ("application/pdf"==mimeType) {
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                        val size = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE))
                        val date = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED))
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)

                        val dateModified: String = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(
                            Date(date)
                        )
                        val sizeModified: String = formatFileSize(size)
                        val file = FileEntity(uri = documentUri, fileName = displayName.substringBefore('.'), size = sizeModified, date = dateModified, isFavorite = false)
                        Log.d("PDF", "file type is PDF File: $file")
                        fileDao.insert(file)
                    }else if(DocumentsContract.Document.MIME_TYPE_DIR==mimeType){
                        val folderUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
                        Log.d("PDF", "file type is dire -> Entering directory: $folderUri")
                        this.loadPdfFilesFromDirectory(contentResolver, folderUri)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.d("PDF", "failure : catch ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun getFilesAfterId(id:Long):List<FileEntity>{
        return withContext(Dispatchers.IO){
            fileDao.getFilesAfterId(id)
        }
    }

    suspend fun getAllFiles(): MutableList<FileEntity> {
        return withContext(Dispatchers.IO) {
            fileDao.getAllFiles()
        }
    }

    suspend fun insertFile(fileEntity: FileEntity) {
        withContext(Dispatchers.IO) {
            fileDao.insert(fileEntity)
        }
    }

    suspend fun updateFile(fileEntity: FileEntity) {
        withContext(Dispatchers.IO) {
            fileDao.update(fileEntity)
        }
    }
    suspend fun getFileById(id:Long):FileEntity{
        return withContext(Dispatchers.IO){
            fileDao.getFileById(id)
        }
    }
    suspend fun deleteFile(fileEntity: FileEntity) {
        withContext(Dispatchers.IO) {
            fileDao.delete(fileEntity)
        }
    }
    suspend fun getFavoriteFiles():MutableList<FileEntity>{
        return withContext(Dispatchers.IO){
            fileDao.getFavoriteFiles()
        }
    }
    suspend fun setFavoriteFile(id:Long,fav:Boolean){
        withContext(Dispatchers.IO){
            fileDao.setFavorite(id,fav)
        }
    }
    suspend fun getFileName(id:Long):String{
        return withContext(Dispatchers.IO){
            fileDao.getFileName(id)
        }
    }
    suspend fun deleteAllFiles(){
        withContext(Dispatchers.IO){
            fileDao.deleteAllFiles()
        }
    }
    var pagesPerFile:Int
        get()=preferenceManager.pagesPerFile
        set(value){
            preferenceManager.pagesPerFile=value
        }

    var permissionGranted: Boolean
        get() = preferenceManager.permissionGranted
        set(value) {
            preferenceManager.permissionGranted = value
        }

    var currFileIndex: Long
        get() = preferenceManager.currFileId
        set(value) {
            preferenceManager.currFileId = value
        }

    var hasFavList: Boolean
        get() = preferenceManager.hasFavList
        set(value) {
            preferenceManager.hasFavList = value
        }
    var hideLeftRight:Boolean
        get()=preferenceManager.hideLeftRight
        set(value){
            preferenceManager.hideLeftRight=value
        }
    var currentUri:String
        get()=preferenceManager.currentUri
        set(value){
            preferenceManager.currentUri=value
        }
    var lastAdShown:Long
        get()=preferenceManager.lastAdShown
        set(value){
            preferenceManager.lastAdShown=value
        }
    var firstStart:Boolean
        get()=preferenceManager.firstStart
        set(value){
            preferenceManager.firstStart=value
        }
    suspend fun getCategories():MutableSet<String>{
        return withContext(Dispatchers.IO) {
            preferenceManager.categorySet
        }
    }
    suspend fun setCategories(value:MutableSet<String>){
        withContext(Dispatchers.IO) {
            preferenceManager.categorySet = value
        }
    }
    private fun formatFileSize(size: Long): String {
        val kb = 1024L
        val mb = kb * 1024

        return when {
            size < kb -> "$size B"
            size < mb -> String.format("%.2f KB", size / kb.toFloat())
            else -> String.format("%.2f MB", size / mb.toFloat())
        }
    }



}