/*
 * Infomaniak android-pdf-viewer
 * Copyright (C) 2026-2026 Infomaniak Network SA
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

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * A unified PDF preview [FrameLayout] that uses AndroidX native PDF preview when available
 * and falls back to [PDFView] otherwise.
 *
 * The AndroidX backend requires calling [attach] before [loadFromUri].
 */
class UnifiedPdfPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    fun interface FallbackConfigurator {
        fun configure(configurator: PDFView.Configurator)
    }

    fun interface NativeFragmentFactory {
        fun create(): Fragment
    }

    enum class Backend {
        ANDROIDX_NATIVE,
        PDF_VIEW,
    }

    private val fallbackPdfView = PDFView(context, null)
    private val nativeContainer = FrameLayout(context)
    private val nativeFragmentTag = "unified-pdf-preview-native-${hashCode()}"

    private var attachedFragmentManager: FragmentManager? = null
    private var attachedLifecycleOwner: LifecycleOwner? = null
    private var lifecycleObserver: DefaultLifecycleObserver? = null
    private var lastBackend: Backend? = null
    private var nativeFragmentFactory: NativeFragmentFactory? = null

    init {
        if (id == NO_ID) id = generateViewId()
        nativeContainer.id = generateViewId()
        nativeContainer.layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        fallbackPdfView.layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        fallbackPdfView.isGone = true
        nativeContainer.isGone = true
        addView(nativeContainer)
        addView(fallbackPdfView)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        fallbackPdfView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        setPadding(0, 0, 0, 0)
    }

    /**
     * Attaches this view to a [FragmentManager]. Required when AndroidX native backend is selected.
     */
    fun attach(fragmentManager: FragmentManager, lifecycleOwner: LifecycleOwner) {
        detach()
        attachedFragmentManager = fragmentManager
        attachedLifecycleOwner = lifecycleOwner
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                detach()
            }
        }
        lifecycleObserver = observer
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    fun detach() {
        lifecycleObserver?.let { observer ->
            attachedLifecycleOwner?.lifecycle?.removeObserver(observer)
        }
        lifecycleObserver = null
        attachedLifecycleOwner = null

        attachedFragmentManager?.let { fragmentManager ->
            val fragment = fragmentManager.findFragmentByTag(nativeFragmentTag)
            if (fragment != null && !fragmentManager.isStateSaved) {
                fragmentManager.commitNow { remove(fragment) }
            }
        }
        attachedFragmentManager = null
    }

    fun loadFromUri(
        uri: Uri?,
        password: String? = null,
        preferNativeBackend: Boolean = true,
        fallbackConfigurator: FallbackConfigurator? = null,
    ) {
        require(uri != null) { "uri must not be null" }
        if (preferNativeBackend && canUseAndroidXNativeBackend()) {
            showNativeBackend(uri)
        } else {
            showFallbackBackend(uri, password, fallbackConfigurator)
        }
    }

    fun getCurrentBackend(): Backend? = lastBackend

    fun setNativeFragmentFactory(factory: NativeFragmentFactory?) {
        nativeFragmentFactory = factory
    }

    private fun canUseAndroidXNativeBackend(): Boolean {
        return Build.VERSION.SDK_INT >= 35 && isAndroidXPdfViewerAvailable()
    }

    private fun showNativeBackend(uri: Uri) {
        val fragmentManager = requireNotNull(attachedFragmentManager) {
            "attach(fragmentManager, lifecycleOwner) must be called before loadFromUri() when native backend is selected."
        }
        val nativeFragment = fragmentManager.findFragmentByTag(nativeFragmentTag) ?: createNativeFragment().also {
            fragmentManager.commitNow { replace(nativeContainer.id, it, nativeFragmentTag) }
        }

        fallbackPdfView.recycle()
        fallbackPdfView.isGone = true
        nativeContainer.isVisible = true
        setDocumentUri(nativeFragment, uri)
        lastBackend = Backend.ANDROIDX_NATIVE
    }

    private fun showFallbackBackend(uri: Uri, password: String?, fallbackConfigurator: FallbackConfigurator?) {
        nativeContainer.isGone = true
        fallbackPdfView.isVisible = true
        fallbackPdfView.background = background?.constantState?.newDrawable(resources)?.mutate()

        val configurator = fallbackPdfView.fromUri(uri).password(password)
        fallbackConfigurator?.configure(configurator)
        configurator.load()
        lastBackend = Backend.PDF_VIEW
    }

    private fun isAndroidXPdfViewerAvailable(): Boolean {
        return runCatching {
            Class.forName(ANDROIDX_PDF_VIEWER_FRAGMENT_CLASS)
        }.isSuccess
    }

    private fun createAndroidXPdfViewerFragment(): Fragment {
        val fragmentClass = Class.forName(ANDROIDX_PDF_VIEWER_FRAGMENT_CLASS)
        return fragmentClass.getDeclaredConstructor().newInstance() as Fragment
    }

    private fun createNativeFragment(): Fragment {
        return nativeFragmentFactory?.create() ?: createAndroidXPdfViewerFragment()
    }

    private fun setDocumentUri(fragment: Fragment, uri: Uri) {
        val setDocumentUriMethod = fragment::class.java.methods.firstOrNull { method ->
            method.name == "setDocumentUri" &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0] == Uri::class.java
        } ?: error("androidx.pdf PdfViewerFragment#setDocumentUri(Uri) is not available.")
        setDocumentUriMethod.invoke(fragment, uri)
    }

    private companion object {
        private const val ANDROIDX_PDF_VIEWER_FRAGMENT_CLASS = "androidx.pdf.viewer.fragment.PdfViewerFragment"
    }
}
