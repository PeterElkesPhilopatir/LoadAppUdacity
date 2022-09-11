package com.peter.loadappudacity

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.peter.loadappudacity.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var downloadID: Long = 0
    private var downloadStatus = "Fail"
    private lateinit var selectedDownloadUri: URL

    private val NOTIFICATION_ID = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        createNotificationChannel()

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        binding.radioDownloadGroup.setOnCheckedChangeListener { radioGroup, i ->
            selectedDownloadUri = when (i) {
                R.id.radio_retrofit -> URL.RETROFIT_URI
                R.id.radio_udacity -> URL.UDACITY_URI
                R.id.radio_glide -> URL.GLIDE_URI
                else -> URL.RETROFIT_URI
            }
        }

        binding.btnCustomLoading.setOnClickListener {
            if (this::selectedDownloadUri.isInitialized) {
                val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
                val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
                if (isConnected) {
                    Log.i("LoadAppDownload", "connected")
                    download()

//                    if (ContextCompat.checkSelfPermission(
//                            this,
//                            Manifest.permission.WRITE_EXTERNAL_STORAGE
//                        ) == PackageManager.PERMISSION_GRANTED
//                    ) {
//                        binding.customButton.buttonState = ButtonState.Loading
//                        download()
//                    } else {
//                        requestPermissions(
//                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
//                            PermissionInfo.PROTECTION_DANGEROUS
//                        )
//                    }
                } else {
                    Toast.makeText(this, getString(R.string.no_network), Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this, getString(R.string.select_option), Toast.LENGTH_SHORT)
                    .show()
            }

        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                downloadStatus = "Success"
                binding.btnCustomLoading.buttonState = ButtonState.Completed
                Log.i("LoadAppDownload", downloadStatus)
                createNotification()
            }
        }
    }

    private fun download() {
        binding.btnCustomLoading.buttonState = ButtonState.Loading
        val request =
            DownloadManager.Request(Uri.parse(selectedDownloadUri.uri))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "/repository.zip"
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.

        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadID))
        if (cursor.moveToFirst()) {
            when (cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) {
                DownloadManager.STATUS_FAILED -> {
                    downloadStatus = "Fail"
                    binding.btnCustomLoading.buttonState = ButtonState.Completed
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    downloadStatus = "Success"
                }
            }
        }
    }

    private fun createNotification() {
        notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager

        val detailIntent = Intent(this, DetailsActivity::class.java)
        detailIntent.putExtra("fileName", selectedDownloadUri.title)
        detailIntent.putExtra("status", downloadStatus)
        pendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(detailIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        } as PendingIntent
        action = NotificationCompat.Action(
            R.drawable.ic_launcher_foreground,
            getString(R.string.notification),
            pendingIntent
        )

        val contentIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(selectedDownloadUri.title)
            .setContentText(selectedDownloadUri.text)
            .setContentIntent(contentPendingIntent)
            .addAction(action)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                "LoadAppChannel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
            }
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.BLUE
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Download complete!"

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    companion object {
        private enum class URL(val uri: String, val title: String, val text: String) {
            GLIDE_URI(
                "https://github.com/bumptech/glide/archive/master.zip",
                "Glide: Image Loading Library By BumpTech",
                "Glide repository is downloaded"
            ),
            UDACITY_URI(
                "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip",
                "Udacity: Android Kotlin Nanodegree",
                "The Project 3 repository is downloaded"
            ),
            RETROFIT_URI(
                "https://github.com/square/retrofit/archive/master.zip",
                "Retrofit: Type-safe HTTP client by Square, Inc",
                "Retrofit repository is downloaded"
            ),
        }

        private const val CHANNEL_ID = "channelId"
    }
}