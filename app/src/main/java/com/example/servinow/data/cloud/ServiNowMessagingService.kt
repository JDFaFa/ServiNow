package com.example.servinow.data.cloud

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.servinow.MainActivity
import com.example.servinow.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ServiNowMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { registerToken(token) }
    override fun onMessageReceived(message: RemoteMessage) {
        val uid=FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Data-only messages are filtered locally after logout or account changes.
        if(message.data["recipient"]!=uid) return
        if(android.os.Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val manager=getSystemService(NotificationManager::class.java)
        val channel="job_updates"
        manager.createNotificationChannel(NotificationChannel(channel,getString(R.string.push_channel),NotificationManager.IMPORTANCE_HIGH))
        val notice=message.data["notice"] ?: message.data["post"].orEmpty()
        val body=message.data["body"].orEmpty()
        val content=getString(when(body) {
            "NOTICE_MESSAGE" -> R.string.notice_message
            "NOTICE_OFFER" -> R.string.notice_offer
            "NOTICE_ACCEPTED" -> R.string.notice_accepted
            "NOTICE_FINISHED" -> R.string.notice_finished
            "NOTICE_APPROVED" -> R.string.notice_approved
            "NOTICE_REJECTED" -> R.string.notice_rejected
            "NOTICE_COMMENT" -> R.string.notice_comment
            else -> R.string.push_update
        })
        val intent=Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("notice_id",notice).putExtra("notice_post",message.data["post"])
            .putExtra("notice_body",body).putExtra("notice_recipient",uid)
        val pending=PendingIntent.getActivity(this,notice.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(notice.hashCode(),NotificationCompat.Builder(this,channel)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(getString(R.string.app_name))
            .setContentText(content).setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOnlyAlertOnce(true).setContentIntent(pending).setAutoCancel(true).build())
    }
    companion object {
        fun registerToken(token:String) {
            val uid=FirebaseAuth.getInstance().currentUser?.uid ?: return
            val id=java.security.MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString(""){"%02x".format(it)}
            FirebaseFirestore.getInstance().collection("members").document(uid).collection("devices").document(id)
                .set(mapOf("token" to token,"updatedAt" to FieldValue.serverTimestamp()))
        }
    }
}
