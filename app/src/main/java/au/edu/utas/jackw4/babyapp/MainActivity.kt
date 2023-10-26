package au.edu.utas.jackw4.babyapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import au.edu.utas.jackw4.babyapp.databinding.ActivityMainBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var ui : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)
        val db = Firebase.firestore


        ui.historyBtn.setOnClickListener {
            val i = Intent(this, HistoryTab::class.java)
            startActivity(i)
        }

        ui.feedingBtn.setOnClickListener {
            val i = Intent(this, FeedingTab::class.java)
            startActivity(i)
        }

        ui.sleepBtn.setOnClickListener {
            val i = Intent(this, SleepingTab::class.java)
            startActivity(i)
        }

        ui.nappyBtn.setOnClickListener {
            val i = Intent(this, NappyTab::class.java)
            startActivity(i)
        }


        ui.heightBtn.setOnClickListener {
            val i = Intent(this, MeasurementsTab::class.java)
            startActivity(i)
        }

    }
}