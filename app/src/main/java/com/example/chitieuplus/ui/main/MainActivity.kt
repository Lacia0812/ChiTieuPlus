package com.example.chitieuplus.ui.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.chitieuplus.R
import com.example.chitieuplus.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var vb: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        setSupportActionBar(vb.toolbar)
        // Ẩn title, chỉ hiển thị logo trên toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController
        setupActionBarWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        return navHost.navController.navigateUp() || super.onSupportNavigateUp()
    }
}
