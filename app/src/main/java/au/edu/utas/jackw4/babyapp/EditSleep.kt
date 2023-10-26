package au.edu.utas.jackw4.babyapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import au.edu.utas.jackw4.babyapp.databinding.ActivityEditSleepBinding
import com.google.firebase.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import au.edu.utas.jackw4.babyapp.DatePicker
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class EditSleep : AppCompatActivity() {
    private val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
    private val date = DatePicker()
    private lateinit var ui : ActivityEditSleepBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityEditSleepBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.sendHome.setOnClickListener {
            val i = Intent(this, HistoryTab::class.java)
            startActivity(i)
        }
        val sleepID = intent.getIntExtra(ENTRY_INDEX, -1)
        val sleepObject = all_entries[sleepID]
        val timestampStart = sleepObject.startTime?.toDate()
        val timestampEnd = sleepObject.endTime?.toDate()


        ui.sleepChooseStartEdit.setText(dateFormat.format(timestampStart))
        ui.sleepChooseEndEdit.setText(dateFormat.format(timestampEnd))
        ui.notesSleepEdit.imeOptions = EditorInfo.IME_ACTION_DONE
        ui.notesSleepEdit.setRawInputType(InputType.TYPE_CLASS_TEXT)

        if (sleepObject.notes.toString().isNotEmpty()) {
            ui.notesSleepEdit.setText(sleepObject.notes.toString())

        }

        ui.sleepChooseStartEdit.setOnClickListener{
            date.pickDateAndTime(this, ui.sleepChooseStartEdit)
        }

        ui.sleepChooseEndEdit.setOnClickListener {
            date.pickDateAndTime(this, ui.sleepChooseEndEdit)
        }

        val db = Firebase.firestore
        val entries = db.collection("entries")

        ui.saveEditSleepBtn.setOnClickListener {
            val dateTimeStart = dateFormat.parse(ui.sleepChooseStartEdit.text.toString())
            val dateTimeEnd = dateFormat.parse(ui.sleepChooseEndEdit.text.toString())
            val sleepTime = dateTimeEnd.time - dateTimeStart.time

            // seconds arent too important here so we will just insert hours and minutes for the duration
            val duration = TimeSpan(
                hours = TimeUnit.MILLISECONDS.toHours(sleepTime),
                minutes = TimeUnit.MILLISECONDS.toMinutes(sleepTime) % 60,
            )

            val timestampStart = Timestamp(dateTimeStart)
            val timestampEnd = Timestamp(dateTimeEnd)
            sleepObject.notes = ui.notesSleepEdit.text.toString()
            sleepObject.duration = duration
            sleepObject.startTime = timestampStart
            sleepObject.endTime = timestampEnd

            entries.document(sleepObject.id!!)
                .set(sleepObject)
                .addOnSuccessListener {
                    Log.d(FIREBASE_TAG, "Successfull update sleep ${sleepObject.id}")
                    finish()
                }


        }
    }




}