package com.symtera.parousiarfid.activities

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Typeface
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.opencsv.CSVWriter
import com.symtera.parousiarfid.R
import com.symtera.parousiarfid.database.ParousiaDatabase
import com.symtera.parousiarfid.database.dao.AttendanceDao
import com.symtera.parousiarfid.database.dao.UserDao
import com.symtera.parousiarfid.models.AttendanceModel
import com.symtera.parousiarfid.models.UserModel
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class AttendanceDisplay : AppCompatActivity() {

    lateinit var table: TableLayout
    lateinit var row: TableRow
    lateinit var sp_dates: Button
    lateinit var btn_export: Button
    lateinit var sv_attendance: SearchView

    lateinit var database: ParousiaDatabase
    lateinit var attendanceDao: AttendanceDao
    lateinit var userDao: UserDao
    lateinit var attendanceList: List<AttendanceModel>
    lateinit var userList: List<UserModel>

    val myCalendar = Calendar.getInstance()
    lateinit var datepicker: DatePickerDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_display)

        init()

    }

    fun init() {

        database = ParousiaDatabase.getDatabase(this@AttendanceDisplay)
        attendanceDao = database.attendanceDao()
        userDao = database.userDao()
        attendanceList = listOf()
        userList = listOf()

        sv_attendance = findViewById(R.id.sv_attendance)

        btn_export = findViewById(R.id.btn_export)
        sp_dates = findViewById(R.id.sp_dates)
        sp_dates.setText(SimpleDateFormat("dd-MM-yyyy").format(myCalendar.getTime()))

        table = findViewById(R.id.table)
        row = TableRow(this)
        val lp = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT)
        lp.width = TableRow.LayoutParams.MATCH_PARENT
        lp.height = TableRow.LayoutParams.MATCH_PARENT
        lp.weight = 1f
        row.layoutParams = lp
        row.gravity = Gravity.CENTER

        attendanceDao.getAttendanceByDay(sp_dates.text.toString()).observe(
            this,
            androidx.lifecycle.Observer {
                attendanceList = it
            })

        userDao.getAll().observe(this, androidx.lifecycle.Observer {
            userList = it
        })

        listeners()
        Handler().postDelayed({
            if (attendanceList.size > 0) {
                setTable()
            }
        }, 200)
    }

    fun listeners() {

        val date =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth -> // TODO Auto-generated method stub
                myCalendar.set(Calendar.YEAR, year)
                myCalendar.set(Calendar.MONTH, monthOfYear)
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                sp_dates.setText(SimpleDateFormat("dd-MM-yyyy").format(myCalendar.getTime()))
                attendanceDao.getAttendanceByDayAndUser(
                    sp_dates.text.toString(), getUserIDByUserName(
                        sv_attendance.query.toString()
                    )!!.id
                ).observe(this, androidx.lifecycle.Observer {
                    attendanceList = it
                })
                Handler().postDelayed({
                    if (attendanceList.size > 0) {
                        setTable()
                    } else {
                        if (table.getParent() != null) {
                            Log.e("TAG", "View Exist!")
                            row.removeAllViews()
                            table.removeAllViews()
                        }
                        Toast.makeText(
                            this@AttendanceDisplay,
                            "No results found!",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }, 200)
            }

        datepicker = DatePickerDialog(
            this@AttendanceDisplay, date, myCalendar
                .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
            myCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datepicker.datePicker.maxDate = System.currentTimeMillis()

        sp_dates.setOnClickListener {
            datepicker.show()
        }

        sv_attendance.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                search(s)
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {

                return true
            }
        })

        btn_export.setOnClickListener {
            exportCSVAsync().execute()
        }

    }

    fun search(query: String) {
        attendanceDao.getAttendanceByUser(getUserIDByUserName(query)!!.id).observe(
            this,
            androidx.lifecycle.Observer {
                attendanceList = it
            })
        Handler().postDelayed({
            if (attendanceList.size > 0) {
                sp_dates.setText("Date")
                setTable()
            }
        }, 200)
    }

    fun setTable() {

        if (table.getParent() != null) {
            Log.e("TAG", "View Exist!")
            row.removeAllViews()
            table.removeAllViews()
        }

        val title_cell_text_size = 18f

        var id = TextView(this)
        id.setText("Id")
        id.gravity = Gravity.CENTER
        id.setTypeface(null, Typeface.BOLD)
        id.setTextColor(resources.getColor(R.color.colorPrimaryDark))
        id.background = resources.getDrawable(R.drawable.border)
        id.setPadding(16, 8, 16, 8)
        id.setTextSize(TypedValue.COMPLEX_UNIT_SP, title_cell_text_size)
        row.addView(id)

        var name = TextView(this)
        name.setText("Name")
        name.gravity = Gravity.CENTER
        name.setTypeface(null, Typeface.BOLD)
        name.setTextColor(resources.getColor(R.color.colorPrimaryDark))
        name.background = resources.getDrawable(R.drawable.border)
        name.setPadding(16, 8, 16, 8)
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        row.addView(name)

        var date = TextView(this)
        date.setText("Date")
        date.gravity = Gravity.CENTER
        date.setTypeface(null, Typeface.BOLD)
        date.setTextColor(resources.getColor(R.color.colorPrimaryDark))
        date.background = resources.getDrawable(R.drawable.border)
        date.setPadding(16, 8, 16, 8)
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, title_cell_text_size)
        row.addView(date)

        var time = TextView(this)
        time.setText("Time")
        time.gravity = Gravity.CENTER
        time.setTypeface(null, Typeface.BOLD)
        time.setTextColor(resources.getColor(R.color.colorPrimaryDark))
        time.background = resources.getDrawable(R.drawable.border)
        time.setPadding(16, 8, 16, 8)
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, title_cell_text_size)
        row.addView(time)

        var status = TextView(this)
        status.setText("Status")
        status.gravity = Gravity.CENTER
        status.setTypeface(null, Typeface.BOLD)
        status.setTextColor(resources.getColor(R.color.colorPrimaryDark))
        status.background = resources.getDrawable(R.drawable.border)
        status.setPadding(16, 8, 16, 8)
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, title_cell_text_size)
        row.addView(status)

        var img = TextView(this)
        img.setText("Image")
        img.gravity = Gravity.CENTER
        img.setTypeface(null, Typeface.BOLD)
        img.setTextColor(resources.getColor(R.color.colorPrimaryDark))
        img.background = resources.getDrawable(R.drawable.border)
        img.setPadding(16, 8, 16, 8)
        img.setTextSize(TypedValue.COMPLEX_UNIT_SP, title_cell_text_size)
        row.addView(img)

        table.addView(row)

        Handler().postDelayed({
            if (attendanceList.size > 0) {
                setTableChild()
            }
        }, 200)
    }

    fun setTableChild() {
        for (attendance in attendanceList) {

            var row = TableRow(this)
            val lp = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT)
            lp.width = TableRow.LayoutParams.MATCH_PARENT
            lp.height = TableRow.LayoutParams.MATCH_PARENT
            lp.weight = 1f
            row.layoutParams = lp
            row.gravity = Gravity.CENTER
            val cell_text_size = 16f

            var id = TextView(this)
            id.setText(attendance.userId.toString())
            id.gravity = Gravity.CENTER
            id.background = resources.getDrawable(R.drawable.border)
            id.setPadding(16, 8, 16, 8)
            id.setTextSize(TypedValue.COMPLEX_UNIT_SP, cell_text_size)
            row.addView(id)

            var name = TextView(this)
            name.setText(getUserByUserID(attendance.userId)?.name)
            name.gravity = Gravity.CENTER
            name.background = resources.getDrawable(R.drawable.border)
            name.setTextColor(resources.getColor(R.color.colorPrimary))
            name.setPadding(16, 8, 16, 8)
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, cell_text_size)
            row.addView(name)

            var date = TextView(this)
            date.setText(attendance.date)
            date.gravity = Gravity.CENTER
            date.background = resources.getDrawable(R.drawable.border)
            date.setPadding(16, 8, 16, 8)
            date.setTextSize(TypedValue.COMPLEX_UNIT_SP, cell_text_size)
            row.addView(date)

            var time = TextView(this)
            time.setText(attendance.time)
            time.gravity = Gravity.CENTER
            time.background = resources.getDrawable(R.drawable.border)
            time.setPadding(16, 8, 16, 8)
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, cell_text_size)
            row.addView(time)

            var status = TextView(this)
            if (attendance.status == 1) {
                status.setText("In")
            } else if (attendance.status == 0) {
                status.setText("Out")
            }
            status.gravity = Gravity.CENTER
            status.background = resources.getDrawable(R.drawable.border)
            status.setPadding(16, 8, 16, 8)
            status.setTextSize(TypedValue.COMPLEX_UNIT_SP, cell_text_size)
            row.addView(status)

            var img = ImageView(this)
            img.setSelected(true)
            img.background = resources.getDrawable(R.drawable.border)
            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(
                this.contentResolver, getImageContentUri(
                    this@AttendanceDisplay,
                    attendance.img
                )!!
            )
            Glide.with(this@AttendanceDisplay)
                .load(bitmap.rotate(270F))
                .apply(RequestOptions().override(150, 80))
                .placeholder(R.drawable.ic_person_outline)
                //.circleCrop()
                .into(img)
            row.addView(img)

            img.setOnClickListener {
                showPhoto(getImageContentUri(this@AttendanceDisplay, attendance.img)!!)
            }

            table.addView(row)
        }
    }

    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun showPhoto(photoUri: Uri) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(photoUri, "image/*")
        startActivity(intent)
    }

    fun getImageContentUri(context: Context, imageFile: String): Uri? {
        val filePath = imageFile
        val cursor: Cursor? = context.getContentResolver().query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf<String>(MediaStore.Images.Media._ID),
            MediaStore.Images.Media.DATA.toString() + "=? ",
            arrayOf(filePath),
            null
        )
        return if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
            cursor.close()
            Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id)
        } else {
            if (File(filePath).exists()) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATA, filePath)
                context.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
            } else {
                null
            }
        }
    }

    fun getUserByUserID(id: Int): UserModel? {
        for (user in userList) {
            if (id == user.id) {
                return user
            }
        }
        return null
    }

    fun getUserIDByUserName(name: String): UserModel? {
        for (user in userList) {
            if (user.name.contains(name, true)) {
                return user
            }
        }
        return null
    }

    private inner class exportCSVAsync : AsyncTask<Void?, Void?, Void?>() {
        var dialog: ProgressDialog? = ProgressDialog(this@AttendanceDisplay)
        override fun onPreExecute() {
            Log.d("TAG", "initRecAsync onPreExecute called")
            dialog!!.setMessage("Initializing...")
            dialog!!.setCancelable(false)
            dialog!!.show()
            super.onPreExecute()
        }

        override fun doInBackground(vararg args: Void?): Void? {
            exportToCSV()
            changeProgressDialogMessage(dialog, "Exporting...")
            return null
        }

        override fun onPostExecute(result: Void?) {
            if (dialog != null && dialog!!.isShowing) {
                dialog!!.dismiss()
            }
            emailFile()
        }
    }

    fun exportToCSV() {
        val exportDir = File(
            Environment.getExternalStorageDirectory().absolutePath,
            "com.symtera.parousialite" + File.separator + "exports"
        )
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, "attendance_export.csv")
        try {
            file.createNewFile()
            val csvWrite = CSVWriter(FileWriter(file))
            var curUser: Cursor = userDao.allUsersCursor()
            csvWrite.writeNext(arrayOf("UserId", "Name", "Status", "Time", "Date"))
            var arr = arrayOf("")
            while (curUser.moveToNext()) {
                val userId = curUser.getString(0)
                Log.e("TAG1", "User Id Out of Loop: " + userId)
                var curAttendance: Cursor = attendanceDao.allAttendanceCursor()
                while (curAttendance.moveToNext()) {
                    Log.e("TAG1", "User Id In Loop: " + userId)
                    Log.e("TAG1", "Attendance Id: " + curAttendance.getString(1))
                    if (curAttendance.getString(1).equals(userId)) {
                        var status = "In"
                        if (curAttendance.getString(2).equals("1")) {
                            status = "In"
                        } else {
                            status = "Out"
                        }
                        Log.e("TAG", "Id: " + curUser.getString(0))
                        Log.e("TAG", "Name: " + curUser.getString(1))
                        Log.e("TAG", "Status: " + status)
                        Log.e("TAG", "Time: " + curAttendance.getString(3))
                        Log.e("TAG", "Date: " + curAttendance.getString(4))
                        arr = arrayOf(
                            curUser.getString(0), curUser.getString(1), status,
                            curAttendance.getString(3), curAttendance.getString(4)
                        )
                        csvWrite.writeNext(arr)
                    }
                }
                curAttendance.close()
                //csvWrite.writeNext(arr)
            }
            csvWrite.close()
            curUser.close()
            Log.e("TAG", "Exported!")
        } catch (sqlEx: Exception) {
            Log.e("Attendance Display", sqlEx.message, sqlEx)
        }
    }

    private fun changeProgressDialogMessage(pd: ProgressDialog?, msg: String) {
        val changeMessage = Runnable { pd!!.setMessage(msg) }
        runOnUiThread(changeMessage)
    }

    fun emailFile() {
        Log.e("TAG", "EmailFile!")
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.setType("text/*")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("huzaifano1@hotmail.com"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Report")
        val root = Environment.getExternalStorageDirectory()
        val pathToMyAttachedFile =
            "com.symtera.parousialite" + File.separator + "exports" + File.separator + "attendance_export.csv"
        val file = File(root, pathToMyAttachedFile)
        Log.e("TAG", "Email File Path: " + file.path)
        if (!file.exists() || !file.canRead()) {
            return
        }
        //val uri: Uri = Uri.parse("file:/"+file.path
        val uri: Uri = FileProvider.getUriForFile(this, packageName + ".fileprovider", file)
        Log.e("TAG", "Email URI: " + uri)
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        //emailIntent.setDataAndType(uri,"text/csv")
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(emailIntent, "Pick an Email provider"))
    }

}