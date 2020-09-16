package com.symtera.parousiarfid.database.dao

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.symtera.parousiarfid.models.AttendanceModel

@Dao
interface AttendanceDao {

    /**
     * Get all users in database ordered by ASC
     * @return a list with all phrases
     */
    @Query("SELECT * FROM Attendance_table ORDER BY id ASC")
    fun allAttendance(): LiveData<AttendanceModel>

    @Query("SELECT * FROM Attendance_table ORDER BY id ASC")
    fun allAttendanceCursor(): Cursor

    @Query("SELECT * FROM attendance_table ORDER BY id ASC")
    fun getAll(): LiveData<List<AttendanceModel>>

    @Query("SELECT * FROM attendance_table where userId = :userId ORDER BY id ASC")
    fun getAttendanceByUser(userId: Int): LiveData<List<AttendanceModel>>

    @Query("SELECT * FROM attendance_table where date LIKE '%' || :date and userId = :id ORDER BY date ASC")
    fun getAttendanceByDayAndUser(date: String,id: Int): LiveData<List<AttendanceModel>>

    @Query("SELECT * FROM attendance_table where date LIKE '%' || :date and userId = :id ORDER BY date ASC")
    fun getAttendanceTimeByDayAndUser(date: String,id: Int): List<AttendanceModel>

    @Query("SELECT * FROM attendance_table where date LIKE '%' || :date ORDER BY date ASC")
    fun getAttendanceByDay(date: String): LiveData<List<AttendanceModel>>

    @Query("SELECT COUNT(*) FROM attendance_table where date = :date AND userId = :userId")
    fun getAttendanceByDayCount(userId: Int,date: String): Int

    @Query("SELECT * FROM Attendance_table where id = :attendanceId ORDER BY id ASC")
    fun getAttendance(attendanceId: Int): AttendanceModel

    @Query("SELECT status FROM Attendance_table where userId = :userId and date = :date ORDER BY time ASC")
    fun getAttendanceStatus(userId: Int,date: String): Int

    /**
     * Function to insert a users in room database
     * @param users to be inserted in database
     */
    @Insert
    fun insert(attendanceModel: AttendanceModel)

    /**
     * Function to delete an users in room database
     * @param phrase the object to be deleted
     */
    @Delete
    fun delete(attendanceModel: AttendanceModel)

    @Query("DELETE FROM user_table")
    fun nukeTable()
}