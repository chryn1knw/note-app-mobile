package com.chryn1knw.note

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var myAdapter: AdapterList
    private lateinit var itemList: MutableList<ItemList>
    private lateinit var db: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupFirebase()
        setupUI()
        setupListeners()
    }

    private fun initializeViews(){
        recyclerView = findViewById(R.id.rcvNews)
        floatingActionButton = findViewById(R.id.floatAddNews)
    }

    private fun setupFirebase() {
        db = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()
        FirebaseApp.initializeApp(this)

        val currentUser = mAuth.currentUser
        if (currentUser == null) {
            navigateToSessionActivity()
        } else {
            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (mAuth.currentUser == null) {
                        navigateToSessionActivity()
                    }
                } else {
                    progressDialog.dismiss()
                    navigateToSessionActivity()
                }
            }
        }
    }

    private fun navigateToSessionActivity() {
        val intent = Intent(this, SessionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupUI() {
        setuprecycleView()
        setuptoolbar()
        setupProgressDialog()
    }

    private fun setuprecycleView(){
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        itemList = ArrayList()
        myAdapter = AdapterList(itemList)
        recyclerView.adapter = myAdapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }

    private fun setuptoolbar(){
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun setupProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setTitle("Loading...")
            setCancelable(false)
        }
    }

    private fun setupListeners() {
        floatingActionButton.setOnClickListener {
            val toAddPage = Intent(this, CreateNote::class.java)
            startActivity(toAddPage)
        }

        myAdapter.setOnItemClickListener(object : AdapterList.OnItemClickListener {
            override fun onItemClick(item: ItemList) {
                val intent = Intent(this@MainActivity, ViewNote::class.java).apply {
                    putExtra("id", item.id)
                    putExtra("title", item.judul)
                    putExtra("desc", item.subJudul)
                    putExtra("imageUrl", item.imageUrl)
                    putExtra("voiceUrl", item.voiceurl)
                    putExtra("timestamp", item.timestamp)
                }
                startActivity(intent)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        setupFirebase()
        getData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_logout) {
            mAuth.signOut()
            val intent = Intent(this, SessionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getData() {
        progressDialog.show()
        val userId = mAuth.currentUser?.uid
        if (userId != null) {
            db.collection("note")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        itemList.clear()
                        for (document in task.result) {
                            val timestamp = document.getTimestamp("timestamp")?.toDate()?.let { date ->
                                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                dateFormat.format(date)
                            } ?: ""
                            val item = ItemList(
                                document.id,
                                document.getString("title") ?: "",
                                document.getString("desc") ?: "",
                                document.getString("imageUrl") ?: "",
                                document.getString("voiceUrl") ?: "",
                                timestamp
                            )
                            itemList.add(item)
                        }
                        myAdapter.notifyDataSetChanged()
                    } else {
                        task.exception?.let {
                            Toast.makeText(this, "Error getting documents", Toast.LENGTH_LONG).show()
                            it.printStackTrace()
                        }
                    }
                    progressDialog.dismiss()
                }
        }
    }

}