package com.example.custom_calendar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import kotlin.collections.ArrayList


/**
 * Created by venu on 28/10/2019.
 */
class CalendarView : LinearLayout {

    // date format
    private var monthFormat: String? = null
    private var yearFormat: String? = null
    lateinit var calendar: CalendarView
    var hidePreviousDates: Boolean = true

    // current displayed month
    private val currentDate = Calendar.getInstance()

    //event handling
    private var eventHandler: EventHandler? = null

    // internal components
    private var header: LinearLayout? = null
    private var btnPrev: ImageView? = null
    private var btnNext: ImageView? = null
    private var txtDate: TextView? = null
    private var grid: GridView? = null
    private var yearPrev: ImageView? = null
    private var yearNext: ImageView? = null
    private var tvYearView: TextView? = null
    private var fromDate: Date? = null
    private lateinit var appStartDate: Date
    private var toDate: Date? = null
    var isSelectFromDate: Boolean = false
    lateinit var enabledPositions : ArrayList<Int>

    // seasons' rainbow
    internal var rainbow = intArrayOf(R.color.summer, R.color.fall, R.color.winter, R.color.spring)

    // month-season association (northern hemisphere, sorry australia :)
    internal var monthSeason = intArrayOf(2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initControl(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initControl(context, attrs)
    }

    /**
     * Load control xml layout
     */
    private fun initControl(context: Context, attrs: AttributeSet) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.control_calendar, this)

        enabledPositions= ArrayList()
        loadDateFormat(attrs)
        assignUiElements()
        assignClickHandlers()

