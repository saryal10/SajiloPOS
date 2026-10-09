package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ProductItem
import com.example.data.model.SaleLineItem
import com.example.data.model.SaleTransaction

@Database(
    entities = [ProductItem::class, SaleTransaction::class, SaleLineItem::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        private const val MIGRATION_1_2 = """
            CREATE TABLE IF NOT EXISTS sale_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transactionId INTEGER NOT NULL,
                invoiceNumber TEXT NOT NULL,
                productId INTEGER NOT NULL,
                productName TEXT NOT NULL,
                category TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                listPrice REAL NOT NULL,
                unitPrice REAL NOT NULL,
                lineTotal REAL NOT NULL,
                costTotal REAL NOT NULL,
                timestamp INTEGER NOT NULL,
                passengerType TEXT NOT NULL,
                notes TEXT NOT NULL
            )
        """

        private val MIGRATION_1_TO_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(MIGRATION_1_2)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_items_timestamp ON sale_items (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_items_productId ON sale_items (productId)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sajilo_pos_database"
                )
                    .addMigrations(MIGRATION_1_TO_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}