package com.n0tez.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class N0tezApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Install custom crash handler for CI debugging
        setupCrashHandler()
        
        try {
            FirebaseApp.initializeApp(this)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            FirebaseCrashlytics.getInstance().setCustomKey("app_version", pInfo.versionName ?: "")
            val buildCode = try { pInfo.longVersionCode } catch (e: Throwable) { pInfo.versionCode.toLong() }
            FirebaseCrashlytics.getInstance().setCustomKey("app_build", buildCode)
        } catch (e: Throwable) {
            android.util.Log.e("N0tezApplication", "Firebase init error", e)
        }
    }
    
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                val crashReport = buildCrashReport(throwable, timestamp)
                
                // Write to file for CI capture
                val crashFile = File(filesDir, "crash_${timestamp}.txt")
                crashFile.writeText(crashReport)
                
                // Also log to Android log for logcat capture
                Log.e("N0tezCrash", "CRASH DETECTED: ${throwable.message}", throwable)
                Log.e("N0tezCrash", "Crash report saved to: ${crashFile.absolutePath}")
                
                // Write summary to logcat.txt location for CI
                val logcatFile = File(filesDir.parentFile?.parentFile, "logcat.txt")
                if (logcatFile.parentFile?.exists() == true) {
                    logcatFile.appendText("\n=== CRASH DETECTED ===\n$crashReport\n")
                }
                
            } catch (e: Exception) {
                Log.e("N0tezCrash", "Failed to handle crash", e)
            }
            
            // Call original handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun buildCrashReport(throwable: Throwable, timestamp: String): String {
        return buildString {
            appendLine("=== N0TEZ CRASH REPORT ===")
            appendLine("Timestamp: $timestamp")
            appendLine("App Version: ${packageName}")
            appendLine("Android Version: ${android.os.Build.VERSION.RELEASE}")
            appendLine("Device: ${android.os.Build.MODEL} (${android.os.Build.MANUFACTURER})")
            appendLine()
            appendLine("Exception: ${throwable.javaClass.simpleName}")
            appendLine("Message: ${throwable.message}")
            appendLine()
            appendLine("Stack Trace:")
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            appendLine(sw.toString())
            appendLine("=== END CRASH REPORT ===")
        }
    }
    
    companion object {
        private var instance: N0tezApplication? = null
        
        fun getContext(): Context {
            return instance?.applicationContext ?: throw IllegalStateException("Application not initialized")
        }
    }
}
