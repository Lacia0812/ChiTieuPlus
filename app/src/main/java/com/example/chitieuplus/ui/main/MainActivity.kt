package com.example.chitieuplus.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

        // ❌ Không dùng enableEdgeToEdge nữa
        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        // Toolbar + logo
        setSupportActionBar(vb.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // NavController từ NavHost (id = nav_host trong activity_main.xml)
        val navHost =
            supportFragmentManager.findFragmentById(vb.navHost.id) as NavHostFragment
        val navController = navHost.navController

        // Nút back trên toolbar + bottom nav
        setupActionBarWithNavController(navController)
        vb.bottomNav.setupWithNavController(navController)

        // Nếu cần thông báo thì xin quyền (không ảnh hưởng gì tới bàn phím)
        requestNotificationPermissionIfNeeded()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHost =
            supportFragmentManager.findFragmentById(vb.navHost.id) as NavHostFragment
        val navController = navHost.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

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
