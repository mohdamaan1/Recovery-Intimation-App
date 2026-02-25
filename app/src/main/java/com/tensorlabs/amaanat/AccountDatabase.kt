package com.tensorlabs.amaanat.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountNumber: String,
    val name: String,
    val mobileNumber: String,
    val amount: String,
    val bankName: String, // <--- NEW FIELD
    val payByDate: String,
    val g1Name: String,
    val g1Mobile: String,
    val g2Name: String,
    val g2Mobile: String
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)
}

@Database(entities = [AccountEntity::class], version = 5, exportSchema = false) // Version 5
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "amaanat_db_v5"
                ).fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}