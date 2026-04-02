package com.example.audiorecorder

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.audiorecorder.databinding.ActivityAudioPlayerBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import java.text.DecimalFormat
import java.text.NumberFormat

class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioPlayerBinding
    private var mediaPlayer: MediaPlayer? = null
    
    private lateinit var btnPlay: ImageButton
    private lateinit var btnBackward: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var chip: Chip
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvFileName: TextView
    private lateinit var tvTrackProgress: TextView
    private lateinit var tvTrackDuration: TextView

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 1000L
    private var jumpValue = 1000

    private var playbackSpeed = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra("filepath")
        val fileName = intent.getStringExtra("filename")

        toolbar = binding.toolbar
        tvFileName = binding.tvFileName
        tvTrackProgress = binding.tvTrackProgress
        tvTrackDuration = binding.tvTrackDuration
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""
        tvFileName.text = fileName

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnPlay = binding.btnPlay
        btnBackward = binding.btnBackward
        btnForward = binding.btnForward
        seekBar = binding.seekBar
        chip = binding.chip

        if (filePath == null) {
            Toast.makeText(this, "File path not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.apply {
                setDataSource(filePath)
                prepare()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error loading file: ${e.message}")
            Toast.makeText(this, "Could not play recording", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        seekBar.max = mediaPlayer?.duration ?: 0
        tvTrackDuration.text = dateFormat(mediaPlayer?.duration ?: 0)

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            mediaPlayer?.let {
                seekBar.progress = it.currentPosition
                tvTrackProgress.text = dateFormat(it.currentPosition)
                handler.postDelayed(runnable, delay)
            }
        }

        btnPlay.setOnClickListener {
            playPausePlayer()
        }

        // Auto play on start
        playPausePlayer()

        btnForward.setOnClickListener {
            mediaPlayer?.let {
                val newPos = it.currentPosition + jumpValue
                it.seekTo(newPos)
                seekBar.progress = newPos
                tvTrackProgress.text = dateFormat(it.currentPosition)
            }
        }

        btnBackward.setOnClickListener {
            mediaPlayer?.let {
                val newPos = it.currentPosition - jumpValue
                it.seekTo(newPos)
                seekBar.progress = newPos
                tvTrackProgress.text = dateFormat(it.currentPosition)
            }
        }

        chip.setOnClickListener {
            if (playbackSpeed != 2.0f) {
                playbackSpeed += 0.5f
            } else {
                playbackSpeed = 0.5f
            }
            mediaPlayer?.let {
                it.playbackParams = it.playbackParams.setSpeed(playbackSpeed)
            }
            chip.text = "${playbackSpeed}x"
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    tvTrackProgress.text = dateFormat(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mediaPlayer?.setOnCompletionListener {
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_circle, theme)
            handler.removeCallbacks(runnable)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                handler.removeCallbacks(runnable)
                finish()
            }
        })
    }

    private fun dateFormat(duration: Int): String {
        val totalSeconds = duration / 1000
        val s = totalSeconds % 60
        val m = (totalSeconds / 60) % 60
        val h = totalSeconds / 3600
        
        val f: NumberFormat = DecimalFormat("00")
        
        return if (h > 0) {
            "$h:${f.format(m)}:${f.format(s)}"
        } else {
            "${f.format(m)}:${f.format(s)}"
        }
    }

    private fun playPausePlayer() {
        val player = mediaPlayer ?: return
        
        if (!player.isPlaying) {
            player.start()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_pause_circle, theme)
            handler.postDelayed(runnable, 0)
        } else {
            player.pause()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_circle, theme)
            handler.removeCallbacks(runnable)
        }
    }
}
