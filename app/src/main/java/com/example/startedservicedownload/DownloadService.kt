package com.example.startedservicedownload

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.*

private const val MSG = 1
private const val URL = "url"
private const val PATH = "path"

class DownloadService : Service() {
    private lateinit var mMessenger: Messenger

    @SuppressLint("HandlerLeak")
    inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG -> runBlocking {
                    val url = msg.data.getString(URL)!!
                    val path = downloadCor(url)

                    val m = Message.obtain(null, MSG, 0, 0)
                    m.data = Bundle().apply {
                        putString(PATH, path)
                    }

                    try {
                        msg.replyTo.send(m)
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runBlocking {
            val url = intent?.extras?.getString("url")!!
            val path = downloadCor(url)
            val int = Intent()
            int.action = "com.example.startedservicedownload.PATH"
            int.putExtra("path", path)
            sendBroadcast(int)
        }
        stopSelf(startId)
        return START_STICKY
    }

    private suspend fun downloadCor(url: String): String? =
        withContext(Dispatchers.IO) {
            var path: String? = null
            var out: FileOutputStream? = null
            var inp: InputStream? = null
            try {
                inp = URL(url).openStream()
                val mIcon = BitmapFactory.decodeStream(inp)
                val uuid = UUID.randomUUID().toString().substring(0, 7)
                val name = "$uuid.png"

                out = openFileOutput(name, MODE_PRIVATE)
                mIcon.compress(Bitmap.CompressFormat.PNG, 100, out)
                path = "$filesDir/$name"

                Log.i("path", path)
            } catch (e: Exception) {
                Log.e("Error", e.message!!)
                e.printStackTrace()
            } finally {
                inp?.close()
                out?.close()
            }
            return@withContext path
        }

        override fun onBind(intent: Intent?): IBinder? {
        mMessenger = Messenger(IncomingHandler())
        return mMessenger.binder
    }
}