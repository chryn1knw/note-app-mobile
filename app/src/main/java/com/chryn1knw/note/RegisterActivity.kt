package com.chryn1knw.note

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class RegisterActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var email: TextView
    private lateinit var password: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()
        email = findViewById(R.id.et_register_username)
        password = findViewById(R.id.et_register_password)
        val registerButton = findViewById<TextView>(R.id.button_register)

        registerButton.setOnClickListener {
            if (validateInput()) {
                register(email.text.toString(), password.text.toString())
            }
        }

        val tvLogin = findViewById<TextView>(R.id.tv_login)
        val text = "Already have an account? Log in"

        val spannableString = SpannableString(text)
        val color = Color.parseColor("#FE504E")
        val colorSpan = ForegroundColorSpan(color)
        spannableString.setSpan(
            colorSpan,
            text.indexOf("Log in"),
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvLogin.text = spannableString

        tvLogin.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        val tvGoogleacc = findViewById<TextView>(R.id.tv_login_google)
        val text1 = "Or try using google account"

        val spangooglebtn = SpannableString(text1)
        spangooglebtn.setSpan(
            colorSpan,
            text1.indexOf("account"),
            text1.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvGoogleacc.text = spangooglebtn

        tvGoogleacc.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginWithGoogle::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validateInput(): Boolean {
        var isValid = true

        val emailInput = email.text.toString().trim { it <= ' ' }
        if (emailInput.isEmpty()) {
            email.error = "Email is required"
            email.requestFocus()
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            email.error = "Invalid email format"
            email.requestFocus()
            isValid = false
        }

        val passwordInput = password.text.toString().trim { it <= ' ' }
        if (passwordInput.isEmpty()) {
            password.error = "Password is required"
            password.requestFocus()
            isValid = false
        } else if (passwordInput.length < 6) {
            password.error = "Password must be at least 6 characters"
            password.requestFocus()
            isValid = false
        }
        return isValid
    }

    private fun register(email:String, password:String){
        mAuth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    updateUI(user)
                } else {
                    Toast.makeText(baseContext, "User has already been registered", Toast.LENGTH_LONG).show()
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}