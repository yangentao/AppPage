@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.entao.app.page


import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

class PageContainer(val activity: PageActivity, private val frameLayout: FrameLayout) : LifecycleEventObserver {
    private val pageQueue: ArrayList<Page> = ArrayList()
    private val lifecycleOwnerActivity: LifecycleOwner = activity
    val pageCount: Int get() = pageQueue.size
    val topPage: Page? get() = pageQueue.lastOrNull()
    val bottomPage: Page? get() = pageQueue.firstOrNull()
    var animDuration: Long = 240
    private var lastAnimPage: WeakReference<Page>? = null

    init {
        lifecycleOwnerActivity.lifecycle.addObserver(this)
    }

    fun getPage(index: Int): Page {
        return pageQueue[index]
    }

    private fun addPage(page: Page, anim: Boolean) {
        if (lifecycleOwnerActivity.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            return
        }
        if (pageQueue.contains(page)) {
            error("Already Exist Page:" + page::class.qualifiedName)
        }
        lastAnimPage?.get()?.pageView?.animation?.cancel()
        val oldPage = topPage
        oldPage?.pageView?.animation?.cancel()
        pageQueue.add(page)

        if (page.pageView.layoutParams is FrameLayout.LayoutParams) {
            frameLayout.addView(page.pageView)
        } else {
            frameLayout.addView(page.pageView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        page.onAttach()
        val currState = lifecycleOwnerActivity.lifecycle.currentState
        page.moveToState(currState)

        if (anim && oldPage != null && currState.isAtLeast(LifeState.STARTED)) {
            oldPage.beginAnimPause()
            page.beginAnimPush()
            lastAnimPage = WeakReference(page)
        }
        oldPage?.moveToState(LifeState.CREATED)
    }

    private fun Page.beginAnimPause() {
        val a = this.animPause ?: return
        a.duration = animDuration
        this.pageView.beginAnimation(a)
    }

    private fun Page.beginAnimResume() {
        val a = this.animResume ?: return
        a.duration = animDuration
        this.pageView.beginAnimation(a)
    }

    private fun Page.beginAnimPush() {
        val a = this.animPush ?: return
        a.duration = animDuration
        this.pageView.beginAnimation(a)
    }

    private fun Page.beginAnimPop() {
        val a = this.animPop ?: return
        a.duration = animDuration
        this.pageView.beginAnimation(a)
    }

    private fun detachPage(page: Page) {
        page.moveToState(LifeState.DESTROYED)
        page.onDetach()
        pageQueue.remove(page)
        frameLayout.removeView(page.pageView)
    }


    private fun removePage(page: Page, anim: Boolean) {
        page.pageView.animation?.cancel()

        if (pageQueue.size <= 1 || this.topPage != page) {
            detachPage(page)
            return
        }

        //p is top page, need anim
        val newTopPage = pageQueue[pageQueue.size - 2]
        newTopPage.pageView.animation?.cancel()
        newTopPage.moveToState(lifecycleOwnerActivity.lifecycle.currentState)
        if (anim) {
            lastAnimPage = WeakReference(page)
            page.beginAnimPop()
            newTopPage.beginAnimResume()
        }
        detachPage(page)
    }


    fun pushPage(page: Page) {
        addPage(page, true)
    }

    fun pushPage(page: Page, anim: Boolean) {
        addPage(page, anim)
    }

    fun popPage(anim: Boolean) {
        val p = topPage ?: return
        finishPage(p, anim)

    }

    fun setContentPage(p: Page) {
        while (pageQueue.isNotEmpty()) {
            removePage(pageQueue.last(), false)
        }
        pushPage(p)
    }

    //只保留栈底
    fun popToBottom() {
        while (pageQueue.size > 1) {
            val p = topPage ?: return
            removePage(p, false)
        }
    }


    fun finishAll() {
        while (pageQueue.isNotEmpty()) {
            removePage(topPage!!, false)
        }
    }

    fun finishPage(p: Page, anim: Boolean) {
        removePage(p, anim)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
//        logd("Stack: Source=", source::class.simpleName + " State=", source.lifecycle.currentState, " Event=", event)
        if (source !== lifecycleOwnerActivity) {
            return
        }
        val pages = pageQueue.toList()
        when (event) {
            LifeEvent.ON_CREATE -> {
                pages.forEach {
                    it.handleLifecycleEvent(event)
                }
            }
            LifeEvent.ON_DESTROY -> {
                finishAll()
            }
            LifeEvent.ON_START -> {
                val tp = topPage
                pages.forEach { p ->
                    if (p == tp) {
                        p.handleLifecycleEvent(event)
                        p.pageView.visibility = View.VISIBLE
                    } else {
                        p.moveToState(LifeState.CREATED)
                        p.pageView.visibility = View.GONE
                    }
                }
            }
            LifeEvent.ON_STOP -> {
                val tp = topPage
                pages.forEach { p ->
                    if (p == tp) {
                        p.handleLifecycleEvent(event)
                    } else {
                        p.moveToState(LifeState.CREATED)
                    }
                    p.pageView.visibility = View.GONE
                }
            }
            LifeEvent.ON_RESUME, LifeEvent.ON_PAUSE -> {
                topPage?.handleLifecycleEvent(event)
            }
            else -> {
            }
        }

    }

    companion object {
        val rightInAnim: Animation
            get() = TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 1f, Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 0f,
            ).apply {
                this.fillBefore = true
            }
        val rightOutAnim: Animation
            get() = TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 1f,
                Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 0f,
            ).apply {
                this.fillAfter = true
            }

        val alphaInAnim: Animation
            get() = AlphaAnimation(0.5f, 1.0f).apply {
                this.fillBefore = true
            }
        val alphaOutAnim: Animation
            get() = AlphaAnimation(1f, 0.5f).apply {
                this.fillBefore = true
            }

        val noChangeAnim: Animation
            get() = AlphaAnimation(1f, 1f).apply {
                this.fillBefore = true
            }

    }


}


