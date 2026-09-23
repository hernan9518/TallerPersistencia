package com.example.persistencia.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Tarea::class], version = 2, exportSchema = false) // Versión 2
abstract class AppDatabase : RoomDatabase() {

    abstract fun tareaDao(): TareaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "base_datos_tareas"
                )
                    .fallbackToDestructiveMigration() // Recrea la tabla si cambia la versión
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}