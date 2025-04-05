import java.io.*
import java.net.Socket
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

class User(private val username: String) : Machine() {
    private lateinit var socket: Socket
    private lateinit var input: BufferedReader
    private lateinit var output: PrintWriter

    fun start() {
        socket = Socket("localhost", 8080)
        input = BufferedReader(InputStreamReader(socket.getInputStream()))
        output = PrintWriter(socket.getOutputStream(), true)

        println("New user connecting to server.")
        output.println(username)

        thread { listenForServerMessages() }

        while (true) {
            val userInput = readLine() ?: continue
            if (userInput.lowercase() in listOf("time", "sync")) {
                output.println(userInput.lowercase())
            } else {
                output.println("[$username] $userInput")
            }
        }
    }

    private fun listenForServerMessages() {
        while (true) {
            val message = input.readLine() ?: break
            when {
                message.startsWith("OFFSET_REQUEST:") -> {
                    val serverTimeStr = message.removePrefix("OFFSET_REQUEST:")
                    val serverTime = LocalTime.parse(serverTimeStr)
                    val offset = Duration.between(serverTime, time)
                    output.println("OFFSET:$offset")
                }

                message.startsWith("ADJUST:") -> {
                    val adjustment = Duration.parse(message.removePrefix("ADJUST:"))
                    time = time.plus(adjustment)
                }

                message.startsWith("=== TIME REPORT") || message.contains(":") -> {
                    println(message)
                }

                else -> println(message)
            }
        }
    }
}

fun main() {
    print("Enter your username: ")
    val username = readLine()?.trim() ?: "User"
    User(username).start()
}
