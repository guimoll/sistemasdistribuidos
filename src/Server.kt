import java.io.*
import java.net.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.random.Random

class Server {
    private val serverSocket = ServerSocket(8080)
    private val userList = mutableListOf<ConnectedUser>()
    private val offsets = ConcurrentHashMap<String, Long>()
    private var time: LocalTime = generateRandomTime()

    fun start() {
        println("Server started on port 8080")

        thread { handleCommands() }

        while (true) {
            val socket = serverSocket.accept()
            thread {
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = PrintWriter(socket.getOutputStream(), true)

                val username = input.readLine()
                val userTime = LocalTime.parse(input.readLine(), DateTimeFormatter.ofPattern("HH:mm"))
                val user = ConnectedUser(username, socket, input, output, userTime)
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
                when {
                    message.lowercase() == "time" -> broadcastTimeReport()
                    message.lowercase() == "sync" -> synchronizeTime()
                    message.startsWith("OFFSET:") -> {
                        val offsetInMinutes = message.removePrefix("OFFSET:").toLong()
                        offsets[user.username] = offsetInMinutes
                    }
                    else -> broadcast("[${user.username}]: $message")
                }
            }
        } catch (_: Exception) {
        } finally {
            disconnectUser(user)
        }
    }

    private fun synchronizeTime() {
        println("Server time: ${getTime().format(DateTimeFormatter.ofPattern("HH:mm"))}")
        offsets.clear()

        synchronized(userList) {
            for (user in userList) {
                try {
                    user.output.println("OFFSET_REQUEST:${getTime().format(DateTimeFormatter.ofPattern("HH:mm"))}")
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

        offsets.forEach { (username, offsetInMinutes) ->
            println("Offset from $username: $offsetInMinutes minutes")
        }

        val totalOffsetInMinutes = offsets.values.fold(0L) { acc, offset -> acc + offset }
        val averageOffsetInMinutes = totalOffsetInMinutes / (offsets.size + 1)
        val averageOffset = Duration.ofMinutes(averageOffsetInMinutes)

        println("Average offset: ${averageOffset.toMinutes()}")

        setTime(getTime().plus(averageOffset))
        println("Adjusted server time: ${getTime().format(DateTimeFormatter.ofPattern("HH:mm"))}")

        synchronized(userList) {
            for (user in userList) {
                try {
                    val userOffsetInMinutes = offsets[user.username] ?: 0L
                    val individualAdjustment = Duration.ofMinutes(-userOffsetInMinutes + averageOffsetInMinutes)
                    user.output.println("ADJUST:${individualAdjustment.toMinutes()}")

                    user.setTime(user.getTime().plus(individualAdjustment))
                } catch (e: Exception) {
                    println("Error sending adjustment to ${user.username}: ${e.message}")
                }
            }
        }
    }

    private fun broadcastTimeReport() {
        val report = StringBuilder("=== TIME REPORT ===\n")
        report.append("server: ${getTime().format(DateTimeFormatter.ofPattern("HH:mm"))}\n")

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

    private fun generateRandomTime(): LocalTime {
        val hour = Random.nextInt(0, 24)
        val minute = Random.nextInt(0, 60)
        return LocalTime.of(hour, minute)
    }

    fun getTime(): LocalTime {
        return time
    }

    fun setTime(newTime: LocalTime) {
        time = newTime
    }

    inner class ConnectedUser(
        val username: String,
        val socket: Socket,
        val input: BufferedReader,
        val output: PrintWriter,
        private var time: LocalTime
    ) {
        fun getFormattedTime(): String {
            return getTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        }

        fun getTime(): LocalTime {
            return time
        }

        fun setTime(newTime: LocalTime) {
            time = newTime
        }
    }
}

fun main() {
    Server().start()
}