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
    lateinit var pageContainer: PageContainer
    lateinit var activityView: FrameLayout
        private set

    private var resultCallback: ((ActivityResult) -> Unit)? = null
    private var permCallback: ((Boolean) -> Unit)? = null
    private var permListCallback: ((Map<String, Boolean>) -> Unit)? = null

    private val permReq = this.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        val a = permCallback
        permCallback = null
        a?.invoke(it)
    }
    private val permReqList = this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val a = permListCallback
        permListCallback = null
        a?.invoke(it)
    }
    private val resultReq = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val a = this.resultCallback
        this.resultCallback = null
        a?.invoke(it)
    }

    fun requestPermission(perm: String, block: (Boolean) -> Unit) {
        this.permCallback = block
        this.permReq.launch(perm)
    }

    fun requestPermissionList(perms: Set<String>, block: (Map<String, Boolean>) -> Unit) {
        this.permListCallback = block
        this.permReqList.launch(perms.toTypedArray())
    }


    fun startActivityResult(intent: Intent, block: (ActivityResult) -> Unit) {
        resultCallback = block
        resultReq.launch(intent)
    }

    fun withPermission(perm: String, block: (Boolean) -> Unit) {
        if (this.hasPermission(perm)) {
            block(true)
            return
        }
        requestPermission(perm, block)
    }

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


    fun hasPermission(p: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, p)
        } else {
            true
        }
    }


}

