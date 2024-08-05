package com.tcs.tools.managePdf.ui.baseActivity

sealed class HomeStateIntent {
    object TriggerReload:HomeStateIntent()
    object TriggerIdle:HomeStateIntent()
}