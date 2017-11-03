import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import java.util.*

val exceptionRange = 0                          .. (Int.MAX_VALUE / 3)
val sleepRange     = (Int.MAX_VALUE / 3)        .. ((Int.MAX_VALUE / 3) * 2)
val okRange        = ((Int.MAX_VALUE / 3) * 2)  .. Int.MAX_VALUE

fun main(args : Array<String>) {
    runBlocking { delay(1000) }
    var counter = 0
    val cb = CircuitBreaker(
            retries         = 2,
            fails           = 3,
            timeout         = 300,
            resetTimeout    = 2000,
            defaultResponse = "default response!"
    ) {
        counter++
        if((10..20).contains(counter)) {
            println("not even trying!")
            throw Exception("Will it open the circuit?")
        }
        println("trying")
        var rand = Random().nextInt()
        if(rand < 0) rand *= -1
        when {
            exceptionRange.contains(rand)   -> throw Exception("$rand at range $exceptionRange")
            sleepRange.contains(rand)       -> delay(10000)
            okRange.contains(rand)          -> return@CircuitBreaker "ok!! It worked"
        }
        println("Something really wrong happened!!!")
    }
    runBlocking {
        for(i in 1 .. 50) {
            println("=> ${cb.status}, ${cb.failsCounter}")
            try {
                println(cb.execute())
            } catch (e: Exception) {
                println(e.localizedMessage)
            }
            delay(1000)
        }
    }
}