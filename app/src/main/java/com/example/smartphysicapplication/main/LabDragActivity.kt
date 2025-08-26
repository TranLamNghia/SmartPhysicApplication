package com.example.smartphysicapplication.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.smartphysicapplication.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.github.sceneview.SceneView
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import kotlin.math.*

private const val TAG = "DragSimple"
enum class Gesture { NONE, DRAG_MODEL, PAN_BG }

class LabDragActivity : AppCompatActivity() {
    private val interactables = mutableListOf<ModelNode>()
    private val displayNames = mutableMapOf<ModelNode, String>()
    private val localAabbCache = mutableMapOf<ModelNode, Pair<Position, Position>>()
    private lateinit var sceneView: SceneView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    private var tableTopY = 0f
    private var minX = -0.75f; private var maxX = 0.75f
    private var minZ = -0.75f; private var maxZ = 0.75f

    // Dụng cụ
    private var tableNode: ModelNode? = null
    private var activeNode: ModelNode? = null

    private var centerX = 0f
    private var centerY = 0f
    private var centerZ = 0f

    private var yawDeg   = 180f
    private var pitchDeg = -45f
    private var radius   = 1f

    private var fovDeg   = 60f

    private val minPitchDegDown = -65f
    private val maxPitchDegDown =  -5f

    private var eyeYOffset = 0f

    // ===== Giới hạn khung di chuyển (theo Ox/Oz) =====
    private val boundsMinX = -0.5f
    private val boundsMaxX =  0.5f
    private val boundsMinZ = -1.0f
    private val boundsMaxZ =  0.5f

    private var downX = 0f
    private var downY = 0f
    private var hit0: Position? = null
    private var basisU = floatArrayOf(0f,0f,0f)
    private var basisV = floatArrayOf(0f,0f,0f)
    private var nodeOffset: Position? = null

    private var dragAnchor: Position? = null

    private lateinit var panelDetails: View
    private lateinit var tvItemName: TextView
    private lateinit var btnDone: AppCompatButton
    private lateinit var btnDelete: AppCompatButton

    private fun mulAdd(h: Position, u: FloatArray, du: Float, v: FloatArray, dv: Float): Position =
        Position(h.x + u[0]*du + v[0]*dv, h.y + u[1]*du + v[1]*dv, h.z + u[2]*du + v[2]*dv)

    private fun getLocalAabbSafe(node: ModelNode): Pair<Position, Position>? {
        fun anyToPos(v: Any): Position? {
            fun g(n: String) = v.javaClass.methods.firstOrNull {
                it.name.equals(n, true) && it.parameterTypes.isEmpty()
            }
            val getX = g("getX") ?: g("x") ?: return null
            val getY = g("getY") ?: g("y") ?: return null
            val getZ = g("getZ") ?: g("z") ?: return null
            return Position(
                (getX.invoke(v) as Number).toFloat(),
                (getY.invoke(v) as Number).toFloat(),
                (getZ.invoke(v) as Number).toFloat()
            )
        }

        fun findBbox(obj: Any?): Any? {
            if (obj == null) return null
            val m = obj.javaClass.methods.firstOrNull {
                val n = it.name.lowercase()
                it.parameterTypes.isEmpty() &&
                        (n == "getboundingbox" || n == "boundingbox" || n == "getaabb" || n == "aabb")
            }
            return m?.invoke(obj)
        }

        fun findAsset(obj: Any?): Any? {
            if (obj == null) return null
            val m = obj.javaClass.methods.firstOrNull {
                val n = it.name.lowercase()
                it.parameterTypes.isEmpty() &&
                        (n == "getasset" || n == "asset" || n == "getfilamentasset" || n == "filamentasset")
            }
            return m?.invoke(obj)
        }

        fun extractMinMax(bbox: Any): Pair<Position, Position>? {
            fun find(name1: String, name2: String, name3: String) =
                bbox.javaClass.methods.firstOrNull {
                    it.parameterTypes.isEmpty() &&
                            (it.name.equals(name1, true) || it.name.equals(name2, true) || it.name.equals(name3, true))
                }
            val minM = find("getMin", "min", "getMinPoint") ?: return null
            val maxM = find("getMax", "max", "getMaxPoint") ?: return null
            val vMin = minM.invoke(bbox) ?: return null
            val vMax = maxM.invoke(bbox) ?: return null
            val pMin = anyToPos(vMin) ?: return null
            val pMax = anyToPos(vMax) ?: return null
            return pMin to pMax
        }

        val inst = node.modelInstance
        var bbox = findBbox(inst)
        if (bbox == null) bbox = findBbox(findAsset(inst))
        val local = bbox?.let { extractMinMax(it) } ?: return null
        return local
    }

