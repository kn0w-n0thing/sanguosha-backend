package org.dogcard.model.hero

/**
 * Role assigned in role-based game modes.
 *
 * - Identity mode uses all four roles.
 * - 1v1 mode uses [LORD] and [SPY] only.
 */
enum class Role {
    LORD,       // 主公
    LOYALIST,   // 忠臣
    REBEL,      // 反贼
    SPY,        // 内奸
}