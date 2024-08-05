package com.tcs.tools.managePdf.ui.home.rvFrags

sealed class RvState {
    object Idle:RvState()
    object AllFiles:RvState()
    object FavFiles:RvState()
}