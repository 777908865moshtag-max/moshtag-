package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "studio_projects")
data class StudioProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val categoryTitleAr: String,
    val processingModeAr: String,
    val aspectRatioLabel: String,
    val resolutionLabel: String,
    val outputWidth: Int,
    val outputHeight: Int,
    val exportFormat: String,
    val savedFilePath: String,
    val originalFileName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface StudioProjectDao {
    @Query("SELECT * FROM studio_projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<StudioProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: StudioProjectEntity)

    @Query("DELETE FROM studio_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    @Query("DELETE FROM studio_projects")
    suspend fun clearAllProjects()
}

@Database(entities = [StudioProjectEntity::class], version = 1, exportSchema = false)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun studioProjectDao(): StudioProjectDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        fun getInstance(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "moshtaq_grafx_studio.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class StudioRepository(
    private val dao: StudioProjectDao,
    private val context: Context
) {
    private val prefs = context.getSharedPreferences("moshtaq_studio_prefs", Context.MODE_PRIVATE)

    val allProjects: Flow<List<StudioProjectEntity>> = dao.getAllProjects()

    suspend fun insertProject(project: StudioProjectEntity) = dao.insertProject(project)

    suspend fun deleteProjectById(id: Int) = dao.deleteProjectById(id)

    suspend fun clearAllProjects() = dao.clearAllProjects()

    fun isDarkThemePreferred(): Boolean = prefs.getBoolean("dark_theme", true)

    fun setDarkThemePreferred(isDark: Boolean) {
        prefs.edit().putBoolean("dark_theme", isDark).apply()
    }

    fun getLastSelectedCategoryId(): String =
        prefs.getString("last_category_id", "plastic_bags") ?: "plastic_bags"

    fun setLastSelectedCategoryId(categoryId: String) {
        prefs.edit().putString("last_category_id", categoryId).apply()
    }
}
