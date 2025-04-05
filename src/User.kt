import java.io.*
import java.net.Socket
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

class User(private val username: String) {
    private lateinit var socket: Socket
    private lateinit var input: BufferedReader
    private lateinit var output: PrintWriter
    private var time: LocalTime

    init {
        print("Enter your time (HH:mm): ")
        val timeInput = readLine()?.trim() ?: "00:00"
        time = LocalTime.parse(timeInput, DateTimeFormatter.ofPattern("HH:mm"))
        println("User time set to: $time")
    }

    fun start() {
        socket = Socket("localhost", 8080)
        input = BufferedReader(InputStreamReader(socket.getInputStream()))
        output = PrintWriter(socket.getOutputStream(), true)

        println("New user connecting to server.")
        output.println(username)
        output.println(time.format(DateTimeFormatter.ofPattern("HH:mm")))

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
            println("Received message from server: $message")
            when {
                message.startsWith("OFFSET_REQUEST:") -> {
                    val serverTimeStr = message.removePrefix("OFFSET_REQUEST:")

                    val serverTimeInMinutes = serverTimeStr.split(":").let {
                        if (it.size >= 2) it[0].toInt() * 60 + it[1].toInt() else 0
                    }

                    val currentTimeInMinutes = getTime().hour * 60 + getTime().minute
                    val offsetInMinutes = currentTimeInMinutes - serverTimeInMinutes

                    println("Sending offset to server: $offsetInMinutes")
                    output.println("OFFSET:$offsetInMinutes")
                }

                message.startsWith("ADJUST:") -> {
                    val adjustmentInMinutes = message.removePrefix("ADJUST:").toLong()
                    val adjustment = Duration.ofMinutes(adjustmentInMinutes)
                    setTime(getTime().plus(adjustment))
                    println("Adjusted user time: ${getTime().format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }

                message.startsWith("=== TIME REPORT") || message.contains(":") -> {
                    println(message)
                }

                else -> println(message)
            }
        }
    }

    fun getTime(): LocalTime {
        println("Accessing user time: $time")
        return time
    }

    fun setTime(newTime: LocalTime) {
        println("Setting user time: $newTime")
        time = newTime
    }
}

fun main() {
    print("Enter your username: ")
    val username = readLine()?.trim() ?: "User"
    User(username).start()
}