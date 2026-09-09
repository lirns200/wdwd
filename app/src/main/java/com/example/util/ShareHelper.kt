package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.JumpRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareHelper {

    enum class ShareTarget(val label: String, val packageName: String?) {
        LIKEE("Likee", "video.like"),
        VK("ВКонтакте / Мой Мир", "com.vkontakte.android"),
        ALL("Другие приложения (4kiz и др.)", null)
    }

    fun shareJumpResult(
        context: Context,
        record: JumpRecord,
        target: ShareTarget = ShareTarget.ALL
    ) {
        val shareText = buildString {
            appendLine("🏆 HighJump Challenge!")
            appendLine("👤 Атлет: ${record.userNickname}")
            appendLine("🚀 Высота прыжка: ${String.format(Locale.getDefault(), "%.2f", record.jumpHeight)} м (${(record.jumpHeight * 100).toInt()} см)")
            appendLine("⏱ Время в воздухе: ${record.flightTimeMs} мс")
            appendLine("📅 Дата: ${formatDate(record.date)}")
            appendLine("#HighJumpChallenge #Спорт #ПрыжокВВысоту")
        }

        val cardFile = generateResultCardImage(context, record)
        val imageUri = cardFile?.let {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                it
            )
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (imageUri != null) "image/png" else "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Мой рекорд в HighJump Challenge!")
            if (imageUri != null) {
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        if (target.packageName != null) {
            val packageManager = context.packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(target.packageName)
            if (launchIntent != null) {
                intent.setPackage(target.packageName)
                try {
                    context.startActivity(intent)
                    return
                } catch (e: Exception) {
                    // Fallback to chooser if app package fails
                }
            } else {
                Toast.makeText(
                    context,
                    "Приложение ${target.label} не установлено. Открываем список доступных приложений...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val chooser = Intent.createChooser(intent, "Поделиться результатом через")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun generateResultCardImage(context: Context, record: JumpRecord): File? {
        return try {
            val width = 1080
            val height = 1440
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Dark athletic background
            canvas.drawColor(Color.parseColor("#0A0F1D"))

            val cardPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#131B2E")
            }
            canvas.drawRoundRect(RectF(60f, 60f, width - 60f, height - 60f), 48f, 48f, cardPaint)

            // Title
            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#00F0FF")
                textSize = 64f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawText("HIGHJUMP CHALLENGE", width / 2f, 180f, titlePaint)

            // Athlete nickname badge
            val namePaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 52f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("⚡ ${record.userNickname} ⚡", width / 2f, 290f, namePaint)

            // Huge jump height value
            val heightPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#F59E0B")
                textSize = 150f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val heightStr = String.format(Locale.getDefault(), "%.2f м", record.jumpHeight)
            canvas.drawText(heightStr, width / 2f, 520f, heightPaint)

            val cmPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#94A3B8")
                textSize = 48f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("(${(record.jumpHeight * 100).toInt()} см)", width / 2f, 600f, cmPaint)

            // Flight time & date badges
            val metaPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#10B981")
                textSize = 42f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Время полета: ${record.flightTimeMs} мс", width / 2f, 740f, metaPaint)

            val datePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#64748B")
                textSize = 36f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Дата: ${formatDate(record.date)}", width / 2f, 820f, datePaint)

            // Footer branding
            val brandPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#38BDF8")
                textSize = 34f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Зафиксировано с помощью акселерометра смартфона", width / 2f, height - 120f, brandPaint)

            val file = File(context.cacheDir, "jump_result_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
