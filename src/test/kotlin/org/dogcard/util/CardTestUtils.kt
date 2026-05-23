package org.dogcard.util

import org.dogcard.model.card.Card
import org.dogcard.model.card.CardType

fun List<Card>.firstOfType(type: CardType): Card = first { it.type == type }