package com.example.startedservicedownload

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.*

class DownloadService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runBlocking {
            val url = intent?.extras?.getString("url")!!
            downloadCor(url)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun downloadCor(url: String): Bitmap? =
        withContext(Dispatchers.IO) {
            var mIcon11: Bitmap? = null
            var out: FileOutputStream? = null
            var inp: InputStream? = null
            try {
                inp = URL(url).openStream()
                mIcon11 = BitmapFactory.decodeStream(inp)
                val uuid = UUID.randomUUID().toString().substring(0, 7)
                val name = "$uuid.png"
                out = openFileOutput(name, Context.MODE_PRIVATE)
                mIcon11.compress(Bitmap.CompressFormat.PNG, 100, out)

                val intent = Intent()
                val path = "$filesDir/$name"
                intent.action = "com.example.startedservicedownload.PATH"
                intent.putExtra("path", path)
                sendBroadcast(intent)
                Log.i("path", path)
            } catch (e: Exception) {
                Log.e("Error", e.message!!)
                e.printStackTrace()
            } finally {
                inp?.close()
                out?.close()
            }
            return@withContext mIcon11
        }

    override fun onBind(intent: Intent?): IBinder? = null
}