package com.example.proyecto_tfg.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast

import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.util.SBUserManager
import android.content.Intent
import com.example.proyecto_tfg.MainActivity
import io.supabase.gotrue.http.GoTrueHttpException
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    //Método que se ejecuta al crearse el activity.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailText : EditText = findViewById(R.id.login_email)
        val passwordText : EditText = findViewById(R.id.login_password)
        val loginBtn : Button = findViewById(R.id.login_button)
        val registerBtn : Button = findViewById(R.id.register_button)
        val googleBtn : ImageButton = findViewById(R.id.googleLogin)

        val usrManager = SBUserManager(this)

        loginBtn.setOnClickListener {
            if (passwordText.text.trim() != "" && emailText.text.trim() != "") {
                try {
                    usrManager.signIn(emailText.text.toString(), passwordText.text.toString())
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } catch (e: GoTrueHttpException) {
                    Toast.makeText(
                        this,
                        getString(R.string.err_invalid_credentials),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.err_invalid_credentials), Toast.LENGTH_LONG)
                    .show()
            }
        }

        registerBtn.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        googleBtn.setOnClickListener {
            googleLogin()
        }

        //usrManager.signUp("supatestmyvc@gmail.com", "potato200")
        //usrManager.signIn("supatestmyvc@gmail.com", "potato200")

    }

    override fun onResume() {
        super.onResume()

        val usrManager = SBUserManager(this)

        if (usrManager.loggedIn()){
            Toast.makeText(this, getString(R.string.err_already_logged_in), Toast.LENGTH_LONG).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }

    private fun googleLogin() {
        TODO("Not yet implemented, ehe")
    }

}