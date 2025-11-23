package com.example.chitieuplus.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.chitieuplus.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var vb: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        // Gắn Toolbar làm ActionBar
        setSupportActionBar(vb.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // dùng logo / custom title

        // Lấy NavController từ NavHostFragment
        val navHost =
            supportFragmentManager.findFragmentById(vb.navHost.id) as NavHostFragment
        val navController = navHost.navController

        // Để nút back trên toolbar hoạt động với Navigation
        setupActionBarWithNavController(navController)

        // 🔥 Gắn BottomNavigationView với NavController
        vb.bottomNav.setupWithNavController(navController)

        // Tuỳ chọn: xin quyền thông báo nếu sau này bạn dùng Notification (có thể bỏ nếu không dùng)
        requestNotificationPermissionIfNeeded()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHost =
            supportFragmentManager.findFragmentById(vb.navHost.id) as NavHostFragment
        val navController = navHost.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // ----- OPTIONAL: xin quyền POST_NOTIFICATIONS trên Android 13+ -----
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }
}
