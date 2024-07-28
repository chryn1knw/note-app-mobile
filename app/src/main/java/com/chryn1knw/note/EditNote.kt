package com.chryn1knw.note

import android.app.ProgressDialog
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class EditNote : AppCompatActivity() {
    private var noteId: String? = null
    private var imageUrl: String? = null
    private var voiceUrl: String? = null
    private var timestamp: String? = null
    private var imageUri: Uri? = null
    private var voiceUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1
    private val PICK_VOICE_REQUEST = 2

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var tanggal: TextView
    private lateinit var imageView: ImageView
    private lateinit var btnBack: Button
    private lateinit var btnSave: Button
    private lateinit var btnDeleteImage: Button
    private lateinit var btnDeleteVoice: Button
    private lateinit var btnPlayPause: ImageButton
    private lateinit var tvDeleteImage: TextView
    private lateinit var btnChooseImage: Button
    private lateinit var btnChooseVoice: Button
    private lateinit var seekBar: SeekBar
    private lateinit var tvDuration: TextView
    private lateinit var voiceNoteLayout: View

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private lateinit var storage: FirebaseStorage
    private lateinit var db: FirebaseFirestore

    private var deleteImageFlag = false
    private var deleteVoiceFlag = false
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        initializeViews()
        setupFirebase()
        loadNoteData()
        setupUI()
        setupListeners()
        setupMediaPlayer()
    }

    private fun initializeViews() {
        etTitle = findViewById(R.id.title)
        etDescription = findViewById(R.id.desc)
        imageView = findViewById(R.id.imageView)
        tanggal = findViewById(R.id.tvDateTime)
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnUpdate)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        btnChooseVoice = findViewById(R.id.btnChooseVoice)
        btnDeleteImage = findViewById(R.id.btnHapusImage)
        tvDeleteImage = findViewById(R.id.tvDeleteImage)
        btnDeleteVoice = findViewById(R.id.btnHapusVoice)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBar)
        tvDuration = findViewById(R.id.tvDuration)
        voiceNoteLayout = findViewById(R.id.voiceNoteLayout)
    }

    private fun setupFirebase() {
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    private fun loadNoteData() {
        noteId = intent.getStringExtra("id")
        imageUrl = intent.getStringExtra("imageUrl")
        voiceUrl = intent.getStringExtra("voiceUrl")
        timestamp = intent.getStringExtra("timestamp")

        etTitle.setText(intent.getStringExtra("title"))
        etDescription.setText(intent.getStringExtra("desc"))
        tanggal.text = timestamp
    }

    private fun setupUI() {
        setupImageView()
        setupVoiceNote()
        setupProgressDialog()
    }

    private fun setupImageView() {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(imageUrl).into(imageView)
            imageView.visibility = View.VISIBLE
            btnDeleteImage.visibility = View.VISIBLE
            tvDeleteImage.visibility = View.VISIBLE
        } else {
            imageView.visibility = View.GONE
            btnDeleteImage.visibility = View.GONE
            tvDeleteImage.visibility = View.GONE
        }
    }

    private fun setupVoiceNote() = if (!voiceUrl.isNullOrEmpty()) {
        voiceNoteLayout.visibility = View.VISIBLE
        initializeMediaPlayer(voiceUrl!!)
        btnDeleteVoice.visibility = View.VISIBLE
    } else {
        voiceNoteLayout.visibility = View.GONE
        btnDeleteVoice.visibility = View.GONE
    }

    private fun setupProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setTitle("Loading...")
            setCancelable(false)
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { validateAndSaveNote() }
        btnChooseImage.setOnClickListener { openFileChooser(PICK_IMAGE_REQUEST) }
        btnChooseVoice.setOnClickListener { openFileChooser(PICK_VOICE_REQUEST) }
        btnDeleteImage.setOnClickListener { deleteImage() }
        btnDeleteVoice.setOnClickListener { deleteVoice() }
        btnPlayPause.setOnClickListener { togglePlayPause() }
        setupSeekBarListener()
    }

    private fun setupMediaPlayer() {
        runnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    seekBar.progress = it.currentPosition
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun openFileChooser(requestCode: Int) {
        val intent = Intent().apply {
            type = if (requestCode == PICK_IMAGE_REQUEST) "image/*" else "audio/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(intent, requestCode)
    }

    private fun handleImageResult(uri: Uri?) {
        imageUri = uri
        imageView.setImageURI(imageUri)
        imageView.visibility = View.VISIBLE
        btnDeleteImage.visibility = View.VISIBLE
        tvDeleteImage.visibility = View.VISIBLE
        deleteImageFlag = false
    }

    private fun handleVoiceResult(uri: Uri?) {
        voiceUri = uri
        voiceNoteLayout.visibility = View.VISIBLE
        btnDeleteVoice.visibility = View.VISIBLE
        deleteVoiceFlag = false
    }

    private fun validateAndSaveNote() {
        val noteTitle = etTitle.text.toString().trim()
        val noteDesc = etDescription.text.toString().trim()

        if (noteTitle.isEmpty() || noteDesc.isEmpty()) {
            Toast.makeText(this, "Title and Description cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()
        uploadFiles(noteTitle, noteDesc)
    }

    private fun uploadFiles(noteTitle: String, noteDesc: String) {
        var imageUploadComplete = imageUri == null
        var voiceUploadComplete = voiceUri == null

        if (imageUri != null) {
            uploadImageToStorage { url ->
                imageUrl = url
                imageUploadComplete = true
                if (voiceUploadComplete) saveNote(noteTitle, noteDesc)
            }
        }

        if (voiceUri != null) {
            uploadVoiceToStorage { url ->
                voiceUrl = url
                voiceUploadComplete = true
                if (imageUploadComplete) saveNote(noteTitle, noteDesc)
            }
        }

        if (imageUploadComplete && voiceUploadComplete) {
            saveNote(noteTitle, noteDesc)
        }

        if (deleteImageFlag && imageUri == null) {
            deleteImageFromStorage()
        }

        if (deleteVoiceFlag && voiceUri == null) {
            deleteVoiceFromStorage()
        }
    }

    private fun uploadImageToStorage(callback: (String?) -> Unit) {
        val storageRef = storage.reference.child("images/${System.currentTimeMillis()}.jpg")
        storageRef.putFile(imageUri!!)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(task.result.toString())
                } else {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            }
    }

    private fun uploadVoiceToStorage(callback: (String?) -> Unit) {
        val storageRef = storage.reference.child("voices/${System.currentTimeMillis()}.mp3")
        storageRef.putFile(voiceUri!!)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(task.result.toString())
                } else {
                    Toast.makeText(this, "Failed to upload voice note", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            }
    }

    private fun handleSuccessfulUpdate(id: String, title: String, description: String) {
        if (deleteImageFlag) deleteImageFromStorage()
        if (deleteVoiceFlag) deleteVoiceFromStorage()

        Toast.makeText(this, "Note updated successfully", Toast.LENGTH_SHORT).show()
        val resultIntent = Intent().apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("desc", description)
            putExtra("imageUrl", if (deleteImageFlag) null else imageUrl)
            putExtra("voiceUrl", if (deleteVoiceFlag) null else voiceUrl)
            putExtra("timestamp", timestamp)
        }
        setResult(RESULT_OK, resultIntent)
        progressDialog.dismiss()
        finish()
    }

    private fun deleteImage() {
        deleteImageFlag = true
        imageView.visibility = View.GONE
        btnDeleteImage.visibility = View.GONE
        tvDeleteImage.visibility = View.GONE
    }

    private fun deleteVoice() {
        deleteVoiceFlag = true
        voiceNoteLayout.visibility = View.GONE
        btnDeleteVoice.visibility = View.GONE
    }

    private fun deleteImageFromStorage() {
        imageUrl?.let { url ->
            FirebaseStorage.getInstance().getReferenceFromUrl(url).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show()
                    imageUrl = null
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to delete image", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun deleteVoiceFromStorage() {
        voiceUrl?.let { url ->
            FirebaseStorage.getInstance().getReferenceFromUrl(url).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Voice deleted successfully", Toast.LENGTH_SHORT).show()
                    voiceUrl = null
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to delete voice", Toast.LENGTH_LONG).show()
                }
        }
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
                    Log.e("MediaPlayer", "Error initializing media player")
                    Toast.makeText(this@EditNote, "Error initializing media player", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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

    private fun saveNote(noteTitle: String, noteDesc: String) {
        val updatedNote = hashMapOf(
            "title" to noteTitle,
            "desc" to noteDesc,
            "imageUrl" to if (deleteImageFlag) null else imageUrl,
            "voiceUrl" to if (deleteVoiceFlag) null else voiceUrl,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        noteId?.let { id ->
            db.collection("note").document(id)
                .update(updatedNote as Map<String, Any>)
                .addOnSuccessListener {
                    handleSuccessfulUpdate(id, noteTitle, noteDesc)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to update note", Toast.LENGTH_LONG).show()
                    progressDialog.dismiss()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data?.data != null) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> handleImageResult(data.data)
                PICK_VOICE_REQUEST -> handleVoiceResult(data.data)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        handler.removeCallbacks(runnable)
    }
}
