package com.example.keepalivemyapps

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.keepalivemyapps.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "Activity created")

        loadSavedPackage()
        requestPermissions()
        setupClickListeners()
    }

    private fun loadSavedPackage() {
        val savedPackage = Config.getTargetPackage(this)
        if (savedPackage.isNotEmpty() && savedPackage != "com.example.unwantedcallblocker") {
            binding.etPackageName.setText(savedPackage)
        }
    }

    private fun requestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        // READ_PHONE_STATE permission for call detection
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }

        // Notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // MANAGE_OWN_CALLS permission for call monitoring (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.MANAGE_OWN_CALLS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.MANAGE_OWN_CALLS)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: $permissionsNeeded")
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            Log.d(TAG, "All permissions granted")
        }
    }

    private fun setupClickListeners() {
        Log.d(TAG, "Setting up click listeners")

        binding.btnStartService.setOnClickListener {
            Log.d(TAG, "Start Service button clicked")
            val packageName = binding.etPackageName.text.toString().trim()

            if (TextUtils.isEmpty(packageName)) {
                Toast.makeText(this, "Please enter a package name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save the package name
            Config.setTargetPackage(this, packageName)

            startService(packageName)
        }

        binding.btnStopService.setOnClickListener {
            Log.d(TAG, "Stop Service button clicked")
            stopService()
        }
    }

    private fun stopService() {
        IncomingCallService.stopService(this)
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show()
        binding.tvStatus.text = "Service stopped"
    }

    private fun startService(packageName: String) {
        Log.d(TAG, "Starting service for package: $packageName")

        // First check if we have necessary permissions
        if (!hasRequiredPermissions()) {
            showPermissionAlert()
            return
        }

        // Validate package exists
        if (!isPackageInstalled(packageName)) {
            AlertDialog.Builder(this)
                .setTitle("App Not Found")
                .setMessage("Package '$packageName' not installed.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Check for exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("This app needs exact alarm permission to work reliably.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("Skip") { _, _ ->
                        actuallyStartService(packageName)
                    }
                    .setCancelable(false)
                    .show()
                return
            }
        }

        actuallyStartService(packageName)
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val requiredPermissions = mutableListOf<String>()

        requiredPermissions.add(Manifest.permission.READ_PHONE_STATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.MANAGE_OWN_CALLS)
        }

        for (permission in requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }

        return true
    }

    private fun showPermissionAlert() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs phone and notification permissions to detect calls and work properly.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun actuallyStartService(packageName: String) {
        Log.d(TAG, "Actually starting service for $packageName")

        try {
            // Start the foreground service
            IncomingCallService.startService(this)

            // Start the worker for periodic monitoring
            SimpleWorker.schedule(this)

            Toast.makeText(this, "Service started for $packageName", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Service started successfully")

            // Update UI
            binding.tvStatus.text = "Monitoring: $packageName"

            // Suggest disabling battery optimization
            disableBatteryOptimization()

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting service: ${e.message}")
            Toast.makeText(this, "Permission denied. Please grant all permissions.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service: ${e.message}")
            Toast.makeText(this, "Failed to start service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun disableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(android.os.PowerManager::class.java)
            val packageName = packageName

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("Battery Optimization")
                    .setMessage("For best results, please disable battery optimization for this app.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("Later", null)
                    .show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d(TAG, "onRequestPermissionsResult: requestCode=$requestCode")

        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (i in grantResults.indices) {
                val permission = permissions[i]
                val granted = grantResults[i] == android.content.pm.PackageManager.PERMISSION_GRANTED

                Log.d(TAG, "Permission $permission: ${if (granted) "GRANTED" else "DENIED"}")

                if (!granted) {
                    allGranted = false
                }
            }

            if (allGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "All permissions granted")
            } else {
                Toast.makeText(this, "Some permissions denied. App may not work properly.", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Some permissions were denied")
            }
        }
    }
}