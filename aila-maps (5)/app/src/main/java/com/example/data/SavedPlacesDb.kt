package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "saved_places")
data class SavedPlace(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String, // "HOME", "WORK", "PARK", "CAFE", "GYM", "CUSTOM"
    val imageUrl: String? = null,
    val userEmail: String = "Guest Explorer"
)

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY id ASC")
    fun getAllSavedPlaces(): Flow<List<SavedPlace>>

    @Query("SELECT * FROM saved_places WHERE userEmail = :email ORDER BY id ASC")
    fun getSavedPlacesForUser(email: String): Flow<List<SavedPlace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: SavedPlace)

    @Delete
    suspend fun deletePlace(place: SavedPlace)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deletePlaceById(id: Int)
}

@Database(entities = [SavedPlace::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedPlaceDao(): SavedPlaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aila_maps_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.savedPlaceDao())
                }
            }
        }

        suspend fun populateDatabase(dao: SavedPlaceDao) {
            // Prepopulate default places as shown in the UI designs for Guest Explorer only
            dao.insertPlace(
                SavedPlace(
                    name = "Home",
                    address = "123 Sunset Boulevard, LA",
                    latitude = 34.0928,
                    longitude = -118.3287,
                    category = "HOME",
                    userEmail = "Guest Explorer"
                )
            )
            dao.insertPlace(
                SavedPlace(
                    name = "Work",
                    address = "Tech Plaza, Building 4",
                    latitude = 34.0522,
                    longitude = -118.2437,
                    category = "WORK",
                    userEmail = "Guest Explorer"
                )
            )
            dao.insertPlace(
                SavedPlace(
                    name = "Central Park",
                    address = "Manhattan, New York City",
                    latitude = 40.7851,
                    longitude = -73.9682,
                    category = "PARK",
                    imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBPw7ICQm7E3-LYSUDo58d49Vd_VVNPiNDfWfHr6VL81fiA_HHa3UKbRd5tZ1JxIcs0hRntUYNMwSRjch0kuXMrkGelnNZet8i5lTunMODJgG_ImE_So8Eby_ml-wPsMIp7l1ux2a1LEAwetIdI5XThhYFd9vIogMaBDinPqIT5OMU9N0t8i9-43Paf1iTI6ivxdBKba3Kv3CwyQp-Iuwa1dM1GndcghL3W4vrAUFgzh-Z6WzjiFkJftpww7sloEHGqNiGBW659RuVH",
                    userEmail = "Guest Explorer"
                )
            )
            dao.insertPlace(
                SavedPlace(
                    name = "Brew & Bean",
                    address = "45 Espresso Lane",
                    latitude = 34.0601,
                    longitude = -118.2912,
                    category = "CAFE",
                    userEmail = "Guest Explorer"
                )
            )
            dao.insertPlace(
                SavedPlace(
                    name = "Iron Paradise Gym",
                    address = "North Side Mall, Floor 2",
                    latitude = 34.0750,
                    longitude = -118.3562,
                    category = "GYM",
                    userEmail = "Guest Explorer"
                )
            )
        }
    }
}

class SavedPlacesRepository(private val dao: SavedPlaceDao) {
    val allSavedPlaces: Flow<List<SavedPlace>> = dao.getAllSavedPlaces()

    fun getSavedPlacesForUser(email: String): Flow<List<SavedPlace>> {
        return dao.getSavedPlacesForUser(email)
    }

    suspend fun insert(place: SavedPlace) {
        dao.insertPlace(place)
    }

    suspend fun delete(place: SavedPlace) {
        dao.deletePlace(place)
    }

    suspend fun deleteById(id: Int) {
        dao.deletePlaceById(id)
    }
}
