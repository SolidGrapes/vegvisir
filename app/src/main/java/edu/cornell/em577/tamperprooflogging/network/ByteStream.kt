package edu.cornell.em577.tamperprooflogging.network

import edu.cornell.em577.tamperprooflogging.protocol.exception.UnexpectedTerminationException
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Thread-safe reliable bidirectional byte stream between two network endpoints used to send and
 * receive data over the network, where each local send call is paired with a corresponding
 * remote receive call.
 */
class ByteStream private constructor(val socket: Socket) {

    companion object {

        /** Establishes a TCP connection in the role provided by the specified ConnectionType. */
        fun create(type: ConnectionType, serverIpAddress: String, serverPort: Int): ByteStream {
            return when (type) {
                ConnectionType.SERVER -> getServerEndpoint(serverPort)
                ConnectionType.CLIENT -> getClientEndpoint(serverIpAddress, serverPort)
            }
        }

        /** Retrieves the endpoint used by the server to communicate with the client. */
        private fun getServerEndpoint(serverPort: Int): ByteStream {
            try {
                val serverSocket = ServerSocket(serverPort)
                serverSocket.reuseAddress = true
                val socket = serverSocket.accept()
                socket.reuseAddress = true
                return ByteStream(socket)
            } catch (e: Exception) {
                throw UnexpectedTerminationException("Unexpectedly terminated")
            }
        }

        /** Retrieves the endpoint used by the client to communicate with the server. */
        private fun getClientEndpoint(serverIpAddress: String, serverPort: Int): ByteStream {
            try {
                val serverAddr = InetAddress.getByName(serverIpAddress)
                val socket = Socket(serverAddr, serverPort)
                socket.reuseAddress = true
                return ByteStream(socket)
            } catch (e: Exception) {
                throw UnexpectedTerminationException("No IP Address for the specified host could be found")
            }
        }
    }

    /** Receives bytes from the ByteStream. Paired with a corresponding remote send call. */
    fun send(byteArray: ByteArray) {
        try {
            val outputStream = DataOutputStream(socket.getOutputStream())
            outputStream.writeInt(byteArray.size)
            outputStream.write(byteArray)
        } catch (e: Exception) {
            throw UnexpectedTerminationException("Unexpectedly terminated")
        }
    }

    /**
     * Blocking call that receives bytes from the ByteStream. Paired with a corresponding remote
     * send call.
     */
    fun recv(): ByteArray {
        try {
            val inputStream = DataInputStream(socket.getInputStream())
            val msgLen = inputStream.readInt()
            val byteArray = ByteArray(msgLen)
            inputStream.readFully(byteArray, 0, byteArray.size)
            return byteArray
        } catch (e: Exception) {
            throw UnexpectedTerminationException("Unexpectedly terminated")
        }
    }

    /** Closes the ByteStream. */
    fun close() {
        try {
            socket.close()
        } catch (e: Exception) {
            throw UnexpectedTerminationException("Unexpectedly terminated")
        }
    }

    enum class ConnectionType {
        SERVER,
        CLIENT
    }
}