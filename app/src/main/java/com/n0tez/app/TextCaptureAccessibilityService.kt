package com.n0tez.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.n0tez.app.accessibility.ScreenTextCapture
import com.n0tez.app.accessibility.ScreenTextNode

class TextCaptureAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        lastObservedPackageName = event?.packageName?.toString()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    private fun captureText(targetBounds: Rect, excludedPackageName: String): String {
        val roots = interactiveWindowRoots(excludedPackageName)
        if (roots.isEmpty()) {
            return ""
        }

        val nodes = mutableListOf<ScreenTextNode>()
        roots.forEach { root ->
            collectNodes(root, nodes)
        }
        return ScreenTextCapture.extractText(nodes, targetBounds, excludedPackageName)
    }

    private fun interactiveWindowRoots(excludedPackageName: String): List<AccessibilityNodeInfo> {
        val interactiveRoots = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            windows.orEmpty()
                .asSequence()
                .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
                .mapNotNull { it.root }
                .filter { it.packageName?.toString() != excludedPackageName }
                .toList()
        } else {
            emptyList()
        }

        if (interactiveRoots.isNotEmpty()) {
            return interactiveRoots
        }

        val activeRoot = rootInActiveWindow ?: return emptyList()
        return if (activeRoot.packageName?.toString() == excludedPackageName) {
            emptyList()
        } else {
            listOf(activeRoot)
        }
    }

    private fun collectNodes(node: AccessibilityNodeInfo, destination: MutableList<ScreenTextNode>) {
        val text = buildList {
            node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
            node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
        }.distinct().joinToString(separator = "\n").trim()

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        destination += ScreenTextNode(
            text = text,
            bounds = bounds,
            packageName = node.packageName,
            isVisibleToUser = node.isVisibleToUser,
        )

        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                collectNodes(child, destination)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: TextCaptureAccessibilityService? = null

        @Volatile
        private var lastObservedPackageName: String? = null

        fun isEnabled(context: android.content.Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            val expected = "${context.packageName}/${TextCaptureAccessibilityService::class.java.name}"
            return enabled
                .split(':')
                .any { it.equals(expected, ignoreCase = true) }
        }

        fun captureTextInRegion(targetBounds: Rect, excludedPackageName: String): String {
            return instance?.captureText(targetBounds, excludedPackageName).orEmpty()
        }

        fun latestObservedPackageName(): String? = lastObservedPackageName
    }
}
