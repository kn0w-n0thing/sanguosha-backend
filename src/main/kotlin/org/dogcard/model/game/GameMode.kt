package org.dogcard.model.game

import org.dogcard.model.seat.Seat

interface GameMode {
    val heroEntryDrawCount: Int
    fun assignAllegiances(seats: List<Seat>): List<Seat>
    fun checkWinCondition(seats: List<Seat>): Winner?
    fun onSeatDeath(dead: Seat, seats: List<Seat>): Seat
}