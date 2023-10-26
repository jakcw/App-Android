package au.edu.utas.jackw4.babyapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.EditText
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

// created with the help from a stackoverflow post and a medium post https://stackoverflow.com/questions/2055509/how-to-create-a-date-and-time-picker-in-android and
// https://medium.com/@toyninn/set-timepickerdialog-after-chose-datepickerdialog-in-one-dialog-and-set-range-for-date-and-time-481006bc5b2
class DatePicker {
    private val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
    fun pickDateAndTime(context: Context, input: EditText) {

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