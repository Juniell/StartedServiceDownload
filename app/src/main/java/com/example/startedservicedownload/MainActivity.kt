package com.example.startedservicedownload

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val defaultAddress = "https://clck.ru/SMLw2"

        save_btn.setOnClickListener {
            val intent = Intent(this, DownloadService::class.java)
            intent.putExtra("url",
                    if (url_text.text.isNotEmpty())
                        url_text.text.toString()
                    else
                        defaultAddress
            )
            startService(intent)
        }
    }

}