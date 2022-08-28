package dev.entao.app.page

import android.content.Context
import android.os.Build
import android.view.View
import android.view.animation.Animation
import androidx.lifecycle.Lifecycle


typealias LifeState = Lifecycle.State
typealias LifeEvent = Lifecycle.Event

internal fun View.beginAnimation(a: Animation?) {
    this.animation?.cancel()
    if (a != null) {
        this.startAnimation(a)
    }
}

internal fun isNightMode(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.resources.configuration.isNightModeActive
    } else {
        false
    }
}