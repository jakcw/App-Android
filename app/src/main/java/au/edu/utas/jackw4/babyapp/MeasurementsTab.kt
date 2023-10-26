package au.edu.utas.jackw4.babyapp

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import au.edu.utas.jackw4.babyapp.databinding.ActivityFeedingTabBinding
import au.edu.utas.jackw4.babyapp.databinding.ActivityMeasurementsTabBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.w3c.dom.Text
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


const val FIREBASE_TAG = "FirebaseLogging"

class MeasurementsTab : AppCompatActivity() {
    private lateinit var ui : ActivityMeasurementsTabBinding
    private val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMeasurementsTabBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.sendHome.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }

        ui.chooseDateMeasurement.setOnClickListener{
            pickDate(this, ui.chooseDateMeasurement)
        }

        // set min and max height for slider, user can still input values outside of this range
        val minHeight = 30
        val maxHeight = 120
        val minWeight = 1
        val maxWeight = 40
        ui.seekBarHeight.min = minHeight
        ui.seekBarHeight.max = maxHeight
        ui.seekBarWeight.min = minWeight
        ui.seekBarWeight.max = maxWeight

            // slider bar code adapted from https://stackoverflow.com/questions/35863575/adjust-edittext-with-seekbar

        ui.chooseHeightManual.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toIntOrNull() ?: minHeight
                ui.seekBarHeight.progress = value
            }
        })

        ui.chooseWeightManual.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toIntOrNull() ?: minWeight
                ui.seekBarWeight.progress = value
            }
        })

        ui.seekBarHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) { // Update the EditText's value to match the new SeekBar progress
                ui.chooseHeightManual.setText(value.toString())
            }
        })

        ui.seekBarWeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) { // Update the EditText's value to match the new SeekBar progress
                ui.chooseWeightManual.setText(value.toString())
            }
        })

        val db = Firebase.firestore

        ui.measurementNotes.imeOptions = EditorInfo.IME_ACTION_DONE
        ui.measurementNotes.setRawInputType(InputType.TYPE_CLASS_TEXT)

        ui.chooseWeightManual.addTextChangedListener(checkFields)
        ui.chooseHeightManual.addTextChangedListener(checkFields)
        ui.chooseDateMeasurement.addTextChangedListener(checkFields)
        ui.saveNewMeasurementBtn.isEnabled = false

        ui.saveNewMeasurementBtn.setOnClickListener {
            val dateTimeStr = ui.chooseDateMeasurement.text.toString()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy")
            val date = dateFormat.parse(dateTimeStr)
            val timestmp = Timestamp(date)

            val heightEntry = Entry(
                category = Category.MEASUREMENT,
                height = ui.chooseHeightManual.text.toString().toInt(),
                weight = ui.chooseWeightManual.text.toString().toInt(),
                notes = ui.measurementNotes.text.toString(),
                startTime = timestmp
            )
            val entries = db.collection("entries")
            entries
                .add(heightEntry)
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
            if (ui.chooseWeightManual.text.isNotEmpty() && ui.chooseHeightManual.text.isNotEmpty()
                && ui.chooseDateMeasurement.text.isNotEmpty()) {
                ui.saveNewMeasurementBtn.isEnabled = true
            }

        }
    }

    private fun pickDate(context: Context, input: EditText) {


        val datePicker: DatePickerDialog
        val dateAndTime = Calendar.getInstance()
        val chosenDate = Calendar.getInstance() // date that will be set
        val year = dateAndTime.get(Calendar.YEAR)
        val month = dateAndTime.get(Calendar.MONTH)
        val day = dateAndTime.get(Calendar.DAY_OF_MONTH)

        datePicker = DatePickerDialog(context,
            {_, yearChosen, monthChosen, dayChosen ->
                chosenDate.set(Calendar.YEAR, yearChosen)
                chosenDate.set(Calendar.MONTH, monthChosen)
                chosenDate.set(Calendar.DAY_OF_MONTH, dayChosen)
                input.setText(dateFormat.format(chosenDate.time))
            },year, month, day
        )
        datePicker.show()
    }
}