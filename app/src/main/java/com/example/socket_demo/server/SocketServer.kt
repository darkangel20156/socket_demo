package com.example.socket_demo.server

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.math.log
/**
 * Socket server
 */
object SocketServer {

    private val TAG = SocketServer::class.java.simpleName

    private const val SOCKET_PORT = 9527

    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null

    private lateinit var mCallback: ServerCallback

    private lateinit var outputStream: OutputStream

    var result = true
    /**
     * Start service
     */
    fun startServer(callback: ServerCallback): Boolean {
        mCallback = callback
        Thread {
            try {
                serverSocket = ServerSocket(SOCKET_PORT)
                while (result) {
                    socket = serverSocket?.accept()
                    mCallback.otherMsg("${socket?.inetAddress} to connected")
                    ServerThread(socket!!, mCallback).start()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                result = false
            }
        }.start()
        return result
    }

    /**
     * Close service
     */
    fun stopServer() {
        socket?.apply {
            shutdownInput()
            shutdownOutput()
            close()
        }
        serverSocket?.close()
    }

    /**
     *Send to client
     */
    fun sendToClient(msg: String) {
        Thread {
            if (socket!!.isClosed) {
                Log.e(TAG, "sendToClient: Socket is closed")
                return@Thread
            }
            outputStream = socket!!.getOutputStream()
            try {
                outputStream.write(msg.toByteArray())
                outputStream.flush()
                mCallback.otherMsg("toClient: $msg")
                Log.d(TAG, "Sent to client successfully")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Failed to send message to client")
            }
        }.start()
    }

    class ServerThread(private val socket: Socket, private val callback: ServerCallback):
        Thread() {

        override fun run() {
            val inputStream: InputStream?
            try {
                inputStream = socket.getInputStream()
                val buffer = ByteArray(1024)
                var len : Int
                var receiveStr = ""
                if (inputStream.available() == 0) {
                    Log.e(TAG, "inputStream.available() == 0")
                }
                while (inputStream.read(buffer).also { len = it } != -1) {
                    receiveStr += String(buffer, 0, len, Charsets.UTF_8)
                    if (len < 1024) {
                        callback.receiveClientMsg(true, receiveStr)
                        receiveStr = ""
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                e.message?.let { Log.e("socket error", it) }
                callback.receiveClientMsg(false, "")
            }
        }
    }
}