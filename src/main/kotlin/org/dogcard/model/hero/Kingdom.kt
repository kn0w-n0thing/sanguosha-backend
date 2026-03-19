package org.dogcard.model.hero

/**
 * Faction (势力) a hero permanently belongs to.
 *
 * This is an intrinsic attribute of the hero card itself, always known,
 * and is used across all modes — e.g. displayed on hero cards in Identity mode,
 * and used to resolve allegiance in Kingdom mode.
 *
 * The face-down / unrevealed state of a seat in Kingdom mode (暗将) is modelled
 * separately as [org.dogcard.model.hero.Allegiance.Unrevealed], not here.
 */
enum class Kingdom {
    WEI,    // 魏
    SHU,    // 蜀
    WU,     // 吴
    QUN,    // 群
}