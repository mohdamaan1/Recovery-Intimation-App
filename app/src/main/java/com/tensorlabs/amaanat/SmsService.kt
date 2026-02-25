package com.tensorlabs.amaanat

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null // Binding ki zaroorat nahi hai
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Intent se data nikalo
        val bMobile = intent?.getStringExtra("b_mobile") ?: ""
        val bMsg = intent?.getStringExtra("b_msg") ?: ""
        val gMobiles = intent?.getStringArrayListExtra("g_mobiles") ?: arrayListOf()
        val gMsg = intent?.getStringExtra("g_msg") ?: ""

        // Background Thread pe bhejo taaki UI freeze na ho
        CoroutineScope(Dispatchers.IO).launch {
            sendSMS(bMobile, bMsg, gMobiles, gMsg)
            stopSelf() // Kaam khatam, service band
        }

        return START_NOT_STICKY
    }

    private suspend fun sendSMS(
        bMobile: String,
        bMsg: String,
        gMobiles: ArrayList<String>,
        gMsg: String
    ) {
        val smsManager = SmsManager.getDefault()
        var totalSent = 0

        try {
            // 1. Borrower ko bhejo
            if (bMobile.length == 10 && bMsg.isNotEmpty()) {
                val parts = smsManager.divideMessage(bMsg)
                smsManager.sendMultipartTextMessage(bMobile, null, parts, null, null)
                totalSent++
            }

            // 2. Guarantors ko bhejo
            if (gMobiles.isNotEmpty() && gMsg.isNotEmpty()) {
                val gParts = smsManager.divideMessage(gMsg)
                for (mobile in gMobiles) {
                    if (mobile.length == 10) {
                        smsManager.sendMultipartTextMessage(mobile, null, gParts, null, null)
                        totalSent++
                    }
                }
            }

            // 3. Success Notification (Main Thread pe Toast)
            withContext(Dispatchers.Main) {
                if (totalSent > 0) {
                    Toast.makeText(applicationContext, "Service: Sent to $totalSent people via SMS!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(applicationContext, "Service: No valid numbers found.", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "SMS Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}