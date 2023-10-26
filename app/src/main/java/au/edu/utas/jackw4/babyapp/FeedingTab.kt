package au.edu.utas.jackw4.babyapp


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import android.preference.PreferenceManager
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Chronometer
import androidx.appcompat.app.AppCompatActivity
import au.edu.utas.jackw4.babyapp.databinding.ActivityFeedingTabBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

const val TAG = "BUTTON"
class FeedingTab : AppCompatActivity() {
    private lateinit var ui : ActivityFeedingTabBinding
    private var isRecording = false
    private lateinit var sharedPreferences : SharedPreferences
    private var currentDateTime = Timestamp.now()
    private lateinit var chronometer: Chronometer
    private var timeElapsed: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityFeedingTabBinding.inflate(layoutInflater)
        setContentView(ui.root)



        ui.sendHome.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }

        val db = Firebase.firestore
        var dateTimeRecorded = false

        // save time and selected button - https://www.youtube.com/watch?v=lDpd3mLWYK4
        sharedPreferences  = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        timeElapsed = sharedPreferences.getLong("time_elapsed", 0)
        Log.d("UI_CHECK", timeElapsed.toInt().toString())
        val selectedFeedType = sharedPreferences.getInt("selected_feed_type", -1)
        if (selectedFeedType !=  -1) {
            ui.feedingTypeSelect.check(ui.feedingTypeSelect.getChildAt(selectedFeedType).id)

        }


        // Chronometer based on this video - https://www.youtube.com/watch?v=j4Zwf2foAN0 and
        // kotlin documentation - https://developer.android.com/reference/kotlin/android/widget/Chronometer
        chronometer = ui.stopwatchView
        chronometer.base = SystemClock.elapsedRealtime() - timeElapsed



        ui.buttonPlayPause.setOnClickListener {
            // the time and date of feed will be recorded from when we click the play button
            if (!dateTimeRecorded) {
                currentDateTime = Timestamp.now()
                dateTimeRecorded = true
            }

            if (isRecording) {
                chronometer.stop()
                timeElapsed = SystemClock.elapsedRealtime() - chronometer.base

                with(sharedPreferences.edit()) {
                    putLong("time_elapsed", timeElapsed)
                    putInt("selected_feed_type", ui.feedingTypeSelect.indexOfChild(findViewById(ui.feedingTypeSelect.checkedRadioButtonId)))
                    apply()
                }

                ui.buttonPlayPause.text = "Play"
                ui.saveNewFeedBtn.isEnabled = true
            } else {
                chronometer.base = SystemClock.elapsedRealtime() - timeElapsed
                chronometer.start()
                ui.buttonPlayPause.text = "Pause"
                ui.saveNewFeedBtn.isEnabled = false
            }
            isRecording = !isRecording
        }

        ui.buttonReset.setOnClickListener {
            chronometer.base = SystemClock.elapsedRealtime()
            chronometer.stop()
            timeElapsed = 0
            isRecording = false
            dateTimeRecorded = false
            ui.buttonPlayPause.text = "Start"

            if (ui.feedingTypeSelect.checkedRadioButtonId != -1) {
                ui.saveNewFeedBtn.isEnabled = true
            }

            with(sharedPreferences.edit()) {
                remove("time_elapsed")
                apply()
            }

        }

        ui.feedNotes.imeOptions = EditorInfo.IME_ACTION_DONE
        ui.feedNotes.setRawInputType(InputType.TYPE_CLASS_TEXT)
        ui.feedingTypeSelect.setOnCheckedChangeListener { _, _ -> updateButtonState() }



        ui.saveNewFeedBtn.setOnClickListener {


            with(sharedPreferences.edit()) {
                remove("time_elapsed")
                remove("selected_feed_type")
                apply()
            }

            // convert chronometer time to hours, minutes and seconds - need to calculate this on click or all values will be zero
            val hours = TimeUnit.MILLISECONDS.toHours(timeElapsed)
            val mins = TimeUnit.MILLISECONDS.toMinutes(timeElapsed - TimeUnit.HOURS.toMillis(hours))
            val sec = TimeUnit.MILLISECONDS.toSeconds(timeElapsed - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(mins))

            // get values for database insert

            val feedType = when (ui.feedingTypeSelect.checkedRadioButtonId) {
                ui.leftSideBtn.id -> "Left"
                ui.rightSideBtn.id -> "Right"
                ui.bottleBtn.id -> "Bottle"
                else -> null
            }


            val feedEntry = Entry(
                startTime = currentDateTime,
                category = Category.FEEDING,
                duration = TimeSpan(hours, mins, sec),
                notes = ui.feedNotes.text.toString(),
                feedType = feedType
            )

            val entries = db.collection("entries")
            entries
                .add(feedEntry)
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

    override fun onPause() {
        super.onPause()

        with(sharedPreferences.edit()) {
            putInt("selected_feed_type", ui.feedingTypeSelect.indexOfChild(findViewById(ui.feedingTypeSelect.checkedRadioButtonId)))
            apply()
        }
        if (isRecording) {
            chronometer.stop()
            timeElapsed = SystemClock.elapsedRealtime() - chronometer.base

            // Save the elapsed time to SharedPreferences
            with(sharedPreferences.edit()) {

                putLong("time_elapsed", timeElapsed)
                apply()
            }

        }


    }

    override fun onResume() {
        super.onResume()

        // if a timer has started, all buttons should be usable
        currentDateTime = Timestamp.now()
        timeElapsed = sharedPreferences.getLong("time_elapsed", 0)
        if(timeElapsed > 0) {
            ui.saveNewFeedBtn.isEnabled = true
            ui.buttonPlayPause.isEnabled = true
        } else {
            ui.feedingTypeSelect.clearCheck()
            ui.saveNewFeedBtn.isEnabled = false
            ui.buttonPlayPause.isEnabled = false
        }
        if (isRecording) {
            chronometer.base = SystemClock.elapsedRealtime() - timeElapsed
            chronometer.start()
        }
    }




    private fun updateButtonState() {
        if (ui.feedingTypeSelect.checkedRadioButtonId != -1) {
            ui.saveNewFeedBtn.isEnabled = true
            ui.buttonPlayPause.isEnabled = true
        }
    }

}