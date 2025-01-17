package com.robotemi.sdk.sample

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.robotemi.sdk.sample.databinding.ActivityStartBinding
import pl.droidsonroids.gif.GifImageView

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding
    private lateinit var fullscreenContent: TextView
    private lateinit var fullscreenContentControls: LinearLayout
    private var serviceStarted: Boolean = false
    private var isFullscreen: Boolean = false
    private lateinit var leftPupil: View
    private lateinit var rightPupil: View
    private lateinit var talking_mouth: GifImageView
    private lateinit var smiling_mouth: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    var distance: Float = 0F
    var angle: Float = 0F
    var is_human_detected: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        //////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////
        binding = ActivityStartBinding.inflate(layoutInflater)
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

        talking_mouth = findViewById(R.id.welcome_gif)
        smiling_mouth = findViewById(R.id.smile_image_view)

        // Define the runnable
        runnable = Runnable {
            distance = HumanDectection.distance
            angle = HumanDectection.angle
            is_human_detected = HumanDectection.is_detection

            if (distance < 1F && is_human_detected)
            {
                show_dialog(true)
            }
            else
            {
                show_dialog(false)
            }

            // Your repeated task here
            movePupilsToAngle(angle) // Example: move to a random angle
//            movePupilsToAngle((0..360).random().toFloat()) // Example: move to a random angle
            handler.postDelayed(runnable, 500) // Schedule the runnable to run again in 1 second

        // Put TEMi head on 45 degrees so its easy to detect a human
        Temi.robot.tiltAngle(45, 0.7F)

        }
    }

    fun show_dialog(activate: Boolean) {

        if(activate){
            talking_mouth.visibility = View.VISIBLE
            smiling_mouth.visibility = View.INVISIBLE
        }
        else{
            talking_mouth.visibility = View.INVISIBLE
            smiling_mouth.visibility = View.VISIBLE
        }

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

        val dx = angleRad*150
        val dy = 0F

        // Animate the left pupil
        val animatorXLeft = ObjectAnimator.ofFloat(leftPupil, "translationX", dx)
        val animatorYLeft = ObjectAnimator.ofFloat(leftPupil, "translationY", dy)
        animatorXLeft.interpolator = LinearInterpolator()
        animatorYLeft.interpolator = LinearInterpolator()

        val animatorSetLeft = AnimatorSet()
        animatorSetLeft.playTogether(animatorXLeft, animatorYLeft)
        animatorSetLeft.duration = 300
        animatorSetLeft.start()

        // Animate the right pupil
        val animatorXRight = ObjectAnimator.ofFloat(rightPupil, "translationX", dx)
        val animatorYRight = ObjectAnimator.ofFloat(rightPupil, "translationY", dy)
        animatorXRight.interpolator = LinearInterpolator()
        animatorYRight.interpolator = LinearInterpolator()

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
        val mainActivityIntent = Intent(this, MainActivity::class.java)

        mainActivityIntent.putExtra("should_speak", true)
        startActivity(mainActivityIntent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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