package org.dogcard.model.seat

import org.dogcard.model.hero.Role

sealed class Allegiance {
    data object Unknown : Allegiance()
    data class RoleBased(val role: Role) : Allegiance()
}