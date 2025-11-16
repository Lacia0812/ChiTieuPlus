package com.example.chitieuplus.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,      // <-- bảng danh mục
        BudgetEntity::class         // Ngân sách
    ],
    version = 3,                   // <-- tăng version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao   // <-- thêm DAO danh mục

    abstract fun budgetDao(): BudgetDao        // DAO Ngân sách


    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Migration từ v1 -> v2: tạo bảng categories + unique index (name, type)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tạo bảng categories
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categories(
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        archived INTEGER NOT NULL DEFAULT 0 CHECK(archived IN (0,1))
                    )
                    """.trimIndent()
                )
                // Unique index để tránh trùng tên trong cùng loại (Thu/Chi)
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name_type
                    ON categories(name, type)
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chitieu_db"
                )
                    .addMigrations(MIGRATION_1_2)   // <-- đăng ký migration
                       // .fallbackToDestructiveMigration() // chỉ bật khi DEV muốn reset DB
                    .build().also { INSTANCE = it }
            }
    }
}
