package com.example.socket_demo.server

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Socket server
 */
object SocketServer {

    private val TAG = SocketServer::class.java.simpleName

    private const val SOCKET_PORT = 9527

    private var serverSocket: ServerSocket? = null

    private lateinit var mCallback: ServerCallback

    private val clientThreads = mutableListOf<ServerThread>()

    /**
     * Start service
     */
    fun startServer(callback: ServerCallback): Boolean {
        mCallback = callback
        Thread {
            try {
                serverSocket = ServerSocket(SOCKET_PORT)
                while (true) {
                    val socket = serverSocket?.accept()
                    socket?.let {
                        mCallback.otherMsg("${socket.inetAddress} connected")
                        val clientThread = ServerThread(socket, mCallback)
                        clientThreads.add(clientThread)
                        clientThread.start()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
        return true
    }

    /**
     * Close service
     */
    fun stopServer() {
        serverSocket?.close()
        for (clientThread in clientThreads) {
            clientThread.stopThread()
        }
        clientThreads.clear()
    }

    /**
     * Send to all clients
     */
    fun sendToAllClients(msg: String) {
        for (clientThread in clientThreads) {
            clientThread.sendToClient(msg)
        }
    }

    class ServerThread(private val socket: Socket, private val callback: ServerCallback) :
        Thread() {

        private lateinit var outputStream: OutputStream
        private var running = true

        override fun run() {
            try {
                outputStream = socket.getOutputStream()
                val inputStream = socket.getInputStream()
                val buffer = ByteArray(1024)
                var len = 0
                var receiveStr = ""

                while (running && inputStream.read(buffer).also { len = it } != -1) {
                    receiveStr += String(buffer, 0, len, Charsets.UTF_8)
                    if (len < 1024) {
                        callback.receiveClientMsg(true, receiveStr)
                        receiveStr = ""
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                callback.receiveClientMsg(false, "")
            }
        }

        fun sendToClient(msg: String) {
            try {
                outputStream.write(msg.toByteArray())
                outputStream.flush()
                callback.otherMsg("toClient: $msg")
            } catch (e: IOException) {
                e.printStackTrace()
                callback.receiveClientMsg(false, "")
            }
        }

        fun stopThread() {
            running = false
            socket.close()
        }
    }
}
