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

        // Drag trên mặt phẳng bàn
//        enableObliqueControls()

        // Move model
//        setupDragOnTableAccurate()

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
        var dragOffset = Position(0f, 0f, 0f)
        var lastX = 0f
        var lastY = 0f

        // Thêm detector cho pinch zoom
        val scaleDetector = ScaleGestureDetector(this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    // Chỉ cho zoom khi đang ở BG
                    if (dragTarget == DragTarget.BACKGROUND) {
                        // thay đổi khoảng cách camera (radius), không đổi fov
                        val newRadius = (radius / detector.scaleFactor).coerceIn(0.35f, 3.0f)
                        // (Tuỳ chọn) zoom hướng về tâm pinch để đỡ “trôi”
                        val fpX = detector.focusX
                        val fpY = detector.focusY
                        screenRayForDrag(fpX, fpY)?.let { (ro, rd) ->
                            intersectRayWithPlane(ro, rd, Position(0f, tableTopY, 0f), Direction(y = 1f))
                                ?.let { hit ->
                                    // dịch nhẹ center về phía điểm hit khi thay đổi radius
                                    val k = 0.15f
                                    centerX = centerX * (1 - k) + hit.x * k
                                    centerZ = centerZ * (1 - k) + hit.z * k
                                }
                        }
                        radius = newRadius
                        updateCamera()
                    }
                    return true
                }
            })

        sceneView.setOnTouchListener { _, ev ->
            // Luôn chuyển sự kiện qua detector trước
            scaleDetector.onTouchEvent(ev)

            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = ev.x; lastY = ev.y

                    when (dragTarget) {
                        DragTarget.BACKGROUND -> dragging = true
                        DragTarget.AMMETER, DragTarget.JET -> {
                            val node = activeNode ?: return@setOnTouchListener true
                            val (ro, rd) = screenRayForDrag(ev.x, ev.y) ?: return@setOnTouchListener true
                            val hit = intersectRayWithPlane(ro, rd, Position(0f, tableTopY, 0f), Direction(y = 1f))
                                ?: return@setOnTouchListener true
                            dragOffset = Position(node.position.x - hit.x, 0f, node.position.z - hit.z)
                            dragging = true
                        }
                    }
                    true
                }

                // Khi có ngón thứ 2: hủy drag model để tránh xung đột, và chỉ zoom BG
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (dragTarget != DragTarget.BACKGROUND) dragging = false
                    // cập nhật tâm 2 ngón để pan BG mượt nếu muốn
                    lastX = (0 until ev.pointerCount).sumOf { ev.getX(it).toDouble() }.toFloat() / ev.pointerCount
                    lastY = (0 until ev.pointerCount).sumOf { ev.getY(it).toDouble() }.toFloat() / ev.pointerCount
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!dragging) return@setOnTouchListener true

                    when (dragTarget) {
                        DragTarget.BACKGROUND -> {
                            // Nếu đang pinch, detector.isInProgress = true → chỉ zoom, không pan
                            if (!scaleDetector.isInProgress) {
                                val dx = ev.x - lastX
                                val dy = ev.y - lastY
                                lastX = ev.x; lastY = ev.y
                                panScreenPlane(dx, dy)     // chiều pan giữ như bạn đang dùng
                            }
                        }
                        DragTarget.AMMETER, DragTarget.JET -> {
                            // Nếu có 2 ngón trong lúc kéo model → bỏ qua move (tránh “giật”)
                            if (ev.pointerCount > 1) return@setOnTouchListener true

                            val node = activeNode ?: return@setOnTouchListener true
                            val (ro, rd) = screenRayForDrag(ev.x, ev.y) ?: return@setOnTouchListener true
                            val hit = intersectRayWithPlane(ro, rd, Position(0f, tableTopY, 0f), Direction(y = 1f))
                                ?: return@setOnTouchListener true

                            var tx = hit.x + dragOffset.x
                            var tz = hit.z + dragOffset.z
                            tx = tx.coerceIn(minX, maxX)
                            tz = tz.coerceIn(minZ, maxZ)
                            node.position = Position(tx, tableTopY + 0.0005f, tz)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
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
        val eye = sceneView.cameraNode.position
        return Position(eye.x, eye.y, eye.z) to Direction(dir[0], dir[1], dir[2])
    }



    private fun enableObliqueControls(
        minRadius: Float = 0.5f,
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
//                MotionEvent.ACTION_DOWN -> {
//                    Log.d(TAG, "Touch down @(${ev.x}, ${ev.y})")
//                    lastX = ev.x; lastY = ev.y
//
//                    val node = ammeterNode
//                    val ray = screenRay(ev.x, ev.y)
//                    if (node == null || ray == null) {
//                        touchingModel = false
//                        Log.d(TAG, "Touch miss (no node or no ray)")
//                        return@setOnTouchListener true
//                    }
//
//                    val (ro, rd) = ray
//                    // 1) thử AABB nếu có (khỏi bỏ, nhưng không bắt buộc)
//                    val aabb = getWorldAabb(node)
//                    val hitByAabb = if (aabb != null) {
//                        val (mn0, mx0) = aabb
//                        val eps = 0.01f
//                        val mn = Position(mn0.x - eps, mn0.y - eps, mn0.z - eps)
//                        val mx = Position(mx0.x + eps, mx0.y + eps, mx0.z + eps)
//                        val t = rayAabbT(ro, rd, mn, mx)
//                        if (t != null) Log.d(TAG, "HIT Ammeter by AABB, t=$t")
//                        t != null
//                    } else {
//                        Log.d(TAG, "No AABB available → fallback to footprint")
//                        false
//                    }
//
//                    // 2) fallback chắc chắn: footprint vòng tròn trên mặt bàn
//                    val rPick = approxPickRadius(node)       // ~10cm * scale
//                    val hitByCircle = hitByFootprintCircle(node, ro, rd, tableTopY, rPick)
//
//                    touchingModel = (hitByAabb || hitByCircle)
//                    Log.d(TAG, if (touchingModel) "HIT Ammeter (AABB or Footprint)" else "Touch miss (background)")
//                    return@setOnTouchListener true
//                }


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

    // --- Vector helpers nhỏ
    private fun cross(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): FloatArray =
        floatArrayOf(ay*bz - az*by, az*bx - ax*bz, ax*by - ay*bx)

    private fun norm(x: Float, y: Float, z: Float): FloatArray {
        val l = kotlin.math.sqrt((x*x + y*y + z*z).toDouble()).toFloat().coerceAtLeast(1e-6f)
        return floatArrayOf(x/l, y/l, z/l)
    }

    // Tính tia từ pixel (x,y) với camera yaw/pitch/fov hiện tại
    private fun screenRay(x: Float, y: Float): Pair<Position, Direction>? {
        // 1) Ưu tiên gọi API nội bộ nếu tồn tại
        try {
            val camField = sceneView::class.java.methods.firstOrNull {
                it.name.equals("getCamera", true) && it.parameterTypes.isEmpty()
            }
            val cam = camField?.invoke(sceneView)
            val m = cam?.javaClass?.methods?.firstOrNull {
                it.name.lowercase().contains("screen") &&
                        it.name.lowercase().contains("ray") &&
                        it.parameterTypes.size == 2
            }
            if (m != null) {
                val ray = m.invoke(cam, x, y)
                val oM = ray.javaClass.methods.first { it.name.lowercase().contains("origin") }
                val dM = ray.javaClass.methods.first { it.name.lowercase().contains("direction") }
                val o = oM.invoke(ray)
                val d = dM.invoke(ray)
                fun anyToPos(v: Any): Position {
                    fun g(n: String) = v.javaClass.methods.first { it.name.equals(n, true) && it.parameterTypes.isEmpty() }
                    return Position(
                        (g("getX").invoke(v) as Number).toFloat(),
                        (g("getY").invoke(v) as Number).toFloat(),
                        (g("getZ").invoke(v) as Number).toFloat()
                    )
                }
                fun anyToDir(v: Any): Direction {
                    fun g(n: String) = v.javaClass.methods.first { it.name.equals(n, true) && it.parameterTypes.isEmpty() }
                    val dx = (g("getX").invoke(v) as Number).toFloat()
                    val dy = (g("getY").invoke(v) as Number).toFloat()
                    val dz = (g("getZ").invoke(v) as Number).toFloat()
                    val len = kotlin.math.sqrt((dx*dx + dy*dy + dz*dz).toDouble()).toFloat().coerceAtLeast(1e-6f)
                    return Direction(dx/len, dy/len, dz/len)
                }
                return anyToPos(o) to anyToDir(d)
            }
        } catch (_: Throwable) { /* ignore */ }

        // 2) Fallback: tự tính theo yaw/pitch/fov hiện tại (đồng bộ dấu với pan của bạn)
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

        // NDC: đảo dấu X cho khớp với pan (bạn đã đảo ở panScreenPlane)
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
        val eye = sceneView.cameraNode.position
        return Position(eye.x, eye.y, eye.z) to Direction(dir[0], dir[1], dir[2])
    }


    // Lấy AABB local của 1 ModelNode (min,max) bằng reflection
    private fun getLocalAabb(node: ModelNode): Pair<Position, Position> {
        val inst = node.modelInstance
        val bboxM = inst.javaClass.methods.first { it.name.contains("bounding", true) }
        val bbox = bboxM.invoke(inst)
        val minM = bbox.javaClass.methods.first { it.name.contains("min", true) }
        val maxM = bbox.javaClass.methods.first { it.name.contains("max", true) }
        val vMin = minM.invoke(bbox); val vMax = maxM.invoke(bbox)

        fun anyToPos(v: Any): Position {
            fun g(n: String) = v.javaClass.methods.first { it.name.equals(n, true) && it.parameterTypes.isEmpty() }
            return Position(
                (g("getX").invoke(v) as Number).toFloat(),
                (g("getY").invoke(v) as Number).toFloat(),
                (g("getZ").invoke(v) as Number).toFloat()
            )
        }
        return anyToPos(vMin) to anyToPos(vMax)
    }

    // Quy đổi AABB local -> world (giả định không rotation)
    private fun getWorldAabb(node: ModelNode): Pair<Position, Position>? {
        return try {
            val inst = node.modelInstance
            val bboxM = inst.javaClass.methods.first { it.name.contains("bounding", true) }
            val bbox = bboxM.invoke(inst)
            val minM = bbox.javaClass.methods.first { it.name.contains("min", true) }
            val maxM = bbox.javaClass.methods.first { it.name.contains("max", true) }
            val vMin = minM.invoke(bbox); val vMax = maxM.invoke(bbox)
            fun anyToPos(v: Any): Position {
                fun g(n: String) = v.javaClass.methods.first { it.name.equals(n, true) && it.parameterTypes.isEmpty() }
                return Position(
                    (g("getX").invoke(v) as Number).toFloat(),
                    (g("getY").invoke(v) as Number).toFloat(),
                    (g("getZ").invoke(v) as Number).toFloat()
                )
            }
            val minL = anyToPos(vMin); val maxL = anyToPos(vMax)
            val s = node.scale; val p = node.position
            Position(p.x + minL.x*s.x, p.y + minL.y*s.y, p.z + minL.z*s.z) to
                    Position(p.x + maxL.x*s.x, p.y + maxL.y*s.y, p.z + maxL.z*s.z)
        } catch (_: Throwable) { null }
    }

    // Ray–AABB: trả t gần nhất nếu có giao cắt
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

    private fun hitByFootprintCircle(
        node: ModelNode,
        ro: Position, rd: Direction,
        tableY: Float,
        radius: Float
    ): Boolean {
        val hit = intersectRayWithPlane(ro, rd, Position(0f, tableY, 0f), Direction(y = 1f)) ?: return false
        val pos = node.position
        val dx = hit.x - pos.x
        val dz = hit.z - pos.z
        val d2 = dx*dx + dz*dz
        Log.d(TAG, "Footprint check: hit=(${hit.x},${hit.y},${hit.z}), node=(${pos.x},${pos.y},${pos.z}), r=$radius, d2=$d2")
        return d2 <= radius*radius
    }

    private fun approxPickRadius(node: ModelNode): Float {
        // nếu bạn scaleToUnits=0.1f thì chọn bán kính ~ 8-12cm là hợp lý
        val s = node.scale
        val base = 0.50f
        // tăng nhẹ theo scale lớn nhất trục để an toàn
        val k = maxOf(s.x, maxOf(s.y, s.z))
        return base * k
    }
}
