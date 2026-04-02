package com.example.audiorecorder

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audiorecorder.databinding.ActivityGalleryBinding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var db: AppDatabase
    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var mAdapter: Adapter
    private lateinit var searchInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        records = ArrayList()
        mAdapter = Adapter(records, this)

        db = AppDatabase.getInstance(this)

        binding.recyclerview.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(this@GalleryActivity)
        }

        binding.btnDeleteAll.setOnClickListener {
            showDeleteAllDialog()
        }

        searchInput = binding.searchInput
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                searchDatabase(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        fetchAll()
    }

    private fun searchDatabase(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val queryResult = db.audioRecordDao().searchDatabase("%$query%")
            withContext(Dispatchers.Main) {
                records.clear()
                records.addAll(queryResult)
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun showDeleteAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Recordings")
            .setMessage("Are you sure you want to delete all recordings? This action cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                deleteAllRecordings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAllRecordings() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allRecords = db.audioRecordDao().getAll()
            for (record in allRecords) {
                File(record.filePath).delete()
                File(record.ampsPath).delete()
            }
            db.audioRecordDao().deleteAll()
            withContext(Dispatchers.Main) {
                records.clear()
                mAdapter.notifyDataSetChanged()
                binding.emptyView.visibility = View.VISIBLE
                Toast.makeText(this@GalleryActivity, "All recordings deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchAll() {
        lifecycleScope.launch(Dispatchers.IO) {
            val queryResult = db.audioRecordDao().getAll()
            withContext(Dispatchers.Main) {
                records.clear()
                records.addAll(queryResult)
                if (records.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    binding.emptyView.visibility = View.GONE
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onItemClick(position: Int) {
        val audioRecord = records[position]
        val intent = Intent(this, AudioPlayerActivity::class.java)
        intent.putExtra("filepath", audioRecord.filePath)
        intent.putExtra("filename", audioRecord.fileName)
        startActivity(intent)
    }

    override fun onItemLongClick(position: Int) {
        Toast.makeText(this, "Long Click", Toast.LENGTH_SHORT).show()
    }
}
