package com.tcs.tools.managePdf.utils
import android.net.Uri
import androidx.room.TypeConverter

class UriConverters {
    @TypeConverter
    fun fromUri(uri: Uri): String {
        return uri.toString()
    }

    @TypeConverter
    fun toUri(uriString: String): Uri {
        return Uri.parse(uriString)
    }
}