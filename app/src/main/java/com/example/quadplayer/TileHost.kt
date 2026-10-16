package com.example.quadplayer

/** What a tile needs from the screen that owns it. */
interface TileHost {
    /** The tile's swap button was pressed. */
    fun requestSwap(tile: VideoTile)

    /** The tile wants to loop its folder: find/ask for access to the folder holding its video. */
    fun requestFolder(tile: VideoTile)
}
