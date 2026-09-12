package io.github.mauricem0ss.fastdroid.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Fast::class], version = 1, exportSchema = true)
abstract class FastDatabase : RoomDatabase() {
    abstract fun fastDao(): FastDao

    companion object {
        fun create(context: Context): FastDatabase =
            Room.databaseBuilder(context, FastDatabase::class.java, "fastdroid.db").build()
    }
}
