package com.chryn1knw.note

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class ViewNote : AppCompatActivity() {
    private var imageUrl: String? = null
    private var voiceUrl: String? = null
    private var timestamp: String? = null

    private lateinit var tvTitle: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var tvDescription: TextView
    private lateinit var imageView: ImageView
    private lateinit var btnBack: Button
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button
    private lateinit var btnPlayPause: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var tvDuration: TextView
    private lateinit var voiceNoteLayout: View

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private lateinit var db: FirebaseFirestore
    private var noteId: String? = null

    companion object {
        private const val EDIT_NOTE_REQUEST_CODE = 1
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_note)


        initializeViews()
        setupFirebase()
        setupListeners()
        setupMediaPlayer()

        noteId = intent.getStringExtra("id")
        noteId?.let { id ->
            loadNoteData(id)
        } ?: finish()
    }

    private fun initializeViews(){
        tvTitle = findViewById(R.id.tvTitle)
        tvTimestamp = findViewById(R.id.tvTimestamp)
        tvDescription = findViewById(R.id.tvDescription)
        imageView = findViewById(R.id.imageView)
        btnBack = findViewById(R.id.btnBack)
        btnEdit = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBar)
        tvDuration = findViewById(R.id.tvDuration)
        voiceNoteLayout = findViewById(R.id.voiceNoteLayout)
    }

    private fun setupFirebase(){
        db = FirebaseFirestore.getInstance()
    }

    private fun setupListeners(){
        btnBack.setOnClickListener {
            finish()
        }
        btnEdit.setOnClickListener {
            val intent = Intent(this, EditNote::class.java).apply {
                putExtra("id", noteId)
                putExtra("title", tvTitle.text.toString())
                putExtra("desc", tvDescription.text.toString())
                putExtra("imageUrl", imageUrl)
                putExtra("voiceUrl", voiceUrl)
                putExtra("timestamp", timestamp)
            }
            startActivityForResult(intent, EDIT_NOTE_REQUEST_CODE)
        }
        btnDelete.setOnClickListener { showDeleteConfirmationDialog() }
        btnPlayPause.setOnClickListener { togglePlayPause() }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupMediaPlayer(){
        runnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    seekBar.progress = it.currentPosition
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        loadNoteData(noteId!!)
    }

    private fun initializeMediaPlayer(voiceUrl: String) {
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(voiceUrl)
        storageReference.downloadUrl.addOnSuccessListener { uri ->
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(uri.toString())
                    setOnPreparedListener {
                        seekBar.max = it.duration
                        tvDuration.text = formatDuration(it.duration.toLong())
                        updateSeekBar()
                    }
                    setOnCompletionListener {
                        btnPlayPause.setImageResource(R.drawable.ic_play)
                        seekBar.progress = 0
                    }
                    prepareAsync()
                } catch (e: IOException) {
                    Toast.makeText(this@ViewNote, "Error initializing media player.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadNoteData(noteId: String) {
        db.collection("note").document(noteId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    tvTitle.text = document.getString("title")
                    tvDescription.text = document.getString("desc")
                    timestamp = document.getTimestamp("timestamp")?.toDate()?.let {
                        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(it)
                    }
                    tvTimestamp.text = timestamp

                    imageUrl = document.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(imageUrl).into(imageView)
                        imageView.visibility = View.VISIBLE
                    } else {
                        imageView.visibility = View.GONE
                    }

                    voiceUrl = document.getString("voiceUrl")
                    if (!voiceUrl.isNullOrEmpty()) {
                        voiceNoteLayout.visibility = View.VISIBLE
                        initializeMediaPlayer(voiceUrl!!)
                    } else {
                        voiceNoteLayout.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading note data.", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteNote() {
        noteId?.let { id ->
            db.collection("note").document(id)
                .delete()
                .addOnSuccessListener {
                    voiceUrl?.let { url ->
                        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(url)
                        storageReference.delete().addOnSuccessListener {
                            Toast.makeText(this, "Note successfully deleted", Toast.LENGTH_SHORT).show()
                            finish()
                        }.addOnFailureListener { exception ->
                            Toast.makeText(this, "Failed to delete voice", Toast.LENGTH_LONG).show()
                        }
                    } ?: run {
                        Toast.makeText(this, "Note successfully deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to delete note", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Notes")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Yes") { dialog, _ ->
                deleteNote()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                btnPlayPause.setImageResource(R.drawable.ic_play)
            } else {
                it.start()
                btnPlayPause.setImageResource(R.drawable.ic_pause)
                updateSeekBar()
            }
        }
    }

    private fun updateSeekBar() {
        handler.post(runnable)
    }

    private fun formatDuration(duration: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_NOTE_REQUEST_CODE && resultCode == RESULT_OK) {
            noteId?.let {
                loadNoteData(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        handler.removeCallbacks(runnable)
    }
}