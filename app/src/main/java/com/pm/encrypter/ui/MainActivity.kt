package com.pm.encrypter.ui

import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pm.encrypter.R

class MainActivity : AppCompatActivity() {

    lateinit var bottomNav: BottomNavigationView

    lateinit var homeFragment: Home
    lateinit var folderFragment: EncryptFile

    var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById<CoordinatorLayout>(R.id.main)) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                // Bottom navigation padding
                view.setPadding(0, systemBars.top, 0, systemBars.bottom)

                WindowInsetsCompat.CONSUMED
            }

            val typedValue = TypedValue()
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurfaceContainer,
                typedValue,
                true
            )

            window.navigationBarColor = typedValue.data
        }

        bottomNav = findViewById(R.id.bottom_navigation)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->

            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            view.setPadding(
                0,
                0,
                0,
                navBar.bottom
            )

            insets
        }

        if (savedInstanceState == null) {

            // 🔥 Create only once
            homeFragment = Home()
            folderFragment = EncryptFile()

            supportFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment, folderFragment, "FOLDER")
                .hide(folderFragment)
                .commit()

            supportFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment, homeFragment, "HOME")
                .commit()

            activeFragment = homeFragment

        } else {
            // 🔥 Restore existing fragments
            homeFragment =
                supportFragmentManager.findFragmentByTag("HOME") as Home
            folderFragment =
                supportFragmentManager.findFragmentByTag("FOLDER") as EncryptFile

            // 🔥 find currently visible fragment
            activeFragment =
                supportFragmentManager.fragments.find { !it.isHidden } ?: homeFragment
        }

        // 🔥 restore selected tab
        bottomNav.selectedItemId =
            if (activeFragment == homeFragment) R.id.navigation_home
            else R.id.navigation_files



        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {

                R.id.navigation_home -> {
                    switchFragment(homeFragment)
                    true
                }

                R.id.navigation_files -> {
                    switchFragment(folderFragment)
                    true
                }

                else -> false
            }
        }

        onBackPressedDispatcher.addCallback(this) {

            if (activeFragment == folderFragment) {
                switchFragment(homeFragment)
                bottomNav.selectedItemId = R.id.navigation_home
            } else {
                finish()
            }
        }
    }


    fun switchFragment(fragment: Fragment) {

        if (fragment == activeFragment) return

        val transaction = supportFragmentManager.beginTransaction()

        activeFragment?.let { transaction.hide(it) }

        if (!fragment.isAdded) {
            transaction.add(R.id.nav_host_fragment, fragment)
        } else {
            transaction.show(fragment)
        }


        transaction.commit()

        activeFragment = fragment
    }


}