package com.example.smartphysicapplication.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.smartphysicapplication.R
import io.github.sceneview.SceneView
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

private const val TAG = "DragSimple"
private enum class DragTarget { AMMETER, JET, BACKGROUND }
private var dragTarget: DragTarget = DragTarget.AMMETER

class LabDragActivity : AppCompatActivity() {
    private val interactables = mutableListOf<ModelNode>()
    private val displayNames = mutableMapOf<ModelNode, String>()
    private val localAabbCache = mutableMapOf<ModelNode, Pair<Position, Position>>()
    private lateinit var sceneView: SceneView

    private var tableTopY = 0f
    private var minX = -1f; private var maxX = 1f
    private var minZ = -1f; private var maxZ = 1f

    // Dụng cụ
    private var tableNode: ModelNode? = null
    private var ammeterNode: ModelNode? = null
    private var jetNode: ModelNode? = null
    private var activeNode: ModelNode? = null

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
    private val boundsMaxZ =  0.2f

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
        // 1) Thử trực tiếp trên instance
        var bbox = findBbox(inst)
        // 2) Nếu không có, thử qua asset
        if (bbox == null) bbox = findBbox(findAsset(inst))
        // 3) Nếu vẫn không có → đành chịu (null)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lab_drag)   // dùng layout 1 SceneView + nút Back mà bạn đã có

        sceneView = findViewById(R.id.sceneView)
        findViewById<android.widget.Button>(R.id.btnBack).setOnClickListener { finish() }

        // Ánh sáng
        sceneView.mainLightNode = LightNode(
            sceneView.engine, sceneView.engine.entityManager.create()
        ).apply { intensity = 80_000f }

        setupEnvironment()

//         --- (1) Nạp bàn nếu có (bỏ qua nếu bạn chưa tách bàn thành GLB riêng) ---
        tableNode = ModelNode(modelInstance = sceneView.modelLoader.createModelInstance("models/Table.glb"), scaleToUnits = 3f)
        sceneView.addChildNode(tableNode!!)

        tableTopY = 0f                       // nếu chưa có bàn riêng, để 0f
        minX = -1.0f; maxX = 1.0f            // biên kéo tạm thời
        minZ = -1.0f; maxZ = 1.0f

        // --- (2) Nạp Ammeter.glb (ModelNode độc lập) ---
        ammeterNode = ModelNode(
            modelInstance = sceneView.modelLoader.createModelInstance("models/Ammeter.glb"),
            autoAnimate = false,
            scaleToUnits = 0.1f
        )
        ammeterNode!!.position = Position(0f, tableTopY, 0f)
        sceneView.addChildNode(ammeterNode!!)

        jetNode = ModelNode(
            modelInstance = sceneView.modelLoader.createModelInstance("models/Jet.glb"),
            autoAnimate = false,
            scaleToUnits = 0.1f
        ).apply {
            position = Position(+0.2f, tableTopY, 0.0f)
        }
        sceneView.addChildNode(jetNode!!)

        // Đăng kí NODE
        registerInteractable(ammeterNode!!, "Ammeter")
        registerInteractable(jetNode!!, "Jet")
        Log.d(TAG, "Interactables = ${interactables.map { displayNames[it] }}")

        findViewById<Button>(R.id.btnAmmeter).setOnClickListener {
            activeNode = ammeterNode
            dragTarget = DragTarget.AMMETER
            Log.d(TAG, "Active = Ammeter")
        }

        findViewById<Button>(R.id.btnJet).setOnClickListener {
            activeNode = jetNode
            dragTarget = DragTarget.JET
            Log.d(TAG, "Active = Jet")
        }

        findViewById<Button>(R.id.btnBG).setOnClickListener {
            activeNode = null
            dragTarget = DragTarget.BACKGROUND
            Log.d(TAG, "Active = Background (pan)")
        }

        activeNode = ammeterNode

        // Camera nhìn xuống bàn
        updateCamera()

