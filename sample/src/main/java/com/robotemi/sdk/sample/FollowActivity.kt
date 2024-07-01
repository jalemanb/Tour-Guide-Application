package com.robotemi.sdk.sample

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.sample.databinding.ActivityFollowBinding

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FollowActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFollowBinding
    private lateinit var fullscreenContent: TextView
    private lateinit var fullscreenContentControls: LinearLayout
    private var serviceStarted: Boolean = false
    private var isFullscreen: Boolean = false
    private lateinit var leftPupil: View
    private lateinit var rightPupil: View
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    var distance: Float = 0F
    var angle: Float = 0F
    var is_human_detected: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Temi to follow a Human
        followInit()

        //////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////
        binding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isFullscreen = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = binding.fullscreenContent
        fullscreenContent.setOnClickListener { start_service() }

        fullscreenContentControls = binding.fullscreenContentControls

        if(!serviceStarted)
        {
            val serviceIntent = Intent(this, TemiListeners::class.java)
            startService(serviceIntent)
            serviceStarted = true
        }
        //////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////
        leftPupil = findViewById(R.id.leftPupil)
        rightPupil = findViewById(R.id.rightPupil)


        // Define the runnable
        runnable = Runnable {
            // Get Human Detection Distance
            distance = HumanDectection.distance
            angle = HumanDectection.angle
            is_human_detected = HumanDectection.is_detection
            // Your repeated task here
            movePupilsToAngle(angle)
            handler.postDelayed(runnable, 400) // Schedule the runnable to run again in 1 second
        }
    }

    private fun followInit() {
        // Put TEMi head on 45 degrees so its easy to detect a human
        Temi.robot.tiltAngle(45, 0.7F)
        Temi.robot.beWithMe()
        Temi.robot.speak(TtsRequest.create("Ich werde dir folgen, wohin du auch gehst. Wenn Sie möchten, dass ich aufhöre, berühren Sie bitte den Bildschirm", language = TtsRequest.Language.DE_DE, isShowOnConversationLayer = false))
    }

    override fun onStart() {
        super.onStart()
        handler.post(runnable) // Start the runnable when the activity is started
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable) // Stop the runnable when the activity is stopped
    }

    private fun movePupilsToAngle(angle: Float, distance: Float = 0F) {
        val radius = 100f // Radius within which the pupil can move
        val angleRad =angle

        val dx = angleRad*200
        val dy = 0F

        // Animate the left pupil
        val animatorXLeft = ObjectAnimator.ofFloat(leftPupil, "translationX", dx)
        val animatorYLeft = ObjectAnimator.ofFloat(leftPupil, "translationY", dy)
        val animatorSetLeft = AnimatorSet()
        animatorSetLeft.playTogether(animatorXLeft, animatorYLeft)
        animatorSetLeft.duration = 300
        animatorSetLeft.start()

        // Animate the right pupil
        val animatorXRight = ObjectAnimator.ofFloat(rightPupil, "translationX", dx)
        val animatorYRight = ObjectAnimator.ofFloat(rightPupil, "translationY", dy)
        val animatorSetRight = AnimatorSet()
        animatorSetRight.playTogether(animatorXRight, animatorYRight)
        animatorSetRight.duration = 300
        animatorSetRight.start()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
    }

    private fun start_service()
    {
        Temi.robot.stopMovement()
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        startActivity(mainActivityIntent)
    }

    companion object {

        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}