package com.example.startedservicedownload

import android.annotation.SuppressLint
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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
            var inp: InputStream? = null
            var res: String? = null
            try {
                inp = URL(url).openStream()
                val mIcon = BitmapFactory.decodeStream(inp)
                res = save(mIcon)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                inp?.close()
            }
            return@withContext res
        }

    private fun save(bitmap: Bitmap): String? {
        val uuid = UUID.randomUUID().toString().substring(0, 7)
        val name = "$uuid.png"

        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,  Environment.DIRECTORY_PICTURES)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")

        var stream: OutputStream? = null
        var uri: Uri? = null
        try {
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            uri = contentResolver.insert(contentUri, values)
            stream = uri?.let { contentResolver.openOutputStream(it) }
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                throw IOException()
        } catch (e: Exception) {
            if (uri != null) {
                contentResolver.delete(uri, null, null)
                uri = null
            }
            e.printStackTrace()
        } finally {
            stream?.close()
        }
        return uri.toString()
    }

    override fun onBind(intent: Intent?): IBinder? {
        mMessenger = Messenger(IncomingHandler())
        return mMessenger.binder
    }
}