        setupActiveNodeDrag()
    }

    private fun setupEnvironment() {
        sceneView.environment = sceneView.environmentLoader
            .createHDREnvironment("environments/studio.hdr")!!
        sceneView.environment?.indirectLight?.intensity = 20_000f
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupActiveNodeDrag() {
        var dragging = false

        var lastX = 0f
        var lastY = 0f
        var suppressNextMove = false

        val scaleDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    if (dragTarget == DragTarget.BACKGROUND) {
                        radius = (radius / d.scaleFactor).coerceIn(0.35f, 3.0f)
                        updateCamera()
                    }
                    return true
                }
            }
        )

        sceneView.setOnTouchListener { _, ev ->
            // luôn chuyển qua detector trước
            scaleDetector.onTouchEvent(ev)

            when (ev.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    lastX = ev.x; lastY = ev.y
                    suppressNextMove = false

                    if (dragTarget == DragTarget.BACKGROUND) {
                        dragging = true
                    } else {
                        // Chọn model gần nhất tại điểm chạm
                        val picked = pickInteractable(ev.x, ev.y)
                        Log.d(TAG, "Picked = ${picked?.let { displayNames[it] } ?: "<none>"}")
                        activeNode = picked
                        dragging = (picked != null)
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    // Bỏ qua frame MOVE đầu tiên sau pinch để tránh giật
                    if (suppressNextMove) {
                        lastX = ev.x; lastY = ev.y
                        suppressNextMove = false
                    } else if (dragging) {

                        if (dragTarget == DragTarget.BACKGROUND) {
                            // Pan nền khi KHÔNG pinch
                            if (!scaleDetector.isInProgress) {
                                val dx = ev.x - lastX
                                val dy = ev.y - lastY
                                lastX = ev.x; lastY = ev.y
                                panScreenPlane(dx, dy)
                            }
                        } else {
                            // Kéo model theo screen-space delta (đi đúng theo tay)
                            val node = activeNode
                            if (node != null && ev.pointerCount == 1) {
                                val dxPx = ev.x - lastX
                                val dyPx = ev.y - lastY
                                lastX = ev.x; lastY = ev.y

                                val worldPerPx =
                                    (2f * radius * tan(Math.toRadians((fovDeg / 2f).toDouble())).toFloat()) /
                                            sceneView.height.toFloat()

                                val dX = dxPx * worldPerPx   // sang phải = +X
                                val dZ = dyPx * worldPerPx   // kéo xuống = +Z (khớp pan nền)

                                val newX = (node.position.x + dX).coerceIn(minX, maxX)
                                val newZ = (node.position.z + dZ).coerceIn(minZ, maxZ)
                                node.position = Position(newX, tableTopY + 0.0005f, newZ)
                            }
                        }
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    // Vào pinch: nếu đang kéo model thì dừng kéo
                    if (dragTarget != DragTarget.BACKGROUND) dragging = false
                    // lấy tâm đa ngón để lần pan kế tiếp mượt
                    lastX = (0 until ev.pointerCount).sumOf { ev.getX(it).toDouble() }.toFloat() / ev.pointerCount
                    lastY = (0 until ev.pointerCount).sumOf { ev.getY(it).toDouble() }.toFloat() / ev.pointerCount
                    suppressNextMove = true
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    // khi còn lại 1 ngón, đồng bộ lại lastX/lastY theo ngón còn lại
                    if (ev.pointerCount >= 2) {
                        val remainingIndex = if (ev.actionIndex == 0) 1 else 0
                        lastX = ev.getX(remainingIndex)
                        lastY = ev.getY(remainingIndex)
                    }
                    suppressNextMove = true
                    if (dragTarget != DragTarget.BACKGROUND) dragging = false
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    suppressNextMove = false
                }
            }

            // Trả true ở CUỐI listener để nhận tiếp sự kiện
            true
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
        val up = floatArrayOf(0f, 1f, 0f)
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

        val dir = norm(
            (fwd[0] + sx * right[0] + sy * camUp[0]),
            (fwd[1] + sx * right[1] + sy * camUp[1]),
            (fwd[2] + sx * right[2] + sy * camUp[2])
        )

// HOTFIX: nếu tia nhìn… ngước lên trời, lật Y lại để chắc chắn cắt được mặt bàn
        var dy = dir[1]
        if (dy >= 0f) {
            dy = -dy
        }
        val eye = sceneView.cameraNode.position
        return Position(eye.x, eye.y, eye.z) to Direction(dir[0], dy, dir[2])
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

    // --- Vector helpers nhỏ
    private fun cross(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): FloatArray =
        floatArrayOf(ay*bz - az*by, az*bx - ax*bz, ax*by - ay*bx)

    private fun norm(x: Float, y: Float, z: Float): FloatArray {
        val l = kotlin.math.sqrt((x*x + y*y + z*z).toDouble()).toFloat().coerceAtLeast(1e-6f)
        return floatArrayOf(x/l, y/l, z/l)
    }

    private fun intersectRayWithPlane(
        ro: Position, rd: Direction,
        planePoint: Position, planeNormal: Direction
    ): Position? {
        val denom = planeNormal.x*rd.x + planeNormal.y*rd.y + planeNormal.z*rd.z
        if (kotlin.math.abs(denom) < 1e-6f) return null
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

    // Ray–AABB: trả t gần nhất nếu giao cắt, null nếu không
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

    // Bán kính pick fallback (tuỳ chỉnh theo scale/model)
    private fun approxPickRadius(node: ModelNode): Float {
        // Tạm thời dùng giá trị “dễ trúng”: 12cm
        return 0.12f
    }

    private fun raySphereT(ro: Position, rd: Direction, c: Position, r: Float): Float? {
        val ox = ro.x - c.x; val oy = ro.y - c.y; val oz = ro.z - c.z
        val b = ox*rd.x + oy*rd.y + oz*rd.z
        val c2 = ox*ox + oy*oy + oz*oz - r*r
        val disc = b*b - c2
        if (disc < 0f) return null
        val sqrtD = kotlin.math.sqrt(disc.toDouble()).toFloat()
        val t1 = -b - sqrtD
        val t2 = -b + sqrtD
        return when {
            t1 > 0f -> t1
            t2 > 0f -> t2
            else -> null
        }
    }

    // Pick model gần nhất theo tia từ (x,y)
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
}
