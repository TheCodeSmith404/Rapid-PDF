package data.room

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    val id:Long=0,
    var uri:Uri,
    var fileName:String,
    var size:String,
    var date:String,
    var isFavorite:Boolean
)
