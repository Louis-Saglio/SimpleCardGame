package saglio.simplecardgame

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import saglio.simplecardgame.game.Card
import saglio.simplecardgame.game.Game
import saglio.simplecardgame.game.Player
import saglio.simplecardgame.game.buildDefaultDeck

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


val deck = buildDefaultDeck()

@Serializable
class GetGameRequestPayload(val name: String, val color: String, val gameIndex: Int) {
    fun getCard(): Card? {
        return deck.find { it.color.name == color && it.name == name }
    }
}


class GameWrapper(val game: Game) {
    val players = mutableListOf<Player>()
}

val games = mutableListOf<GameWrapper>()


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.simpleCardGame(testing: Boolean = false) {

    install(ContentNegotiation) {
        json()
    }

    routing {
        route("/game") {
            get {
                createNewGameController()
            }
        }
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        post("/play") {
            val payload = try {
                call.receive<GetGameRequestPayload>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "")
                return@post
            }
            val card = payload.getCard()
            val game = games[payload.gameIndex]
            if (card == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "There is no card whose color is ${payload.color} and name is ${payload.name}"
                )
            } else {
                game.game.play(card)
                @Suppress("EXPERIMENTAL_API_USAGE")
                call.respond(Json.encodeToString(Card.serializer(), card))
            }
        }
    }
}

//fun createNewGameController(unit: Unit): Unit {
fun createNewGameController() {
    games.add(GameWrapper(Game()))
}
