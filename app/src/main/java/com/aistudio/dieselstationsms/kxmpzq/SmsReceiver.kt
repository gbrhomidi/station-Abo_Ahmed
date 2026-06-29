package com.aistudio.dieselstationsms.kxmpzq

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        // FIXED: Check SEND_SMS permission using ContextCompat instead of packageManager
        val hasPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.e(TAG, "SEND_SMS permission not granted, cannot send auto-replies")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val db = DatabaseHelper(context)

        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: continue
            val msgBody = sms.displayMessageBody?.lowercase() ?: continue

            // Log incoming SMS
            db.logSms(sender, msgBody, "received", "success")
            Log.d(TAG, "SMS received from: $sender, body: $msgBody")

            try {
                when {
                    msgBody.contains("رصيد") || msgBody.contains("حساب") || msgBody.contains("balance") -> {
                        handleBalanceQuery(context, db, sender)
                    }
                    msgBody.contains("دفع") || msgBody.contains("تسديد") -> {
                        sendReply(
                            context, db, sender,
                            "شكراً لتواصلك. يرجى زيارة المحطة لإتمام عملية الدفع وتزويدنا بوصل الاستلام."
                        )
                    }
                    msgBody.contains("استعلام") || msgBody.contains("help") -> {
                        sendReply(
                            context, db, sender,
                            "مرحباً بك في محطة أبو أحمد لمشتقات الديزل.\n" +
                                    "الخدمات المتاحة:\n" +
                                    "1. الاستعلام عن الرصيد (أرسل: رصيد)\n" +
                                    "2. معرفة العروض (أرسل: عروض)\n" +
                                    "3. الموقع (أرسل: موقع)"
                        )
                    }
                    msgBody.contains("عروض") || msgBody.contains("offer") -> {
                        sendReply(
                            context, db, sender,
                            "عروض اليوم:\n- سعر اللتر: 500 ريال\n" +
                                    "- خصم الولاء: 5% للعملاء الذهبيين\n" +
                                    "- توصيل مجاني للطلبات +5000 لتر"
                        )
                    }
                    msgBody.contains("موقع") || msgBody.contains("location") -> {
                        sendReply(
                            context, db, sender,
                            "موقع محطة أبو أحمد:\nبجانب مدرسة الاتحاد برأس وادي ثاة - الحميدة - العرش\nأوقات العمل: 24 ساعة"
                        )
                    }
                    else -> {
                        sendReply(
                            context, db, sender,
                            "شكراً لتواصلك مع محطة أبو أحمد.\n" +
                                    "للحصول على المساعدة، أرسل: استعلام"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}", e)
                try {
                    sendReply(context, db, sender, "عذراً، حدث خطأ في معالجة طلبك. يرجى المحاولة مرة أخرى.")
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to send error reply: ${ex.message}", ex)
                }
            }
        }
    }

    private fun handleBalanceQuery(context: Context, db: DatabaseHelper, sender: String) {
        try {
            val customers = db.getCustomers()
            var found = false
            val cleanSender = sender.replace("[^0-9+]".toRegex(), "")

            for (i in 0 until customers.length()) {
                val c = customers.getJSONObject(i)
                val cPhone = c.optString("phone", "").replace("[^0-9+]".toRegex(), "")

                if (cPhone.isNotEmpty() && (
                            cleanSender.contains(cPhone) || cPhone.contains(cleanSender) ||
                                    cleanSender.endsWith(cPhone.takeLast(7)) || cPhone.endsWith(cleanSender.takeLast(7))
                            )) {

                    val bal = c.optDouble("current_balance", 0.0)
                    val points = c.optInt("loyalty_points", 0)
                    val vip = c.optInt("vip_level", 0)
                    val vipText = when (vip) {
                        3 -> "ذهبي 🥇"
                        2 -> "فضي 🥈"
                        else -> "عادي"
                    }
                    val name = c.optString("full_name", "عميلنا العزيز")
                    val reply = "مرحباً $name,\nالرصيد الحالي: $bal ريال\nنقاط الولاء: $points\nالعضوية: $vipText"

                    sendReply(context, db, sender, reply)
                    found = true
                    break
                }
            }

            if (!found) {
                // Search in transactions as fallback
                val transactions = db.getTransactions()
                for (i in 0 until transactions.length()) {
                    val t = transactions.getJSONObject(i)
                    val customerId = t.optInt("customer_id", 0)
                    if (customerId > 0) {
                        val customer = db.getCustomer(customerId)
                        if (customer != null) {
                            val cPhone = customer.optString("phone", "").replace("[^0-9+]".toRegex(), "")
                            if (cPhone.isNotEmpty() && (cleanSender.contains(cPhone) || cPhone.contains(cleanSender))) {
                                val bal = customer.optDouble("current_balance", 0.0)
                                val points = customer.optInt("loyalty_points", 0)
                                val name = customer.optString("full_name", "عميلنا العزيز")
                                val reply = "مرحباً $name,\nالرصيد الحالي: $bal ريال\nنقاط الولاء: $points"
                                sendReply(context, db, sender, reply)
                                found = true
                                break
                            }
                        }
                    }
                }
            }

            if (!found) {
                sendReply(context, db, sender, "عذراً، لم يتم العثور على حساب مرتبط بهذا الرقم. يرجى التسجيل في المحطة.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleBalanceQuery: ${e.message}", e)
            sendReply(context, db, sender, "عذراً، حدث خطأ في الاستعلام عن الرصيد. يرجى المحاولة مرة أخرى.")
        }
    }

    private fun sendReply(context: Context, db: DatabaseHelper, phone: String, message: String) {
        try {
            // FIXED: Use ContextCompat.checkSelfPermission instead of packageManager
            val hasPermission = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.e(TAG, "SEND_SMS permission not granted")
                db.logSms(phone, message, "auto_reply", "failed: permission denied")
                return
            }

            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)

            if (parts.size > 1) {
                val sentIntents = ArrayList<android.app.PendingIntent?>(parts.size) { null }
                val deliveryIntents = ArrayList<android.app.PendingIntent?>(parts.size) { null }
                smsManager.sendMultipartTextMessage(phone, null, parts, sentIntents, deliveryIntents)
                Log.d(TAG, "Multipart SMS sent to $phone: ${parts.size} parts")
            } else {
                smsManager.sendTextMessage(phone, null, message, null, null)
                Log.d(TAG, "SMS sent to $phone: $message")
            }

            db.logSms(phone, message, "auto_reply", "sent")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending SMS: ${e.message}", e)
            db.logSms(phone, message, "auto_reply", "failed: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException sending SMS: ${e.message}", e)
            db.logSms(phone, message, "auto_reply", "failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS: ${e.message}", e)
            db.logSms(phone, message, "auto_reply", "failed: ${e.message}")
        }
    }
}
