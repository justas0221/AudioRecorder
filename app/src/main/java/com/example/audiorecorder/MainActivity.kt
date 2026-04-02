package com.example.audiorecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    private lateinit var amplitudes: ArrayList<Float>
    private lateinit var binding: ActivityMainBinding
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    private var recorder: MediaRecorder? = null
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPaused = false

    private var duration = ""

    private lateinit var timer: Timer
    private lateinit var db: AppDatabase
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding.main.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            val bottomInset = if (ime.bottom > 0) ime.bottom else systemBars.bottom
            binding.bottomSheet.root.setPadding(0, 0, 0, bottomInset)
            binding.main.getChildAt(0).setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED

        if(!permissionGranted)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)

        db = AppDatabase.getInstance(this)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.root)
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    binding.bottomSheetBG.visibility = View.GONE
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.bottomSheetBG.visibility = View.VISIBLE
                binding.bottomSheetBG.alpha = slideOffset.coerceAtLeast(0f)
            }
        })

        timer = Timer(this)

        binding.btnRecord.setOnClickListener {
            when {
                isPaused -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }
        }

        binding.btnList.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        binding.btnDone.setOnClickListener {
            stopRecording()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBG.visibility = View.VISIBLE
            binding.bottomSheet.fileNameInput.setText(fileName)
        }

        binding.bottomSheet.btnCancel.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        binding.bottomSheet.btnOk.setOnClickListener {
            dismiss()
            save()
        }

        binding.bottomSheetBG.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            stopRecording()
            File("$dirPath$fileName.mp3").delete()
            Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show()
        }

        binding.btnDelete.isClickable = false
    }

    private fun save(){
        val newFileName = binding.bottomSheet.fileNameInput.text.toString()
        val oldFile = File("$dirPath$fileName.mp3")
        val newFile = File("$dirPath$newFileName.mp3")
        
        if(newFileName != fileName) {
            oldFile.renameTo(newFile)
        }
        
        val filePath = newFile.absolutePath
        val timestamp = Date().time
        val ampsPath = File(filesDir, "$newFileName.amps").absolutePath

        lifecycleScope.launch(Dispatchers.IO) {
            try{
                val fos = FileOutputStream(ampsPath)
                val out = ObjectOutputStream(fos)
                out.writeObject(amplitudes)
                out.close()
                fos.close()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to save amplitudes: ${e.message}")
            }

            val record = AudioRecord(newFileName, filePath, timestamp, duration, ampsPath)
            db.audioRecordDao().insert(record)
            
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Recording saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dismiss(){
        hideKeyboard(binding.bottomSheet.fileNameInput)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun hideKeyboard(view: View){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE)
            permissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun pauseRecording(){
        try {
            recorder?.pause()
            isPaused = true
            binding.btnRecord.setImageResource(R.drawable.ic_record)
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

        amplitudes = ArrayList()
        dirPath = "${filesDir.absolutePath}/"
        val date = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss", Locale.getDefault()).format(Date())
        fileName = "audio_record_$date"

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        
        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile("$dirPath$fileName.mp3")
            try {
                prepare()
                start()
            } catch (e: Exception) {
                Log.e("MainActivity", "start() failed: ${e.message}")
                return
            }
        }

        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false
        timer.start()

        binding.btnDelete.isClickable = true
        binding.btnDelete.setImageResource(R.drawable.ic_delete)
        binding.btnList.visibility = View.GONE
        binding.btnDone.visibility = View.VISIBLE
    }

    private fun stopRecording(){
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.e("MainActivity", "stop failed")
        }
        
        recorder?.release()
        recorder = null
        timer.stop()
        isRecording = false
        isPaused = false
        
        binding.btnRecord.setImageResource(R.drawable.ic_record)
        binding.tvTimer.text = "00:00.00"
        binding.btnList.visibility = View.VISIBLE
        binding.btnDone.visibility = View.GONE
        binding.btnDelete.isClickable = false
        binding.btnDelete.setImageResource(R.drawable.ic_delete_disabled)
        
        amplitudes = binding.waveformView.clearAndGetAmplitudes()
    }

    override fun onTimerTick(duration: String) {
        binding.tvTimer.text = duration
        this.duration = duration.dropLast(3)
        if (isRecording && !isPaused) {
            try {
                val amp = recorder?.maxAmplitude ?: 0
                binding.waveformView.addAmplitude(amp.toFloat())
            } catch (e: Exception) {}
        }
    }
}
