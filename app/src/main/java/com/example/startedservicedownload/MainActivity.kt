package com.example.startedservicedownload

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*

private const val MSG = 1
private const val URL = "url"
private const val PATH = "path"

class MainActivity : AppCompatActivity() {
    private var mService: Messenger? = null
    private var bound = false
    private val replyMess = Messenger(HandlerReplyMsg())

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = Messenger(service)
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            bound = false
        }
    }

    @SuppressLint("HandlerLeak")
    inner class HandlerReplyMsg : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG -> {
                    val uri = msg.data.getString(PATH)
                    changeView(uri)
                }
                else -> super.handleMessage(msg)
            }
        }
    }
    fun changeView(uri: String?) {
        if (uri == "null" || uri == null)
            path_view.setText(R.string.failed)
        else {
            path_view.text = uri
            Glide.with(this).load(uri).into(imageView)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val defaultAddress = "https://clck.ru/SMLw2"

        started_btn.setOnClickListener {
            val intent = Intent(this, DownloadService::class.java)
            intent.putExtra(
                "url",
                if (url_text.text.isNotEmpty())
                    url_text.text.toString()
                else
                    defaultAddress
            )
            startService(intent)
        }

        bound_btn.setOnClickListener {
            if (!bound)
                return@setOnClickListener

            val msg = Message.obtain(null, MSG, 0, 0)
            msg.replyTo = replyMess
            msg.data = Bundle().apply {
                putString(
                    URL,
                    if (url_text.text.isNotEmpty())
                        url_text.text.toString()
                    else
                        defaultAddress
                )
            }

            try {
                mService?.send(msg)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, DownloadService::class.java)
        val bind = bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        if (!bind)
            unbindService(mConnection)
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(mConnection)
            bound = false
        }
    }

}