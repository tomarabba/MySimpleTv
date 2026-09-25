package com.ncorti.kotlin.template.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.URLDecoder
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private var isServerRunning = true

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
        thread {
            try {
                // Using raw Android ServerSocket instead of desktop HttpServer
                val serverSocket = ServerSocket(8080)
                while (isServerRunning) {
                    val client = serverSocket.accept()
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val requestLine = reader.readLine()
                    
                    if (requestLine != null && requestLine.startsWith("GET")) {
                        val path = requestLine.split(" ")[1]
                        
                        if (path.startsWith("/play?url=")) {
                            val encodedUrl = path.substringAfter("url=")
                            val streamUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                            runOnUiThread { playStream(streamUrl) }
                        } else if (path.startsWith("/home")) {
                            runOnUiThread { goToTvHome() }
                        }
                        
                        // Send HTTP success response
                        val response = "HTTP/1.1 200 OK\r\n\r\nOK\n"
                        client.getOutputStream().write(response.toByteArray())
                    }
                    client.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
        isServerRunning = false
        player.release()
    }
}
