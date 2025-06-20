package io.github.rxfa.prometheus.ktor_example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/"){
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

        get("/cart"){
            simulateError(1..95)?.let { (status, msg) ->
                call.respond(status, msg)
                return@get
            }
            call.respondText("Here is your cart!")
        }
    }
}

fun simulateError(code: IntRange): Pair<HttpStatusCode, String>? {
    return when (code.random()) {
        1 -> HttpStatusCode.BadRequest to "Bad Request"
        2 -> HttpStatusCode.Unauthorized to "Unauthorized"
        3 -> HttpStatusCode.Forbidden to "Forbidden"
        4 -> HttpStatusCode.NotFound to "Not Found"
        5 -> HttpStatusCode.InternalServerError to "Internal Server Error"
        6 -> throw IllegalStateException("Simulated logic error")
        7 -> throw IllegalArgumentException("Simulated input error")
        else -> null
    }
}
