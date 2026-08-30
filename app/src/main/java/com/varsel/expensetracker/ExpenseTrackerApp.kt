package com.varsel.expensetracker

import android.app.Application
import android.content.Intent
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ExpenseTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PDFBox-Android resource loader for direct PDF text extraction
        try {
            PDFBoxResourceLoader.init(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // CRITICAL: Explicitly load the native SQLCipher library to prevent UnsatisfiedLinkError
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }

        // Intercepts remaining Java fatal crashes and displays them visually
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val intent = Intent(applicationContext, CrashActivity::class.java).apply {
                    putExtra("STACK_TRACE", throwable.stackTraceToString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                applicationContext.startActivity(intent)
                System.exit(2)
            } catch (e: Exception) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    
    }
}
