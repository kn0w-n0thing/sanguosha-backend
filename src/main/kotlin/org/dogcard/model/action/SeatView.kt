package org.dogcard.model.action

import org.dogcard.model.seat.HpState

data class SeatView(
    val seatIndex: Int,
    val hp: HpState,
    val handCount: Int,
)