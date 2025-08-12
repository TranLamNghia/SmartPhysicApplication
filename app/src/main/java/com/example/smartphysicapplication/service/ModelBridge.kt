package com.example.smartphysicapplication.service

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView

class ModelBridge(private val webView: WebView) {

    // ====== JS -> Android (được gọi từ viewer.html) ======
    @JavascriptInterface
    fun onModelLoaded(src: String) {
        Log.d("ModelBridge", "Model loaded: $src")
    }

    @JavascriptInterface
    fun onProgress(percent: Int) {
        Log.d("ModelBridge", "Loading: $percent%")
    }

    @JavascriptInterface
    fun onToggleAutoRotate(enabled: Boolean) {
        Log.d("ModelBridge", "Auto-rotate: $enabled")
    }

    // ====== Android -> JS (gọi sang viewer.html) ======
    fun setModelSrc(path: String) {
        webView.evaluateJavascript("window.setModelSrc(${path.js()});", null)
    }

    fun setAutoRotate(on: Boolean) {
        webView.evaluateJavascript("window.setAutoRotate(${on});", null)
    }

    // Helper: escape chuỗi JS
    private fun String.js(): String = "\"" + this.replace("\"", "\\\"") + "\""
}
