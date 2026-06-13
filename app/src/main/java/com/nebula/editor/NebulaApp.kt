package com.nebula.editor

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nebula.editor.util.CrashRecoveryManager
import com.nebula.editor.util.SecurityGuard
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class NebulaApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var crashRecoveryManager: CrashRecoveryManager
    @Inject lateinit var securityGuard: SecurityGuard

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Logging — only in debug builds
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Security checks (root, tamper, debugger)
        if (BuildConfig.ENABLE_TAMPER_CHECK) {
            securityGuard.runChecks(this)
        }

        // Register global crash handler for project auto-save
        crashRecoveryManager.install()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR
            )
            .build()

    companion object {
        lateinit var instance: NebulaApp
            private set

        fun context(): Context = instance.applicationContext
    }
}
