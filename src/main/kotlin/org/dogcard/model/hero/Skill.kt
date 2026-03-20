package org.dogcard.model.hero

import org.dogcard.model.turn.PhaseHook

interface Skill {
    val name: String
    val hooks: List<PhaseHook> get() = emptyList()
}