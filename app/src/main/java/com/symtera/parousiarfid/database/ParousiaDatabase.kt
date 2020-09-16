package com.symtera.parousiarfid.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.symtera.parousiarfid.database.dao.AttendanceDao
import com.symtera.parousiarfid.database.dao.UserDao
import com.symtera.parousiarfid.models.AttendanceModel
import com.symtera.parousiarfid.models.UserModel

@Database(entities = [UserModel::class,AttendanceModel::class], version = 1, exportSchema = false)
abstract class ParousiaDatabase : RoomDatabase() {

    /**
     * This is the Phrase data access object instance
     * @return the dao to phrase database operations
     */
    abstract fun userDao(): UserDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {

        /**
         * This is just for singleton pattern
         */
        private var INSTANCE: ParousiaDatabase? = null

        fun getDatabase(context: Context): ParousiaDatabase {
            if (INSTANCE == null) {
                synchronized(ParousiaDatabase::class.java) {
                    if (INSTANCE == null) {
                        // Get PhraseRoomDatabase database instance
                        INSTANCE = Room.databaseBuilder(context.applicationContext,
                            ParousiaDatabase::class.java, "parousia_rfid_database"
                        )
                            .allowMainThreadQueries()
                            .build()
                    }
                }
            }
            return INSTANCE!!
        }
    }
}