package com.example.tranki

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class SetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val doneButton: Button = findViewById(R.id.doneButton)
        doneButton.setOnClickListener { finish() }
    }
}