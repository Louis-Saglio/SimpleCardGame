package saglio.simplecardgame.game

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.first
import kotlin.collections.last
import kotlin.collections.map
import kotlin.collections.maxBy
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.random
import kotlin.collections.set
import kotlin.collections.sum
import kotlin.collections.sumBy
import kotlin.concurrent.withLock

class RuleViolationException: Exception()

class Game(private val players: List<Player>) {
    private val rounds: MutableList<Round> = mutableListOf()
    var playingPlayer = players.first()
    private val roundsLock = ReentrantLock()
    private val playingPlayerLock = ReentrantLock()

    fun play(card: Card): Player? {
        if (card in playingPlayer.cards) {
            playingPlayer.cards.remove(card)
            val round = when (rounds.last().cards.size) {
                players.size -> {
                    Round(mutableMapOf())
                }
                else -> {
                    rounds.last()
                }
            }
            roundsLock.withLock { round.cards[playingPlayer] = card }
            if (round.cards.size == players.size) {
                val winner = round.getWinner()
                if (winner == null) {
                    throw Exception("no winner on completed round")
                } else {
                    winner.earnedRounds.add(round)
                }
            }
            if (players.map { it.cards.size } == (0..players.size).map { 0 }) {
                return players.maxBy {
                    it.earnedRounds.map { round -> round.cards.values.sumBy { card -> card.value } }.sum()
                }
            }
            playingPlayerLock.withLock { playingPlayer = getNextPlayer() }
        } else {
            throw RuleViolationException()
        }
        return null
    }

    private fun getNextPlayer(): Player {
        return when (playingPlayer) {
            players.last() -> players.first()
            else -> {
                players[players.indexOf(playingPlayer) + 1]
            }
        }
    }
}

class Player(val cards: MutableList<Card>, val name: String) {
    val earnedRounds = mutableListOf<Round>()
}

class Round(val cards: MutableMap<Player, Card>) {
    // todo : map of player / card
    fun getWinner(): Player? {
        val color = cards.values.first().color
        return cards.maxBy { if (it.value.color == color) it.value.value else 0 }?.key
    }
}

enum class Color {
    COEUR, PIQUE, CARREAU, TREFLE
}

enum class CardName(val value: Int) {
    DEUX(0),
    TROIS(1),
    QUATRE(2),
    CINQ(3),
    SIX(4),
    SEPT(5),
    HUIT(6),
    NEUF(7),
    DIX(8),
    VALET(9),
    DAME(10),
    ROI(11),
    AS(12)
}

@Serializable
class Card(val color: Color, val name: String, val value: Int) {
    override fun toString(): String {
        return "Card(color=$color, name='$name', value=$value)"
    }

    @ExperimentalSerializationApi
    @Serializer(forClass = Card::class)
    companion object : KSerializer<Card> {
        override fun deserialize(decoder: Decoder): Card {
            return buildDefaultDeck().random()
        }
    }
}

fun buildDefaultDeck(): List<Card> {
    val cards = mutableListOf<Card>()
    for (color in Color.values()) {
        for (cardName in CardName.values()) {
            cards.add(Card(color, cardName.name, cardName.value))
        }
    }
    return cards
}
