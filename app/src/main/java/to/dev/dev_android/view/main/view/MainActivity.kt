package to.dev.dev_android.view.main.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebView
import to.dev.dev_android.R
import com.pusher.pushnotifications.PushNotifications
import to.dev.dev_android.base.BuildConfig
import to.dev.dev_android.base.activity.BaseActivity
import to.dev.dev_android.databinding.ActivityMainBinding
import to.dev.dev_android.util.AndroidWebViewBridge

class MainActivity : BaseActivity<ActivityMainBinding>(), CustomWebChromeClient.CustomListener {
    private val webViewBridge: AndroidWebViewBridge = AndroidWebViewBridge(this)

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun layout(): Int {
        return R.layout.activity_main
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWebViewSettings()
        savedInstanceState?.let { restoreState(it) } ?: navigateToHome()
        handleIntent(intent)
        PushNotifications.start(getApplicationContext(), BuildConfig.pusherInstanceId);
        PushNotifications.addDeviceInterest("broadcast");
    }

    override fun onResume() {
        if (intent.extras != null && intent.extras["url"] != null) {
            binding.webView.loadUrl(intent.extras["url"].toString())
        }
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val appLinkData: Uri? = intent.data
        if (appLinkData != null) {
            binding.webView.loadUrl(appLinkData.toString())
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setWebViewSettings() {
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.userAgentString = BuildConfig.userAgent

        binding.webView.addJavascriptInterface(webViewBridge, "AndroidBridge")
        val webViewClient = CustomWebViewClient(this@MainActivity, binding.webView) {
            binding.splash.visibility = View.GONE
        }
        binding.webView.webViewClient = webViewClient
        webViewBridge.webViewClient = webViewClient
        binding.webView.webChromeClient = CustomWebChromeClient(BuildConfig.baseUrl, this)
    }

    private fun restoreState(savedInstanceState: Bundle) {
        binding.webView.restoreState(savedInstanceState)
    }

    private fun navigateToHome() {
        binding.webView.loadUrl(BuildConfig.baseUrl)
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun launchGallery(filePathCallback: ValueCallback<Array<Uri>>?) {
        this.filePathCallback = filePathCallback

        val galleryIntent = Intent().apply {
            // Show only images, no videos or anything else
            type = "image/*"
            action = Intent.ACTION_PICK
        }

        // Always show the chooser (if there are multiple options available)
        startActivityForResult(
            Intent.createChooser(galleryIntent, "Select Picture"),
            PIC_CHOOSER_REQUEST,
            null    // No additional data
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != PIC_CHOOSER_REQUEST) {
            return super.onActivityResult(requestCode, resultCode, data)
        }

        when (resultCode) {
            Activity.RESULT_OK -> data?.data?.let {
                filePathCallback?.onReceiveValue(arrayOf(it))
                filePathCallback = null
            }
            Activity.RESULT_CANCELED -> {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            }
        }
    }

    companion object {
        private const val PIC_CHOOSER_REQUEST = 100
    }
}
