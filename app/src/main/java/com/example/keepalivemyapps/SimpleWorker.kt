package com.example.keepalivemyapps

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class SimpleWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "SimpleWorker"

        fun schedule(context: Context) {
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<SimpleWorker>(
                15, TimeUnit.MINUTES
            )
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()

            androidx.work.WorkManager.getInstance(context)
                .enqueue(workRequest)

            val targetPackage = Config.getTargetPackage(context)
            Log.d(TAG, "Worker scheduled for $targetPackage")
        }
    }

    override fun doWork(): Result {
        val targetPackage = Config.getTargetPackage(applicationContext)
        Log.d(TAG, "Worker running - checking service health")
        Log.d(TAG, "Monitoring target: $targetPackage")

        val packageInfo = try {
            applicationContext.packageManager.getPackageInfo(targetPackage, 0)
            true
        } catch (e: Exception) {
            false
        }

        Log.d(TAG, "Target app '$targetPackage' installed: $packageInfo")
        return Result.success()
    }
}