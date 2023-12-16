package com.example.socket_demo.client

/**
 * Client callback
 */
interface ClientCallback {
    //Receive messages from the server
    fun receiveServerMsg(msg: String)
    //Other news
    fun otherMsg(msg: String)
}