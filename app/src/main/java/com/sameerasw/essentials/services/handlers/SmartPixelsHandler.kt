
/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: SmartPixelsHandler.kt
 * Description: Advanced OLED burn-in protection with multi-position pixel shifting.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.NotificationListener

class SmartPixelsHandler(
    private val service: AccessibilityService,
) {
    companion object {
        private const val TAG = "SmartPixelsHandler"

        // Pixel shifting interval: 10 minutes
        private const val SHIFT_INTERVAL_MS = 10 * 60 * 1000L

        // 16 different physical-pixel offsets
        private val SHIFT_POSITIONS = arrayOf(
            0 to 0,
            1 to 0,
            1 to 1,
            0 to 1,
            2 to 1,
            2 to 0,
            3 to 0,
            3 to 1,
            3 to 2,
            2 to 2,
            1 to 2,
            0 to 2,
            0 to 3,
            1 to 3,
            2 to 3,
            3 to 3
        )
    }

    private var windowManager: WindowManager? = null
    private var displayManager: DisplayManager? = null
    private var overlayView: SmartPixelsOverlayView? = null

    private var isOverlayAdded = false
    private var isDestroyed = false

    private val handler = Handler(Looper.getMainLooper())

    private var isScreenRecordingActive = false
    private var screenRecordingCallback:
        java.util.function.Consumer<Int>? = null

    private val shiftPatternRunnable = object : Runnable {
        override fun run() {
            if (isDestroyed || !isOverlayAdded) return

            overlayView?.shiftPattern()

            handler.postDelayed(
                this,
                SHIFT_INTERVAL_MS
            )
        }
    }

    private val displayListener =
        object : DisplayManager.DisplayListener {

            override fun onDisplayAdded(displayId: Int) {
                updateState()
            }

            override fun onDisplayRemoved(displayId: Int) {
                updateState()
            }

            override fun onDisplayChanged(displayId: Int) {
                updateState()
            }
        }

    private val prefs by lazy {
        service.getSharedPreferences(
            SettingsRepository.PREFS_NAME,
            AccessibilityService.MODE_PRIVATE
        )
    }

    fun init() {
        isDestroyed = false

        windowManager =
            service.getSystemService(
                AccessibilityService.WINDOW_SERVICE
            ) as? WindowManager

        displayManager =
            service.getSystemService(
                AccessibilityService.DISPLAY_SERVICE
            ) as? DisplayManager

        displayManager?.registerDisplayListener(
            displayListener,
            handler
        )

        // Android 15+ screen recording detection
        if (
            android.os.Build.VERSION.SDK_INT >= 35 &&
            windowManager != null
        ) {
            try {
                val callback =
                    java.util.function.Consumer<Int> { state ->

                        isScreenRecordingActive =
                            state ==
                                WindowManager.SCREEN_RECORDING_STATE_VISIBLE

                        updateState()
                    }

                screenRecordingCallback = callback

                windowManager?.addScreenRecordingCallback(
                    { command ->
                        handler.post(command)
                    },
                    callback
                )

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Failed to register screen recording callback",
                    e
                )
            }
        }

        updateState()
    }

    private fun isScreenCastingOrMirroring(): Boolean {
        if (isScreenRecordingActive) return true

        if (NotificationListener.isScreenCaptureActive()) {
            return true
        }

        val displays = displayManager?.displays ?: return false

        for (display in displays) {
            if (display.displayId == Display.DEFAULT_DISPLAY) {
                continue
            }

            val flags = display.flags

            val isPresentation =
                (flags and Display.FLAG_PRESENTATION) != 0

            val isPrivate =
                (flags and Display.FLAG_PRIVATE) != 0

            // Any secondary display can represent mirroring,
            // presentation or wireless display output.
            if (
                isPresentation ||
                isPrivate ||
                display.displayId != Display.DEFAULT_DISPLAY
            ) {
                return true
            }
        }

        return false
    }

    fun updateState() {
        if (isDestroyed) return

        val enabled = prefs.getBoolean(
            SettingsRepository.KEY_SMART_PIXELS_ENABLED,
            false
        )

        val disableOnCast = prefs.getBoolean(
            SettingsRepository.KEY_SMART_PIXELS_DISABLE_ON_CAST,
            true
        )

        val intensity = prefs.getFloat(
            SettingsRepository.KEY_SMART_PIXELS_INTENSITY,
            50f
        )

        if (
            enabled &&
            !(disableOnCast && isScreenCastingOrMirroring())
        ) {
            showOverlay(intensity)
        } else {
            hideOverlay()
        }
    }

    private fun showOverlay(intensity: Float) {
        if (isDestroyed) return

        if (overlayView == null) {
            overlayView = SmartPixelsOverlayView(service)
        }

        overlayView?.setIntensity(intensity)

        if (
            !isOverlayAdded &&
            windowManager != null &&
            overlayView != null
        ) {
            val params =
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
                ).apply {

                    if (
                        android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.P
                    ) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams
                                .LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }

            try {
                overlayView?.visibility = View.VISIBLE
                overlayView?.alpha = 0f

                windowManager?.addView(
                    overlayView,
                    params
                )

                isOverlayAdded = true

                // Fade in
                ObjectAnimator.ofFloat(
                    overlayView,
                    "alpha",
                    0f,
                    1f
                ).apply {
                    duration = 200
                    start()
                }

                // Start burn-in protection shifting cycle
                handler.removeCallbacks(shiftPatternRunnable)

                handler.postDelayed(
                    shiftPatternRunnable,
                    SHIFT_INTERVAL_MS
                )

            } catch (e: Exception) {
                isOverlayAdded = false

                Log.e(
                    TAG,
                    "Failed to add Smart Pixels overlay",
                    e
                )
            }

        } else if (isOverlayAdded && overlayView != null) {

            overlayView?.visibility = View.VISIBLE
            overlayView?.alpha = 1f
            overlayView?.invalidate()
        }
    }

    private fun hideOverlay() {
        handler.removeCallbacks(shiftPatternRunnable)

        if (isOverlayAdded && overlayView != null) {

            val currentView = overlayView ?: return

            currentView.visibility = View.GONE

            try {
                windowManager?.removeView(currentView)
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Failed to remove overlay cleanly",
                    e
                )
            }

            isOverlayAdded = false
        }
    }

    fun destroy() {
        isDestroyed = true

        handler.removeCallbacks(shiftPatternRunnable)

        if (
            android.os.Build.VERSION.SDK_INT >= 35 &&
            screenRecordingCallback != null
        ) {
            try {
                windowManager?.removeScreenRecordingCallback(
                    screenRecordingCallback!!
                )
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Failed to unregister screen recording callback",
                    e
                )
            }

            screenRecordingCallback = null
        }

        displayManager?.unregisterDisplayListener(
            displayListener
        )

        hideOverlay()

        overlayView = null
        displayManager = null
        windowManager = null
    }

    private class SmartPixelsOverlayView(
        context: AccessibilityService
    ) : View(context) {

        private val paint = Paint().apply {
            isFilterBitmap = false
            isAntiAlias = false
        }

        private var cachedPatternBitmap: Bitmap? = null

        private var lastWidth = 0
        private var lastHeight = 0

        private var currentIntensity = 50f

        // Current position in the 16-position shifting sequence
        private var patternOffset = 0

        init {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        fun setIntensity(intensity: Float) {
            val safeIntensity = intensity.coerceIn(0f, 100f)

            if (currentIntensity != safeIntensity) {
                currentIntensity = safeIntensity
                invalidatePattern()
            }
        }

        fun shiftPattern() {
            patternOffset =
                (patternOffset + 1) % SHIFT_POSITIONS.size

            invalidatePattern()
        }

        private fun invalidatePattern() {
            cachedPatternBitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }

            cachedPatternBitmap = null
            invalidate()
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int
        ) {
            super.onSizeChanged(
                w,
                h,
                oldw,
                oldh
            )

            if (
                w != lastWidth ||
                h != lastHeight
            ) {
                lastWidth = w
                lastHeight = h

                invalidatePattern()
            }
        }

        private fun createPatternBitmap(
            w: Int,
            h: Int
        ): Bitmap {

            val bmp = Bitmap.createBitmap(
                w,
                h,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bmp)

            val p = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = false

                // Preserve the selected intensity
                alpha = (
                    currentIntensity / 100f * 255f
                ).toInt().coerceIn(20, 230)
            }

            // Higher intensity = denser protection pattern
            val step = when {
                currentIntensity >= 75f -> 2
                currentIntensity >= 50f -> 3
                currentIntensity >= 30f -> 4
                else -> 6
            }

            // Physical pixel offset
            val (offsetX, offsetY) =
                SHIFT_POSITIONS[patternOffset]

            // Draw a repeating checkerboard pattern.
            // Negative starting coordinates ensure complete
            // coverage even when the pattern is shifted.

            var row = -2

            while (row * step + offsetY < h) {

                val y = row * step + offsetY

                val phaseX =
                    if (row % 2 == 0) 0 else step

                var x =
                    -2 * step + phaseX + offsetX

                while (x < w) {

                    canvas.drawRect(
                        x.toFloat(),
                        y.toFloat(),
                        (x + step).toFloat(),
                        (y + step).toFloat(),
                        p
                    )

                    x += step * 2
                }

                row++
            }

            return bmp
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val w = width
            val h = height

            if (w <= 0 || h <= 0) return

            var bitmap = cachedPatternBitmap

            if (
                bitmap == null ||
                bitmap.isRecycled ||
                bitmap.width != w ||
                bitmap.height != h
            ) {
                bitmap = createPatternBitmap(w, h)
                cachedPatternBitmap = bitmap
            }

            canvas.drawBitmap(
                bitmap,
                0f,
                0f,
                paint
            )
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()

            cachedPatternBitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }

            cachedPatternBitmap = null
        }
    }
}
