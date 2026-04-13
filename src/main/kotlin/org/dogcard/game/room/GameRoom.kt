package org.dogcard.game.room

import org.dogcard.model.game.GameMode
import org.dogcard.model.seat.Seat

data class GameRoom(
    val mode: GameMode,
    val seats: List<Seat>,
)