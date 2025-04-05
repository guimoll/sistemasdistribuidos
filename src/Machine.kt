import java.time.LocalTime
import kotlin.random.Random

abstract class Machine {
    var time: LocalTime = generateRandomTime().also {
        println("Initial time generated: $it")
    }

    val currentTime: LocalTime
        get() {
            println("Accessing currentTime: $time")
            return time
        }

    private fun generateRandomTime(): LocalTime {
        val hour = Random.nextInt(0, 24)
        val minute = Random.nextInt(0, 60)
        return LocalTime.of(hour, minute)
    }
}