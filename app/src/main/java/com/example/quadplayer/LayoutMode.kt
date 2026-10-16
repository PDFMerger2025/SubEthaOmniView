package com.example.quadplayer

/** Every screen layout the app can show. Slot 1 is always the "big" video where there is one. */
enum class LayoutMode(val count: Int, val label: String) {
    SINGLE(1, "Full screen"),

    SIDE_BY_SIDE(2, "Vertical split"),
    STACKED(2, "Horizontal split"),

    BIG_LEFT(3, "Big left"),        // 50% left, two 25% stacked on the right
    BIG_TOP(3, "Big top"),          // 50% top, two 25% side by side below
    BIG_RIGHT(3, "Big right"),      // 50% right, two 25% stacked on the left
    COLUMNS_3(3, "3 vertical"),     // three equal columns

    GRID_2X2(4, "2 x 2 grid"),
    COLUMNS_4(4, "4 vertical");     // four equal columns

    companion object {
        fun forCount(n: Int): List<LayoutMode> = values().filter { it.count == n }

        fun parse(name: String?, count: Int): LayoutMode =
            values().firstOrNull { it.name == name && it.count == count } ?: forCount(count).first()
    }
}
