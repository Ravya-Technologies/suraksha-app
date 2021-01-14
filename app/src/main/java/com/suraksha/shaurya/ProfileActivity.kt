package com.suraksha.shaurya

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val btnText = intent?.extras?.getString("btn_text")

        val pref = PreferenceHelper(this)

        val etUserName = findViewById<AppCompatEditText>(R.id.etUserName)
        val etAge = findViewById<AppCompatEditText>(R.id.etAge)
        val etPhone = findViewById<AppCompatEditText>(R.id.etPhone)

        etUserName.setText(pref.getUserName())
        etAge.setText(pref.getAge())
        etPhone.setText(pref.getRelativeNumber())

        val btnSave = findViewById<Button>(R.id.btnSave)
        if (btnText != null) {
            btnSave.text = btnText
        }

        btnSave.setOnClickListener {
            if (etUserName.text.toString().isNotEmpty()) {
                if (etAge.text.toString().isNotEmpty() && (etAge.text.toString().toInt() > 0)) {
                    if (etPhone.text.toString().isNotEmpty()) {
                        if (etPhone.text.toString().length == 10) {
                            pref.setUserName(etUserName.text.toString())
                            pref.setAge(etAge.text.toString())
                            pref.setRelativeNumber(etPhone.text.toString())

                            if (btnSave.text == "NEXT") {
                                startActivity(Intent(this, HomeActivity::class.java))
                                finish()
                            } else finish()

                            showToast("User Profile Saved")
                        } else showToast("Please enter a valid number")
                    } else showToast("Please enter the phone number")
                } else showToast("Please enter the age")
            } else showToast("Please enter the user name")
        }
    }
}

fun Activity.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}