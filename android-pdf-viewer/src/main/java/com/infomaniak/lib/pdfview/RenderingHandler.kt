/*
 * Infomaniak PDF Viewer - Android
 * Copyright (C) 2025-2026 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.infomaniak.lib.pdfview

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.infomaniak.lib.pdfview.RenderingHandler.RenderingTask
import com.infomaniak.lib.pdfview.exception.PageRenderingException
import com.infomaniak.lib.pdfview.model.PagePart

/**
 * A [Handler] that will process incoming [RenderingTask] messages
 * and alert [PDFView.onBitmapRendered] when the portion of the
 * PDF is ready to render.
 */
internal class RenderingHandler(
    looper: Looper?,
    private val pdfView: PDFView,
) : Handler(looper!!) {
    private val renderBounds = RectF()
    private val roundedRenderBounds = Rect()
    private val renderMatrix = Matrix()
    private var running = false

    @Volatile
    private var renderingGeneration = 0L

    fun addRenderingTask(
        page: Int,
        renderingSize: RenderingSize,
        thumbnail: Boolean,
        cacheOrder: Int,
        bestQuality: Boolean,
        annotationRendering: Boolean,
        isForPrinting: Boolean,
    ) {
        val task = RenderingTask(
            renderingSize,
            page,
            thumbnail,
            cacheOrder,
            bestQuality,
            annotationRendering,
            isForPrinting,
            renderingGeneration,
        )
        val msg = obtainMessage(MSG_RENDER_TASK, task)
        sendMessage(msg)
    }

    fun stop() {
        running = false
        cancelAllTasks()
    }

    fun start() {
        running = true
    }

    fun cancelAllTasks() {
        renderingGeneration++
        removeMessages(MSG_RENDER_TASK)
    }

    override fun handleMessage(message: Message): Unit = with(pdfView) {
        val task = message.obj as RenderingTask
        runCatching {
            proceed(task)?.let { pagePart ->
                if (task.renderingGeneration != renderingGeneration) {
                    pagePart.renderedBitmap.recycle()
                } else {
                    post {
                        if (running && task.renderingGeneration == renderingGeneration) {
                            onBitmapRendered(pagePart, task.isForPrinting)
                        } else {
                            pagePart.renderedBitmap.recycle()
                        }
                    }
                }
            }
        }.onFailure { exception ->
            if (exception is PageRenderingException) post { onPageError(exception) }
        }
    }

    @Throws(PageRenderingException::class)
    private fun proceed(renderingTask: RenderingTask): PagePart? {
        val pdfFile = pdfView.pdfFile
        pdfFile.openPage(renderingTask.page)

        val w = Math.round(renderingTask.renderingSize.width)
        val h = Math.round(renderingTask.renderingSize.height)

        if (w == 0 || h == 0 || pdfFile.pageHasError(renderingTask.page)) {
            return null
        }

        var render: Bitmap? = null
        runCatching {
            Bitmap.createBitmap(
                /* width = */ w,
                /* height = */ h,
                /* config = */ if (renderingTask.bestQuality) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565,
            )
        }.onSuccess { renderedBitmap ->
            render = renderedBitmap
        }.onFailure {
            Log.e(TAG, "Cannot create bitmap", it)
            render = null
        }

        calculateBounds(w, h, renderingTask.renderingSize.bounds)

        pdfFile.renderPageBitmap(
            render, renderingTask.page, roundedRenderBounds, renderingTask.annotationRendering
        )

        return PagePart(
            renderingTask.page,
            render,
            renderingTask.renderingSize.bounds,
            renderingTask.thumbnail,
            renderingTask.cacheOrder
        )
    }

    private fun calculateBounds(width: Int, height: Int, pageSliceBounds: RectF) {
        renderMatrix.reset()
        renderMatrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height)
        renderMatrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height())

        renderBounds[0f, 0f, width.toFloat()] = height.toFloat()
        renderMatrix.mapRect(renderBounds)
        renderBounds.round(roundedRenderBounds)
    }

    data class RenderingSize(
        var width: Float,
        var height: Float,
        var bounds: RectF,
    )

    private data class RenderingTask(
        var renderingSize: RenderingSize,
        var page: Int,
        var thumbnail: Boolean,
        var cacheOrder: Int,
        var bestQuality: Boolean,
        var annotationRendering: Boolean,
        var isForPrinting: Boolean,
        val renderingGeneration: Long,
    )

    companion object {
        /**
         * [Message.what] kind of message this handler processes.
         */
        const val MSG_RENDER_TASK: Int = 1

        private val TAG: String = RenderingHandler::class.java.name
    }
}
