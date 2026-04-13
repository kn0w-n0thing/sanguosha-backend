package org.dogcard.game.factory

import org.dogcard.game.deck.StandardDeck
import org.dogcard.game.hero.BlankHero
import org.dogcard.game.mode.OneVsOneMode
import org.dogcard.game.room.GameRoom
import org.dogcard.model.card.Card
import org.dogcard.model.card.CardType
import org.dogcard.model.card.StandardCards
import org.dogcard.model.seat.HpState
import org.dogcard.model.seat.Seat

private const val SEAT_COUNT_1V1 = 2
private const val HEROES_PER_SEAT = 3

class GameRoomFactory {

    val currentCardPool: List<Card> = StandardCards.filter {
        it.type in setOf(CardType.ATTACK, CardType.DODGE)
    }

    fun create1v1Setup(): GameSetup {
        val seats = List(SEAT_COUNT_1V1) { index ->
            Seat(
                seatIndex = index,
                heroes = List(HEROES_PER_SEAT) { BlankHero },
                handCards = emptyList(),
                hp = HpState(4, 4)
            )
        }
        val room = GameRoom(mode = OneVsOneMode(), seats = seats)
        return GameSetup(room = room, deck = StandardDeck.shuffled(currentCardPool))
    }
}