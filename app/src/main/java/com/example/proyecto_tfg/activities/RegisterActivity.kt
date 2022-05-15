package com.example.proyecto_tfg.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.example.proyecto_tfg.MainActivity
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.util.SBUserManager
import io.supabase.gotrue.http.GoTrueHttpException


class RegisterActivity : AppCompatActivity() {

    //Método que se ejecuta al crearse el activity.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val emailText : EditText = findViewById(R.id.register_email)
        val usernameText : EditText = findViewById(R.id.register_username)
        val passwordText : EditText = findViewById(R.id.register_password)
        val confirmPasswordText : EditText = findViewById(R.id.register_confirmPassword)
        val createAccountBtn : Button = findViewById(R.id.createAccount_button)

        val usrManager = SBUserManager(this)
        //usrManager.signUp("supatestmyvc@gmail.com", "potato200")
        //usrManager.signIn("supatestmyvc@gmail.com", "potato200")

        createAccountBtn.setOnClickListener(View.OnClickListener {

            when {

                usernameText.text.isBlank()
                        || emailText.text.isBlank()
                        || passwordText.text.isBlank()
                        || confirmPasswordText.text.isBlank() -> {Toast.makeText(this, getString(R.string.err_empty_fields), Toast.LENGTH_LONG).show()}

                !emailText.text.contains('@') || !emailText.text.contains('.') -> {Toast.makeText(this, getString(R.string.err_invalid_email), Toast.LENGTH_LONG).show()}

                passwordText.text.trim().length < 6 -> {Toast.makeText(this, getString(R.string.err_short_pass), Toast.LENGTH_LONG).show()}

                passwordText.text.toString() != confirmPasswordText.text.toString() -> {Toast.makeText(this, getString(R.string.err_different_passwords), Toast.LENGTH_LONG).show()}

                else -> {
                    try {
                        usrManager.signUp(
                            usernameText.text.toString(),
                            emailText.text.toString(),
                            passwordText.text.toString())

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)

                    } catch (e: GoTrueHttpException) {
                        Toast.makeText(this, getString(R.string.err_register), Toast.LENGTH_LONG).show()
                    }
                }

            }

        })

    }
}