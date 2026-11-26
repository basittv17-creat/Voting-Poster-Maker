package com.awisogera.votingpostermaker

import android.content.Context
import android.net.Uri
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Entity
data class SavedPoster(@PrimaryKey val id: String, val title: String, val updated: Long, val data: String)

@Dao
interface PosterDao {
    @Query("SELECT * FROM SavedPoster ORDER BY updated DESC")
    fun watch(): Flow<List<SavedPoster>>
    @Upsert suspend fun save(poster: SavedPoster)
    @Query("DELETE FROM SavedPoster WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [SavedPoster::class], version = 1, exportSchema = false)
abstract class PosterDatabase : RoomDatabase() {
    abstract fun posters(): PosterDao

    companion object {
        @Volatile private var instance: PosterDatabase? = null
        fun get(context: Context): PosterDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, PosterDatabase::class.java, "posters.db")
                .build().also { instance = it }
        }
    }
}

class ProjectStore(private val context: Context) {
    private val dao = PosterDatabase.get(context).posters()
    private val json = Json { ignoreUnknownKeys = true }
    private val assets = File(context.filesDir, "poster-assets").apply { mkdirs() }
    val projects = dao.watch()

    suspend fun save(document: PosterDocument) {
        dao.save(SavedPoster(document.id, document.title, System.currentTimeMillis(), json.encodeToString(document)))
    }

    fun read(poster: SavedPoster): PosterDocument = json.decodeFromString(poster.data)
    fun asset(key: String): File = File(assets, key)
    fun newAsset(): File = asset(UUID.randomUUID().toString())

    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val target = newAsset()
        try {
            val source = requireNotNull(context.contentResolver.openInputStream(uri)) { "Could not open this file." }
            source.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        total += count
                        require(total <= 80L * 1024 * 1024) { "Choose a file under 80 MB." }
                        output.write(buffer, 0, count)
                    }
                    require(total > 0L) { "The file is empty." }
                }
            }
            target.name
        } catch (failure: Throwable) {
            target.delete()
            throw failure
        }
    }
}