        updateCalendar()
    }

    public fun clearDates() {
        fromDate = null
        toDate = null
        isSelectFromDate = false
        updateCalendar(null)
    }

    private fun loadDateFormat(attrs: AttributeSet) {
        val ta = getContext().obtainStyledAttributes(attrs, R.styleable.CalendarView);

        hidePreviousDates = ta.getBoolean(R.styleable.CalendarView_hidepreviousdates, true)

        /*try
		{
			// try to load provided date format, and fallback to default otherwise
			monthFormat = ta.getString(R.styleable.CalendarView_dateFormat);
			if (monthFormat == null)
				monthFormat = MONTH_FORMAT;
		}
		finally
		{
			ta.recycle();
		}*/
        monthFormat = MONTH_FORMAT
        yearFormat = YEAR_FORMAT
    }

    private fun assignUiElements() {
        // layout is inflated, assign local variables to components
        header = findViewById<View>(R.id.calendar_header) as LinearLayout
        btnPrev = findViewById<View>(R.id.calendar_prev_button) as ImageView
        btnNext = findViewById<View>(R.id.calendar_next_button) as ImageView
        txtDate = findViewById<View>(R.id.calendar_date_display) as TextView
        grid = findViewById<View>(R.id.calendar_grid) as GridView
        yearNext = findViewById(R.id.year_next_button)
        yearPrev = findViewById(R.id.year_prev_button)
        tvYearView = findViewById(R.id.year_date_display)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun assignClickHandlers() {
        // add one month and refresh UI
        btnNext!!.setOnClickListener {
            currentDate.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        // subtract one month and refresh UI
        btnPrev!!.setOnClickListener {
            currentDate.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        yearPrev!!.setOnClickListener {
            currentDate.add(Calendar.YEAR, -1)
            updateCalendar()
        }

        yearNext!!.setOnClickListener {
            currentDate.add(Calendar.YEAR, 1)
            updateCalendar()
        }


        // long-pressing a day
        grid!!.onItemClickListener =
            AdapterView.OnItemClickListener({ view, cell, position, id ->
                // handle long-press
              //  val isCickable = view.getTag(view.id) as Boolean
                /*if (!isCickable) {*/
                /*    return@OnItemClickListener*/
                /*}*/
                if (eventHandler == null)
                    return@OnItemClickListener

                if (!isSelectFromDate) {
                    fromDate = view.getItemAtPosition(position) as Date
                    isSelectFromDate = true
                } else {
                    toDate = view.getItemAtPosition(position) as Date
                }
                if (fromDate != null && toDate != null && toDate!!.before(fromDate)) {
                    return@OnItemClickListener
                }
                updateCalendar(null)
                eventHandler!!.onDayLongPress(view.getItemAtPosition(position) as Date)
            })

        grid!!.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { parent, view, position, id ->

                return@OnItemLongClickListener false
            }

/*
        grid!!.setOnTouchListener(object : OnSwipeTouchListener(context){
            override fun onSwipeRight() {
                super.onSwipeRight()
                Log.e("position"," right")
                currentDate.add(Calendar.MONTH, -1)
                updateCalendar()
            }

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                Log.e("position"," left")
                currentDate.add(Calendar.MONTH, 1)
                updateCalendar()
            }

            override fun onSingleTap() {
                super.onSingleTap()
                Log.e("position","singleTap1")
            }
        })
*/
    }

    /**
     * Display dates correctly in grid
     */
    @JvmOverloads
    fun updateCalendar(events: HashSet<Date>? = null) {
        enabledPositions.clear()
        val cells = ArrayList<Date>()
        val calendar = currentDate.clone() as Calendar


        // determine the cell for current month's beginning
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthBeginningCell = calendar.get(Calendar.DAY_OF_WEEK) - 1

        // move calendar backwards to the beginning of the week
        calendar.add(Calendar.DAY_OF_MONTH, -monthBeginningCell)

        // fill cells
        while (cells.size < DAYS_COUNT) {
            cells.add(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // update grid
        grid!!.adapter = CalendarAdapter(context, cells, events)

        // update title
        val monthDateFormat = SimpleDateFormat(monthFormat!!)
        txtDate!!.text = monthDateFormat.format(currentDate.time)

        val yearDateFormat = SimpleDateFormat(yearFormat!!)
        tvYearView!!.text = yearDateFormat.format(currentDate.time)


        // set header color according to current season
        val month = currentDate.get(Calendar.MONTH)
        val season = monthSeason[month]
        val color = rainbow[season]

        // header!!.setBackgroundColor(resources.getColor(color))
    }

    private inner class CalendarAdapter(
        context: Context, days: ArrayList<Date>, // days with events
        private val eventDays: HashSet<Date>?
    ) : ArrayAdapter<Date>(context, R.layout.control_calendar_day, days) {

        // for view inflation
        private val inflater: LayoutInflater

        init {
            inflater = LayoutInflater.from(context)
        }

        override fun getView(position: Int, view: View?, parent: ViewGroup): View {
            var view = view
            // day in question
            val date = getItem(position)
            val day = date!!.date
            val month = date.month
            val year = date.year

            val gridEnabledDate = getItem(20)
            val gridEnabledMonth = gridEnabledDate!!.month

            // today
            val today = Date()

            val milliseconds = 730.toLong() * 24 * 60 * 60 * 1000
            appStartDate = Date(today.time - milliseconds)
            // inflate item if it does not exist yet
            if (view == null)
                view = inflater.inflate(R.layout.control_calendar_day, parent, false)

            // if this day has an event, specify event image
            view!!.setBackgroundResource(0)
            view.setTag(view.id, true)
            if (eventDays != null) {
                for (eventDate in eventDays) {
                    if (eventDate.date == day &&
                        eventDate.month == month &&
                        eventDate.year == year
                    ) {
                        // mark this day for event
                        view.setBackgroundResource(R.drawable.image)
                        break
                    }
                }
            }


            // clear styling
            (view as TextView).setTypeface(null, Typeface.NORMAL)
            view.setTextColor(Color.BLACK)

           /* if (appStartDate.after(date)) {
                view.setTag(view.id, false)
                view.setTextColor(resources.getColor(R.color.greyed_out))
                view.isClickable = false
                view.isEnabled=false
                enabledPositions.add(position)
                isEnabled(position)
            }
            if (toDate != null)
                if (toDate!!.let { toDate!!.before(date) }) {
                    view.setTextColor(resources.getColor(R.color.greyed_out))
                    view.setTag(view.id, false)
                    view.isClickable = false
                    view.isEnabled=false
                    enabledPositions.add(position)
                    isEnabled(position)
                }
*/
            if (month != gridEnabledMonth) {
                // if this day is outside current month, grey it out
                view.setTextColor(resources.getColor(R.color.greyed_out))
                view.visibility = View.INVISIBLE
            } else if (day == today.date && month == today.month && year == today.year) {
                // if it is today, set it to blue/bold
                view.setTypeface(null, Typeface.BOLD)
                // view.setBackgroundResource(R.drawable.calendar_bg)
                view.setTextColor(Color.RED)
            }

            // set text
            view.text = date.date.toString()

            return view
        }

        override fun isEnabled(position: Int): Boolean {
            Log.e("position",enabledPositions.toString())
            if(enabledPositions.size>0)
            for(i in 0..enabledPositions.size-1) {
                if (enabledPositions[i] == position) {
                    return false
                }
            }
            return true
        }
    }

    /**
     * Assign event handler to be passed needed events
     */
    fun setEventHandler(eventHandler: EventHandler) {
        this.eventHandler = eventHandler

    }

    fun setMininmumDate(date: Date) {
        fromDate = date
        updateCalendar(null)
    }

    /**
     * This interface defines what events to be reported to
     * the outside world
     */
    interface EventHandler {
        fun onDayLongPress(date: Date)
    }

    companion object {
        // for logging
        private val LOGTAG = "Calendar View"

        // how many days to show, defaults to six weeks, 42 days
        private val DAYS_COUNT = 42

        // default date format
        private val MONTH_FORMAT = "MMM"
        private val YEAR_FORMAT = "yyyy"
    }
}
/**
 * Display dates correctly in grid
 */
