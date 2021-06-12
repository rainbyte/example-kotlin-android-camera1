package ar.net.rainbyte.examplecamera1

import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PreviewCallback
import android.util.AttributeSet
import android.util.Log
import android.view.*
import java.io.IOException
import java.util.*

class MainView : SurfaceView, SurfaceHolder.Callback, PreviewCallback {

    companion object {
        private const val MIN_SIZE = 320 * 240
        private const val TAG = "CameraView"
    }

    private var camera: Camera? = null

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context)
    }

    constructor(context: Context) : super(context) {
        initialize(context)
    }

    private fun initialize(context: Context) {
        // Add SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        this.holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i("LifeCycle", "surfaceCreated")

        // Enable force redraw via invalidate
        setWillNotDraw(false)

        // Get camera instance
        val opened = openCameraInstance()
        if (!opened) return

        // The Surface has been created, now tell the camera where to draw the
        // preview.
        try {
            camera!!.setPreviewCallback(this)
            camera!!.setPreviewDisplay(holder)
        } catch (e: IOException) {
            Log.d(TAG, "Error setting camera preview: " + e.message)
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder, format: Int, width: Int,
        height: Int
    ) {
        Log.i("LifeCycle", "surfaceChanged")

        // Get camera instance
        if (camera == null) {
            val opened = openCameraInstance()
            if (!opened) return
        }

        // Update parameters
        updateView()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i("LifeCycle", "surfaceDestroyed")
        stopView()
        releaseCameraInstance()
    }

    private fun openCameraInstance(): Boolean {

        // Try to open back-facing camera
        try {
            camera = Camera.open()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera:" + e.message)
        }

        // Try to open other camera if present
        if (camera == null) {
            val cameraCount = Camera.getNumberOfCameras()
            if (cameraCount > 0) {
                // There isn't a backfacing camera
                try {
                    camera = Camera.open(0)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open camera:" + e.message)
                }
            } else {
                camera = null
            }
        }

        // Close when there isn't a camera
        return if (camera == null) {
            Log.e(TAG, "The device doesn't have a camera.")
            false
        } else {
            true
        }
    }

    private fun releaseCameraInstance() {
        if (camera != null) {
            camera!!.release()
            camera = null
        }
    }

    private fun updateView() {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (this.holder.surface == null) {
            // preview surface does not exist
            return
        }

        // Check for camera
        if (camera == null) {
            Log.e(TAG, "Camera is null!")
            return
        }

        // stop preview before making changes
        try {
            stopView()
        } catch (e: Exception) {
            // ignore: tried to stop a non-existent preview
            Log.i(TAG, "Tried to stop a non-existent preview")
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        val mParams = camera!!.parameters
        val minSize = getMinCameraSize(mParams)
        mParams.setPreviewSize(minSize!!.width, minSize.height)

        // Rotation support
        val info = CameraInfo()
        Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, info)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val rotation = display.rotation
        val degrees: Int
        degrees = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        var displayOrientation: Int
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            displayOrientation = (info.orientation + degrees) % 360
            displayOrientation = (360 - displayOrientation) % 360 // compensate the mirror
        } else {  // back-facing
            displayOrientation = (info.orientation - degrees + 360) % 360
        }
        Log.i(
            "ROTATION",
            String.format(
                "New: degrees = %s -- info.orientation = %s -- displayOrientation = %s",
                degrees,
                info.orientation,
                displayOrientation
            )
        )
        camera!!.setDisplayOrientation(displayOrientation)

        // start preview with new settings
        try {
            camera!!.setPreviewDisplay(this.holder)
            startView()
        } catch (e: Exception) {
            Log.d(TAG, "Error starting camera preview: " + e.message)
        }
    }

    fun reopen() {
        stopView()
        releaseCameraInstance()
        val opened = openCameraInstance()
        if (opened) updateView()
    }

    fun startView() {
        if (camera != null) {
            // Enable onPreviewFrame
            camera!!.setPreviewCallback(this)

            camera!!.startPreview()
        }
    }

    fun stopView() {
        if (camera != null) {
            // Disable onPreviewFrame
            camera!!.setPreviewCallback(null)

            camera!!.stopPreview()
        }
    }

    private fun getMinCameraSize(parameters: Camera.Parameters): Camera.Size? {
        /* Obtain camera sizes */
        val camSizes = parameters.supportedPreviewSizes.toTypedArray()

        /* Sort camera sizes */
        val camSizeComparator: Comparator<Camera.Size?> = object : Comparator<Camera.Size?> {
            override fun compare(a: Camera.Size?, b: Camera.Size?): Int {
                return a!!.width * a.height - b!!.width * b.height
            }
        }
        Arrays.sort(camSizes, camSizeComparator)

        /* Find and return minimum size greater than MIN_AREA */
        var camSize: Camera.Size? = null
        var camSizeArea = 0
        var i = 0
        while (i < camSizes.size && camSizeArea < MIN_SIZE) {
            camSize = camSizes[i]
            camSizeArea = camSize.width * camSize.height
            i++
        }
        return camSize
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        // consume frame
    }
}
