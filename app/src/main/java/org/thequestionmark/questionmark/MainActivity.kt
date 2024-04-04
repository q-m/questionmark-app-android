package org.thequestionmark.questionmark

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject
import java.net.URL


class MainActivity : ComponentActivity() {

    // Site entry-point. Only URLs on this domain are considered local for the app.
    private val siteUrl = URL("https://checker.thequestionmark.org/index-app?app=Questionmark")
    // Paths to consider local URLs, other paths are opened in an external web browser.
    // Can be overridden by the embedded website (TODO).
    private val localPaths = arrayOf(
        "/index-app", "/lookup", "/search", "/404", "/contact", "/over-ons",
        "/categories", "/categories/*", "/products/*"
    )

    private lateinit var returnUrlTemplate: String
    private lateinit var webview: WebView
    private lateinit var webviewClient: MyWebViewClient
    private lateinit var networkCallback: NetworkCallback

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prepare webview
        webview = WebView(this)
        webviewClient = MyWebViewClient(::shouldOverrideUrlLoading)
        webview.webViewClient = webviewClient
        // Setup debugging; See https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews for reference
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        webview.settings.javaScriptEnabled = true
        webviewClient.onLoadJavascript = onLoadJavascript()

        // Show one or the other
        if (isNetworkAvailable()) {
            webview.loadUrl(siteUrl.toString())
            setContentView(webview)
        } else {
            setContentView(R.layout.offline_layout)
        }
        registerNetworkListener()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check whether the key event is the Back button and if there's history.
        if (keyCode == KeyEvent.KEYCODE_BACK && webview.canGoBack()) {
            webview.goBack()
            return true
        }
        // If it isn't the Back button or there isn't web page history, bubble up to
        // the default system behavior. Probably exit the activity.
        return super.onKeyDown(keyCode, event)
    }

    @Suppress("DEPRECATION")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val ni = connectivityManager.activeNetworkInfo ?: return false
        return ni.isConnected
    }

    @Suppress("UNUSED_PARAMETER")
    private fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // open local URLs in the webview
        if (isLocal(request)) return false

        // handle app-specific links
        if (request?.url?.scheme == "app") {
            // initiate barcode scan
            if (request.url.host == "mobile-scan") {
                openScan(request.url.getQueryParameter("ret"))
            } else {
                // ignore unrecognized app action
                Log.w("MainActivity", "Unrecognized app action: " + request.url.toString())
            }
            return true
        }

        // open in external browser
        openExternal(request)
        return true

    }

    private fun isLocal(request: WebResourceRequest?): Boolean {
        // NOTE keep in sync with onLoadJavascript()
        // local is either host-relative, or on our own domain
        if (request?.url?.host != null && request.url.host != siteUrl.host) return false
        if (request?.url?.port != null && request.url.port != siteUrl.port) return false
        // local must also start with one of the whitelisted paths
        // path must be equal, or if path ends with '*' anything is allowed there
        val isLocalPath = localPaths.any { path ->
            if (path.endsWith('*')) {
                request?.url?.path?.startsWith(path.slice(1..<path.length))!!
            } else {
                request?.url?.path == path
            }
        }
        return isLocalPath
    }

    private fun onLoadJavascript(): String {
        // NOTE keep in sync with isLocal()
        return """
            // Handle click events on links, so that even internal links handled by Javascript
            // will be handled by the browser instead of the Javascript framework. This makes sure
            // the handler kicks in that opens external links in an external browser. 
            window.addEventListener("click", function (e) {
                if (e.target.tagName !== "A") return;
                var href = e.target.href;
                if (!href || href.startsWith("#") || href.startsWith("app:")) return;
                var url = new URL(href, window.location.href);
                if (url.protocol.replace(":", "") !== ${JSONObject.quote(siteUrl.protocol)}) return;
                if (url.host !== ${JSONObject.quote(siteUrl.host)}) return;
                if (url.port !== ${JSONObject.quote(if (siteUrl.port > 0) siteUrl.port.toString() else "")}) return;
                var isLocalPath = [${localPaths.joinToString(",") { e -> JSONObject.quote(e) }}].find(function (path) {
                    if (path.endsWith("*")) {
                        return url.pathname.startsWith(path.slice(0, -1));
                    } else {
                        return url.pathname === path;
                    }
                });
                if (isLocalPath) return;
                // non-local URLs must be handled by the browser, not within the Javascript app
                console.log('click link event that is not local, navigating browser to', href);
                e.preventDefault();
                window.location.assign(href);
            });
            console.log("installed click event listener for external links");
        """.trimIndent()
    }

    private fun openExternal(request: WebResourceRequest?) {
        Intent(Intent.ACTION_VIEW, request?.url).apply {
            startActivity(this)
        }
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract(), ::onScanResult)

    private fun openScan(returnUrlTemplate: String?) {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.EAN_8, ScanOptions.EAN_13, ScanOptions.UPC_A, ScanOptions.UPC_E)
        options.setPrompt(getString(R.string.scan_product_barcode))
        options.setOrientationLocked(false)
        options.setBeepEnabled(false)

        // we have no state in the intent to store this, so we do it in the app (its value is usually constant)
        if (returnUrlTemplate != null) {
            this.returnUrlTemplate = returnUrlTemplate
        }

        barcodeLauncher.launch(options)
    }

    private fun onScanResult(result: ScanIntentResult) {
        if (result.contents == null) {
            val originalIntent = result.originalIntent
            if (originalIntent == null) {
                Log.d("MainActivity", "Cancelled scan")
            } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                logAndToast(getString(R.string.scan_cancelled))
            }
        } else {
            // Be a bit safer and only keep numbers (XSS risk).
            val barcode = result.contents.replace(Regex("\\D"), "")
            // Get return url template
            if (returnUrlTemplate != null) {
                logAndToast(getString(R.string.scan_success, barcode))
                // Navigate webview (must be absolute URL)
                val returnUrl = returnUrlTemplate.replace("{CODE}", barcode)
                val absReturnUrl = URL(siteUrl, returnUrl).toString()
                Log.d("MainActivity", "Navigating to: $absReturnUrl")
                webview.loadUrl(absReturnUrl)
            } else {
                logAndToast(getString(R.string.scan_template_missing))
            }
        }
    }

    private fun logAndToast(msg: String) {
        Log.d("MainActivity", msg)
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun registerNetworkListener() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread { setOnline(true)  }
            }
            override fun onLost(network: Network) {
                runOnUiThread { setOnline(false) }
            }
        }
        // https://stackoverflow.com/a/58468010
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
    }

    private fun setOnline(online: Boolean) {
        if (online) {
            Log.d("MainActivity", "Network back, showing and reloading page")
            setContentView(webview)
            // TODO navigate to avoid reloading, and only reload when necessary
            if (webview.getUrl() == null) {
                webview.loadUrl(siteUrl.toString())
            } else {
                webview.reload()
            }
        } else {
            Log.d("MainActivity", "Network gone, showing offline layout")
            setContentView(R.layout.offline_layout)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (networkCallback != null) {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    private class MyWebViewClient : WebViewClient {

        var onLoadJavascript: String? = null  // Javascript code to execute on each loaded page
        var lastUrl: String? = null           // Last loaded URL
        private val callback: (view: WebView?, request: WebResourceRequest?) -> Boolean

        constructor(callback: (view: WebView?, request: WebResourceRequest?) -> Boolean) : super() {
            this.callback = callback
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return callback.invoke(view, request)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (view != null && onLoadJavascript != null) {
                view.evaluateJavascript(onLoadJavascript!!, null)
            }
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            lastUrl = url
        }
    }
}
