package com.ncorti.kotlin.template.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private var server: HttpServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerView = PlayerView(this)
        setContentView(playerView)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        startLocalServer()
    }

    private fun startLocalServer() {
        server = HttpServer.create(InetSocketAddress(8080), 0).apply {
            createContext("/play") { exchange ->
                val query = exchange.requestURI.query ?: ""
                val streamUrl = query.substringAfter("url=").let { 
                    URLDecoder.decode(it, "UTF-8") 
                }
                runOnUiThread { playStream(streamUrl) }
                val response = "OK"
                exchange.sendResponseHeaders(200, response.length.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }

            createContext("/home") { exchange ->
                runOnUiThread { goToTvHome() }
                val response = "GOING_HOME"
                exchange.sendResponseHeaders(200, response.length.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            start()
        }
    }

    private fun playStream(url: String) {
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    private fun goToTvHome() {
        player.stop()
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop(0)
        player.release()
    }
}
