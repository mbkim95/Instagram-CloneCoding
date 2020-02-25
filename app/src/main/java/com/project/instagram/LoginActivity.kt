package com.project.instagram

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    var auth: FirebaseAuth? = null
    var googleSignInClient: GoogleSignInClient? = null
    val GOOGLE_LOGIN_CODE = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        email_login_button.setOnClickListener {
            signinAndSignup()
        }

        google_login_button.setOnClickListener {
            // First step for Google Login
            googleLogin()
        }

        val googleSigninOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSigninOptions)
    }

    fun googleLogin() {
        var singinIntent = googleSignInClient?.signInIntent
        startActivityForResult(singinIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_LOGIN_CODE) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                var account = result.signInAccount
                // Second step for Google Login
                firebaseAuthWithGoogle(account)
            }
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Login
                moveMainPage(task.result?.user)
            } else {
                // Show the error message
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun signinAndSignup() {
        auth?.createUserWithEmailAndPassword(
            email_editText.text.toString(),
            password_editText.text.toString()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Creating a user account
                moveMainPage(task.result?.user)
            } else if (task.exception?.message.isNullOrEmpty()) {
                // Show the error message
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
            } else {
                // Login if you have account
                signinEmail()
            }
        }
    }

    fun signinEmail() {
        auth?.signInWithEmailAndPassword(
            email_editText.text.toString(),
            password_editText.text.toString()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Login
                moveMainPage(task.result?.user)
            } else {
                // Show the error message
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun moveMainPage(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
