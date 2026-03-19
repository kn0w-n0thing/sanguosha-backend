package org.dogcard.model.card

enum class CardType {

    // ── Basic Cards (基本牌) ──────────────────────────────────────────────
    ATTACK,
    FIRE_ATTACK,
    THUNDER_ATTACK,
    DODGE,
    PEACH,
    WINE,

    // ── Instant Trick Cards (即时锦囊) ────────────────────────────────────
    DUEL,
    SOMETHING_FROM_NOTHING,
    DISMANTLE,
    STEAL,
    BORROW_SWORD,
    NEGATE,
    NEGATE_KINGDOM,
    CHAIN,
    FIRE_RAID,
    WAIT_AT_EASE,
    KNOW_THYSELF,
    ALLY_FAR_ATTACK_NEAR,
    HAIL_OF_ARROWS,
    BARBARIAN_INVASION,
    PEACH_GARDEN_OATH,
    BOUNTIFUL_HARVEST,

    // ── Delayed Trick Cards (延时锦囊) ────────────────────────────────────
    ECSTASY,
    SUPPRESSION,
    LIGHTNING,

    // ── Equipment — Weapons (武器) ────────────────────────────────────────
    ZHUGE_CROSSBOW,
    VERMILION_BIRD_FAN,
    BOULDER_AXE,
    WU_SIX_SWORD,
    THREE_POINTED_BLADE,
    QILIN_BOW,
    GENDER_SWORDS,
    ICE_SWORD,
    QINGGANG_SWORD,
    SNAKE_SPEAR,
    GREEN_DRAGON_CRESCENT_BLADE,
    HALBERD,

    // ── Equipment — Armor (防具) ──────────────────────────────────────────
    RATTAN_ARMOR,
    BENEVOLENT_SHIELD,
    SILVER_LION,
    EIGHT_TRIGRAMS,

    // ── Equipment — Defensive Horses (防御马) ─────────────────────────────
    CHITU,
    ZIXING,
    DAYUAN,

    // ── Equipment — Offensive Horses (进攻马) ─────────────────────────────
    DILU,
    JUEYING,
    ZHUHUANGFEIDIAN;

    companion object {
        val chineseNames: Map<CardType, String> = mapOf(
            ATTACK                       to "杀",
            FIRE_ATTACK                  to "火杀",
            THUNDER_ATTACK               to "雷杀",
            DODGE                        to "闪",
            PEACH                        to "桃",
            WINE                         to "酒",
            DUEL                         to "决斗",
            SOMETHING_FROM_NOTHING       to "无中生有",
            DISMANTLE                    to "过河拆桥",
            STEAL                        to "顺手牵羊",
            BORROW_SWORD                 to "借刀杀人",
            NEGATE                       to "无懈可击",
            NEGATE_KINGDOM               to "无懈可击·国",
            CHAIN                        to "铁索连环",
            FIRE_RAID                    to "火攻",
            WAIT_AT_EASE                 to "以逸待劳",
            KNOW_THYSELF                 to "知己知彼",
            ALLY_FAR_ATTACK_NEAR         to "远交近攻",
            HAIL_OF_ARROWS               to "万箭齐发",
            BARBARIAN_INVASION           to "南蛮入侵",
            PEACH_GARDEN_OATH            to "桃园结义",
            BOUNTIFUL_HARVEST            to "五谷丰登",
            ECSTASY                      to "乐不思蜀",
            SUPPRESSION                  to "兵粮寸断",
            LIGHTNING                    to "闪电",
            ZHUGE_CROSSBOW               to "诸葛连弩",
            VERMILION_BIRD_FAN           to "朱雀羽扇",
            BOULDER_AXE                  to "贯石斧",
            WU_SIX_SWORD                 to "吴六剑",
            THREE_POINTED_BLADE          to "三尖两刃刀",
            QILIN_BOW                    to "麒麟弓",
            GENDER_SWORDS                to "雌雄双股剑",
            ICE_SWORD                    to "寒冰剑",
            QINGGANG_SWORD               to "青釭剑",
            SNAKE_SPEAR                  to "丈八蛇矛",
            GREEN_DRAGON_CRESCENT_BLADE  to "青龙偃月刀",
            HALBERD                      to "方天画戟",
            RATTAN_ARMOR                 to "藤甲",
            BENEVOLENT_SHIELD            to "仁王盾",
            SILVER_LION                  to "白银狮子",
            EIGHT_TRIGRAMS               to "八卦阵",
            CHITU                        to "赤兔",
            ZIXING                       to "紫骍",
            DAYUAN                       to "大宛",
            DILU                         to "的卢",
            JUEYING                      to "绝影",
            ZHUHUANGFEIDIAN              to "爪黄飞电",
        )
    }
}