package com.example.chitieuplus.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import com.example.chitieuplus.R
import com.example.chitieuplus.ui.main.MainActivity
import java.text.NumberFormat
import java.util.Locale

class BudgetAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val spent = intent.getLongExtra("spent", 0L)
        val limit = intent.getLongExtra("limit", 0L)

        val channelId = "budget_alert_channel"
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo channel cho Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cảnh báo ngân sách",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi chi tiêu vượt hạn mức"
            }
            manager.createNotificationChannel(channel)
        }

        // 🔗 Khi bấm vào thông báo -> nhảy thẳng tới BudgetFragment
        val pendingIntent = NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)     // activity chứa NavHostFragment
            .setGraph(R.navigation.nav_graph)               // nav_graph của bạn
            .setDestination(R.id.budgetFragment)            // đích: màn Quản lý ngân sách
            .createPendingIntent()

        val title = "Đã vượt ngân sách!"
        val message = "Đã chi ${formatVnd(spent)} / hạn mức ${formatVnd(limit)}"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(1001, notification)
    }

    private fun formatVnd(value: Long): String {
        val nf = NumberFormat.getInstance(Locale("vi", "VN"))
        return nf.format(value) + " ₫"
    }
}
