package com.chryn1knw.note

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CreateNote : AppCompatActivity() {
    private var id: String? = null
    private var judul: String? = null
    private var deskripsi: String? = null
    private var imageUrl: String? = null
    private var voiceUrl: String? = null
    private var timestamp: String? = null

    private val PICK_IMAGE_REQUEST = 1
    private val PICK_VOICE_REQUEST = 2

    private lateinit var title: EditText
    private lateinit var desc: EditText
    private lateinit var tanggal: TextView
    private lateinit var voiceView: TextView
    private lateinit var tvDeleteImage: TextView
    private lateinit var imageView: ImageView
    private lateinit var btnSimpan: Button
    private lateinit var btnChooseImage: Button
    private lateinit var btnChooseVoice: Button
    private lateinit var btnDeleteImage: Button
    private lateinit var btnDeleteVoice: Button
    private lateinit var btnBack: Button
    private var imageUri: Uri? = null
    private var voiceUri: Uri? = null
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        initializeViews()
        setupFirebase()
        loadNoteData()
        setupUI()
        setupListeners()
    }

    private fun initializeViews(){
        title = findViewById(R.id.title)
        desc = findViewById(R.id.desc)
        tanggal = findViewById(R.id.tvDateTime)
        tvDeleteImage = findViewById(R.id.tvDeleteImage)
        voiceView = findViewById(R.id.voiceView)
        imageView = findViewById(R.id.imageView)
        btnSimpan = findViewById(R.id.btnAdd)
        btnBack = findViewById(R.id.btnBack)
        btnDeleteImage = findViewById(R.id.btnHapusImage)
        btnDeleteVoice = findViewById(R.id.btnHapusVoice)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        btnChooseVoice = findViewById(R.id.btnAddVoice)
    }

    private fun setupFirebase() {
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    private fun loadNoteData() {
        intent?.let {
            id = it.getStringExtra("id")
            judul = it.getStringExtra("title")
            deskripsi = it.getStringExtra("desc")
            imageUrl = it.getStringExtra("imageUrl")
            voiceUrl = it.getStringExtra("voiceUrl")
            timestamp = it.getStringExtra("timestamp")

            title.setText(judul)
            desc.setText(deskripsi)
            tanggal.text = timestamp

            if (!imageUrl.isNullOrEmpty()) {
                imageView.visibility = View.VISIBLE
                Glide.with(this).load(imageUrl).into(imageView)
                btnDeleteImage.visibility = View.VISIBLE
                tvDeleteImage.visibility = View.VISIBLE
            } else {
                imageView.visibility = View.GONE
                btnDeleteImage.visibility = View.GONE
                tvDeleteImage.visibility = View.GONE
            }

            if (!voiceUrl.isNullOrEmpty()) {
                voiceView.visibility = View.VISIBLE
                voiceView.text = getFileNameFromUrl(voiceUrl!!)
                btnDeleteVoice.visibility = View.VISIBLE
            } else {
                voiceView.visibility = View.GONE
                btnDeleteVoice.visibility = View.GONE
            }
        }
    }

    private fun setupUI(){
        setupProgressDialog()
    }

    private fun setupProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setTitle("Loading...")
            setCancelable(false)
        }
    }

    private fun setupListeners(){
        btnChooseImage.setOnClickListener {
            openFileChooser(PICK_IMAGE_REQUEST)
        }

        btnChooseVoice.setOnClickListener {
            openFileChooser(PICK_VOICE_REQUEST)
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnDeleteImage.setOnClickListener {
            imageView.setImageResource(0)
            imageUri = null
            imageView.visibility = View.GONE
            tvDeleteImage.visibility = View.GONE
            btnDeleteImage.visibility = View.GONE
        }

        btnDeleteVoice.setOnClickListener {
            voiceView.text = null
            voiceUri = null
            voiceView.visibility = View.GONE
            btnDeleteVoice.visibility = View.GONE
        }

        btnSimpan.setOnClickListener {
            val noteTitle = title.text.toString().trim()
            val noteDesc = desc.text.toString().trim()

            if (noteTitle.isEmpty() || noteDesc.isEmpty()) {
                Toast.makeText(this, "Title and Description cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressDialog.show()

            var imageUploadComplete = imageUri == null
            var voiceUploadComplete = voiceUri == null

            if (imageUri != null) {
                uploadImageToStorage(noteTitle, noteDesc) { url ->
                    imageUrl = url
                    imageUploadComplete = true
                    if (voiceUploadComplete) saveData(noteTitle, noteDesc, imageUrl, voiceUrl)
                }
            } else {
                imageUrl = null
                imageUploadComplete = true
            }

            if (voiceUri != null) {
                uploadVoiceToStorage(noteTitle, noteDesc) { url ->
                    voiceUrl = url
                    voiceUploadComplete = true
                    if (imageUploadComplete) saveData(noteTitle, noteDesc, imageUrl, voiceUrl)
                }
            } else {
                voiceUrl = null
                voiceUploadComplete = true
            }

            if (imageUploadComplete && voiceUploadComplete) {
                saveData(noteTitle, noteDesc, imageUrl, voiceUrl)
            }
        }
    }

    private fun openFileChooser(requestCode: Int) {
        val intent = Intent()
        intent.type = if (requestCode == PICK_IMAGE_REQUEST) "image/*" else "audio/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, requestCode)
    }

    private fun uploadImageToStorage(noteTitle: String, noteDesc: String, callback: (String?) -> Unit) {
        imageUri?.let { uri ->
            val storageRef = storage.reference.child("images/${System.currentTimeMillis()}.jpg")
            val uploadTask = storageRef.putFile(uri)

            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString())
                }.addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error uploading image", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            }.addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error uploading image", Toast.LENGTH_SHORT).show()
                callback(null)
            }
        } ?: callback(null)
    }

    private fun uploadVoiceToStorage(noteTitle: String, noteDesc: String, callback: (String) -> Unit) {
        voiceUri?.let { uri ->
            val fileName = "${System.currentTimeMillis()}.mp3"
            val storageRef = storage.reference.child("voices/$fileName")
            val uploadTask = storageRef.putFile(uri)

            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString())
                }.addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Failed to upload voice", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload voice", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return it.getString(nameIndex)
                }
            }
        }
        return null
    }

    private fun getFileNameFromUrl(url: String): String {
        return url.substringAfterLast('/')
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data?.data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                imageView.visibility = View.VISIBLE
                btnDeleteImage.visibility = View.VISIBLE
                tvDeleteImage.visibility = View.VISIBLE
                imageUri = data.data
                imageView.setImageURI(imageUri)
            } else if (requestCode == PICK_VOICE_REQUEST) {
                voiceView.visibility = View.VISIBLE
                btnDeleteVoice.visibility = View.VISIBLE
                voiceUri = data.data
                voiceView.text = getFileNameFromUri(voiceUri!!)
            }
        }
    }

    private fun saveData(noteTitle: String, noteDesc: String, imageUrl: String?, voiceUrl: String?) {
        val userId = mAuth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return
        }

        val note = hashMapOf<String, Any>(
            "userId" to userId,
            "title" to noteTitle,
            "desc" to noteDesc,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        if (!imageUrl.isNullOrEmpty()) {
            note["imageUrl"] = imageUrl
        }

        if (!voiceUrl.isNullOrEmpty()) {
            note["voiceUrl"] = voiceUrl
        }

        db.collection("note")
            .add(note)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Note added successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error adding note", Toast.LENGTH_SHORT).show()
            }
    }

}