package com.tcs.tools.managePdf.ui.baseActivity



sealed class HomeState {
    object Idle:HomeState()
    object Reload:HomeState()
}