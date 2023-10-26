package au.edu.utas.jackw4.babyapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.RequiresApi
import au.edu.utas.jackw4.babyapp.databinding.ActivityFeedingTabBinding
import au.edu.utas.jackw4.babyapp.databinding.ActivityMainBinding
import au.edu.utas.jackw4.babyapp.databinding.ActivitySleepingTabBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.lang.Math.abs
import java.text.DateFormat
import java.text.SimpleDateFormat
import kotlin.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class SleepingTab : AppCompatActivity() {
    private lateinit var ui : ActivitySleepingTabBinding

    // format date time - obviously the americas uses "mm/dd/yyyy" but for the sake of comfort ill stick with this
    private val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivitySleepingTabBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.sendHome.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }

        ui.sleepChooseStartNew.setOnClickListener{
            pickDateAndTime(this, ui.sleepChooseStartNew)
        }
        ui.sleepChooseEndNew.setOnClickListener{
            pickDateAndTime(this, ui.sleepChooseEndNew)
        }

        val db = Firebase.firestore

        // setting ime options wasnt working so had to do it programatically - https://stackoverflow.com/questions/23036890/setting-edittext-imeoptions-to-actionnext-has-no-effect
        ui.notesSleepNew.imeOptions = EditorInfo.IME_ACTION_DONE
        ui.notesSleepNew.setRawInputType(InputType.TYPE_CLASS_TEXT)

        ui.sleepChooseStartNew.addTextChangedListener(checkFields)
        ui.sleepChooseEndNew.addTextChangedListener(checkFields)
        ui.saveNewSleepBtn.isEnabled = false

        ui.saveNewSleepBtn.setOnClickListener {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
            val dateTimeStart = dateFormat.parse(ui.sleepChooseStartNew.text.toString())
            val dateTimeEnd = dateFormat.parse(ui.sleepChooseEndNew.text.toString())
            val sleepTime = dateTimeEnd.time - dateTimeStart.time

            // seconds arent too important here so we will just insert hours and minutes for the duration
            val duration = TimeSpan(
                hours = TimeUnit.MILLISECONDS.toHours(sleepTime),
                minutes = TimeUnit.MILLISECONDS.toMinutes(sleepTime) % 60,
            )

            val timestampStart = Timestamp(dateTimeStart)
            val timestampEnd = Timestamp(dateTimeEnd)


            val sleepEntry = Entry(
                category = Category.SLEEP,
                notes = ui.notesSleepNew.text.toString(),
                duration = duration,
                startTime = timestampStart,
                endTime = timestampEnd
            )
            val entries = db.collection("entries")
            entries
                .add(sleepEntry)
                .addOnSuccessListener {
                    Log.d(FIREBASE_TAG, "Document created with ${it.id}")
                }
                .addOnFailureListener{
                    Log.e(FIREBASE_TAG, "Error writing document, it", it)
                }
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }
    }

    private val checkFields = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(p0: Editable?) {
            if (ui.sleepChooseStartNew.text.isNotEmpty() && ui.sleepChooseEndNew.text.isNotEmpty()) {
                ui.saveNewSleepBtn.isEnabled = true
            }

        }
    }
    private fun pickDateAndTime(context: Context, input: EditText) {

        //
        val datePicker: DatePickerDialog
        val dateAndTime = Calendar.getInstance()
        val hour = dateAndTime.get(Calendar.HOUR_OF_DAY)
        val minute = dateAndTime.get(Calendar.MINUTE)
        val year = dateAndTime.get(Calendar.YEAR)
        val month = dateAndTime.get(Calendar.MONTH)
        val day = dateAndTime.get(Calendar.DAY_OF_MONTH)

        datePicker = DatePickerDialog(this,
            {_, yearChosen, monthChosen, dayChosen ->
                val selectedDateTime = Calendar.getInstance()
                selectedDateTime.set(Calendar.YEAR, yearChosen)
                selectedDateTime.set(Calendar.MONTH, monthChosen)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayChosen)

                val timePicker = TimePickerDialog(
                    this,
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