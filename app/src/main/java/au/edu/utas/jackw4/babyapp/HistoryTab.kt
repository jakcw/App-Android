package au.edu.utas.jackw4.babyapp

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.edu.utas.jackw4.babyapp.databinding.ActivityFeedingTabBinding
import au.edu.utas.jackw4.babyapp.databinding.ActivityHistoryTabBinding
import au.edu.utas.jackw4.babyapp.databinding.MyHistoryItemBinding
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

val all_entries = mutableListOf<Entry>()
const val ENTRY_INDEX = "Entry_Index"
class HistoryTab : AppCompatActivity() {
    private lateinit var ui : ActivityHistoryTabBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityHistoryTabBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.sendHome.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }

        ui.entryList.adapter = EntryAdapter(entries = all_entries)
        val db = Firebase.firestore
        ui.entryList.layoutManager = LinearLayoutManager(this)

        val entries = db.collection("entries")

        entries
            .orderBy("startTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                all_entries.clear()
                for (document in result) {
                    val entry = document.toObject<Entry>()
                    entry.id = document.id
                    all_entries.add(entry)
                }

                (ui.entryList.adapter as EntryAdapter).notifyDataSetChanged()
            }

        // Implementing filter by category functionality - https://www.youtube.com/watch?v=sJ-Z9G0SDhc&t=792s
        val entryTypes = mutableSetOf<Category>() // holds category of data we are currently displaying

        // onCheckedChangeListener help - https://stackoverflow.com/questions/44150185/kotlin-android-how-implement-checkbox-oncheckedchangelistener
        ui.feedFilter.setOnCheckedChangeListener { _, selected ->
            if (selected) {
                entryTypes.add(Category.FEEDING)
            } else {
                entryTypes.remove(Category.FEEDING)
            }
            filterType(entryTypes)
        }
        ui.nappyFilter.setOnCheckedChangeListener { _, selected ->
            if (selected) {
                entryTypes.add(Category.NAPPY_CHANGE)
            } else {
                entryTypes.remove(Category.NAPPY_CHANGE)
            }
            filterType(entryTypes)
        }

        ui.sleepFilter.setOnCheckedChangeListener { _, selected ->
            if (selected) {
                entryTypes.add(Category.SLEEP)
            } else {
                entryTypes.remove(Category.SLEEP)
            }
            filterType(entryTypes)
        }
        ui.heightFilter.setOnCheckedChangeListener { _, selected ->
            if (selected) {
                entryTypes.add(Category.MEASUREMENT)
            } else {
                entryTypes.remove(Category.MEASUREMENT)
            }
            filterType(entryTypes)
        }
    }

    private fun filterType(entryTypes: Set<Category>) {
        val filteredEntries = if (entryTypes.isEmpty()) {
            all_entries
        } else {
            all_entries.filter {it.category in entryTypes} // filter by category in the list of all entries
        }

        (ui.entryList.adapter as EntryAdapter).updateEntries(filteredEntries.toMutableList())



    }

    inner class EntryHolder(var ui: MyHistoryItemBinding) : RecyclerView.ViewHolder(ui.root) {}

    inner class EntryAdapter(private var entries: MutableList<Entry>) : RecyclerView.Adapter<EntryHolder>()
    {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryHolder {
            val ui = MyHistoryItemBinding.inflate(layoutInflater, parent, false)
            return EntryHolder(ui)
        }

        override fun getItemCount(): Int {
            return entries.size
        }

        // called within filterType() so that the entries displayed in the onBindViewHolder change.
        // this changes the values in "entries" for the whole EntryAdapter innerclass
        fun updateEntries(entries: MutableList<Entry>) {
            this.entries = entries
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: EntryHolder, position: Int) {
            val entry = entries[position]


            // long click for deleting entries
            holder.ui.root.setOnLongClickListener {


                // https://stackoverflow.com/questions/27965662/how-can-i-change-default-dialog-button-text-color-in-android-5 changing color of alert-dialog
                val builder = AlertDialog.Builder(holder.ui.root.context, R.style.AlertDialogTheme)

                builder.setTitle("Delete entry")
                    .setMessage("Are you sure you want to delete this entry?")
                    .setPositiveButton("Delete") { _, _ ->
                        val db = Firebase.firestore
                        db.collection("entries").document(entry.id!!).delete()
                            .addOnSuccessListener {
                                val removed = holder.adapterPosition
                                entries.removeAt(position)
                                notifyItemRemoved(position)
                                notifyItemRangeChanged(removed, itemCount - removed) //notifyitemremoved was not working along so had to use this - https://ericntd.medium.com/advanced-recyclerview-insertion-and-deletion-how-to-maintain-position-aebc0b0b5327
                            }
                            .addOnFailureListener {
                                Log.d(FIREBASE_TAG, "Failed to delete item")
                            }
                    }
                    .setNegativeButton("Cancel") { alert, _ ->
                        alert.dismiss()
                    }
                    .show()

                true
            }


            // each linear layout within the recycler view displays data depending on the category in firebase
            val context = holder.itemView.context
            when (entry.category) {
                Category.MEASUREMENT -> {
                    holder.ui.iconHolder.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.measure_resize))
                    holder.ui.editEntryBtn.visibility = View.INVISIBLE
                    holder.ui.txtCategory.text = "Height and weight measurement"
                    val timestamp = entry.startTime?.toDate()
                    val dateformat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    holder.ui.txtDateTime.text = dateformat.format(timestamp)
                    holder.ui.txtVarious.text = "Height: " + entry.height.toString() + "cm, Weight: " + entry.weight.toString() + "kg"

                    if (entry.notes?.isNotEmpty() == true) {
                        holder.ui.txtNotes.text = entry.notes.toString()
                        holder.ui.txtNotes.visibility = View.VISIBLE
                    } else {
                        holder.ui.txtNotes.visibility = View.GONE
                    }
                }
                Category.FEEDING -> {
                    holder.ui.iconHolder.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.bottle_resize))

                    holder.ui.editEntryBtn.visibility = View.VISIBLE
                    holder.ui.txtCategory.text = "Feeding session - " + entry.feedType.toString()
                    val timestamp = entry.startTime?.toDate()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())

                    // could have made this a lot cleaner for displaying in history
                    if (entry.duration?.hours?.toInt() == 0 && entry.duration?.minutes?.toInt() == 0) {
                        holder.ui.txtVarious.text = entry.duration?.seconds.toString() + " seconds"
                    } else {
                        holder.ui.txtVarious.text = entry.duration?.hours.toString() + " hours, " +
                                entry.duration?.minutes.toString() + " minutes, " + entry.duration?.seconds.toString() + " seconds"
                    }

                    holder.ui.txtDateTime.text = dateFormat.format(timestamp)

                    if (entry.notes?.isNotEmpty() == true) {
                        holder.ui.txtNotes.text = entry.notes.toString()
                        holder.ui.txtNotes.visibility = View.VISIBLE
                    } else {
                        holder.ui.txtNotes.visibility = View.GONE
                    }

                }

                Category.NAPPY_CHANGE -> {
                    holder.ui.iconHolder.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.nappy_resize))
                    holder.ui.editEntryBtn.visibility = View.VISIBLE
                    val timestamp = entry.startTime?.toDate()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
                    holder.ui.txtCategory.text = "Nappy change"
                    holder.ui.txtDateTime.text = dateFormat.format(timestamp)
                    holder.ui.txtVarious.text = entry.nappy.toString().lowercase().replaceFirstChar { it.uppercase() }
                    if (entry.notes?.isNotEmpty() == true) {
                        holder.ui.txtNotes.text = entry.notes.toString()
                        holder.ui.txtNotes.visibility = View.VISIBLE
                    } else {
                        holder.ui.txtNotes.visibility = View.GONE
                    }

                }

                Category.SLEEP -> {
                    holder.ui.iconHolder.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.sleep_resize))
                    holder.ui.editEntryBtn.visibility = View.VISIBLE
                    val timestampStart = entry.startTime?.toDate()
                    val timestampEnd = entry.endTime?.toDate()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
                    holder.ui.txtCategory.text = "Sleep"
                    holder.ui.txtDateTime.text = dateFormat.format(timestampStart) + " - " + dateFormat.format(timestampEnd)
                    holder.ui.txtVarious.text = "Slept for " + entry.duration?.hours + " hrs, " + entry.duration?.minutes + " minutes"

                    if (entry.notes?.isNotEmpty() == true) {
                        holder.ui.txtNotes.text = entry.notes.toString()
                        holder.ui.txtNotes.visibility = View.VISIBLE
                    } else {
                        holder.ui.txtNotes.visibility = View.GONE
                    }
                }
                else -> null
            }

            holder.ui.root.setOnClickListener {
                Log.d("POSITION", holder.adapterPosition.toString() + " "+ holder.layoutPosition.toString())
                when (entry.category) {
                    Category.SLEEP -> {
                        val i = Intent(holder.ui.root.context, EditSleep::class.java)
                        i.putExtra(ENTRY_INDEX, all_entries.indexOf(entry))
                        startActivity(i)
                    }
                    Category.NAPPY_CHANGE -> {
                        val i = Intent(holder.ui.root.context, EditNappy::class.java)
                        i.putExtra(ENTRY_INDEX, all_entries.indexOf(entry))
                        startActivity(i)
                    }
                    Category.FEEDING -> {
                        val i = Intent(holder.ui.root.context, EditFeed::class.java)
                        i.putExtra(ENTRY_INDEX, all_entries.indexOf(entry))
                        startActivity(i)
                    }
                }
            }






        }


    }

    override fun onResume() {
        super.onResume()
        ui.entryList.adapter?.notifyDataSetChanged() //without a more complicated set-up, we can't be more specific than "dataset changed"
    }



}