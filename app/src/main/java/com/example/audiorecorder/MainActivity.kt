package com.example.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.audiorecorder.databinding.ActivityMainBinding
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    private lateinit var binding: ActivityMainBinding
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    private var recorder: MediaRecorder? = null
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPaused = false

    private lateinit var timer: Timer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED

        if(!permissionGranted)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)

        timer = Timer(this)

        binding.btnRecord.setOnClickListener {
            when {
                isPaused -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == REQUEST_CODE)
            permissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun pauseRecording(){
        try {
            recorder?.pause()
            isPaused = true
            binding.btnRecord.setImageResource(0)
            timer.pause()
        } catch (e: Exception) {
            Log.e("MainActivity", "pauseRecording: ${e.message}")
        }
    }

    private fun resumeRecording(){
        try {
            recorder?.resume()
            isPaused = false
            binding.btnRecord.setImageResource(R.drawable.ic_pause)
            timer.start()
        } catch (e: Exception) {
            Log.e("MainActivity", "resumeRecording: ${e.message}")
        }
    }

    private fun startRecording(){
        if(!permissionGranted){
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        
        dirPath = "${externalCacheDir?.absolutePath}/"

        val simpleDateFormat = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss", Locale.getDefault())
        val date = simpleDateFormat.format(Date())
        fileName = "audio_record_$date"

        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp4")
            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("MainActivity", "prepare() failed: ${e.message}")
                return
            } catch (e: Exception) {
                Log.e("MainActivity", "start() failed: ${e.message}")
                return
            }
        }

        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false

        timer.start()
    }

    private fun stopRecording(){
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            timer.stop()
            isRecording = false
            isPaused = false
            binding.btnRecord.setImageResource(0)
            binding.tvTimer.text = "00:00.00"
        } catch (e: Exception) {
            Log.e("MainActivity", "stopRecording failed: ${e.message}")
        }
    }

    override fun onTimerTick(duration: String) {
        binding.tvTimer.text = duration
        binding.waveformView.addAmplitude(recorder?.maxAmplitude?.toFloat() ?: 0f)
    }
}
