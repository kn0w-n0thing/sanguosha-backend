package org.dogcard.model.hero

/**
 * Marker interface for all hero skills.
 *
 * Concrete skill implementations (active, passive, locked, limited, etc.)
 * will extend this interface in a later iteration.
 */
interface Skill {
    val name: String
}