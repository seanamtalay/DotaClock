package com.example.namtalaylaorat.dotaclock

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.namtalaylaorat.dotaclock.util.PrefUtil

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    lateinit var darkBountyDrawable: Drawable
    lateinit var darkBountyImageView: ImageView
    lateinit var darkNeutralDrawable: Drawable
    lateinit var darkNeutralImageView: ImageView

    enum class TimerState{
         Stopped, Paused, Running
    }

    private val TAG = "MainActivity"
    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds: Long = 0
    private var timerState = TimerState.Paused

    private var secondsRemaining: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab_play.setOnClickListener{ v ->
            startTimer()
            timerState = TimerState.Running
            updateButtons()

        }

        fab_pause.setOnClickListener { view ->
            timer.cancel()
            timerState = TimerState.Paused
            updateButtons()
        }

        fab_stop.setOnClickListener { v ->
            timer.cancel()
            onTimerFinished()
            updateButtons()
        }


        //progress image level from 0 - 10000
        darkBountyImageView = runes_progress_dark_imageView as ImageView
        darkBountyDrawable = darkBountyImageView.drawable as ClipDrawable
        darkBountyDrawable.level = 0

        darkNeutralImageView = neutral_progress_dark_imageView as ImageView
        darkNeutralDrawable = darkNeutralImageView.drawable as ClipDrawable
        darkNeutralDrawable.level = 0
    }

    override fun onResume() {
        super.onResume()

        initTimer()
    }

    override fun onPause() {
        super.onPause()

        if(timerState == TimerState.Running){
            timer.cancel()
        }
        else if(timerState == TimerState.Paused){
            //TODO show notification
        }

        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondsRemaining, this)
        PrefUtil.setTimerState(timerState, this)
    }

    private fun initTimer(){
        timerState = PrefUtil.getTimerState(this)

        if(timerState == TimerState.Stopped){
            setNewTimerLength()
            secondsRemaining = timerLengthSeconds
        }
        else{
            setPreviousTimerLength()
            secondsRemaining = PrefUtil.getSecondsRemaining(this)
        }


        if(timerState == TimerState.Running)
            startTimer()


        updateCountdownUI()

    }

    private fun onTimerFinished(){
        timerState = TimerState.Stopped

        setNewTimerLength()

        PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
        secondsRemaining = timerLengthSeconds

        updateCountdownUI()
    }

    private fun startTimer(){
        timerState = TimerState.Running

        timer = object : CountUpTimer(Long.MAX_VALUE) {
            override fun onTick(second: Int) {
                secondsRemaining = second.toLong()
                updateCountdownUI()

                checkTimeCondition(3L)
                progressDarkOverlayBounty()
                progressDarkOverlayNeutral()
            }

            override fun onFinish() = onTimerFinished()

        }.start()
    }

    private fun setNewTimerLength(){
        val lengthInMinutes = PrefUtil.getTimerLength(this)
        timerLengthSeconds = (lengthInMinutes * 0L)

    }

    private fun setPreviousTimerLength(){
        timerLengthSeconds = PrefUtil.getPreviousTimerLengthSeconds(this)

    }

    @SuppressLint("SetTextI18n")
    private fun updateCountdownUI(){
        val minutesUntilFinished = secondsRemaining/60
        val secondsInMinuteUntilFinished = secondsRemaining - minutesUntilFinished * 60
        val secondsStr = secondsInMinuteUntilFinished.toString()
        countdown_textView.text = "$minutesUntilFinished:${
            if(secondsStr.length == 2) secondsStr
            else "0" + secondsStr
        }"
    }

    private fun updateButtons(){
        when(timerState){
            TimerState.Running ->{
                fab_play.isEnabled = false
                fab_pause.isEnabled = true
                fab_stop.isEnabled = true
            }
            TimerState.Paused ->{
                fab_play.isEnabled = true
                fab_pause.isEnabled = false
                fab_stop.isEnabled = true
            }
            TimerState.Stopped ->{
                fab_play.isEnabled = true
                fab_pause.isEnabled = true
                fab_stop.isEnabled = false
            }
        }
    }

    /**
     * Function that check if the time value(in seconds) has met any conditions.
     * It will notify the use when a condition is met.
     * The user can set how many seconds beforehand they want to be notified
     */
    private fun checkTimeCondition(secBefore: Long){
        //TODO check if 5min runes, stack time, rosh?

        if(secondsRemaining%10 == (10L-secBefore)%10 && secondsRemaining != 0L){
            Toast.makeText(this@MainActivity, "10sec!", Toast.LENGTH_SHORT).show()
            //increasing the dark layer overtime
            darkBountyDrawable.level = darkBountyDrawable.level + 1000
        }

        //Start match 0:00
        if(secondsRemaining == 0L){
            Toast.makeText(this@MainActivity, "The battle begins!", Toast.LENGTH_SHORT).show()
        }

        //Runes every 5 min
        if((secondsRemaining%300) == (300L-secBefore)%300){
            Toast.makeText(this@MainActivity, "Runes!", Toast.LENGTH_SHORT).show()
            //clear bounty dark overlay progress
            darkBountyDrawable.level = 10000

        }

        //stack neutral creep. About every XX:52 seconds. Each camp stack time is not the same, so we are just going to use 52.
        if((secondsRemaining%60) == (52L-secBefore)%60){
            Toast.makeText(this@MainActivity, "Stack neutrals!", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "inside if")

        }

        //rosh?

    }

    /**
     * Make the bounty progress
     * Level range is 0 - 10000
     */
    private fun progressDarkOverlayBounty(){
        //increment every second. mod 10000 at the end to reset when the process it the limit
        darkBountyDrawable.level = 10000 - ((secondsRemaining.toInt() * 10000 / 300)%10000)
        //Log.e(TAG, "current level: ${darkBountyDrawable.level}")

    }

    /**
     * Make the neutral creep progress
     * Level range is 0 - 10000
     */
    private fun progressDarkOverlayNeutral(){
        //increment every second.
        darkNeutralDrawable.level = 10000 - ((secondsRemaining.toInt()%60) * 10000 / 52)
        //Log.e(TAG, "current level: ${darkNeutralDrawable.level}")


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }



}


