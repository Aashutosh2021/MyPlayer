package com.myplayer.app.ui.screens.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myplayer.app.data.spotify.SpotifyIntegrationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

// Firefox Android UA bypasses Google's "disallowed_useragent" & broken WebView Passkey prompts
private const val USER_AGENT_GOOGLE_AUTH =
    "Mozilla/5.0 (Android 14; Mobile; rv:132.0) Gecko/132.0 Firefox/132.0"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SpotifyLoginDialog(
    spotifyManager: SpotifyIntegrationManager,
    onDismiss: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Web Login, 1 = Cookie Paste
    var isLoading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(SpotifyIntegrationManager.LOGIN_URL) }
    val tokenFetchStarted = remember { AtomicBoolean(false) }

    // Error state kept SEPARATE from statusText: previously any failure wrote into
    // statusText, which kept the full-screen loading overlay up forever with a
    // still-spinning loader and no dismiss button ("stuck on loading").
    var loginError by remember { mutableStateOf<String?>(null) }

    // Cookie pair of the last capture attempt, so the error card can offer "Try Again"
    var lastLoginAttempt by remember { mutableStateOf<Pair<String, String>?>(null) }

    var cookieInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    val spotifyGreen = Color(0xFF1DB954)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val isGooglePage = currentUrl.contains("accounts.google.") || currentUrl.contains("google.com/signin")

    /**
     * Single entry point for the completeLoginWithCookies chain (web auto-capture,
     * cookie paste, and error-card retry). The tokenFetchStarted atomic guards
     * against double launches from WebView double-firing. completeLoginWithCookies
     * now returns in <1s (heavy sync moved to a background scope), so we confirm
     * with a Toast and let the dialog close right away.
     */
    fun startLogin(spDc: String, spKey: String, loadingMessage: String) {
        if (!tokenFetchStarted.compareAndSet(false, true)) return
        loginError = null
        statusText = loadingMessage
        scope.launch {
            spotifyManager.completeLoginWithCookies(spDc, spKey)
                .onSuccess { username ->
                    Toast.makeText(
                        context,
                        "Spotify Connected ($username)! Syncing library in background...",
                        Toast.LENGTH_LONG
                    ).show()
                    onLoginSuccess(username)
                    onDismiss()
                }
                .onFailure { err ->
                    statusText = null
                    loginError = err.message ?: "Unknown login error"
                    Toast.makeText(
                        context,
                        "Login error: ${err.message ?: "Unknown"}",
                        Toast.LENGTH_SHORT
                    ).show()
                    tokenFetchStarted.set(false)
                }
        }
    }

    /**
     * Fast-path cookie check used by the 500ms poller AND every WebView callback:
     * reads both open.spotify.com and accounts.spotify.com cookie jars (after a
     * Google/email login sp_dc is sometimes only visible via the .spotify.com
     * wildcard on accounts.spotify.com) and fires login the instant the cookie
     * appears — without waiting for the 8 MB open.spotify.com page to finish.
     * Returns true once login is captured / in-flight / already failed.
     */
    fun checkCookiesAndCompleteLogin(): Boolean {
        if (tokenFetchStarted.get()) return true
        if (loginError != null) return true // past failure — only the manual "Try Again" button retries

        val cm = CookieManager.getInstance()
        val combinedCookies = buildString {
            cm.getCookie("https://open.spotify.com")?.let { append(it).append(";") }
            cm.getCookie("https://accounts.spotify.com")?.let { append(it) }
        }
        if (combinedCookies.isBlank()) return false

        val cookieMap = combinedCookies.split(";").mapNotNull { c ->
            val p = c.trim().split("=", limit = 2)
            if (p.size == 2 && p[0].trim().isNotEmpty()) p[0].trim() to p[1].trim() else null
        }.toMap()

        val spDc = cookieMap["sp_dc"]
        if (spDc.isNullOrBlank()) return false

        webViewRef?.stopLoading()
        lastLoginAttempt = spDc to (cookieMap["sp_key"] ?: "")
        startLogin(spDc, cookieMap["sp_key"] ?: "", "Connecting to Spotify...")
        return true
    }

    // 500ms cookie poller: completes login ~0.5s after a successful Google/email
    // sign-in instead of waiting for the heavy 8 MB page's on-page-finished.
    LaunchedEffect(Unit) {
        while (!tokenFetchStarted.get()) {
            delay(500L)
            CookieManager.getInstance().flush()
            if (checkCookiesAndCompleteLogin()) break
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(surfaceColor)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedTab == 0 && (isGooglePage || webViewRef?.canGoBack() == true)) {
                        IconButton(
                            onClick = {
                                if (isGooglePage) {
                                    webViewRef?.loadUrl(SpotifyIntegrationManager.LOGIN_URL)
                                } else {
                                    webViewRef?.goBack()
                                }
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = spotifyGreen
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = "Connect Spotify",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = onSurfaceColor
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = onSurfaceColor)
                }
            }

            // Mode Tabs: Web Sign-In vs Cookie Paste
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Web Sign-In", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = spotifyGreen,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Paste Cookie", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = spotifyGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }

            if ((isLoading && selectedTab == 0) || statusText != null) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = spotifyGreen
                )
            }

            if (selectedTab == 0) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.removeAllCookies(null)
                            cookieManager.flush()

                            WebView(ctx).apply {
                                webViewRef = this
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                // Ensure keyboard pops up inside Compose Dialog for Google email/password fields
                                isFocusable = true
                                isFocusableInTouchMode = true
                                requestFocus()

                                cookieManager.setAcceptThirdPartyCookies(this, true)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                                // Enable popup interception so "Continue with Google" window.open() works!
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.setSupportMultipleWindows(true)
                                settings.userAgentString = SpotifyIntegrationManager.USER_AGENT_DESKTOP

                                fun updateUaForUrl(view: WebView?, url: String?) {
                                    if (view == null || url == null) return
                                    val isGoogleUrl = url.contains("accounts.google.") ||
                                        url.contains("google.com/o/oauth") ||
                                        url.contains("google.com/signin")

                                    val targetUa = if (isGoogleUrl) {
                                        USER_AGENT_GOOGLE_AUTH
                                    } else {
                                        SpotifyIntegrationManager.USER_AGENT_DESKTOP
                                    }
                                    if (view.settings.userAgentString != targetUa) {
                                        view.settings.userAgentString = targetUa
                                    }
                                }

                                // Cookie auto-capture is handled by the composable-level
                                // checkCookiesAndCompleteLogin() (polled every 500ms + called
                                // from every WebView callback) — no factory-local copy needed.

                                // 1. Intercept window.open() popups from "Continue with Google" button
                                webChromeClient = object : WebChromeClient() {
                                    override fun onCreateWindow(
                                        view: WebView?,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: Message?
                                    ): Boolean {
                                        val mainWebView = view ?: return false
                                        val popupWebView = WebView(mainWebView.context).apply {
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.userAgentString = USER_AGENT_GOOGLE_AUTH
                                            webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(
                                                    v: WebView?,
                                                    request: WebResourceRequest?
                                                ): Boolean {
                                                    val popupUrl = request?.url?.toString() ?: return false
                                                    updateUaForUrl(mainWebView, popupUrl)
                                                    mainWebView.loadUrl(popupUrl)
                                                    v?.destroy()
                                                    return true
                                                }

                                                override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                                                    if (!url.isNullOrBlank() && url != "about:blank") {
                                                        updateUaForUrl(mainWebView, url)
                                                        mainWebView.loadUrl(url)
                                                        v?.destroy()
                                                    }
                                                }
                                            }
                                        }
                                        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                        transport.webView = popupWebView
                                        resultMsg.sendToTarget()
                                        return true
                                    }
                                }

                                // 2. Handle navigation, Google UA switching, intent:// URLs, and CSS fix
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                        url?.let { currentUrl = it }
                                        updateUaForUrl(view, url)
                                        // Early instant capture: sp_dc may already be in the jar
                                        // mid-redirect (e.g. right after the Google OAuth hop).
                                        checkCookiesAndCompleteLogin()

                                        // Disable broken WebView Passkey prompt on Google Sign-In so email/password works
                                        if (url?.contains("google.") == true) {
                                            view?.evaluateJavascript(
                                                """
                                                (function() {
                                                  try {
                                                    delete window.PublicKeyCredential;
                                                    if (navigator.credentials) {
                                                      navigator.credentials.get = function() {
                                                        return Promise.reject(new Error('WebAuthn disabled in WebView'));
                                                      };
                                                    }
                                                  } catch(e) {}
                                                })();
                                                """.trimIndent(),
                                                null
                                            )
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        url?.let { currentUrl = it }
                                        CookieManager.getInstance().flush()

                                        // Apply Meld's CSS layout fix on Spotify pages
                                        if (url?.contains("spotify.com") == true) {
                                            view?.evaluateJavascript(
                                                """
                                                (function() {
                                                  var id = 'meld-login-layout-fix';
                                                  if (!document.getElementById(id)) {
                                                    var st = document.createElement('style');
                                                    st.id = id;
                                                    st.textContent =
                                                      'html, body { height: auto !important; min-height: 100% !important; overflow: visible !important; }' +
                                                      'body > div { height: auto !important; min-height: 100% !important; }' +
                                                      'main { position: static !important; height: auto !important; min-height: 100dvh !important; max-height: none !important; overflow: visible !important; }';
                                                    document.head.appendChild(st);
                                                  }
                                                })();
                                                """.trimIndent(),
                                                null
                                            )
                                        }

                                        // Unconditional: self-guards + polls BOTH spotify
                                        // domains (open + accounts) via .spotify.com wildcard.
                                        checkCookiesAndCompleteLogin()
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val reqUrl = request?.url?.toString() ?: return false
                                        currentUrl = reqUrl
                                        // Catch sp_dc on intermediate redirects too (poller's
                                        // check is guarded, so multiple callers are safe).
                                        checkCookiesAndCompleteLogin()

                                        // Handle intent:// or custom scheme links from Google/Spotify OAuth
                                        if (!reqUrl.startsWith("http://") && !reqUrl.startsWith("https://")) {
                                            if (reqUrl.startsWith("intent://")) {
                                                try {
                                                    val intent = Intent.parseUri(reqUrl, Intent.URI_INTENT_SCHEME)
                                                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                                    if (!fallbackUrl.isNullOrBlank()) {
                                                        updateUaForUrl(view, fallbackUrl)
                                                        view?.loadUrl(fallbackUrl)
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                            return true
                                        }

                                        // After successful Google/email login Spotify can land the
                                        // WebView on accounts.spotify.com/en/status — the old
                                        // open.spotify.com-only check never fired there and the
                                        // page sat stuck. Jump straight to the web player instead.
                                        if (reqUrl.contains("accounts.spotify.com") && reqUrl.contains("/status")) {
                                            updateUaForUrl(view, "https://open.spotify.com/")
                                            view?.loadUrl("https://open.spotify.com/")
                                            return true
                                        }

                                        // If switching between Spotify and Google OAuth, swap User-Agent before loading!
                                        val isGoogle = reqUrl.contains("accounts.google.") || reqUrl.contains("google.com/o/oauth")
                                        val expectedUa = if (isGoogle) USER_AGENT_GOOGLE_AUTH else SpotifyIntegrationManager.USER_AGENT_DESKTOP
                                        if (view != null && view.settings.userAgentString != expectedUa) {
                                            view.settings.userAgentString = expectedUa
                                            view.loadUrl(reqUrl)
                                            return true
                                        }

                                        // Final navigation check (guarded — no double launch)
                                        checkCookiesAndCompleteLogin()
                                        return false
                                    }
                                }

                                loadUrl(SpotifyIntegrationManager.LOGIN_URL)
                            }
                        }
                    )

                    if (statusText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(surfaceColor.copy(alpha = 0.94f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = spotifyGreen)
                                Spacer(Modifier.height(16.dp))
                                Text(statusText!!, color = onSurfaceColor, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Failure card: replaces the old behaviour of leaving the
                    // forever-spinning full-screen overlay up with no way out.
                    // Floats at the bottom so the top bar (close) stays reachable.
                    loginError?.let { errMsg ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Login failed: $errMsg",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Tip: agar repeat hota hai toh 'Paste Cookie' tab se sp_dc cookie try karo.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            lastLoginAttempt?.let { (dc, key) ->
                                                startLogin(dc, key, "Syncing your Spotify taste profile...")
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("Try Again", color = spotifyGreen) }
                                    TextButton(
                                        onClick = { loginError = null },
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("Close", color = onSurfaceColor) }
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Paste sp_dc Cookie Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "How to get your sp_dc cookie:",
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceColor,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "1. Open your browser and log in to open.spotify.com (via Google, Apple, or Facebook).\n" +
                                "2. Inspect / Developer Tools > Application/Storage > Cookies > open.spotify.com.\n" +
                                "3. Copy the value of 'sp_dc' and paste it below.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = cookieInput,
                        onValueChange = { cookieInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Spotify sp_dc Cookie") },
                        placeholder = { Text("AQB... (long token)") },
                        singleLine = false,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = spotifyGreen,
                            cursorColor = spotifyGreen
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { text ->
                                    cookieInput = text.trim()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Paste from Clipboard")
                        }

                        Button(
                            onClick = {
                                val cleanCookie = cookieInput.trim()
                                if (cleanCookie.isNotBlank()) {
                                    lastLoginAttempt = cleanCookie to ""
                                    startLogin(cleanCookie, "", "Validating cookie & training AI Brain...")
                                }
                            },
                            enabled = cookieInput.isNotBlank() && statusText == null,
                            colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Connect", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (statusText != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            CircularProgressIndicator(color = spotifyGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(statusText!!, fontSize = 12.sp, color = onSurfaceColor)
                        }
                    }

                    // Cookie-paste failure card with Retry (button re-enables as well
                    // because statusText resets to null on failure now).
                    loginError?.let { errMsg ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Cookie validation failed: $errMsg",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        startLogin(cookieInput.trim(), "", "Validating cookie & training AI Brain...")
                                    }
                                ) { Text("Retry", fontSize = 12.sp) }
                                TextButton(
                                    onClick = { loginError = null }
                                ) { Text("Close", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}
