package org.dogcard.model.card

/**
 * A single physical game card.
 *
 * @param type   The card type, which determines its category, effect, and equipment slot.
 * @param suit   One of SPADE / HEART / CLUB / DIAMOND.
 * @param number 1 (Ace) through 13 (King).
 */
data class Card(
    val type: CardType,
    val suit: Suit,
    val number: Int,
) {
    init {
        require(number in 1..13) { "Card number must be 1–13, got $number" }
    }

    val category: CardCategory get() = type.category
    val equipmentType: EquipmentType? get() = type.equipmentType
    val weaponRange: Int? get() = type.weaponRange
    val damageType: DamageType? get() = type.damageType

    override fun toString(): String {
        val name = CardType.chineseNames[type] ?: type.name
        val suitSymbol = when (suit) {
            Suit.SPADE   -> "♠"
            Suit.HEART   -> "♥"
            Suit.CLUB    -> "♣"
            Suit.DIAMOND -> "♦"
        }
        val rank = when (number) {
            1    -> "A"
            11   -> "J"
            12   -> "Q"
            13   -> "K"
            else -> number.toString()
        }
        return "($suitSymbol$rank)$name"
    }
}