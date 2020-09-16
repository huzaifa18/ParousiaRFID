package com.symtera.parousiarfid.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.skyfishjy.library.RippleBackground
import com.symtera.parousiarfid.R
import com.symtera.parousiarfid.database.ParousiaDatabase
import com.symtera.parousiarfid.database.dao.AttendanceDao
import com.symtera.parousiarfid.database.dao.UserDao
import com.symtera.parousiarfid.models.AttendanceModel
import com.symtera.parousiarfid.models.UserModel
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class RFIDScanner : AppCompatActivity() {

    lateinit var fab_view: FloatingActionButton
    lateinit var fab_add: FloatingActionButton
    lateinit var et_rfid: EditText

    lateinit var database: ParousiaDatabase
    lateinit var userDao: UserDao
    lateinit var attendanceDao: AttendanceDao

    var dialogDisplayed = false

    lateinit var iv_gif: ImageView

    lateinit var userModel: UserModel
    lateinit var pictureFile: File

    val MEDIA_TYPE_IMAGE = 1
    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rfid_scanner)
        getWindow()!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        init()
    }

    fun init() {

        database = ParousiaDatabase.getDatabase(this@RFIDScanner)
        userDao = database.userDao()
        attendanceDao = database.attendanceDao()

        iv_gif = findViewById(R.id.iv_gif)
        Glide.with(this).asGif().load(R.raw.scan).into(iv_gif)

        fab_view = findViewById(R.id.fab_view)
        fab_add = findViewById(R.id.fab_add)
        et_rfid = findViewById(R.id.et_rfid)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            et_rfid.showSoftInputOnFocus = false
        }

        initCamera()
        listener()
    }

    fun initCamera() {
        if (checkCameraHardware(this@RFIDScanner)) {
            // Create an instance of Camera
            mCamera = getCameraInstance()
            mCamera!!.setDisplayOrientation(90)

            mPreview = mCamera?.let {
                // Create our Preview view
                CameraPreview(this, it)
            }

            // Set the Preview view as the content of our activity.
            mPreview?.also {
                val preview: FrameLayout = findViewById(R.id.camera_preview)
                preview.addView(it)
            }
        }
    }

    fun listener() {
        fab_add.setOnClickListener {
            startActivity(Intent(this@RFIDScanner, AddUser::class.java))
        }

        fab_view.setOnClickListener {
            //mCamera?.takePicture(null, null, mPicture)
            startActivity(Intent(this@RFIDScanner, AttendanceDisplay::class.java))
        }

        et_rfid.requestFocus()
        et_rfid.isCursorVisible = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            et_rfid.showSoftInputOnFocus = false
        }
        et_rfid.setOnKeyListener(View.OnKeyListener { view, keyCode, keyEvent ->
            hideKeyboard(et_rfid)
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                // Perform action on key press
                Log.e("TAG", "Key pressed!")
                if (!dialogDisplayed) {
                    checkUser(et_rfid.text.toString())
                }
                return@OnKeyListener true
            }
            return@OnKeyListener false
        })

    }

    fun checkUser(rfid: String) {
        //Toast.makeText(this@RFIDScanner, "RFID: $rfid", Toast.LENGTH_LONG).show()
        Log.e("TAG", "$rfid")
        userDao.getUserByRFID(rfid).observe(this, Observer {
            Log.e("TAG", "User: $it")
            if (it != null) {
                //Toast.makeText(this@RFIDScanner, "User: ${it.name}", Toast.LENGTH_LONG).show()
                userModel = it
                capturePicture(it)
            } else {
                Toast.makeText(this@RFIDScanner, "User Does Not Exist!", Toast.LENGTH_SHORT).show()
            }
        })
        hideKeyboard(et_rfid)
        et_rfid.setText("")
    }

    fun capturePicture(user: UserModel) {
        mCamera?.takePicture(null, null, mPicture)
        Handler().postDelayed({
            markAttendance(user)
        }, 500)
    }

    fun markAttendance(user: UserModel) {
        val formattedDate =
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        val lastTime = attendanceDao.getAttendanceTimeByDayAndUser(formattedDate, user.id)
        var diff: Long = 0
        if (lastTime.size > 0) {
            val time =
                lastTime.get(lastTime.size - 1).date + " " + lastTime.get(lastTime.size - 1).time
            val abc = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
            try {
                //Log.e("TAG", "Old Time: " + abc.parse(time))
                //Log.e("TAG", "New Time: " + abc.parse(abc.format(Calendar.getInstance().getTime())))
                diff = getDifferenceInMinutes(
                    abc.parse(time),
                    abc.parse(abc.format(Calendar.getInstance().getTime()))
                )
            } catch (e: ParseException) {
                Log.e("TAG", "Parse Exception: " + e)
            }
        } else {
            diff = 1
        }
        diff = 1
        if (diff >= 1) {
            var status = 0
            val count = attendanceDao.getAttendanceByDayCount(
                user.id,
                SimpleDateFormat("dd-MM-yyyy").format(Calendar.getInstance().getTime())
            )
            if (!isOdd(count)) {
                Log.e("TAG", "In")
                status = 1
                attendanceMarked(user.name, status)
            } else {
                Log.e("TAG", "Out")
                status = 0
                attendanceMarked(user.name, status)
            }
            Handler().postDelayed({
                Log.e("TAG","Image Path: ${pictureFile.absolutePath}")
                attendanceDao.insert(
                    AttendanceModel(
                        0,
                        user.id,
                        status,
                        SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()),
                        SimpleDateFormat("dd-MM-yyyy").format(Calendar.getInstance().getTime()),
                        pictureFile.absolutePath
                    )
                )

                Log.e("TAG", "Attendance Marked!")
            }, 1000)
        } else {
            attendanceMarked(user.name, 2)
        }
    }

    fun attendanceMarked(label: String, status: Int) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.overlay)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        var relativeLayout = dialog.findViewById<RelativeLayout>(R.id.rv_overlay)
        var tv_overlay_name = dialog.findViewById<TextView>(R.id.tv_overlay_name)
        var iv_overlay_status = dialog.findViewById<ImageView>(R.id.iv_overlay_status)
        var tv_overlay_status = dialog.findViewById<TextView>(R.id.tv_overlay_status)
        var ripple_bg = dialog.findViewById<RippleBackground>(R.id.ripple_bg)
        ripple_bg.startRippleAnimation()

        tv_overlay_name.text = label

        if (status == 0) {
            //in
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tv_overlay_name.setTextColor(getColor(R.color.colorRed))
                iv_overlay_status.setImageDrawable(getDrawable(R.drawable.ic_done_red))
                tv_overlay_status.text = "Out Marked"
                tv_overlay_status.setTextColor(getColor(R.color.colorRed))
            }
        } else if (status == 1) {
            //out
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tv_overlay_name.setTextColor(getColor(R.color.colorGreen))
                iv_overlay_status.setImageDrawable(getDrawable(R.drawable.ic_done_green))
                tv_overlay_status.text = "In Marked"
                tv_overlay_status.setTextColor(getColor(R.color.colorGreen))
            }

        } else if (status == 2) {
            //already marked
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tv_overlay_name.setTextColor(getColor(R.color.colorPrimaryDark))
                iv_overlay_status.setImageDrawable(getDrawable(R.drawable.ic_timer))
                tv_overlay_status.text = "Already Marked. Please wait for 1 minute!"
                tv_overlay_status.setTextColor(getColor(R.color.colorPrimaryDark))
            }
        }

        dialog.show()
        dialogDisplayed = true

        Handler().postDelayed({
            ripple_bg.stopRippleAnimation()
            dialog.dismiss()
            dialogDisplayed = false
        }, 5000)
    }

    fun getDifferenceInMinutes(startDate: Date, endDate: Date): Long {
        //milliseconds
        var different = endDate.time - startDate.time
        println("startDate : $startDate")
        println("endDate : $endDate")
        println("different : $different")
        Log.e("TAG", "Elapsed DAYS: " + TimeUnit.DAYS.convert(different, TimeUnit.MILLISECONDS))
        Log.e("TAG", "Elapsed HOURS: " + TimeUnit.HOURS.convert(different, TimeUnit.MILLISECONDS))
        Log.e(
            "TAG",
            "Elapsed MINUTES: " + TimeUnit.MINUTES.convert(different, TimeUnit.MILLISECONDS)
        )
        return TimeUnit.MINUTES.convert(different, TimeUnit.MILLISECONDS)

    }

    fun isOdd(`val`: Int): Boolean {
        return `val` and 0x01 != 0
    }

    fun hideKeyboard(view: View) {
        val inputMethodManager: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun checkCameraHardware(context: Context): Boolean {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true
        } else {
            // no camera on this device
            return false
        }
    }

    fun getCameraInstance(): Camera? {
        return try {
            Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT) // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    private val mPicture = Camera.PictureCallback { data, _ ->
        Log.e("TAG", "Picture Captured")
        pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE) ?: run {
            Log.e("TAG", "Error creating media file, check storage permissions")
            return@PictureCallback
        }

        try {
            val fos = FileOutputStream(pictureFile)
            fos.write(data)
            fos.close()
            /*Toast.makeText(
                this@RFIDScanner,
                "Picture saved at ${pictureFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show()*/
            resumeCamera()
        } catch (e: FileNotFoundException) {
            Log.e("TAG", "File not found: ${e.message}")
        } catch (e: IOException) {
            Log.e("TAG", "Error accessing file: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        //resumeCamera()
        initCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCamera() // release the camera immediately on pause event
    }

    private fun resumeCamera() {
        if (mCamera != null) {
            mCamera!!.startPreview()
        }
    }

    private fun releaseCamera() {
        mCamera?.release() // release the camera for other applications
        mCamera = null
    }

    private fun getOutputMediaFileUri(type: Int): Uri {
        return Uri.fromFile(getOutputMediaFile(type))
    }

    /** Create a File for saving an image or video */
    private fun getOutputMediaFile(type: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "com.parousia.rfid"
        )
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.e("TAG", "failed to create directory")
                    return null
                }
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("dd-MM-yyyy_HH:mm:ss").format(Date())
        return when (type) {
            MEDIA_TYPE_IMAGE -> {
                File("${mediaStorageDir.path}${File.separator}IMG_${userModel.name}$timeStamp.jpg")
            }
            /*MEDIA_TYPE_VIDEO -> {
                File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4")
            }*/
            else -> null
        }
    }

}