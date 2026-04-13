package org.dogcard.game.hero

import org.dogcard.model.hero.Gender
import org.dogcard.model.hero.Hero
import org.dogcard.model.hero.HeroId
import org.dogcard.model.hero.HpValue

val BlankHero = Hero(
    heroId = HeroId("blank"),
    gender = Gender.MALE,
    maxHp = HpValue.of(4),
    skills = emptyList(),
)