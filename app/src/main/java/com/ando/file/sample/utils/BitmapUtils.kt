package com.ando.file.sample.utils

import ando.file.compressor.ImageChecker.needCompress
import ando.file.compressor.ImageCompressEngine.compressPure
import ando.file.core.FileGlobal
import ando.file.core.FileLogger
import ando.file.core.FileSizeUtils.getFileSize
import ando.file.core.FileUtils
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.AsyncTask
import android.widget.Toast
import androidx.annotation.Size
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference


/**
 * Created by javakam on 2016年7月30日17:21:19 .
 */
object BitmapUtils {
    fun stitchImages(bitmaps: List<Bitmap>): Bitmap? {
        var width = 0
        var height = 0
        // 计算拼接后的图片大小
        for (bitmap in bitmaps) {
            width += bitmap.width
            height = Math.max(height, bitmap.height)
        }
        // 创建拼接后的空白图片
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        // 拼接图片
        var x = 0F
        for (bitmap in bitmaps) {
            canvas.drawBitmap(bitmap, x, 0F, null)
            x += bitmap.width
        }
        return result
    }

    /**
     * 质量压缩方法 -> Object[0](Bitmap) 压缩后的 Bitmap ; Object[1](int) 大小
     *
     * https://www.jianshu.com/p/4ba3e63c8cdc
     * 一张 ARGB_8888 的Bitmap占用内存计算公式：bmpWidth * bmpHeight * 4byte
     * RGB_565 * 2byte
     *
     * 一般对图片没有透明度要求的话，可以改成 RGB_565，相比ARGB_8888将节省一半的内存开销
     */
    fun compressBitmap(uri: Uri?, maxSize: Long = 300L): Array<Any>? {
        if (uri == null || !FileUtils.checkImage(uri)) {
            return null
        }

        //300 KB 以下不压缩
        val needCompress = needCompress(maxSize.toInt(), uri)
        if (!needCompress) {
            return try {
                val bitmap = BitmapFactory.decodeFileDescriptor(FileGlobal.openFileDescriptor(uri)?.fileDescriptor ?: return null)

                arrayOf(bitmap, getFileSize(uri))
            } catch (e: Exception) {
                FileLogger.e(e.message)
                null
            }
        }
        val bitmap = compressPure(uri, maxSize = maxSize)
        FileLogger.e("compressBitmap uri= $uri  bitmap= $bitmap")
        if (bitmap == null) {
            return null
        }
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 60, baos)
        return arrayOf(bitmap, (baos.toByteArray().size * 4).toLong())
    }

    /**
     * 异步压缩图片
     */
    fun compressBitmapAsync(activity: Activity?, uri: Uri, callBack: OnCompressAsyncCallBack?) {
        CompressAsyncTask(activity, uri, callBack).execute()
    }

    interface OnCompressAsyncCallBack {
        /**
         * @param objects [0] Bitmap ; [1] Bitmap Size , Long
         */
        fun onResult(@Size(value = 2) objects: Array<Any>?)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    class CompressAsyncTask(
        activity: Activity?,
        private val uri: Uri,
        private val callBack: OnCompressAsyncCallBack? = null,
    ) : AsyncTask<Int, Int, Array<Any>?>() {

        private val mWeakActivity: WeakReference<Activity?> = WeakReference(activity)

        @Deprecated("Deprecated in Java")
        override fun onPreExecute() {
            super.onPreExecute()
            if (mWeakActivity.get() != null) {
                //CLoadDialogUtils.showProgressDialog(mWeakActivity.get(), "正在压缩图片...", false, null)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Int?): Array<Any>? {
            //调用onProgressUpdate方法
            //publishProgress(i);
            return compressBitmap(uri)
        }

        @Deprecated("Deprecated in Java", ReplaceWith("super.onProgressUpdate(*values)", "android.os.AsyncTask"))
        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: Array<Any>?) {
            super.onPostExecute(result)
            if (result == null && mWeakActivity.get() != null) {
                Toast.makeText(mWeakActivity.get(), "不支持此类型的文件!", Toast.LENGTH_SHORT).show()
            }
            callBack?.onResult(result)
            //CLoadDialogUtils.cancelProgressDialog()
            if (mWeakActivity.get() != null) {
                mWeakActivity.clear()
            }
        }
    }
}