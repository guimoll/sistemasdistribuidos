import java.io.*
import java.net.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class Server : Machine() {
    private val serverSocket = ServerSocket(8080)
    private val userList = mutableListOf<ConnectedUser>()
    private val offsets = ConcurrentHashMap<String, Duration>()

    fun start() {
        println("Server started on port 8080")

        thread { handleCommands() }

        while (true) {
            val socket = serverSocket.accept()
            thread {
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = PrintWriter(socket.getOutputStream(), true)

                val username = input.readLine()
                val user = ConnectedUser(username, socket, input, output)
                synchronized(userList) { userList.add(user) }

                println("User '${user.username}' connected.")
                broadcast("User '${user.username}' connected.")

                handleUser(user)
            }
        }
    }

    private fun handleCommands() {
        val reader = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            when (reader.readLine()?.trim()) {
                "time" -> broadcastTimeReport()
                "sync" -> synchronizeTime()
            }
        }
    }

    private fun handleUser(user: ConnectedUser) {
        try {
            while (true) {
                val message = user.input.readLine() ?: break
                when (message.lowercase()) {
                    "time" -> broadcastTimeReport()
                    "sync" -> synchronizeTime()
                    else -> broadcast("[${user.username}]: $message")
                }
            }
        } catch (_: Exception) {
        } finally {
            disconnectUser(user)
        }
    }

    private fun synchronizeTime() {
        println("Server time: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
        offsets.clear()

        synchronized(userList) {
            for (user in userList) {
                try {
                    user.output.println("OFFSET_REQUEST:${time}")
                } catch (e: Exception) {
                    println("Error sending request to ${user.username}: ${e.message}")
                }
            }
        }

        val timeout = 5000L
        val start = System.currentTimeMillis()

        while (offsets.size < userList.size && System.currentTimeMillis() - start < timeout) {
            Thread.sleep(100)
        }

        if (offsets.isEmpty()) {
            println("No offsets received.")
            return
        }

        val averageOffset = offsets.values
            .fold(Duration.ZERO) { acc, d -> acc.plus(d) }
            .dividedBy(offsets.size.toLong())

        time = time.plus(averageOffset)
        println("Average offset: $averageOffset")
        println("Adjusted server time: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")

        synchronized(userList) {
            for (user in userList) {
                try {
                    user.output.println("ADJUST:${averageOffset}")
                } catch (_: Exception) {}
            }
        }
    }

    private fun broadcastTimeReport() {
        val report = StringBuilder("=== TIME REPORT ===\n")
        report.append("server: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}\n")

        synchronized(userList) {
            for (user in userList) {
                report.append("${user.username}: ${user.getFormattedTime()}\n")
            }
        }

        report.append("====================")
        broadcast(report.toString())
    }

    private fun broadcast(message: String) {
        println(message)
        synchronized(userList) {
            userList.forEach {
                try {
                    it.output.println(message)
                } catch (_: Exception) {}
            }
        }
    }

    private fun disconnectUser(user: ConnectedUser) {
        synchronized(userList) { userList.remove(user) }
        println("User '${user.username}' disconnected.")
        broadcast("User '${user.username}' disconnected.")
    }

    inner class ConnectedUser(
        val username: String,
        val socket: Socket,
        val input: BufferedReader,
        val output: PrintWriter
    ) : Machine() {
        fun getFormattedTime(): String {
            return time.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }
}

fun main() {
    Server().start()
}
