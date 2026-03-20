package org.dogcard.model.game

import org.dogcard.model.seat.Seat

interface GameMode {
    fun assignAllegiances(seats: List<Seat>): List<Seat>
    fun checkWinCondition(room: IGameRoom): Winner?
    fun onSeatDeath(dead: Seat, room: IGameRoom)
}