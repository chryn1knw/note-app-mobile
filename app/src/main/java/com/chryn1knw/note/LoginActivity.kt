package com.chryn1knw.note

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

class LoginActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var email: TextView
    private lateinit var password: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        email = findViewById(R.id.et_register_username)
        password = findViewById(R.id.et_register_password)

        val loginButton = findViewById<TextView>(R.id.button_login)

        loginButton.setOnClickListener {
            if (validateInput()) {
                signin(email.text.toString(), password.text.toString())
            }
        }

        val tvRegister = findViewById<TextView>(R.id.tv_register)
        val text = "Don't have an account? Register"

        val spannableString = SpannableString(text)
        val color = Color.parseColor("#FE504E")
        val colorSpan = ForegroundColorSpan(color)
        spannableString.setSpan(
            colorSpan,
            text.indexOf("Register"),
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvRegister.text = spannableString

        tvRegister.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
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
            val intent = Intent(this@LoginActivity, LoginWithGoogle::class.java)
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

    private fun signin(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    updateUI(user)
                } else {
                    Toast.makeText(baseContext, "User Not Found or Incorrect Password", Toast.LENGTH_LONG).show()
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
