package com.example.smartphysicapplication.main

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.smartphysicapplication.R
import io.github.sceneview.SceneView
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.RenderableNode
import kotlin.math.*

private const val TAG = "CustomModel"

class LabCustomActivity : AppCompatActivity() {

    private lateinit var sceneView: SceneView
    private var rootModel: ModelNode? = null
    private val partNodes = mutableMapOf<String, Node>()

    private var tableNode: ModelNode? = null

    private var centerX = 0f
    private var centerY = 0f
    private var centerZ = 0f

    private var yawDeg   = 180f
    private var pitchDeg = -45f
    private var radius   = 1f

    private var fovDeg   = 60f

    // Giới hạn yaw/pitch
    private val minPitchDegDown = -65f
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
    private val boundsMaxZ =  0.5f

    private var tableTopY = 0f

    private lateinit var panelDetails: View
    private lateinit var tvItemName: TextView
    private lateinit var btnShow: AppCompatButton
    private lateinit var btnHide: AppCompatButton

    private fun nodeName(n: Node?): String? = n?.name

    data class ModelStep(val id: String, val name: String, val iconRes: Int, val nodeKey: String)

    private val steps = listOf(
        ModelStep("cube",  "Cube",  R.drawable.ic_cube,  "Cube"),
        ModelStep("jet",  "Jet",  R.drawable.ic_cylinder,     "Jet")
    )
    private var currentStepIndex = 0
    private var pendingStepIndex: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lab_custom)

        sceneView = findViewById(R.id.sceneView)
        enablePicking()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

//        sceneView.mainLightNode = LightNode(
//            sceneView.engine, sceneView.engine.entityManager.create()
//        ).apply { intensity = 80_000f }


        panelDetails = findViewById(R.id.panelDetails)
        tvItemName = findViewById(R.id.item_name)
        btnShow = findViewById(R.id.btnShow)
        btnHide = findViewById(R.id.btnHide)

