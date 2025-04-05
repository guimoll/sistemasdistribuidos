import java.time.LocalTime
import kotlin.random.Random

abstract class Machine {
    var time: LocalTime = generateRandomTime()

    private fun generateRandomTime(): LocalTime {
        val hour = Random.nextInt(0, 24)
        val minute = Random.nextInt(0, 60)
        return LocalTime.of(hour, minute)
    }
}
