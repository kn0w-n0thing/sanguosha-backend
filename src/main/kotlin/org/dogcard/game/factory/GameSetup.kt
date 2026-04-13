package org.dogcard.game.factory

import org.dogcard.game.room.GameRoom
import org.dogcard.model.deck.IDeck

data class GameSetup(
    val room: GameRoom,
    val deck: IDeck,
)