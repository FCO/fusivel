import kotlinx.coroutines.experimental.withTimeout

enum class CircuitBreakerState {
    CLOSED,
    OPENED,
    HALFOPENED
}

open class CircuitBreakerException(msg : String) : Exception(msg)
class CircuitBreakerCircuitOpenedException       : CircuitBreakerException("The circuit is opened")

class CircuitBreaker<T>(
        val retries         : Int  = 0,
        val fails           : Int  = 10,
        val timeout         : Long = 1000,
        val resetTimeout    : Long = 10000,
        val defaultResponse : T?   = null,
        val exec            : suspend () -> T
) {
    var failsCounter        = 0
    var status              = CircuitBreakerState.CLOSED
    var halfOpensAt : Long? = null

    suspend fun execute() : T {
        if(status == CircuitBreakerState.OPENED) {
            if(halfOpensAt != null && halfOpensAt!! >= System.currentTimeMillis()) {
                if(defaultResponse != null)
                    return defaultResponse
                throw CircuitBreakerCircuitOpenedException()
            }
            status = CircuitBreakerState.HALFOPENED
        }
        var ret : T? = null
        var error : Throwable? = null
        for(i in 0 .. retries) {
            try {
                withTimeout(timeout) {
                    ret = exec()
                }
            } catch (e : Exception) {
                error = e
                if(status == CircuitBreakerState.HALFOPENED) {
                    status = CircuitBreakerState.OPENED
                    halfOpensAt = System.currentTimeMillis() + resetTimeout
                    throw error
                }
            }
            if(error == null) {
                if(status == CircuitBreakerState.HALFOPENED) {
                    status = CircuitBreakerState.CLOSED
                    halfOpensAt = null
                }
                failsCounter = 0
                return ret!!
            }
        }

        if(status == CircuitBreakerState.CLOSED) {
            failsCounter++
            if (failsCounter >= fails) {
                status = CircuitBreakerState.OPENED
                halfOpensAt = System.currentTimeMillis() + resetTimeout
            }
        }
        if(defaultResponse != null)
            return defaultResponse
        if(error != null) throw error
        throw CircuitBreakerException("Error on Circuit Breaker")
    }
}