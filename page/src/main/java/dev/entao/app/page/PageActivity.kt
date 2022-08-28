@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.entao.app.page


import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Window
import android.widget.FrameLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


open class PageActivity : AppCompatActivity() {
    private val permReq = this.registerForActivityResult(ActivityResultContracts.RequestPermission(), ::onPermReq)
    private val permReqs = this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), ::onPermReqs)
    private val startActivityR = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::onStartResult)
    private var resultBlock: ((ActivityResult) -> Unit)? = null
    private var permBlock: ((Boolean) -> Unit)? = null
    private var permsBlock: ((Map<String, Boolean>) -> Unit)? = null


    lateinit var pageContainer: PageContainer
    lateinit var activityView: FrameLayout
        private set

    open fun getInitPage(): Page? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        activityView = FrameLayout(this).apply {
            fitsSystemWindows = true
        }
        setContentView(activityView)
        pageContainer = PageContainer(this, activityView)
        val p = getInitPage()
        if (p != null) {
            pageContainer.pushPage(p)
        }
    }

    fun setContentPage(p: Page) {
        pageContainer.setContentPage(p)
    }

    fun <T : Page> setContentPage(p: T, block: T.() -> Unit) {
        p.block()
        pageContainer.setContentPage(p)
    }

    fun pushPage(p: Page) {
        pageContainer.pushPage(p)
    }

    fun <T : Page> pushPage(p: T, block: T.() -> Unit) {
        p.block()
        pageContainer.pushPage(p)
    }

    fun finishPage(p: Page, anim: Boolean) {
        pageContainer.finishPage(p, anim)
    }

    fun isTopPage(p: Page): Boolean {
        return pageContainer.topPage === p
    }

    fun isBottomPage(p: Page): Boolean {
        return pageContainer.bottomPage === p
    }

    val topPage: Page? get() = pageContainer.topPage
    val bottomPage: Page? get() = pageContainer.bottomPage

    override fun onBackPressed() {
        if (pageContainer.topPage?.onBackPressed() == true) {
            return
        }
        if (pageContainer.pageCount > 1) {
            pageContainer.popPage(true)
            return
        }
        if (allowFinish()) {
            super.onBackPressed()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (pageContainer.topPage?.onKeyDown(keyCode, event) == true) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (pageContainer.topPage?.onKeyUp(keyCode, event) == true) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }


    protected open fun allowFinish(): Boolean {
        return true
    }



    private fun onPermReq(ok: Boolean) {
        val a = permBlock
        permBlock = null
        a?.invoke(ok)
    }

    private fun onPermReqs(map: Map<String, Boolean>) {
        val a = permsBlock
        permsBlock = null
        a?.invoke(map)
    }

    private fun onStartResult(result: ActivityResult) {
        val a = this.resultBlock
        this.resultBlock = null
        a?.invoke(result)
    }

    fun hasPerm(p: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, p)
        } else {
            true
        }
    }
    fun withPermission(perm: String, block: (Boolean) -> Unit) {
        if (this.hasPerm(perm)) {
            block(true)
            return
        }
        requestPermission(perm, block)
    }

    fun requestPermission(perm: String, block: (Boolean) -> Unit) {
        this.permBlock = block
        this.permReq.launch(perm)
    }

    fun requestPermissions(perms: Set<String>, block: (Map<String, Boolean>) -> Unit) {
        this.permsBlock = block
        this.permReqs.launch(perms.toTypedArray())
    }


    fun startActivityResult(intent: Intent, block: (ActivityResult) -> Unit) {
        resultBlock = block
        startActivityR.launch(intent)
    }

}

