package com.aistudio.dieselstationsms.kxmpzq

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "BackupWorker"
        private const val BACKUP_PREFIX = "auto_backup_"
        private const val MAX_BACKUPS = 10   // ✅ حد أقصى للنسخ القديمة
    }

    override suspend fun doWork(): Result {
        return try {
            val db = DatabaseHelper(applicationContext)
            val out = db.exportAllData().toString(2)

            // ✅ تشفير النسخة الاحتياطية قبل الحفظ
            val encrypted = encryptBackup(out)

            val dir = File(applicationContext.filesDir, "backups")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            // ✅ إزالة النسخ القديمة الزائدة عن الحد
            cleanupOldBackups(dir)

            val file = File(dir, "${BACKUP_PREFIX}${System.currentTimeMillis()}.enc")
            file.writeText(encrypted)

            Log.d(TAG, "Auto backup completed: ${file.absolutePath}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Auto backup failed: ${e.message}", e)
            Result.failure()
        }
    }

    /**
     * تشفير البيانات باستخدام Android Keystore (سيتم استكماله لاحقاً)
     * حالياً يعيد البيانات كما هي دون تشفير فعلي.
     */
    private fun encryptBackup(data: String): String {
        // TODO: تنفيذ التشفير باستخدام Android Keystore
        return data
    }

    /**
     * يحذف أقدم النسخ الاحتياطية إذا تجاوز عددها الحد الأقصى [MAX_BACKUPS].
     */
    private fun cleanupOldBackups(dir: File) {
        dir.listFiles { f -> f.name.startsWith(BACKUP_PREFIX) }
            ?.sortedBy { it.lastModified() }
            ?.dropLast(MAX_BACKUPS)
            ?.forEach { it.delete() }
    }
}
