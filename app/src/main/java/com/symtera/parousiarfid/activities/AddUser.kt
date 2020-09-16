package com.symtera.parousiarfid.activities

import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.symtera.parousiarfid.R
import com.symtera.parousiarfid.database.ParousiaDatabase
import com.symtera.parousiarfid.database.dao.UserDao
import com.symtera.parousiarfid.models.UserModel


class AddUser : AppCompatActivity() {

    lateinit var et_username: EditText
    lateinit var et_mail: EditText
    lateinit var et_phone: EditText
    lateinit var btn_sign_up: Button

    lateinit var rfid: String

    lateinit var database: ParousiaDatabase
    lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user)

        init()
    }

    fun init() {

        database = ParousiaDatabase.getDatabase(this@AddUser)
        userDao = database.userDao()

        btn_sign_up = findViewById(R.id.btn_sign_up)
        et_username = findViewById(R.id.et_username)
        et_mail = findViewById(R.id.et_mail)
        et_phone = findViewById(R.id.et_phone)

        listener()
    }

    fun listener() {

        btn_sign_up.setOnClickListener {
            if (et_username.text.isEmpty()) {
                et_username.setError("This field cannot be empty!")
            } else if (et_mail.text.isEmpty()) {
                et_mail.setError("This field cannot be empty!")
            } else if (et_phone.text.isEmpty()) {
                et_phone.setError("This field cannot be empty!")
            } else {
                rfidDialog()
            }
        }
    }

    fun rfidDialog() {

        var mBottomSheetDialog = BottomSheetDialog(this)
        mBottomSheetDialog.setContentView(R.layout.modal_bottom_sheet)
        mBottomSheetDialog.getWindow()!!.setBackgroundDrawableResource(R.color.transparent)
        mBottomSheetDialog.getWindow()!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        mBottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        var et_rfid: TextView
        et_rfid = mBottomSheetDialog.findViewById(R.id.et_rfid)!!
        var iv_gif: ImageView
        iv_gif = mBottomSheetDialog.findViewById(R.id.iv_gif)!!
        Glide.with(this).asGif().load(R.raw.scan).into(iv_gif)
        mBottomSheetDialog.setOnDismissListener(DialogInterface.OnDismissListener {
            //mBottomSheetDialog = null
        })
        mBottomSheetDialog.show()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            et_rfid.showSoftInputOnFocus = false
        }
        et_rfid.setOnKeyListener(View.OnKeyListener { view, keyCode, keyEvent ->
            Log.e("TAG", "Key pressed!")
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                // Perform action on key press
                rfid = et_rfid.text.toString()
                register()
                mBottomSheetDialog.dismiss()
                //tv_rfid.setText("")
                return@OnKeyListener true
            }
            return@OnKeyListener false
        })
    }

    fun register() {

        Log.e("TAG","${et_username.text.toString()}")
        Log.e("TAG","${et_mail.text.toString()}")
        Log.e("TAG","${et_phone.text.toString()}")
        Log.e("TAG","${rfid}")

        /*Toast.makeText(this@AddUser,"${et_username.text.toString()} \n " +
                "${et_mail.text.toString()} \n " +
                "${et_phone.text.toString()} \n" +
                "${rfid} ",Toast.LENGTH_LONG).show()*/

        addPersonDB(
            UserModel(
                0,
                et_username.text.toString(),
                et_mail.text.toString(),
                et_phone.text.toString(),
                rfid
            )
        )
    }

    fun addPersonDB(user: UserModel) {
        var exception = false
        AsyncTask.execute {
            try {
                userDao.insert(user)
                Log.e("TAG", "User Added in DB!")
                finish()
            } catch (e: SQLiteConstraintException){
                exception = true
            }
        }
        Handler().postDelayed({
            if (exception){
                Toast.makeText(this@AddUser, "User Already Exists!", Toast.LENGTH_LONG).show()
            }
        },500)

    }

}