package au.edu.utas.jackw4.babyapp

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import au.edu.utas.jackw4.babyapp.databinding.ActivityEditNappyBinding
import au.edu.utas.jackw4.babyapp.databinding.ActivityEditSleepBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class EditNappy : AppCompatActivity() {

    private val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
    private val date = DatePicker()
    private lateinit var ui : ActivityEditNappyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityEditNappyBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.sendHome.setOnClickListener {
            val i = Intent(this, HistoryTab::class.java)
            startActivity(i)
        }

        val db = Firebase.firestore
        val entries = db.collection("entries")

        val nappyId = intent.getIntExtra(ENTRY_INDEX, -1)
        val nappyObject = all_entries[nappyId]
        val timestampStart = nappyObject.startTime?.toDate()
        val nappyType = nappyObject.nappy

        for (i in 0 until ui.nappyTypeGroup.childCount) {
            val selected = ui.nappyTypeGroup.getChildAt(i) as RadioButton
            if (selected.text.toString().uppercase() == nappyType.toString()) {
                selected.isChecked = true
                break
            }
        }
        val entryRef = FirebaseStorage.getInstance().getReference("entries/" + nappyObject.id + ".jpg")
        val ONE_MEGABYTE: Long = 1024 * 1024

        entryRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
            val takenImage = BitmapFactory.decodeByteArray(it, 0, it.size)
            ui.nappyPic.setImageBitmap(takenImage)
            Log.d(FIREBASE_TAG, "Downloaded image")
        }.addOnFailureListener {
            Log.d(FIREBASE_TAG, "Failed to download image")
        }
        ui.nappyChooseTime.setText(dateFormat.format(timestampStart))

        ui.notesNappyEdit.imeOptions = EditorInfo.IME_ACTION_DONE
        ui.notesNappyEdit.setRawInputType(InputType.TYPE_CLASS_TEXT)

        if (nappyObject.notes.toString().isNotEmpty()) {
            ui.notesNappyEdit.setText(nappyObject.notes.toString())
        }

        ui.nappyChooseTime.setOnClickListener {
            date.pickDateAndTime(this, ui.nappyChooseTime)
        }

        ui.saveEditNappyBtn.setOnClickListener {
            val dateTimeStart = dateFormat.parse(ui.nappyChooseTime.text.toString())
            val timestampStart = Timestamp(dateTimeStart)
            nappyObject.notes = ui.notesNappyEdit.text.toString()
            nappyObject.startTime = timestampStart

            val nappyType = when (ui.nappyTypeGroup.checkedRadioButtonId) {
                ui.radioWet.id -> NappyType.WET
                ui.radioBoth.id -> NappyType.BOTH
                ui.radioDirty.id -> NappyType.DIRTY

                else -> null
            }

            nappyObject.nappy = nappyType

            entries.document(nappyObject.id!!)
                .set(nappyObject)
                .addOnSuccessListener {
                    Log.d(FIREBASE_TAG, nappyObject.id.toString())
                    Log.d(FIREBASE_TAG, "Successful update nappy ${nappyObject.id}")
                    finish()
                }
        }





    }
}