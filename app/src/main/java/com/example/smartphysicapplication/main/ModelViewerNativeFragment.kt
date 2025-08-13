package com.example.smartphysicapplication.main

import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R
import io.github.sceneview.SceneView
import io.github.sceneview.environment.*
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import kotlin.math.*

class ModelViewerNativeFragment : Fragment() {

    private lateinit var sceneView: SceneView
    private var rootModel: ModelNode? = null

    private val cubeName = "Cube"
    private val jetName  = "Jet"

    private var centerX = 0f
    private var centerY = 0f
    private var centerZ = 0f

    private var yawDeg   = 180f
    private var pitchDeg = -15f
    private var radius   = 1f

    private var fovDeg   = 60f

    // Giới hạn yaw/pitch
    private val minPitchDegDown = -85f
    private val maxPitchDegDown =  -5f

    private var touchingModel = false
    private var lastX = 0f
    private var lastY = 0f
    private lateinit var scaleDetector: ScaleGestureDetector

    private var eyeYOffset = 0f

    // ===== Giới hạn khung di chuyển (theo Ox/Oz) =====
    private val boundsMinX = -0.5f
    private val boundsMaxX =  0.5f
    private val boundsMinZ = -1.0f
    private val boundsMaxZ =  -0.1f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_model_viewer, container, false)
        sceneView = root.findViewById(R.id.sceneView)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView.mainLightNode = LightNode(
            sceneView.engine, sceneView.engine.entityManager.create()
        ).apply { intensity = 80_000f }

        loadGlb("models/Untitled.glb")
        setNodeScale(cubeName, 3.0f)
        setNodeScale(jetName,  3.0f)
        setupEnvironment()
        enableObliqueControls()

        view.findViewById<Button>(R.id.btnToggleCube).setOnClickListener {
            toggleChildVisibility(cubeName)
        }
        view.findViewById<Button>(R.id.btnToggleJet).setOnClickListener {
            toggleChildVisibility(jetName)
        }
    }

    private fun loadGlb(assetPath: String) {
        val inst = sceneView.modelLoader.createModelInstance(assetPath)
        rootModel = ModelNode(modelInstance = inst, autoAnimate = false, scaleToUnits = 1.0f)
        sceneView.addChildNode(rootModel!!)
    }

    private fun setNodeScale(nodeName: String, uniform: Float) {
        rootModel?.childNodes?.firstOrNull { it.name == nodeName }?.let {
            it.scale = Scale(uniform)
        }
    }

    private fun setupEnvironment() {
        sceneView.environment = sceneView.environmentLoader
            .createHDREnvironment("environments/studio.hdr")!!
        sceneView.environment?.indirectLight?.intensity = 20_000f
    }

    private fun toggleChildVisibility(childName: String) {
        val child = rootModel?.childNodes?.firstOrNull { it.name == childName } ?: return
        child.isVisible = !child.isVisible
    }

    private fun enableObliqueControls(
        minRadius: Float = 0.3f,
        maxRadius: Float = 2f
    ) {
        sceneView.cameraManipulator = null

        scaleDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    radius = (radius / d.scaleFactor).coerceIn(minRadius, maxRadius)
                    updateCamera()
                    return true
                }
            })

        sceneView.setOnTouchListener { _, ev ->
            scaleDetector.onTouchEvent(ev)

            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = ev.x; lastY = ev.y
                    touchingModel = pickNodeAt(ev.x, ev.y) != null
                }
                MotionEvent.ACTION_POINTER_DOWN -> if (ev.pointerCount == 2) {
                    lastX = (ev.getX(0) + ev.getX(1)) / 2f
                    lastY = (ev.getY(0) + ev.getY(1)) / 2f
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!scaleDetector.isInProgress && ev.pointerCount == 1 && !touchingModel) {
                        val dx = ev.x - lastX
                        val dy = ev.y - lastY
                        lastX = ev.x; lastY = ev.y
                        panScreenPlane(dx, dy)
                    } else if (!scaleDetector.isInProgress && ev.pointerCount == 2) {
                        val cx = (ev.getX(0) + ev.getX(1)) / 2f
                        val cy = (ev.getY(0) + ev.getY(1)) / 2f
                        val dx = cx - lastX
                        val dy = cy - lastY
                        lastX = cx; lastY = cy
                        panScreenPlane(dx, dy)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    touchingModel = false
                }
            }
            true
        }

        updateCamera()
    }

    private fun panScreenPlane(dxPx: Float, dyPx: Float) {
        if (sceneView.height == 0) return

        val worldPerPx =
            (2f * radius * tan(Math.toRadians((fovDeg / 2f).toDouble())).toFloat()) /
                    sceneView.height.toFloat()

        val dX =  dxPx * worldPerPx
        val dZ = -dyPx * worldPerPx

        centerX -= dX
        centerZ += dZ

        // Giới hạn trong khung
        clampCenterXZ()

        updateCamera()
    }

    private fun clampCenterXZ() {
        centerX = centerX.coerceIn(boundsMinX, boundsMaxX)
        centerZ = centerZ.coerceIn(boundsMinZ, boundsMaxZ)
    }

    private fun updateCamera() {
        clampCenterXZ()

        pitchDeg = pitchDeg.coerceIn(minPitchDegDown, maxPitchDegDown)

        val yaw   = Math.toRadians(yawDeg.toDouble())
        val pitch = Math.toRadians(pitchDeg.toDouble())

        val cosP = cos(pitch); val sinP = sin(pitch)
        val cosY = cos(yaw);   val sinY = sin(yaw)

        val fwdX = (sinY * cosP).toFloat()
        val fwdY = (sinP).toFloat()
        val fwdZ = (cosY * cosP).toFloat()

        val eyeX = centerX - fwdX * radius
        val eyeY = eyeYOffset - fwdY * radius
        val eyeZ = centerZ - fwdZ * radius

        sceneView.cameraNode.position = Position(eyeX, eyeY, eyeZ)
        sceneView.cameraNode.lookAt(Position(centerX, centerY, centerZ), upDirection = Direction(y = 1f))
        sceneView.invalidate()
    }

    private fun pickNodeAt(x: Float, y: Float): ModelNode? {
        return try {
            val m1 = sceneView::class.java.methods.firstOrNull { it.name == "pickNode" && it.parameterTypes.size == 2 }
            val n1 = m1?.invoke(sceneView, x, y)
            if (n1 is ModelNode) return n1
            val m2 = sceneView::class.java.methods.firstOrNull { it.name == "hitTest" && it.parameterTypes.size == 2 }
            val hr = m2?.invoke(sceneView, x, y)
            if (hr != null) {
                val nodeGetter = hr.javaClass.methods.firstOrNull { it.name == "getNode" || it.name == "node" }
                nodeGetter?.invoke(hr) as? ModelNode
            } else null
        } catch (_: Throwable) { null }
    }

    private fun cross(a: FloatArray, b: FloatArray) = floatArrayOf(
        a[1]*b[2] - a[2]*b[1],
        a[2]*b[0] - a[0]*b[2],
        a[0]*b[1] - a[1]*b[0]
    )

    private fun normalize(v: FloatArray): FloatArray {
        val len = max(1e-6f, sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]))
        return floatArrayOf(v[0]/len, v[1]/len, v[2]/len)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sceneView.destroy()
    }
}
