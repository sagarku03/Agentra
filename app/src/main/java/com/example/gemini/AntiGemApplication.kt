package com.example.gemini

import android.app.Application
import android.os.Build
import com.example.gemini.data.adb.AdbConnectManager
import com.example.gemini.data.updater.PluginUpdateCheckWorker
import com.example.gemini.data.updater.UpdateCheckWorker
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class AntiGemApplication : Application(), coil.ImageLoaderFactory {
    /**
     * Images that live on the bridge (a server's files, attachments) need the security header, which Coil's
     * default client does not send. Only requests to the bridge address get it.
     */
    override fun newImageLoader(): coil.ImageLoader {
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val bridge = com.example.gemini.data.preferences.AuthPreferences.currentBridgeHttpUrl.toHttpUrlOrNull()
                val sameServer = bridge != null && req.url.host == bridge.host && req.url.port == bridge.port
                chain.proceed(
                    if (sameServer) req.newBuilder()
                        .header(com.example.gemini.data.preferences.AuthPreferences.currentFramedHeader, "true")
                        .apply { com.example.gemini.data.preferences.AuthPreferences.edgeHeader()?.let { (n, v) -> header(n, v) } }
                        .build()
                    else req
                )
            }
            .build()
        return coil.ImageLoader.Builder(this).okHttpClient(client).build()
    }

    override fun onCreate() {
        super.onCreate()
        // loads the saved server addresses, Remote mode and the SSH key into memory before anything connects
        com.example.gemini.data.preferences.AuthPreferences(this)
        // the chosen light / dark themes and the custom ones, before the first screen draws
        com.example.gemini.theme.ThemeManager.init(this)
        UpdateCheckWorker.schedule(this)
        PluginUpdateCheckWorker.schedule(this)
        // Reconnects adb when Wireless debugging turns back on (it gets a new port every time)
        AdbConnectManager.init(this)
        com.example.gemini.data.ssh.SshSessionFactory.init(this)
        // Connect to PC: the paired PCs and the one in use (AuthPreferences reads the latter on its own too)
        com.example.gemini.data.pc.PcConnect.init(this)
        // the cloud project new Antigravity chats start in (Remote Agents), before the first chat is created
        com.example.gemini.data.agent.agy.AgyCloud.init(this)
    }
}
