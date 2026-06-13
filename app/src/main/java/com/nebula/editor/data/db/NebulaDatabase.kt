package com.nebula.editor.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nebula.editor.data.db.entity.ExportJobEntity
import com.nebula.editor.data.db.entity.HistoryEntity
import com.nebula.editor.data.db.entity.ProjectEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Database(
    entities = [
        ProjectEntity::class,
        HistoryEntity::class,
        ExportJobEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class NebulaDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun historyDao(): HistoryDao
    abstract fun exportJobDao(): ExportJobDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NebulaDatabase {
        // Encrypted database key stored in EncryptedSharedPreferences
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            "nebula_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Generate or retrieve DB encryption passphrase
        val passphraseKey = "db_passphrase"
        val passphrase = prefs.getString(passphraseKey, null) ?: run {
            val newPass = generateSecurePassphrase()
            prefs.edit().putString(passphraseKey, newPass).apply()
            newPass
        }

        val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))

        return Room.databaseBuilder(
            context,
            NebulaDatabase::class.java,
            "nebula_projects.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()   // upgrade strategy — change for production
            .build()
    }

    @Provides fun provideProjectDao(db: NebulaDatabase) = db.projectDao()
    @Provides fun provideHistoryDao(db: NebulaDatabase) = db.historyDao()
    @Provides fun provideExportJobDao(db: NebulaDatabase) = db.exportJobDao()

    private fun generateSecurePassphrase(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*"
        return (1..48).map { chars.random() }.joinToString("")
    }
}
