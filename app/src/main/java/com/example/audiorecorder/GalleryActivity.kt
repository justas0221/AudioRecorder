package com.example.audiorecorder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audiorecorder.databinding.ActivityGalleryBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
    
    private lateinit var editBar: View
    private lateinit var btnClose: ImageButton
    private lateinit var btnSelectAll: ImageButton
    private var allChecked = false

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

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
        
        editBar = binding.editBar
        btnClose = binding.btnClose
        btnSelectAll = binding.btnSelectAll
        
        btnClose.setOnClickListener {
            leaveEditMode()
        }
        
        btnSelectAll.setOnClickListener {
            allChecked = !allChecked
            records.forEach { it.isChecked = allChecked }
            mAdapter.notifyDataSetChanged()
        }

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        binding.btnDelete.setOnClickListener {
            val checkedRecords = records.filter { it.isChecked }
            if (checkedRecords.isNotEmpty()) {
                showDeleteConfirmDialog(checkedRecords)
            }
        }

        binding.btnEdit.setOnClickListener {
            val checkedRecords = records.filter { it.isChecked }
            if (checkedRecords.size == 1) {
                showRenameDialog(checkedRecords[0])
            } else if (checkedRecords.isEmpty()) {
                Toast.makeText(this, "Select an item to rename", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Select exactly one item to rename", Toast.LENGTH_SHORT).show()
            }
        }

        fetchAll()
    }
    
    private fun leaveEditMode() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        editBar.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        records.forEach { it.isChecked = false }
        mAdapter.setEditMode(false)
        allChecked = false
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

    private fun showDeleteConfirmDialog(checkedRecords: List<AudioRecord>) {
        AlertDialog.Builder(this)
            .setTitle("Delete Recordings")
            .setMessage("Are you sure you want to delete ${checkedRecords.size} selected recordings?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSelectedRecordings(checkedRecords)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSelectedRecordings(checkedRecords: List<AudioRecord>) {
        lifecycleScope.launch(Dispatchers.IO) {
            for (record in checkedRecords) {
                File(record.filePath).delete()
                File(record.ampsPath).delete()
                db.audioRecordDao().delete(record)
            }
            withContext(Dispatchers.Main) {
                leaveEditMode()
                fetchAll()
                Toast.makeText(this@GalleryActivity, "Deleted successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRenameDialog(record: AudioRecord) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
        val input = dialogView.findViewById<TextInputEditText>(R.id.renameInput)
        input.setText(record.fileName)
        
        builder.setTitle("Rename Recording")
        builder.setView(dialogView)
        builder.setPositiveButton("OK") { _, _ ->
            val newName = input.text.toString()
            if (newName.isNotEmpty() && newName != record.fileName) {
                checkAndRename(record, newName)
            }
        }
        builder.setNegativeButton("Cancel", null)
        val dialog = builder.create()
        
        // Request focus and show keyboard when dialog is shown
        input.requestFocus()
        dialog.setOnShowListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
        
        dialog.show()
    }

    private fun checkAndRename(record: AudioRecord, newName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val exists = db.audioRecordDao().countByName(newName) > 0
            if (exists) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GalleryActivity, "A recording with this name already exists", Toast.LENGTH_SHORT).show()
                }
            } else {
                renameRecord(record, newName)
            }
        }
    }

    private fun renameRecord(record: AudioRecord, newName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val oldFile = File(record.filePath)
            val newFile = File(oldFile.parent, "$newName.mp3")
            
            val oldAmpsFile = File(record.ampsPath)
            val newAmpsFile = File(oldAmpsFile.parent, "$newName.amps")

            if (oldFile.renameTo(newFile) && oldAmpsFile.renameTo(newAmpsFile)) {
                record.fileName = newName
                record.filePath = newFile.absolutePath
                record.ampsPath = newAmpsFile.absolutePath
                db.audioRecordDao().update(record)
                
                withContext(Dispatchers.Main) {
                    leaveEditMode()
                    fetchAll()
                    Toast.makeText(this@GalleryActivity, "Renamed successfully", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GalleryActivity, "Rename failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
        if (mAdapter.isEditMode()) {
            records[position].isChecked = !records[position].isChecked
            mAdapter.notifyItemChanged(position)
        } else {
            val audioRecord = records[position]
            val intent = Intent(this, AudioPlayerActivity::class.java)
            intent.putExtra("filepath", audioRecord.filePath)
            intent.putExtra("filename", audioRecord.fileName)
            startActivity(intent)
        }
    }

    override fun onItemLongClick(position: Int) {
        if (!mAdapter.isEditMode()) {
            mAdapter.setEditMode(true)
            records[position].isChecked = true
            mAdapter.notifyDataSetChanged()
            
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)
            editBar.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
}
