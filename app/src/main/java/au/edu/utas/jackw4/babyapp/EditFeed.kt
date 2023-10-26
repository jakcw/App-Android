package au.edu.utas.jackw4.babyapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.text.InputType.TYPE_CLASS_NUMBER
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import au.edu.utas.jackw4.babyapp.databinding.ActivityEditFeedBinding
import au.edu.utas.jackw4.babyapp.databinding.ActivityEditNappyBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration

class EditFeed : AppCompatActivity() {

    private val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
    private val date = DatePicker()
    private lateinit var ui : ActivityEditFeedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityEditFeedBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.sendHome.setOnClickListener {
            val i = Intent(this, HistoryTab::class.java)
            startActivity(i)
        }

        val db = Firebase.firestore
        val entries = db.collection("entries")

        val feedID = intent.getIntExtra(ENTRY_INDEX, -1)
        val feedObject = all_entries[feedID]
        val timestampStart = feedObject.startTime?.toDate()
        val feedType = feedObject.feedType
        val duration = feedObject.duration

        ui.hrsEdit.setText(feedObject.duration?.hours.toString())
        ui.minsEdit.setText(feedObject.duration?.minutes.toString())
        ui.secsEdit.setText(feedObject.duration?.seconds.toString())

        for (i in 0 until ui.feedingTypeSelect.childCount) {
            val selected = ui.feedingTypeSelect.getChildAt(i) as RadioButton
            if (selected.text.toString() == feedType.toString()) {
                selected.isChecked = true
                break
            }

        }
        ui.chooseDateFeed.setText(dateFormat.format(timestampStart))

        ui.feedNotes.imeOptions = EditorInfo.IME_ACTION_DONE
        ui.feedNotes.setRawInputType(InputType.TYPE_CLASS_TEXT)

        if (feedObject.notes.toString().isNotEmpty()) {
            ui.feedNotes.setText(feedObject.notes.toString())
        }

        ui.chooseDateFeed.setOnClickListener {
            date.pickDateAndTime(this, ui.chooseDateFeed)
        }

        // basically so user is restricted to input numbers
        ui.hrsEdit.inputType = TYPE_CLASS_NUMBER
        ui.minsEdit.inputType = TYPE_CLASS_NUMBER
        ui.secsEdit.inputType = TYPE_CLASS_NUMBER

        ui.saveEditFeedBtn.setOnClickListener {
            val dateTimeStart = dateFormat.parse(ui.chooseDateFeed.text.toString())
            val timestampStart = Timestamp(dateTimeStart)
            val hrs = ui.hrsEdit.text.toString().toLong()
            val mins = ui.minsEdit.text.toString().toLong()
            val secs = ui.secsEdit.text.toString().toLong()

            val feedType = when (ui.feedingTypeSelect.checkedRadioButtonId) {
                ui.leftSideBtn.id -> "Left"
                ui.rightSideBtn.id -> "Right"
                ui.bottleBtn.id -> "Bottle"
                else -> null
            }

            feedObject.startTime = timestampStart
            feedObject.feedType = feedType
            feedObject.notes = ui.feedNotes.text.toString()
            feedObject.duration = TimeSpan(hrs, mins, secs)

            entries.document(feedObject.id!!)
                .set(feedObject)
                .addOnSuccessListener {
                    Log.d(FIREBASE_TAG, "Successful update feed ${feedObject.id}")
                    finish()
                }
        }
    }
}