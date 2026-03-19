package org.dogcard.model.hero

import org.dogcard.model.card.Card

/**
 * A single hero (武将) unit, representing the hero card definition.
 *
 * Hero does not track current HP — that is owned by the Seat via [HpState],
 * because the effective max HP may depend on combining multiple heroes
 * (e.g. Kingdom mode 国战 half-fish calculation).
 *
 * Hand cards and judgment area also belong to the Seat and are shared
 * across heroes in dual-hero mode.
 *
 * @param heroId        Unique hero identifier.
 * @param gender        Affects some card and skill targeting rules.
 * @param maxHp         Base HP capacity from the hero card; may be X.5 in Kingdom mode.
 * @param skills        Skills granted by this hero card.
 * @param equipmentArea The five equipment slots this hero occupies.
 * @param specialArea   Hero-specific staging area (e.g. Zhuge Liang's 七星 cards).
 */
data class Hero(
    val heroId: HeroId,
    val gender: Gender,
    val maxHp: HpValue,
    val skills: List<Skill>,
    val equipmentArea: EquipmentArea = EquipmentArea(),
    val specialArea: List<Card>? = null,
)