//        loadGlb("models/Untitled.glb")
//        setNodeScale(cubeName, 1.0f)
//        setNodeScale(jetName,  1.0f)
//        setupEnvironment()
//        enablePicking()
//        enableObliqueControls()

        tableNode = ModelNode(modelInstance = sceneView.modelLoader.createModelInstance("models/Table.glb"), scaleToUnits = 3f)
        sceneView.addChildNode(tableNode!!)

        loadExperimentGlb("models/Untitled.glb")
        setupEnvironment()
        currentStepIndex = 0
        populateModelTray()
        enableObliqueControls()

    }

    private fun loadExperimentGlb(assetPath: String) {
        rootModel = ModelNode(
            modelInstance = sceneView.modelLoader.createModelInstance(assetPath),
            autoAnimate = false,
            scaleToUnits = 1.0f
        ).apply {
            position = Position(0f, tableTopY, 0f)
        }
        sceneView.addChildNode(rootModel!!)
        dumpTree(rootModel)

        fun collectChildren(n: Node) {
            n.childNodes.forEach { child ->
                val key = child.name ?: return@forEach
                partNodes[key] = child
                try { child.isVisible = false } catch (_: Throwable) {
                    // fallback: nếu không có isVisible, thu nhỏ để "ẩn"
                    when (child) {
                        is RenderableNode -> child.scale = Scale(0f)
                        is ModelNode      -> child.scale = Scale(0f)
                    }
                }
                collectChildren(child)
            }
        }
        collectChildren(rootModel!!)
    }

    private fun populateModelTray() {
        val tray = findViewById<LinearLayout>(R.id.modelTray)
        tray.removeAllViews()
        val inflater = LayoutInflater.from(this)

        steps.forEachIndexed { index, step ->
            val v = inflater.inflate(R.layout.item_model_chip, tray, false)
            val img = v.findViewById<ImageView>(R.id.imgModel)
            val tv  = v.findViewById<TextView>(R.id.tvModel)
            val overlay = v.findViewById<View>(R.id.overlayLock)
//            val icLock  = v.findViewById<ImageView?>(R.id.icLock)

            img.setImageResource(step.iconRes)
            tv.text = step.name

            val enabled = index == currentStepIndex
            v.isEnabled = enabled
            v.isClickable = enabled
            overlay.visibility = if (enabled) View.GONE else View.VISIBLE
//            icLock?.visibility = if (enabled) View.GONE else View.VISIBLE
            v.alpha = if (enabled) 1f else 0.6f

            v.setOnClickListener {
                // chỉ có thể tới đây khi enabled = true
                pendingStepIndex = index
                tvItemName.text = step.name
                showDetailsForStep(step)
            }
            tray.addView(v)
        }
    }

    private fun showDetailsForStep(step: ModelStep) {
        btnShow.text = "Bố trí"
        panelDetails.alpha = 0f
        panelDetails.visibility = View.VISIBLE
        panelDetails.animate().alpha(1f).setDuration(150).start()

        btnShow.setOnClickListener {
            placeCurrentPendingStep()
        }
    }

    private fun placeCurrentPendingStep() {
        val idx = pendingStepIndex ?: return
        val step = steps[idx]
        setPartVisible(step.nodeKey, true)

        currentStepIndex = idx + 1
        pendingStepIndex = null

        populateModelTray()

        hideDetails()
    }

    private fun hideDetails() {
        if (panelDetails.visibility == View.VISIBLE) {
            panelDetails.animate()
                .alpha(0f).setDuration(120)
                .withEndAction { panelDetails.visibility = View.GONE; panelDetails.alpha = 1f }
                .start()
        }
    }

    
    private fun setPartVisible(nodeKey: String, visible: Boolean) {
        val node = partNodes[nodeKey]
        if (node == null) {
            Log.w(TAG, "Part node not found: $nodeKey")
            return
        }
        try {
            node.isVisible = visible
        } catch (_: Throwable) {
            when (node) {
                is RenderableNode -> node.scale = if (visible) Scale(1f) else Scale(0f)
                is ModelNode      -> node.scale = if (visible) Scale(1f) else Scale(0f)
                else -> Unit
            }
        }
    }

    private fun setupEnvironment() {
        sceneView.environment = sceneView.environmentLoader
            .createHDREnvironment("environments/studio.hdr")!!
        sceneView.environment?.indirectLight?.intensity = 20_000f
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
                    Log.d(TAG, "picked raw=${picked?.name}")
                    if (picked != null) {
                        val idx = stepIndexForNode(picked)
                        Log.d(TAG, "picked stepIndex=$idx current=$currentStepIndex")
                        if (idx != null && idx == currentStepIndex) {
                            pendingStepIndex = idx
                            tvItemName.text = steps[idx].name
                            showDetailsForStep(steps[idx])
                            return@setOnTouchListener true
                        }
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> if (ev.pointerCount == 2) {
                    lastX = (ev.getX(0) + ev.getX(1)) / 2f
                    lastY = (ev.getY(0) + ev.getY(1)) / 2f
                }

                MotionEvent.ACTION_MOVE -> {
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

    private fun stepIndexForNode(n: Node?): Int? {
        val key = nodeName(n) ?: return null
        val i = steps.indexOfFirst { it.nodeKey == key }
        return if (i >= 0) i else null
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

    private fun findNodeByEntity(root: Node?, entity: Int): Node? {
        if (root == null) return null
        if (root.entity == entity) return root
        root.childNodes.forEach { child ->
            val hit = findNodeByEntity(child, entity)
            if (hit != null) return hit
        }
        return null
    }

    private fun ascendToStepNode(start: Node?): Node? {
        var cur = start
        while (cur != null) {
            val n = cur.name ?: ""
            if (steps.any { it.nodeKey == n }) return cur     // dừng ở node có name trùng nodeKey
            cur = findParent(rootModel, cur)
        }
        return null
    }

    private fun findParent(root: Node?, target: Node): Node? {
        if (root == null) return null
        root.childNodes.forEach { child ->
            if (child === target) return root
            val p = findParent(child, target)
            if (p != null) return p
        }
        return null
    }

    private fun pickDeepNodeAt(x: Float, y: Float): Node? {
        // 1) Prefer SceneView.pickNode(x,y) nếu có
        try {
            val pickNodeM = sceneView::class.java.methods
                .firstOrNull { it.name == "pickNode" && it.parameterTypes.size == 2 }
            val picked = pickNodeM?.invoke(sceneView, x, y)
            if (picked is Node) return ascendToStepNode(picked)
        } catch (_: Throwable) {}

        // 2) Fallback: SceneView.hitTest(x,y)
        return try {
            val hitTestM = sceneView::class.java.methods
                .firstOrNull { it.name == "hitTest" && it.parameterTypes.size == 2 }
            val hr = hitTestM?.invoke(sceneView, x, y) ?: return null

            // a) lấy node trực tiếp nếu API có
            val nodeGetter = hr.javaClass.methods.firstOrNull { m ->
                val n = m.name.lowercase(); m.parameterTypes.isEmpty() && (n == "getnode" || n == "node")
            }
            val nObj = nodeGetter?.invoke(hr)
            if (nObj is Node) return ascendToStepNode(nObj)

            // b) nếu không có node, dùng entity để tìm trong cây đang hiển thị
            val entityGetter = hr.javaClass.methods.firstOrNull { m ->
                val n = m.name.lowercase(); m.parameterTypes.isEmpty() && (n.contains("entity") || n.contains("filament"))
            }
            val entity = (entityGetter?.invoke(hr) as? Int) ?: 0
            if (entity != 0) return ascendToStepNode(findNodeByEntity(rootModel, entity))
            null
        } catch (_: Throwable) { null }
    }

    private fun dumpTree(n: Node?, d: Int = 0) {
        if (n == null) return
        Log.d("Tree", "${" ".repeat(d*2)}- ${n::class.simpleName} name=${n.name} entity=${n.entity}")
        n.childNodes.forEach { dumpTree(it, d + 1) }
    }

    private fun enablePicking() {
        try {
            val m = sceneView::class.java.getMethod("setPickingEnabled", Boolean::class.javaPrimitiveType)
            m.invoke(sceneView, true)
            Log.d(TAG, "Picking enabled via SceneView.setPickingEnabled(true)")
            return
        } catch (_: Throwable) {}

        // Fallback: Filament View bên trong
        try {
            val getView = sceneView::class.java.methods.firstOrNull { it.name == "getView" && it.parameterTypes.isEmpty() }
            val filamentView = getView?.invoke(sceneView)
            val setPick = filamentView?.javaClass?.methods
                ?.firstOrNull { it.name == "setPickingEnabled" && it.parameterTypes.size == 1 }
            setPick?.invoke(filamentView, true)
            Log.d(TAG, "Picking enabled via FilamentView.setPickingEnabled(true)")
        } catch (_: Throwable) {
            Log.w(TAG, "Cannot enable picking (no API found)")
        }
    }

}
