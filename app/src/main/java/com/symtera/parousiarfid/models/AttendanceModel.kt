package com.symtera.parousiarfid.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_table")
data class AttendanceModel(
    @PrimaryKey(autoGenerate = true)
    var id:Int,
    var userId:Int,
    var status:Int,
    var time:String,
    var date:String,
    var img:String)