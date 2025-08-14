package com.example.smartphysicapplication.main

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.smartphysicapplication.R
import io.github.sceneview.SceneView
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import kotlin.math.*

private const val LOG_TAG = "ModelPick"

class ModelViewerNativeActivity : AppCompatActivity() {

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
    private val boundsMaxZ =  0.2f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_viewer)

        sceneView = findViewById(R.id.sceneView)
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        sceneView.mainLightNode = LightNode(
            sceneView.engine, sceneView.engine.entityManager.create()
        ).apply { intensity = 80_000f }

        loadGlb("models/Untitled.glb")
        setNodeScale(cubeName, 1.0f)
        setNodeScale(jetName,  1.0f)
        setupEnvironment()
        enablePicking()
        enableObliqueControls()

        findViewById<Button>(R.id.btnToggleCube).setOnClickListener {
            toggleChildVisibility(cubeName)
        }
        findViewById<Button>(R.id.btnToggleJet).setOnClickListener {
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
            this,
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
                    val picked = pickDeepNodeAt(ev.x, ev.y)
                    touchingModel = picked != null
                    Log.d(LOG_TAG, if (picked != null) "Touched model: ${picked.name}" else "Touched background")
                }

                MotionEvent.ACTION_POINTER_DOWN -> if (ev.pointerCount == 2) {
                    lastX = (ev.getX(0) + ev.getX(1)) / 2f
                    lastY = (ev.getY(0) + ev.getY(1)) / 2f
                }

                MotionEvent.ACTION_MOVE -> {
                    // Nếu ngón bắt đầu trên model -> KHÔNG pan (1 hoặc 2 ngón đều bỏ qua)
                    if (touchingModel) return@setOnTouchListener true

                    val isZooming = scaleDetector.isInProgress

                    if (!isZooming && ev.pointerCount == 1) {
                        val dx = ev.x - lastX
                        val dy = ev.y - lastY
                        lastX = ev.x; lastY = ev.y
                        panScreenPlane(dx, dy)
                    } else if (!isZooming && ev.pointerCount == 2) {
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
        // 1) Thử API trực tiếp nếu tồn tại
        try {
            val mPick = sceneView::class.java.methods
                .firstOrNull { it.name == "pickNode" && it.parameterTypes.size == 2 }
            val n = mPick?.invoke(sceneView, x, y)
            if (n is ModelNode) {
                Log.d("ModelPick", "Has node")
                return n
            }
        } catch (_: Throwable) {}

        // 2) Fallback: hitTest -> lấy node
        return try {
            val mHit = sceneView::class.java.methods
                .firstOrNull { it.name == "hitTest" && it.parameterTypes.size == 2 }
            val hit = mHit?.invoke(sceneView, x, y) ?: return null
            val nodeGetter = hit.javaClass.methods.firstOrNull { it.name == "getNode" || it.name == "node" }
            nodeGetter?.invoke(hit) as? ModelNode
        } catch (_: Throwable) {
            null
        }
    }

    private fun findNodeByEntity(node: ModelNode?, entity: Int): ModelNode? {
        if (node == null) return null
        if (node.entity == entity) return node
        node.childNodes.forEach { child ->
            val found = findNodeByEntity(child as? ModelNode, entity)
            if (found != null) return found
        }
        return null
    }

    private fun pickDeepNodeAt(x: Float, y: Float): ModelNode? {
        return try {
            val pickNodeM = sceneView::class.java.methods
                .firstOrNull { it.name == "pickNode" && it.parameterTypes.size == 2 }
            val picked = pickNodeM?.invoke(sceneView, x, y)
            if (picked is ModelNode) return picked

            val hitTestM = sceneView::class.java.methods
                .firstOrNull { it.name == "hitTest" && it.parameterTypes.size == 2 }
            val hr = hitTestM?.invoke(sceneView, x, y) ?: return null

            val entityGetter = hr.javaClass.methods.firstOrNull { m ->
                val n = m.name.lowercase()
                m.parameterTypes.isEmpty() && (n.contains("entity") || n.contains("filament"))
            }
            val entity = (entityGetter?.invoke(hr) as? Int) ?: 0

            if (entity != 0) {
                findNodeByEntity(rootModel, entity) ?: rootModel
            } else {
                val nodeGetter = hr.javaClass.methods.firstOrNull { m ->
                    val n = m.name.lowercase()
                    m.parameterTypes.isEmpty() && (n == "getnode" || n == "node")
                }
                nodeGetter?.invoke(hr) as? ModelNode ?: rootModel
            }
        } catch (_: Throwable) {
            null
        }
    }


    private fun enablePicking() {
        try {
            // API trực tiếp (nếu phiên bản SceneView của bạn có)
            val field = sceneView::class.java.methods.firstOrNull { it.name == "setPickingEnabled" && it.parameterTypes.size == 1 }
            if (field != null) {
                field.invoke(sceneView, true)
                Log.d(LOG_TAG, "Picking enabled via SceneView.setPickingEnabled(true)")
                return
            }
        } catch (_: Throwable) { }

        try {
            // Fallback: bật trên Filament View bên dưới
            val getView = sceneView::class.java.methods.firstOrNull { it.name == "getView" && it.parameterTypes.isEmpty() }
            val filamentView = getView?.invoke(sceneView)
            val setPick = filamentView?.javaClass?.methods
                ?.firstOrNull { it.name == "setPickingEnabled" && it.parameterTypes.size == 1 }
            setPick?.invoke(filamentView, true)
            Log.d(LOG_TAG, "Picking enabled via Filament View.setPickingEnabled(true)")
        } catch (e: Throwable) {
            Log.w(LOG_TAG, "Cannot enable picking (no API found)", e)
        }
    }

}
