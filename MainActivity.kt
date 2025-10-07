
package com.example.chessapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPlay = findViewById<Button>(R.id.btn_play)
        val btnSettings = findViewById<Button>(R.id.btn_settings)
        val btnExit = findViewById<Button>(R.id.btn_exit)

        btnPlay.setOnClickListener {
            val intent = Intent(this, Chess3DActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            // TODO: Implement settings activity
            // For now, just start chess activity
            val intent = Intent(this, Chess3DActivity::class.java)
            startActivity(intent)
        }

        btnExit.setOnClickListener {
            finish()
        }
    }
}