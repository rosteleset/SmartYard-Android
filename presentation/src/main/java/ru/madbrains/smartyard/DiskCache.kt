package ru.madbrains.smartyard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.jakewharton.disklrucache.DiskLruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.madbrains.smartyard.ui.readExifData
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.locks.ReentrantReadWriteLock

interface ImageCache {
    suspend fun put(url: String, bitmap: Bitmap)
    fun get(url: String): Bitmap?
    fun clear()
}


class DiskCache private constructor(val context: Context) : ImageCache {
    private val lock = ReentrantReadWriteLock()
    private var cache: DiskLruCache =
        DiskLruCache.open(context.cacheDir, 1, 1, 10 * 1024 * 1024)


    override fun get(url: String): Bitmap? {
        lock.readLock().lock()
        try {
            val key = md5(url)
            val snapshot: DiskLruCache.Snapshot? = cache.get(key)
            return if (snapshot != null) {
                val inputStream: InputStream = snapshot.getInputStream(0)
                val buffIn = BufferedInputStream(inputStream, 8 * 1024)
                BitmapFactory.decodeStream(buffIn)
            } else {
                null
            }
        } finally {
            lock.readLock().unlock()
        }
    }

    fun putAll(listBitmap: List<Pair<Int, Bitmap>>) {
        listBitmap.forEach {
            val key = md5(it.first.toString())
            var editor: DiskLruCache.Editor? = null
            try {
                editor = cache.edit(key)
                if (editor == null) {
                    return
                }
                if (writeBitmapToFile(it.second, editor)) {
                    cache.flush()
                    editor.commit()
                } else {
                    editor.abort()
                }
            } catch (e: IOException) {
                try {
                    editor?.abort()
                } catch (ignored: IOException) {

                }
            }
        }
    }

    override suspend fun put(url: String, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            lock.writeLock().lock()
            val key = md5(url)
            var editor: DiskLruCache.Editor? = null
            try {
                editor = cache.edit(key)
                if (editor == null) {
                    return@withContext
                }
                if (writeBitmapToFile(bitmap, editor)) {
                    cache.flush()
                    editor.commit()
                } else {
                    editor.abort()
                }
            } catch (e: IOException) {
                Timber.e(e, "DISK CACHE EXCEPTION")
                try {
                    editor?.abort()
                } catch (ignored: IOException) {
                    Timber.e(ignored, "Failed to abort editor")
                }
            } finally {
                lock.writeLock().unlock()
            }
        }
    }

    override fun clear() {
        cache.delete()
        cache = DiskLruCache.open(
            context.cacheDir, 1, 1,
            10 * 1024 * 1024
        )
    }

    private fun writeBitmapToFile(bitmap: Bitmap, editor: DiskLruCache.Editor): Boolean {
        var out: OutputStream? = null
        try {
            out = BufferedOutputStream(editor.newOutputStream(0), 8 * 1024)
            return bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        } finally {
            out?.close()
        }
    }

    private fun md5(url: String): String? {
        try {
            val md = MessageDigest.getInstance("MD5")
            val messageDigest = md.digest(url.toByteArray())
            val no = BigInteger(1, messageDigest)
            var hashtext = no.toString(16)
            while (hashtext.length < 32) {
                hashtext = "0$hashtext"
            }
            return hashtext
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        @Volatile
        private var instance: DiskCache? = null

        fun getInstance(context: Context): DiskCache {
            return instance ?: synchronized(this) {
                instance ?: DiskCache(context).also { instance = it }
            }
        }
    }
}