    private fun registerInteractable(node: ModelNode, name: String = node.name ?: "Unnamed") {
        interactables += node
        displayNames[node] = name

        val aabb = getLocalAabbSafe(node)
        if (aabb != null) {
            localAabbCache[node] = aabb
            Log.d(TAG, "Registered $name with localAabb=$aabb")
        } else {
            Log.w(TAG, "Registered $name WITHOUT local AABB (will use circular footprint fallback)")
        }
    }

    private fun unregisterInteractable(node: ModelNode) {
        interactables.remove(node)
        displayNames.remove(node)
        localAabbCache.remove(node)
    }

    private fun deleteAllModels() {
        for (node in interactables.toList()) {
            sceneView.removeChildNode(node)
        }
        interactables.clear()
        displayNames.clear()
        localAabbCache.clear()

        activeNode = null
        hideDetails()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lab_drag)

        val bottomSheet = findViewById<FrameLayout>(R.id.bottomSheet)
        bottomSheetBehavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)

        sceneView = findViewById(R.id.sceneView)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<AppCompatButton>(R.id.btnShare).setOnClickListener {
            Toast.makeText(this, "Đã sao chép đường dẫn chia sẻ \n vatlythongminh//TLN147", Toast.LENGTH_SHORT).show()
        }
        val btnDeleteAll = findViewById<AppCompatButton>(R.id.btnDeleteAll)
        btnDeleteAll.setOnClickListener {
            deleteAllModels()
        }

        panelDetails = findViewById(R.id.panelDetails)
        tvItemName   = findViewById(R.id.item_name)
        btnDone      = findViewById(R.id.btnDone)
        btnDelete    = findViewById(R.id.btnDelete)

        btnDone.setOnClickListener {
            activeNode = null
            hideDetails()
        }
        btnDelete.setOnClickListener {
            activeNode?.let {
                sceneView.removeChildNode(it)
                unregisterInteractable(it)
                activeNode = null
            }
            hideDetails()
        }

        // Ánh sáng
        sceneView.mainLightNode = LightNode(
            sceneView.engine, sceneView.engine.entityManager.create()
        ).apply { intensity = 80_000f }

        setupEnvironment()

        tableNode = ModelNode(modelInstance = sceneView.modelLoader.createModelInstance("models/Table.glb"), scaleToUnits = 3f)
        sceneView.addChildNode(tableNode!!)

        populateModelTray()

        // Camera nhìn xuống bàn
        updateCamera()

        setupActiveNodeDrag()
    }

    data class ModelChip(val id: String, val name: String, val iconRes: Int)

    private fun populateModelTray() {
        val tray = findViewById<LinearLayout>(R.id.modelTray)
        tray.removeAllViews()

        val items = listOf(
            ModelChip("ammeter", "Ampe kế", R.drawable.img_ammeter),
            ModelChip("battery", "Pin", R.drawable.img_battery),
            ModelChip("switch", "Công tắc điện", R.drawable.img_switch),
            ModelChip("lamp", "Bóng đèn", R.drawable.img_lamp),
            ModelChip("rheostat", "Biến trở", R.drawable.img_rheostat),
            // thêm…
        )

        val inflater = LayoutInflater.from(this)
        items.forEach { m ->
            val v = inflater.inflate(R.layout.item_model_chip, tray, false)
            val img = v.findViewById<ImageView>(R.id.imgModel)
            val tv  = v.findViewById<TextView>(R.id.tvModel)
            img.setImageResource(m.iconRes)
            tv.text = m.name

            v.setOnClickListener { addModelFromChip(m) }

            tray.addView(v)
        }
    }

    private fun addModelFromChip(m: ModelChip) {
        when (m.id) {
            "ammeter" -> addModel("models/Ammeter.glb", name = m.name, units = 0.15f)
            "battery" -> addModel("models/Battery.glb", name = m.name, units = 0.15f)
            "switch" -> addModel("models/Switch.glb", name = m.name, units = 0.15f)
            "lamp" -> addModel("models/Lamp.glb", name = m.name, units = 0.15f)
            "rheostat" -> addModel("models/Rheostat.glb", name = m.name, units = 0.15f)
            else -> return
        }
    }

    private fun addModel(assetPath: String, name: String, units: Float) {
        val node = ModelNode(
            modelInstance = sceneView.modelLoader.createModelInstance(assetPath),
            autoAnimate = false,
            scaleToUnits = units
        ).apply {
            position = Position(0f, tableTopY, 0f) // hoặc vị trí spawn mong muốn
        }
        sceneView.addChildNode(node)
        registerInteractable(node, name)
        activeNode = node
        showDetails(node)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun setupEnvironment() {
        sceneView.environment = sceneView.environmentLoader
            .createHDREnvironment("environments/studio.hdr")!!
        sceneView.environment?.indirectLight?.intensity = 20_000f
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupActiveNodeDrag() {
        var gesture = Gesture.NONE
        var lastX = 0f
        var lastY = 0f
        var suppressNextMove = false

        val scaleDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    // Chỉ zoom khi đang pan nền
                    if (gesture == Gesture.PAN_BG) {
                        radius = (radius / d.scaleFactor).coerceIn(0.35f, 3.0f)
                        updateCamera()
                    }
                    return true
                }
            }
        )

        sceneView.setOnTouchListener { _, ev ->
            scaleDetector.onTouchEvent(ev)

            when (ev.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    lastX = ev.x; lastY = ev.y
                    downX = ev.x; downY = ev.y
                    suppressNextMove = false

                    val picked = pickInteractable(ev.x, ev.y)
                    if (picked != null) {
                        activeNode = picked
                        gesture = Gesture.DRAG_MODEL
                        showDetails(picked)

                        val (ro0, rd0) = screenRayForDrag(ev.x, ev.y) ?: return@setOnTouchListener true
                        val h0 = intersectRayWithPlane(ro0, rd0, Position(0f, tableTopY, 0f), Direction(y = 1f))
                            ?: return@setOnTouchListener true

                        val (roX, rdX) = screenRayForDrag(ev.x + 1f, ev.y) ?: return@setOnTouchListener true
                        val hX = intersectRayWithPlane(roX, rdX, Position(0f, tableTopY, 0f), Direction(y = 1f)) ?: h0
                        basisU = floatArrayOf(hX.x - h0.x, hX.y - h0.y, hX.z - h0.z)

                        val (roY, rdY) = screenRayForDrag(ev.x, ev.y + 1f) ?: return@setOnTouchListener true
                        val hY = intersectRayWithPlane(roY, rdY, Position(0f, tableTopY, 0f), Direction(y = 1f)) ?: h0
                        basisV = floatArrayOf(hY.x - h0.x, hY.y - h0.y, hY.z - h0.z)

                        hit0 = h0
                        dragAnchor = h0
                        nodeOffset = Position(
                            picked.position.x - h0.x,
                            0f,
                            picked.position.z - h0.z
                        )
                    } else {
                        activeNode = null
                        gesture = Gesture.PAN_BG
                        hideDetails()
                    }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (suppressNextMove) {
                        lastX = ev.x; lastY = ev.y
                        suppressNextMove = false
                        return@setOnTouchListener true
                    }

                    when (gesture) {
                        Gesture.PAN_BG -> {
                            // Chỉ pan khi KHÔNG pinch
                            if (!scaleDetector.isInProgress) {
                                val dx = ev.x - lastX
                                val dy = ev.y - lastY
                                lastX = ev.x; lastY = ev.y
                                panScreenPlane(dx, dy)
                            }
                            true
                        }

                        Gesture.DRAG_MODEL -> {
                            val node = activeNode ?: return@setOnTouchListener true
                            if (ev.pointerCount > 1) return@setOnTouchListener true

                            // dịch chuyển nhỏ từ frame trước
                            val dx = ev.x - lastX
                            val dy = ev.y - lastY
                            lastX = ev.x; lastY = ev.y

                            val anchor = dragAnchor ?: return@setOnTouchListener true
                            val off    = nodeOffset ?: return@setOnTouchListener true

                            // TÍNH basisU/V TẠI VỊ TRÍ HIỆN TẠI (mỗi frame)
                            val (roC, rdC) = screenRayForDrag(ev.x, ev.y) ?: return@setOnTouchListener true
                            val hC = intersectRayWithPlane(roC, rdC, Position(0f, tableTopY, 0f), Direction(y = 1f)) ?: anchor

                            val (roCX, rdCX) = screenRayForDrag(ev.x + 1f, ev.y) ?: return@setOnTouchListener true
                            val hCX = intersectRayWithPlane(roCX, rdCX, Position(0f, tableTopY, 0f), Direction(y = 1f)) ?: hC
                            val uNow = floatArrayOf(hCX.x - hC.x, hCX.y - hC.y, hCX.z - hC.z)

                            val (roCY, rdCY) = screenRayForDrag(ev.x, ev.y + 1f) ?: return@setOnTouchListener true
                            val hCY = intersectRayWithPlane(roCY, rdCY, Position(0f, tableTopY, 0f), Direction(y = 1f)) ?: hC
                            val vNow = floatArrayOf(hCY.x - hC.x, hCY.y - hC.y, hCY.z - hC.z)

                            val newAnchor = mulAdd(anchor, uNow, dx, vNow, dy)
                            dragAnchor = newAnchor

                            var tx = newAnchor.x + off.x
                            var tz = newAnchor.z + off.z
                            tx = tx.coerceIn(minX, maxX)
                            tz = tz.coerceIn(minZ, maxZ)
                            node.position = Position(tx, tableTopY + 0.0005f, tz)
                            true
                        }


                        else -> true
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (gesture == Gesture.DRAG_MODEL) {
                        gesture = Gesture.PAN_BG
                    }
                    lastX = (0 until ev.pointerCount).sumOf { ev.getX(it).toDouble() }.toFloat() / ev.pointerCount
                    lastY = (0 until ev.pointerCount).sumOf { ev.getY(it).toDouble() }.toFloat() / ev.pointerCount
                    suppressNextMove = true
                    true
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    // Còn lại 1 ngón → đồng bộ lastX/lastY theo ngón còn lại
                    if (ev.pointerCount >= 2) {
                        val remainingIndex = if (ev.actionIndex == 0) 1 else 0
                        lastX = ev.getX(remainingIndex)
                        lastY = ev.getY(remainingIndex)
                    }
                    suppressNextMove = true
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (activeNode == null) hideDetails()
                    gesture = Gesture.NONE
                    suppressNextMove = false
                    true
                }

                else -> false
            }
        }
    }


    private fun screenRayForDrag(x: Float, y: Float): Pair<Position, Direction>? {
        val w = sceneView.width.takeIf { it > 0 } ?: return null
        val h = sceneView.height.takeIf { it > 0 } ?: return null

        val yaw = Math.toRadians(yawDeg.toDouble())
        val pitch = Math.toRadians(pitchDeg.toDouble())
        val cosP = cos(pitch); val sinP = sin(pitch)
        val cosY = cos(yaw);   val sinY = sin(yaw)

        val fwd = floatArrayOf((sinY * cosP).toFloat(), sinP.toFloat(), (cosY * cosP).toFloat())
        val up  = floatArrayOf(0f, 1f, 0f)
        val right = run {
            val r = cross(fwd[0], fwd[1], fwd[2], up[0], up[1], up[2])
            norm(r[0], r[1], r[2])
        }
        val camUp = run {
            val u = cross(right[0], right[1], right[2], fwd[0], fwd[1], fwd[2])
            norm(u[0], u[1], u[2])
        }

        val ndcX = (2f * x / w) - 1f
        val ndcY = 1f - (2f * y / h)

        val aspect = w.toFloat() / h.toFloat()
        val t = tan(Math.toRadians((fovDeg / 2f).toDouble())).toFloat()
        val sx = ndcX * t * aspect
        val sy = ndcY * t

        val d = norm(
            (fwd[0] + sx * right[0] + sy * camUp[0]),
            (fwd[1] + sx * right[1] + sy * camUp[1]),
            (fwd[2] + sx * right[2] + sy * camUp[2])
        )

        val rdY = if (abs(d[1]) < 1e-4f) (if (d[1] >= 0f) -1e-4f else -d[1]) else d[1]

        val eye = sceneView.cameraNode.position
        return Position(eye.x, eye.y, eye.z) to Direction(d[0], rdY, d[2])
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

    // --- Vector helpers nhỏ
    private fun cross(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): FloatArray =
        floatArrayOf(ay*bz - az*by, az*bx - ax*bz, ax*by - ay*bx)

    private fun norm(x: Float, y: Float, z: Float): FloatArray {
        val l = sqrt((x*x + y*y + z*z).toDouble()).toFloat().coerceAtLeast(1e-6f)
        return floatArrayOf(x/l, y/l, z/l)
    }

    private fun intersectRayWithPlane(
        ro: Position, rd: Direction,
        planePoint: Position, planeNormal: Direction
    ): Position? {
        val denom = planeNormal.x*rd.x + planeNormal.y*rd.y + planeNormal.z*rd.z
        if (abs(denom) < 1e-6f) return null
        val diff = Position(
            planePoint.x - ro.x,
            planePoint.y - ro.y,
            planePoint.z - ro.z
        )
        val t = (planeNormal.x*diff.x + planeNormal.y*diff.y + planeNormal.z*diff.z) / denom
        if (t < 0f) return null
        return Position(ro.x + rd.x*t, ro.y + rd.y*t, ro.z + rd.z*t)
    }

    private fun toWorldAabb(node: ModelNode, aabbLocal: Pair<Position, Position>): Pair<Position, Position> {
        val (minL, maxL) = aabbLocal
        val s = node.scale; val p = node.position
        val minW = Position(p.x + minL.x*s.x, p.y + minL.y*s.y, p.z + minL.z*s.z)
        val maxW = Position(p.x + maxL.x*s.x, p.y + maxL.y*s.y, p.z + maxL.z*s.z)
        return minW to maxW
    }

    private fun rayAabbT(ro: Position, rd: Direction, mn: Position, mx: Position): Float? {
        var tmin = (if (rd.x >= 0) (mn.x - ro.x)/rd.x else (mx.x - ro.x)/rd.x)
        var tmax = (if (rd.x >= 0) (mx.x - ro.x)/rd.x else (mn.x - ro.x)/rd.x)
        val tymin = (if (rd.y >= 0) (mn.y - ro.y)/rd.y else (mx.y - ro.y)/rd.y)
        val tymax = (if (rd.y >= 0) (mx.y - ro.y)/rd.y else (mn.y - ro.y)/rd.y)
        if (tmin > tymax || tymin > tmax) return null
        if (tymin > tmin) tmin = tymin; if (tymax < tmax) tmax = tymax
        val tzmin = (if (rd.z >= 0) (mn.z - ro.z)/rd.z else (mx.z - ro.z)/rd.z)
        val tzmax = (if (rd.z >= 0) (mx.z - ro.z)/rd.z else (mn.z - ro.z)/rd.z)
        if (tmin > tzmax || tzmin > tmax) return null
        if (tzmin > tmin) tmin = tzmin; if (tzmax < tmax) tmax = tzmax
        return if (tmax < 0f) null else if (tmin >= 0f) tmin else tmax
    }

    private fun approxPickRadius(node: ModelNode): Float {
        return 0.12f
    }

    private fun raySphereT(ro: Position, rd: Direction, c: Position, r: Float): Float? {
        val ox = ro.x - c.x; val oy = ro.y - c.y; val oz = ro.z - c.z
        val b = ox*rd.x + oy*rd.y + oz*rd.z
        val c2 = ox*ox + oy*oy + oz*oz - r*r
        val disc = b*b - c2
        if (disc < 0f) return null
        val sqrtD = sqrt(disc.toDouble()).toFloat()
        val t1 = -b - sqrtD
        val t2 = -b + sqrtD
        return when {
            t1 > 0f -> t1
            t2 > 0f -> t2
            else -> null
        }
    }

    private fun pickInteractable(x: Float, y: Float): ModelNode? {
        val ray = screenRayForDrag(x, y) ?: return null
        val (ro, rd) = ray
        Log.d(TAG, "Ray O=(${ro.x},${ro.y},${ro.z}) D=(${rd.x},${rd.y},${rd.z})")

        var bestT = Float.POSITIVE_INFINITY
        var best: ModelNode? = null

        for (n in interactables) {
            val local = localAabbCache[n]

            if (local != null) {
                // 1) Ray–AABB
                val (mnW, mxW) = toWorldAabb(n, local)
                val t = rayAabbT(ro, rd, mnW, mxW)
                if (t != null && t > 0f && t < bestT) {
                    bestT = t
                    best = n
                }
            } else {
                // 2) Fallback #1: Ray–Sphere quanh tâm node
                val rSphere = approxPickRadius(n)
                val tSphere = raySphereT(ro, rd, n.position, rSphere)
                if (tSphere != null && tSphere < bestT) {
                    bestT = tSphere
                    best = n
                } else {
                    // 3) Fallback #2: footprint tròn trên mặt phẳng bàn
                    val hit = intersectRayWithPlane(ro, rd, Position(0f, tableTopY, 0f), Direction(y = 1f))
                    if (hit != null) {
                        val dx = hit.x - n.position.x
                        val dz = hit.z - n.position.z
                        if (dx*dx + dz*dz <= rSphere*rSphere) {
                            val tPlane = (hit.y - ro.y) / rd.y
                            if (tPlane > 0f && tPlane < bestT) {
                                bestT = tPlane
                                best = n
                            }
                        }
                    }
                }
            }
        }
        Log.d(TAG, "Picked result = ${best?.let { displayNames[it] } ?: "<none>"}  t=$bestT")
        return best
    }

    private fun showDetails(node: ModelNode) {
        tvItemName.text = displayNames[node] ?: node.name ?: "Model"
        if (panelDetails.visibility != View.VISIBLE) {
            panelDetails.alpha = 0f
            panelDetails.visibility = View.VISIBLE
            panelDetails.animate().alpha(1f).setDuration(150).start()
        }
    }

    private fun hideDetails() {
        if (panelDetails.visibility == View.VISIBLE) {
            panelDetails.animate()
                .alpha(0f).setDuration(120)
                .withEndAction { panelDetails.visibility = View.GONE; panelDetails.alpha = 1f }
                .start()
        }
    }
}
