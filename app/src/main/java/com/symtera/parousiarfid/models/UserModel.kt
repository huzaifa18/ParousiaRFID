package com.symtera.parousiarfid.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_table",
    indices = [Index(value = ["rfid"], unique = true)])
data class UserModel(
    @PrimaryKey(autoGenerate = true)
    var id:Int,
    var name:String,
    var email:String,
    var mobile:String,
    var rfid:String)