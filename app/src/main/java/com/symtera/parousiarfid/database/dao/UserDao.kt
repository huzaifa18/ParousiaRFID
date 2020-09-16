package com.symtera.parousiarfid.database.dao

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.room.*
import com.symtera.parousiarfid.models.UserModel

@Dao
interface UserDao {

    /**
     * Get all users in database ordered by ASC
     * @return a list with all phrases
     */
    @Query("SELECT * FROM user_table ORDER BY id ASC")
    fun allUsers(): LiveData<UserModel>

    @Query("SELECT * FROM user_table ORDER BY id ASC")
    fun allUsersCursor(): Cursor

    @Query("SELECT * FROM user_table ORDER BY id ASC")
    fun getAll(): LiveData<List<UserModel>>

    @Query("SELECT * FROM user_table where id = :userId ORDER BY id ASC")
    fun getUser(userId: Int): UserModel

    @Query("SELECT * FROM user_table where rfid = :rfid ORDER BY id ASC")
    fun getUserByRFID(rfid: String): LiveData<UserModel>

    /**
     * Function to insert a users in room database
     * @param users to be inserted in database
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(userModel: UserModel)

    /**
     * Function to delete an users in room database
     * @param phrase the object to be deleted
     */
    @Delete
    fun delete(userModel: UserModel)

    @Query("DELETE FROM user_table")
    fun nukeTable()
}