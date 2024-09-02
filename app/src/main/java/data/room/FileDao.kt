package data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fileEntity: FileEntity): Long

    @Update
    suspend fun update(fileEntity: FileEntity)
    @Delete
    suspend fun delete(fileEntity: FileEntity)

    @Query("SELECT * FROM files where isFavorite=0")
    suspend fun getAllFiles(): MutableList<FileEntity>

    @Query("DELETE FROM files where isFavorite!=1")
    suspend fun deleteAllFiles()

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteById(id:Long)
    @Query("SELECT * FROM files WHERE id=:id")
    suspend fun getFileById(id:Long):FileEntity

    @Query("SELECT * FROM files WHERE isFavorite =1")
    suspend fun getFavoriteFiles(): MutableList<FileEntity>

    @Query("UPDATE files SET isFavorite=:fav WHERE id=:id")
    suspend fun setFavorite(id:Long,fav:Boolean)

    @Query("SELECT fileName from files where id=:id")
    suspend fun getFileName(id:Long):String

    @Query("SELECT * from files where id>=:id")
    suspend fun getFilesAfterId(id:Long):List<FileEntity>
}