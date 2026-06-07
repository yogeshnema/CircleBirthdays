package com.purawale.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import androidx.room.TypeConverters

@Database(entities = [Member::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val existingColumns = mutableSetOf<String>()
                db.query("PRAGMA table_info(members)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        existingColumns += cursor.getString(nameIndex)
                    }
                }

                fun addColumnIfMissing(name: String, definition: String) {
                    if (!existingColumns.contains(name)) {
                        db.execSQL("ALTER TABLE members ADD COLUMN $name $definition")
                    }
                }

                addColumnIfMissing("email", "TEXT")
                addColumnIfMissing("location", "TEXT")
                addColumnIfMissing("spouseName", "TEXT")
                addColumnIfMissing("fatherName", "TEXT")
                addColumnIfMissing("motherName", "TEXT")
                addColumnIfMissing("marriageDate", "TEXT")
                addColumnIfMissing("bereavementDate", "TEXT")
                addColumnIfMissing("photoUrl", "TEXT")
                addColumnIfMissing("immediateFamily", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing("address", "TEXT")
                addColumnIfMissing("latitude", "REAL")
                addColumnIfMissing("longitude", "REAL")
                addColumnIfMissing("flatNumber", "TEXT")
                addColumnIfMissing("floor", "TEXT")
                addColumnIfMissing("landmark", "TEXT")
                addColumnIfMissing("password", "TEXT")
                addColumnIfMissing("isAdmin", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing("isEditor", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing("isPrimaryTree", "INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing("secondaryTreeEnabled", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing("treeId", "TEXT NOT NULL DEFAULT 'primary'")
                addColumnIfMissing("status", "TEXT NOT NULL DEFAULT 'APPROVED'")
                addColumnIfMissing("lastLoggedIn", "INTEGER")
                addColumnIfMissing("relationship", "TEXT")
                addColumnIfMissing("manualRelationships", "TEXT NOT NULL DEFAULT '{}'")
                addColumnIfMissing("fcmToken", "TEXT")
                addColumnIfMissing("facebookUrl", "TEXT")
                addColumnIfMissing("instagramUrl", "TEXT")
                addColumnIfMissing("youtubeUrl", "TEXT")
                addColumnIfMissing("requestedBy", "TEXT")
                addColumnIfMissing("requestedByName", "TEXT")
                addColumnIfMissing("requestedRelationship", "TEXT")
                addColumnIfMissing("points", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing("level", "INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing("badges", "TEXT NOT NULL DEFAULT '[]'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "member_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
