/*
 * Infomaniak PDF Viewer - Android
 * Copyright (C) 2025 Infomaniak Network SA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        )
        val msg = obtainMessage(MSG_RENDER_TASK, task)
        sendMessage(msg)
    }

    fun stop() {
        running = false
    }

    fun start() {
        running = true
    }

    override fun handleMessage(message: Message): Unit = with(pdfView) {
        val task = message.obj as RenderingTask
        runCatching {
            proceed(task)?.let { pagePart ->
                if (running) {
                    post { onBitmapRendered(pagePart, task.isForPrinting) }
                } else {
                    pagePart.renderedBitmap.recycle()
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
                w, h, if (renderingTask.bestQuality) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
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
    )

    companion object {
        /**
         * [Message.what] kind of message this handler processes.
         */
        const val MSG_RENDER_TASK: Int = 1

        private val TAG: String = RenderingHandler::class.java.name
    }
}
