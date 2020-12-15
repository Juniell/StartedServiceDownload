package com.example.startedservicedownload

import android.app.Service
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.atomic.AtomicInteger

private const val MSG = 1
private const val URL = "url"
private const val PATH = "path"

// Задание 3 и 4
class DownloadService : Service() {
    private var count = AtomicInteger(0)
    private val coroutineJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + coroutineJob)
    private val handler = IncomingHandler(WeakReference(this))
    private val mMessenger: Messenger = Messenger(handler)


    class IncomingHandler(private val outerClass: WeakReference<DownloadService>) :
        Handler(Looper.getMainLooper()) {
        private val job = Job()
        private val scopeUi = CoroutineScope(Dispatchers.Main + job)

        override fun handleMessage(msg: Message) {
            val replyTo = msg.replyTo
            val data = msg.data
            when (msg.what) {
                MSG -> scopeUi.launch {
                    withContext(Dispatchers.IO) {
                        val url = data.getString(URL)
                        val m = Message.obtain(null, MSG, 0, 0)
                        var message: String? = null
                        if (url != null)
                            message = outerClass.get()?.downloadCor(url)

                        if (message == null)
                            message = "Failed"

                        m.data = Bundle().apply { putString(PATH, message) }
                        try {
                            replyTo.send(m)
                        } catch (e: RemoteException) {
                            Log.e("DownloadServerError", e.message!!)
                        }
                    }
                }
                else -> super.handleMessage(msg)
            }
        }

        fun stopScope() {
            scopeUi.cancel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                count.incrementAndGet()
                val int = Intent()
                var message: String? = null
                int.action = "com.example.startedservicedownload.PATH"
                if (intent != null && intent.hasExtra("url")) {
                    val url = intent.getStringExtra("url")

                    if (url != null)
                        message = downloadCor(url)
                }

                if (message == null)
                    message = "Failed"

                int.putExtra("path", message)
                sendBroadcast(int)
            }
        }.invokeOnCompletion {
            count.decrementAndGet()
            if (count.toInt() == 0) {
                stopSelf(startId)
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        handler.stopScope()
        uiScope.cancel()
        super.onDestroy()
    }

    suspend fun downloadCor(url: String): String? =
        withContext(Dispatchers.IO) {
            var res: String? = null
            try {
                val connect = URL(url).openConnection()
                res = save(connect)
            } catch (e: Exception) {
                Log.e("DownloadServerError", e.message!!)
            }
            return@withContext res
        }

    private fun save(connect: URLConnection): String? {
        val name = URLUtil.guessFileName(connect.url.toString(), null, null)
        val type = connect.contentType

        if(!type.contains("image"))
            return null

        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        values.put(MediaStore.Images.Media.MIME_TYPE, type)

        var stream: OutputStream? = null
        var uri: Uri? = null
        var inp: InputStream? = null
        var res: String? = null
        try {
            inp = connect.getInputStream()
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            uri = contentResolver.insert(contentUri, values)
            stream = uri?.let { contentResolver.openOutputStream(it) }
            stream?.write(inp.readBytes())
            res = uri.toString()
        } catch (e: Exception) {
            if (uri != null) {
                contentResolver.delete(uri, null, null)
                res = null
            }
            Log.e("DownloadServerError", e.message!!)
        } finally {
            inp?.close()
            stream?.close()
        }
        return res
    }

    override fun onBind(intent: Intent?): IBinder? = mMessenger.binder
}

// Задание 1 и 2.
//class DownloadService : Service() {
//    private var count = AtomicInteger(0)
//    private val coroutineJob = Job()
//    private val uiScope = CoroutineScope(Dispatchers.Main + coroutineJob)
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        uiScope.launch {
//            withContext(Dispatchers.IO) {
//                count.incrementAndGet()
//                val int = Intent()
//                var message: String? = null
//                int.action = "com.example.startedservicedownload.PATH"
//                if (intent != null && intent.hasExtra("url")) {
//                    val url = intent.getStringExtra("url")
//
//                    if (url != null)
//                        message = downloadCor(url)
//                }
//
//                if (message == null)
//                    message = "Failed"
//
//                int.putExtra("path", message)
//                sendBroadcast(int)
//            }
//        }.invokeOnCompletion {
//            count.decrementAndGet()
//            if (count.toInt() == 0) {
//                stopSelf(startId)
//            }
//        }
//        return START_REDELIVER_INTENT
//    }
//
//    private suspend fun downloadCor(url: String): String? =
//        withContext(Dispatchers.IO) {
//            var path: String? = null
//            var out: FileOutputStream? = null
//            var inp: InputStream? = null
//            try {
//                val connect = URL(url).openConnection()
//                inp = connect.getInputStream()
//                val name = URLUtil.guessFileName(connect.url.toString(),null,null)
//                out = openFileOutput(name, MODE_PRIVATE)
//                out.write(inp.readBytes())
//                path = "$filesDir/$name"
//                Log.i("path", path)
//            } catch (e: Exception) {
//                 Log.e("DownloadServerError", e.message!!)
//            } finally {
//                inp?.close()
//                out?.close()
//            }
//            return@withContext path
//        }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    override fun onDestroy() {
//        uiScope.cancel()
//        super.onDestroy()
//    }
//}
