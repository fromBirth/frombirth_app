package com.choongang.frombirth_app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.choongang.frombirth_app.MainActivity

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            MainActivity.setLocalNotification(context)
        }
    }
}