@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package dev.entao.app.page


import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.widget.RelativeLayout
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

abstract class Page(val activity: PageActivity) : LifecycleOwner {
    var animPush: Animation? = PageContainer.rightInAnim
    var animPop: Animation? = PageContainer.rightOutAnim
    var animPause: Animation? = PageContainer.noChangeAnim
    var animResume: Animation? = PageContainer.noChangeAnim

    val context: Context get() = activity
    val pageId: Int = currentPageId_++
    val pageName: String by lazy { this::class.qualifiedName + "@$pageId" }

    val pageView: RelativeLayout = RelativeLayout(activity).apply {
        val resId = if (isNightMode(activity)) {
            android.R.color.background_dark
        } else {
            android.R.color.background_light
        }
        setBackgroundColor(ResourcesCompat.getColor(activity.resources, resId, activity.theme))
    }


    private val externalRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val selfRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val currentState: Lifecycle.State get() = this.selfRegistry.currentState


    val attached: Boolean get() = pageView.parent != null
    val isResumed: Boolean
        get() = currentState.isAtLeast(Lifecycle.State.RESUMED)

    val isStarted: Boolean
        get() = currentState.isAtLeast(Lifecycle.State.STARTED)

    val isCreated: Boolean
        get() = currentState.isAtLeast(Lifecycle.State.CREATED)

    val isDestroyed: Boolean
        get() = currentState == Lifecycle.State.DESTROYED


    val isTopPage: Boolean get() = activity.topPage === this
    val isBottomPage: Boolean get() = activity.bottomPage === this


    fun contentPush() {
        activity.setContentPage(this)
    }

    fun push() {
        activity.pushPage(this)
    }

    open fun finish(anim: Boolean) {
        activity.finishPage(this, anim)
    }


    fun pushPage(p: Page) {
        activity.pushPage(p)
    }

    fun <T : Page> pushPage(p: T, block: T.() -> Unit) {
        p.block()
        pushPage(p)
    }

    override fun getLifecycle(): Lifecycle {
        return externalRegistry
    }

    private val selfObserver: LifecycleEventObserver = LifecycleEventObserver { source, event ->
        onInternalStateChanged(source, event)
    }

    fun onAttach() {
        selfRegistry.addObserver(selfObserver)
    }

    fun onDetach() {
        selfRegistry.removeObserver(selfObserver)
    }


    fun handleLifecycleEvent(event: Lifecycle.Event) {
        selfRegistry.handleLifecycleEvent(event)
    }

    fun moveToState(state: Lifecycle.State) {
        selfRegistry.currentState = state
    }

    private fun onInternalStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (this !== source) {
            return
        }
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                onCreate()
            }
            Lifecycle.Event.ON_START -> {
                this.pageView.visibility = View.VISIBLE
                onStart()
            }
            Lifecycle.Event.ON_RESUME -> {
                onResume()
            }
            else -> {

            }
        }
        externalRegistry.handleLifecycleEvent(event)
        if (event == LifeEvent.ON_CREATE) {
            onPageCreated()
        }
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                onPause()
            }
            Lifecycle.Event.ON_STOP -> {
                onStop()
                this.pageView.visibility = View.GONE
            }
            Lifecycle.Event.ON_DESTROY -> {
                onDestroy()
            }
            else -> {

            }
        }
    }

    open fun onCreate() {
    }

    open fun onPageCreated() {}


    open fun onStart() {
    }


    open fun onResume() {
    }


    open fun onPause() {
    }


    open fun onStop() {
    }


    open fun onDestroy() {

    }

    open fun onBackPressed(): Boolean {
        return false
    }

    open fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    open fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (other is Page) {
            return this.pageId == other.pageId
        }
        return false
    }

    override fun hashCode(): Int {
        return this.pageId
    }

    companion object {
        private var currentPageId_: Int = 1
    }
}
