package org.dogcard.model.deck

import org.dogcard.model.card.Card

interface ICardZone {
    fun contains(card: Card): Boolean
    fun toList(): List<Card>
    fun transfer(card: Card, to: ICardZone): Card
    fun transferAll(batch: List<Card>, to: ICardZone)
}