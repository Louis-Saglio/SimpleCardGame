package saglio.simplecardgame

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.serialization.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import saglio.simplecardgame.game.Card
import saglio.simplecardgame.game.buildDefaultDeck

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


val deck = buildDefaultDeck()

@Serializable
class CardIdentifier(val name: String, val color: String) {
    fun getCard(): Card? {
        return deck.find { it.color.name == color && it.name == name }
    }
}


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.simpleCardGame(testing: Boolean = false) {

    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        post("/play") {
            val cardIdentifier = try {
                call.receive<CardIdentifier>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "")
                return@post
            }
            val card = cardIdentifier.getCard()
            if (card == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "There is no card whose color is ${cardIdentifier.color} and name is ${cardIdentifier.name}"
                )
            } else {
                @Suppress("EXPERIMENTAL_API_USAGE")
                call.respond(Json.stringify(Card.serializer(), card))
            }
        }
    }
}

