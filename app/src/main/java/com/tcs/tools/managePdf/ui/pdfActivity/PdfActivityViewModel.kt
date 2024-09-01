package com.tcs.tools.managePdf.ui.pdfActivity

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.repository.Repository
import data.room.AppDatabase
import data.room.FileEntity
import data.sharedPrefs.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfActivityViewModel:ViewModel() {
    private lateinit var repository: Repository
    var fileUri:Uri?=null
    fun init(context: Context){
        val fileDao = AppDatabase.getDatabase(context).fileDao()
        repository= Repository(context,fileDao, PreferenceManager(context))
    }
    private suspend fun extractFileInfo(pdfUri: Uri, contentResolver: ContentResolver): FileEntity {
        return withContext(Dispatchers.IO) {
            var file: FileEntity
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            contentResolver.query(pdfUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val documentId = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                    val displayName = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE))
                    val date = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED))
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(pdfUri, documentId)
                    val dateModified: String = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date(date))
                    val sizeModified: String = formatFileSize(size)
                    file = FileEntity(
                        uri = documentUri,
                        fileName = displayName.substringBefore('.'),
                        size = sizeModified,
                        date = dateModified,
                        isFavorite = true
                    )
                } else {
                    file = FileEntity(
                        uri = pdfUri,
                        fileName = "false",
                        size = "false",
                        date = "false",
                        isFavorite = false
                    )
                }
                return@withContext file
            } ?: FileEntity(
                uri = pdfUri,
                fileName = "false",
                size = "false",
                date = "false",
                isFavorite = false
            )
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