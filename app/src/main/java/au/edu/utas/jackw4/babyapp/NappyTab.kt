package au.edu.utas.jackw4.babyapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.Manifest.permission.CAMERA
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import au.edu.utas.jackw4.babyapp.databinding.ActivityFeedingTabBinding
import au.edu.utas.jackw4.babyapp.databinding.ActivityNappyTabBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.protobuf.ByteOutput
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.sql.Time
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

const val REQUEST_IMAGE_CAPTURE = 1
const val IMAGE_CODE = 2

class NappyTab : AppCompatActivity() {
    private lateinit var ui : ActivityNappyTabBinding
    private lateinit var currentPhotoPath: String
    private var takenImage: Bitmap? = null

    // format date time - obviously the americas uses "mm/dd/yyyy" but for the sake of comfort ill stick with this
    private val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityNappyTabBinding.inflate(layoutInflater)
        setContentView(ui.root)


        ui.sendHome.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }

        ui.nappyChooseTime.setOnClickListener {
            pickDateAndTime(this, ui.nappyChooseTime)

        }


        // taken from https://www.youtube.com/watch?v=DPHkhamDoyc&t=1s
        ui.takePicNappy.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(this.packageManager) != null) {
                startActivityForResult(takePictureIntent, IMAGE_CODE)
            } else {
                Log.d("CAMERA", "Unable to open camera")
            }
        }

        val db = Firebase.firestore

        // Save button disable unless date has been filled out and at least one radio button is selected
        ui.nappyChooseTime.addTextChangedListener(checkFields)
        val checkRadioGroup = ui.nappyTypeGroup.setOnCheckedChangeListener { buttongroup, i -> updateButtonState() }
        ui.saveNewNappyBtn.isEnabled = false

        ui.notesNappyNew.imeOptions = EditorInfo.IME_ACTION_DONE
        ui.notesNappyNew.setRawInputType(InputType.TYPE_CLASS_TEXT)

        ui.saveNewNappyBtn.setOnClickListener {
            val dateTimeStr = ui.nappyChooseTime.text.toString()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
            val datetime = dateFormat.parse(dateTimeStr)
            Log.d("CHECK", ui.nappyChooseTime.text.toString())
            val timestmp = Timestamp(datetime)

            // Convert the selected radio button to its enum type
            val nappyType = when (ui.nappyTypeGroup.checkedRadioButtonId) {
                ui.radioWet.id -> NappyType.WET
                ui.radioBoth.id -> NappyType.BOTH
                ui.radioDirty.id -> NappyType.DIRTY

                else -> null
            }

            val nappyEntry = Entry(
                category = Category.NAPPY_CHANGE,
                notes = ui.notesNappyNew.text.toString(),
                nappy = nappyType,
                startTime = timestmp // no end time for nappy entries and didn't want to add extra time variable so will use start time
            )
            val entries = db.collection("entries")
            entries
                .add(nappyEntry)
                .addOnSuccessListener {
                    Log.d(FIREBASE_TAG, "Document created with ${it.id}")

                    // taken from https://firebase.google.com/docs/storage/android/upload-files
                    val entryReference = FirebaseStorage.getInstance().reference.child("entries/" + it.id + ".jpg")

                    val baos = ByteArrayOutputStream()
                    takenImage?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val data = baos.toByteArray()
                    val uploadTask = entryReference.putBytes(data)

                    uploadTask.addOnFailureListener {
                        Log.e(FIREBASE_TAG, "Error uploading image to Firebase Storage", it)

                    }.addOnSuccessListener { taskSnapshot ->
                        Log.d(FIREBASE_TAG, "Image uploaded to Firebase Storage")
                    }

                }
                .addOnFailureListener{
                    Log.e(FIREBASE_TAG, "Error writing document, it", it)
                }

            val i = Intent(this, MainActivity::class.java)
            startActivity(i)


        }
    }

    // Partially taken from stack overflow, other half from https://www.youtube.com/watch?v=DPHkhamDoyc&t=1s
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //After taking a picture.
        if(requestCode == IMAGE_CODE && resultCode == Activity.RESULT_OK && data != null) {
            takenImage = data.extras?.get("data") as Bitmap

        }else{
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


    private fun updateButtonState() {
        if (ui.nappyChooseTime.text.isNotEmpty() && ui.nappyTypeGroup.checkedRadioButtonId != -1) {
            ui.saveNewNappyBtn.isEnabled = true
        }
    }

    private val checkFields = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            updateButtonState()
        }

        override fun afterTextChanged(p0: Editable?) {
        }
    }

    private fun pickDateAndTime(context: Context, input: EditText) {


        val datePicker: DatePickerDialog
        val dateAndTime = Calendar.getInstance()
        val hour = dateAndTime.get(Calendar.HOUR_OF_DAY)
        val minute = dateAndTime.get(Calendar.MINUTE)
        val year = dateAndTime.get(Calendar.YEAR)
        val month = dateAndTime.get(Calendar.MONTH)
        val day = dateAndTime.get(Calendar.DAY_OF_MONTH)

        datePicker = DatePickerDialog(context,
            {_, yearChosen, monthChosen, dayChosen ->
                val selectedDateTime = Calendar.getInstance()
                selectedDateTime.set(Calendar.YEAR, yearChosen)
                selectedDateTime.set(Calendar.MONTH, monthChosen)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayChosen)

                val timePicker = TimePickerDialog(
                    context,
                    {_, hourOfDay, minuteOfHour ->
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDateTime.set(Calendar.MINUTE, minuteOfHour)
                        input.setText(dateFormat.format(selectedDateTime.time))
                    }, hour, minute, false
                )
                timePicker.show()
            }, year, month, day
        )

        datePicker.show()
    }

}