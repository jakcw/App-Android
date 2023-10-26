package au.edu.utas.jackw4.babyapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/*
 a lot of data shares characteristics (start and end times, notes) so each entry
 will simply cover all data we will need across all entry categories. This will greatly
 simplify querying the data when its shown in the history tab - we can just filter entries based on
 category
 */

data class Entry(
    @get:Exclude var id : String? = null,

    var category : Category? = null, // sleep, nappy change, feed, etc.
    var startTime: Timestamp? = null, // start time of sleep or feed
    var endTime: Timestamp? = null, // end time of sleep
    var nappy: NappyType? = null, // type of dirt nappy
    var duration: TimeSpan? = null, // duration of feed or sleep
    var feedType: String? = null, // type of feed (left side, right side, bottle)
    val height: Int? = null, // height of baby/child in cm
    val weight: Int? = null, // weight of baby/child in kg
    var notes: String? = null, // optional notes

)


data class TimeSpan (
    var hours : Long ? = null,
    var minutes : Long? = null,
    var seconds : Long? = null,
)

enum class Category {
    FEEDING,
    NAPPY_CHANGE,
    SLEEP,
    MEASUREMENT
}

enum class NappyType {
    WET,
    BOTH,
    DIRTY
}
