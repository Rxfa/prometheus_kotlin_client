import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") {
            simulateError(1..75)?.let { (status, msg) ->
                call.respond(status, msg)
                return@get
            }
            call.respondText("Search results here!")
        }

        get("/search") {
            simulateError(1..80)?.let { (status, msg) ->
                call.respond(status, msg)
                return@get
            }
            call.respondText("Search results here!")
        }

        post("/checkout") {
            simulateError(1..85)?.let { (status, msg) ->
                call.respond(status, msg)
                return@post
            }
            call.respondText("Purchase made successfully!")
        }

        get("/orders") {
            simulateError(1..90)?.let { (status, msg) ->
                call.respond(status, msg)
                return@get
            }
            call.respondText("Here are your orders!")
        }

        get("/cart") {
            simulateError(1..95)?.let { (status, msg) ->
                call.respond(status, msg)
                return@get
            }
            call.respondText("Here is your cart!")
        }
    }
}

fun simulateError(code: IntRange): Pair<HttpStatusCode, String>? =
    when (code.random()) {
        1 -> HttpStatusCode.BadRequest to "Bad Request"
        2 -> HttpStatusCode.Unauthorized to "Unauthorized"
        3 -> HttpStatusCode.Forbidden to "Forbidden"
        4 -> HttpStatusCode.NotFound to "Not Found"
        5 -> HttpStatusCode.InternalServerError to "Internal Server Error"
        6 -> throw IllegalStateException("Simulated logic error")
        7 -> throw IllegalArgumentException("Simulated input error")
        else -> null
    }
