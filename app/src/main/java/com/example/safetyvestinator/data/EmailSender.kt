package com.example.safetyvestinator.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    suspend fun sendImpactAlert(
        recipientEmail: String,
        location: GpsLocation?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        Log.d("EmailSender", "sendImpactAlert called with recipient='$recipientEmail'")
        if (recipientEmail.isBlank()) {
            Log.d("EmailSender", "recipient blank, skipping")
            return@withContext Result.success(Unit)
        }

        try {
            val props = Properties().apply {
                put("mail.smtp.host", EmailConfig.SMTP_HOST)
                put("mail.smtp.port", EmailConfig.SMTP_PORT.toString())
                put("mail.smtp.auth", "true")
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.connectiontimeout", "10000")
                put("mail.smtp.timeout", "10000")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(
                        EmailConfig.SENDER_EMAIL,
                        EmailConfig.SENDER_APP_PASSWORD
                    )
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(EmailConfig.SENDER_EMAIL, EmailConfig.SENDER_DISPLAY_NAME))
                addRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                subject = "Impact detected on safety vest"
                setText(buildBody(location))
            }

            Transport.send(message)
            Log.d("EmailSender", "Impact alert sent to $recipientEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EmailSender", "Failed to send impact alert", e)
            Result.failure(e)
        }
    }

    private fun buildBody(location: GpsLocation?): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
            .format(Date())

        val locationText = if (location != null) {
            val ageMinutes = (System.currentTimeMillis() - location.receivedAtMillis) / 60_000
            val freshness = if (ageMinutes < 1) "current" else "$ageMinutes min old"
            """
            |Location ($freshness):
            |  Latitude:  ${"%.6f".format(location.latitude)}
            |  Longitude: ${"%.6f".format(location.longitude)}
            |  Map: https://maps.google.com/?q=${location.latitude},${location.longitude}
            """.trimMargin()
        } else {
            "Location: not available (no GPS fix)"
        }

        return """
            |An impact has been detected by the safety vest.
            |
            |Time: $timestamp
            |
            |$locationText
            |
            |This is an automated alert from SafetyVestinator.
        """.trimMargin()
    }
}