package com.example.custom_calendar

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        calendarView.setEventHandler(object : CalendarView.EventHandler {
            override fun onDayLongPress(date: Date) {
                // show returned day
                val df = SimpleDateFormat("dd/MM/yyyy", Locale.CANADA)
                Toast.makeText(this@MainActivity, df.format(date), Toast.LENGTH_SHORT).show()
            }
        })
    }
}
