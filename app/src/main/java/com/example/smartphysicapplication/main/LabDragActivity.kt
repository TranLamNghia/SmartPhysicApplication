package com.example.smartphysicapplication.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.app.AppCompatActivity
import com.example.smartphysicapplication.R
import io.github.sceneview.SceneView
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import kotlin.math.abs
import kotlin.math.tan
import io.github.sceneview.node.Node

private const val LOG_TAG = "LabDrag"

class LabDragActivity : AppCompatActivity() {

    private lateinit var sceneView: SceneView
    private var rootModel: ModelNode? = null
    private var tableNode: Node? = null

    // ====== Drag state ======
    private var selectedNode: ModelNode? = null
    private var dragOffset: Position? = null     // offset XZ giữa tâm node và điểm chạm trên mặt phẳng
    private var isDragging = false

    // ====== Bàn ======
    private var tableTopY: Float = 0.0f          // y của mặt phẳng bàn (top surface)
    private var tableMinX = -0.8f
    private var tableMaxX =  0.8f
    private var tableMinZ = -0.6f
    private var tableMaxZ =  0.8f
    private val tableNormal = Direction(y = 1.0f)

    // ====== Camera (tuỳ chọn: bạn có thể copy y nguyên từ Activity cũ) ======
    private var centerX = 0f
    private var centerY = 0f
    private var centerZ = 0f
    private var radius   = 1.0f
    private var fovDeg   = 60f
    private lateinit var scaleDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lab_drag)

        sceneView = findViewById(R.id.sceneView)
        findViewById<android.widget.Button>(R.id.btnBack).setOnClickListener { finish() }

        sceneView.isFocusable = true
        sceneView.isFocusableInTouchMode = true
        sceneView.requestFocus()


        // Ánh sáng
        sceneView.mainLightNode = LightNode(
            sceneView.engine, sceneView.engine.entityManager.create()
        ).apply { intensity = 80_000f }

        setupEnvironment()
        enablePicking() // tái sử dụng hàm dưới (reflection) để chắc ăn

        // Nạp GLB lớn của bạn (chứa "wood table" + các model con)
        loadGlb("models/Untitled.glb") {
            // Sau khi nạp xong, tìm node "wood table" và tính mặt phẳng + biên kéo
            resolveTableAndBounds()
            setupTouchForDrag()
        }
        dumpChildrenNames(rootModel)

        // (Tùy chọn) detector để giữ tính năng pinch-to-zoom cho camera
        scaleDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    radius = (radius / d.scaleFactor).coerceIn(0.3f, 3.0f)
                    updateCamera()
                    return true
                }
            }
        )
        updateCamera()
    }

    // ================== NẠP MODEL ROOT ==================

    private fun loadGlb(assetPath: String, onLoaded: () -> Unit) {
        // API giống với Activity cũ của bạn
        val inst = sceneView.modelLoader.createModelInstance(assetPath)
        rootModel = ModelNode(modelInstance = inst, autoAnimate = false, scaleToUnits = 1.0f)
        sceneView.addChildNode(rootModel!!)
        onLoaded()
    }

    // ================== ENV + PICKING (tái sử dụng) ==================

    private fun setupEnvironment() {
        sceneView.environment = sceneView.environmentLoader
            .createHDREnvironment("environments/studio.hdr")!!
        sceneView.environment?.indirectLight?.intensity = 20_000f
    }

    private fun enablePicking() {
        try {
            val m = sceneView::class.java.methods
                .firstOrNull { it.name == "setPickingEnabled" && it.parameterTypes.size == 1 }
            if (m != null) {
                m.invoke(sceneView, true)
                Log.d(LOG_TAG, "Picking enabled via SceneView.setPickingEnabled(true)")
                return
            }
        } catch (_: Throwable) { }

        try {
            val getView = sceneView::class.java.methods
                .firstOrNull { it.name == "getView" && it.parameterTypes.isEmpty() }
            val filamentView = getView?.invoke(sceneView)
            val setPick = filamentView?.javaClass?.methods
                ?.firstOrNull { it.name == "setPickingEnabled" && it.parameterTypes.size == 1 }
            setPick?.invoke(filamentView, true)
            Log.d(LOG_TAG, "Picking enabled via Filament View.setPickingEnabled(true)")
        } catch (e: Throwable) {
            Log.w(LOG_TAG, "Cannot enable picking", e)
        }
    }

    // ================== XÁC ĐỊNH NODE ==================
    private fun dumpChildrenNames(parent: ModelNode?, indent: String = "") {
        parent?.childNodes?.forEach { n ->
            Log.d("Nodes", indent + "${n.name ?: "<noname>"}  [${n.javaClass.simpleName}]")
            dumpChildrenNames(n as? ModelNode, indent + "  ")
        }
    }


    // ================== XÁC ĐỊNH "WOOD TABLE" & BIÊN ==================
    // Cố gắng lấy AABB ở toạ độ thế giới cho bất kỳ Node nào qua reflection.
    // Trả về Pair(min, max) hoặc null nếu không có API phù hợp.
    /** Trả về AABB thế giới (min,max) cho mọi Node nếu có thể. */
    private fun getAnyNodeWorldAabb(node: Node): Pair<Position, Position>? {
        // a) Nếu là ModelNode: dùng AABB local + scale + position
        if (node is ModelNode) {
            return try {
                val (minL, maxL) = getNodeLocalAabb(node)   // đã có sẵn trong file của bạn
                val s = node.scale
                val p = node.position
                val min = Position(p.x + minL.x * s.x, p.y + minL.y * s.y, p.z + minL.z * s.z)
                val max = Position(p.x + maxL.x * s.x, p.y + maxL.y * s.y, p.z + maxL.z * s.z)
                min to max
            } catch (_: Throwable) { null }
        }

        // b) RenderableNode/Node khác: thử reflection tìm bounding box (min/max)
        val m = node.javaClass.methods.firstOrNull {
            val n = it.name.lowercase()
            it.parameterTypes.isEmpty() && (n.contains("bounding") || n.contains("aabb"))
        } ?: return null

        val bbox = try { m.invoke(node) } catch (_: Throwable) { null } ?: return null

        val minM = bbox.javaClass.methods.firstOrNull { it.name.equals("getMin", true) || it.name.equals("min", true) }
            ?: return null
        val maxM = bbox.javaClass.methods.firstOrNull { it.name.equals("getMax", true) || it.name.equals("max", true) }
            ?: return null

        val vMin = minM.invoke(bbox)
        val vMax = maxM.invoke(bbox)

        fun anyToPos(v: Any): Position {
            fun get(name: String) =
                v.javaClass.methods.first { it.name.equals(name, true) && it.parameterTypes.isEmpty() }
            return Position(
                (get("getX").invoke(v) as Number).toFloat(),
                (get("getY").invoke(v) as Number).toFloat(),
                (get("getZ").invoke(v) as Number).toFloat()
            )
        }

        val minW = anyToPos(vMin)
        val maxW = anyToPos(vMax)
        // Nhiều bản SceneView trả luôn AABB **world** cho RenderableNode → dùng trực tiếp.
        // Nếu bạn thấy không trúng, bạn có thể cộng thêm node.position/scale tương tự ModelNode.
        return minW to maxW
    }


    private fun resolveTableAndBounds() {
        tableNode = rootModel?.childNodes
            ?.firstOrNull { it.name.equals("WoodTable", ignoreCase = true) }

        if (tableNode == null) {
            Log.w(LOG_TAG, "Không tìm thấy node 'WoodTable' – dùng default plane y=0")
            tableTopY = 0f
            return
        }

        // 1) topY
        tableTopY = try {
            // Nếu là ModelNode, dùng AABB local
            if (tableNode is ModelNode) {
                val model = tableNode as ModelNode
                val (minL, maxL) = getNodeLocalAabb(model)
                val s = model.scale
                val p = model.position
                p.y + maxL.y * s.y
            } else {
                // Thử tìm bounding box trên renderable (reflection)
                val bb = getAnyNodeWorldAabb(tableNode!!)
                if (bb != null) {
                    val (mn, mx) = bb
                    mx.y
                } else {
                    tableNode!!.position.y // fallback
                }
            }
        } catch (_: Throwable) {
            tableNode!!.position.y
        }

        // 2) Biên XZ
        try {
            val bb = if (tableNode is ModelNode) {
                val m = tableNode as ModelNode
                val (minL, maxL) = getNodeLocalAabb(m)
                val s = m.scale; val p = m.position
                Position(p.x + minL.x * s.x, p.y + minL.y * s.y, p.z + minL.z * s.z) to
                        Position(p.x + maxL.x * s.x, p.y + maxL.y * s.y, p.z + maxL.z * s.z)
            } else {
                getAnyNodeWorldAabb(tableNode!!)  // có thể null
            }

            if (bb != null) {
                val (mn, mx) = bb
                tableMinX = mn.x; tableMaxX = mx.x
                tableMinZ = mn.z; tableMaxZ = mx.z
            }
        } catch (_: Throwable) {
            /* giữ default nếu thất bại */
        }

        Log.d(LOG_TAG, "TableTopY=$tableTopY  X[$tableMinX,$tableMaxX]  Z[$tableMinZ,$tableMaxZ]")
    }



    /**
     * Cố gắng lấy AABB local của node qua reflection.
     * Trả về Pair(min, max) với Position thay cho vector.
     */
    private fun getNodeLocalAabb(node: ModelNode): Pair<Position, Position> {
        val instField = node.javaClass.methods.firstOrNull { it.name.lowercase().contains("modelinstance") }
        val inst = instField?.invoke(node)
        // Tuỳ phiên bản: tìm method có "getBoundingBox"/"boundingBox"/"aabb"
        val bboxM = inst?.javaClass?.methods?.firstOrNull {
            val n = it.name.lowercase()
            it.parameterTypes.isEmpty() && (n.contains("bounding") || n.contains("aabb"))
        } ?: throw IllegalStateException("No bbox API")

        val bbox = bboxM.invoke(inst)
        // Tìm min/max vector
        val minM = bbox.javaClass.methods.first { it.name.lowercase().contains("min") }
        val maxM = bbox.javaClass.methods.first { it.name.lowercase().contains("max") }
        val vMin = minM.invoke(bbox)
        val vMax = maxM.invoke(bbox)
        fun anyToPos(v: Any): Position {
            val getX = v.javaClass.methods.first { it.name.lowercase().endsWith("x") && it.parameterTypes.isEmpty() }
            val getY = v.javaClass.methods.first { it.name.lowercase().endsWith("y") && it.parameterTypes.isEmpty() }
            val getZ = v.javaClass.methods.first { it.name.lowercase().endsWith("z") && it.parameterTypes.isEmpty() }
            return Position(
                (getX.invoke(v) as Number).toFloat(),
                (getY.invoke(v) as Number).toFloat(),
                (getZ.invoke(v) as Number).toFloat()
            )
        }
        return anyToPos(vMin) to anyToPos(vMax)
    }

    // ================== DRAG ON TABLE ==================

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchForDrag() {
        sceneView.setOnTouchListener { _, ev ->
            scaleDetector.onTouchEvent(ev) // không bắt buộc, chỉ để giữ pinch-zoom camera

            val x = ev.x
            val y = ev.y

            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val picked = pickDeepNodeAt(x, y)

                    // Nếu pick == null hoặc là root, thử ép chọn child gần nhất bằng AABB:
                    val finalPicked = when {
                        picked != null && picked !== rootModel -> picked
                        else -> {
                            val ray = screenPointToRay(x, y)
                            if (ray != null) pickByAabb(ray.first, ray.second) else null
                        }
                    }
                    Log.d(LOG_TAG, "Picked node: ${picked?.name} (raw), final: ${finalPicked?.name}")

                    // Không cho kéo WoodTable
                    if (finalPicked != null && finalPicked !== tableNode) {
                        selectedNode = finalPicked
                        isDragging = true

                        val (ro, rd) = screenPointToRay(x, y) ?: (null to null)
                        if (ro != null && rd != null) {
                            val hit = intersectRayWithPlane(
                                rayOrigin = ro, rayDir = rd,
                                planePoint = Position(0f, tableTopY, 0f),
                                planeNormal = tableNormal
                            )
                            val nodePos = selectedNode!!.position
                            dragOffset = if (hit != null)
                                Position(nodePos.x - hit.x, 0f, nodePos.z - hit.z)
                            else Position(0f, 0f, 0f)
                        } else {
                            dragOffset = Position(0f, 0f, 0f)
                        }
                    } else {
                        selectedNode = null
                        isDragging = false
                        dragOffset = null
                    }
                }


                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging || selectedNode == null) return@setOnTouchListener true

                    val (ro, rd) = screenPointToRay(x, y) ?: return@setOnTouchListener true
                    val p = intersectRayWithPlane(
                        rayOrigin = ro,
                        rayDir = rd,
                        planePoint = Position(0f, tableTopY, 0f),
                        planeNormal = tableNormal
                    ) ?: return@setOnTouchListener true

                    val off = dragOffset ?: Position(0f, 0f, 0f)
                    var targetX = p.x + off.x
                    var targetZ = p.z + off.z

                    // Giới hạn trong mặt bàn
                    targetX = targetX.coerceIn(tableMinX, tableMaxX)
                    targetZ = targetZ.coerceIn(tableMinZ, tableMaxZ)

                    // (Tuỳ chọn) tránh chồng lấn: bạn có thể kiểm tra AABB với các node khác ở đây

                    // Đặt y = mặt bàn (+ epsilon nhỏ)
                    val epsilon = 0.0005f
                    selectedNode!!.position = Position(targetX, tableTopY + epsilon, targetZ)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    selectedNode = null
                    dragOffset = null
                }
            }
            true
        }
    }

    // ===== Ray-Plane utils =====

    private fun intersectRayWithPlane(
        rayOrigin: Position,
        rayDir: Direction,           // assume normalized
        planePoint: Position,
        planeNormal: Direction       // assume normalized
    ): Position? {
        val denom = planeNormal.x * rayDir.x + planeNormal.y * rayDir.y + planeNormal.z * rayDir.z
        if (abs(denom) < 1e-6f) return null
        val diff = Position(
            planePoint.x - rayOrigin.x,
            planePoint.y - rayOrigin.y,
            planePoint.z - rayOrigin.z
        )
        val t = (planeNormal.x * diff.x + planeNormal.y * diff.y + planeNormal.z * diff.z) / denom
        if (t < 0f) return null
        return Position(
            rayOrigin.x + rayDir.x * t,
            rayOrigin.y + rayDir.y * t,
            rayOrigin.z + rayDir.z * t
        )
    }

    /**
     * Cố gắng lấy tia từ điểm màn hình. Ưu tiên API camera.screenPointToRay nếu có.
     */
    private fun screenPointToRay(x: Float, y: Float): Pair<Position, Direction>? {
        // 1) Thử camera.screenPointToRay(x, y)
        try {
            val camField = sceneView::class.java.methods.firstOrNull { it.name.lowercase().contains("camera") && it.parameterTypes.isEmpty() }
            val cam = camField?.invoke(sceneView)
            val m = cam?.javaClass?.methods?.firstOrNull {
                it.name.lowercase().contains("screen") && it.name.lowercase().contains("ray") && it.parameterTypes.size == 2
            }
            if (m != null) {
                val ray = m.invoke(cam, x, y)
                // Tìm origin/direction
                val oM = ray.javaClass.methods.first { it.name.lowercase().contains("origin") }
                val dM = ray.javaClass.methods.first { it.name.lowercase().contains("direction") }
                val o = oM.invoke(ray)
                val d = dM.invoke(ray)
                fun anyToPos(v: Any): Position {
                    val getX = v.javaClass.methods.first { it.name.lowercase().endsWith("x") && it.parameterTypes.isEmpty() }
                    val getY = v.javaClass.methods.first { it.name.lowercase().endsWith("y") && it.parameterTypes.isEmpty() }
                    val getZ = v.javaClass.methods.first { it.name.lowercase().endsWith("z") && it.parameterTypes.isEmpty() }
                    return Position(
                        (getX.invoke(v) as Number).toFloat(),
                        (getY.invoke(v) as Number).toFloat(),
                        (getZ.invoke(v) as Number).toFloat()
                    )
                }
                fun anyToDir(v: Any): Direction {
                    val getX = v.javaClass.methods.first { it.name.lowercase().endsWith("x") && it.parameterTypes.isEmpty() }
                    val getY = v.javaClass.methods.first { it.name.lowercase().endsWith("y") && it.parameterTypes.isEmpty() }
                    val getZ = v.javaClass.methods.first { it.name.lowercase().endsWith("z") && it.parameterTypes.isEmpty() }
                    val dx = (getX.invoke(v) as Number).toFloat()
                    val dy = (getY.invoke(v) as Number).toFloat()
                    val dz = (getZ.invoke(v) as Number).toFloat()
                    val len = kotlin.math.sqrt(dx*dx + dy*dy + dz*dz)
                    return Direction(dx/len, dy/len, dz/len)
                }
                return anyToPos(o) to anyToDir(d)
            }
        } catch (_: Throwable) {}

        // 2) Fallback: ước lượng origin/direction từ cameraNode + fov (đủ chính xác cho drag)
        val camPos = sceneView.cameraNode.position
        val viewH = sceneView.height.takeIf { it > 0 } ?: return null

        // chuyển (x, y) màn hình -> NDC (-1..1)
        val ndcX = (2f * x / sceneView.width) - 1f
        val ndcY = 1f - (2f * y / viewH)

        // hướng forward từ cameraNode -> center
        val fwd = Direction(
            x = centerX - camPos.x,
            y = centerY - camPos.y,
            z = centerZ - camPos.z
        ).normalized()

        // xz plane simple ray (xấp xỉ): dùng fov để mở rộng theo ndc
        val worldPerPx = (2f * radius * tan(Math.toRadians((fovDeg / 2f).toDouble())).toFloat()) / viewH
        val dx = ndcX * worldPerPx * viewH
        val dz = -ndcY * worldPerPx * viewH

        val dir = Direction(fwd.x + dx, fwd.y, fwd.z + dz).normalized()
        return camPos to dir
    }

    // ================== PICK (tái sử dụng từ Activity cũ) ==================

    private fun pickDeepNodeAt(x: Float, y: Float): ModelNode? {
        // 1) Ưu tiên API chính thức nếu có
        try {
            val pickNodeM = sceneView::class.java.methods
                .firstOrNull { it.name == "pickNode" && it.parameterTypes.size == 2 }
            val picked = pickNodeM?.invoke(sceneView, x, y)
            if (picked is ModelNode) return picked
        } catch (_: Throwable) {}

        try {
            val hitTestM = sceneView::class.java.methods
                .firstOrNull { it.name == "hitTest" && it.parameterTypes.size == 2 }
            val hr = hitTestM?.invoke(sceneView, x, y)
            if (hr != null) {
                // Tìm node từ hit result nếu có
                val nodeGetter = hr.javaClass.methods.firstOrNull { m ->
                    val n = m.name.lowercase()
                    m.parameterTypes.isEmpty() && (n == "getnode" || n == "node")
                }
                val n = nodeGetter?.invoke(hr) as? ModelNode
                if (n != null) return n

                val entityGetter = hr.javaClass.methods.firstOrNull { m ->
                    val n = m.name.lowercase()
                    m.parameterTypes.isEmpty() && (n.contains("entity") || n.contains("filament"))
                }
                val entity = (entityGetter?.invoke(hr) as? Int) ?: 0
                if (entity != 0) {
                    findNodeByEntity(rootModel, entity)?.let { return it }
                }
            }
        } catch (_: Throwable) {}

        // 2) Fallback chắc chắn: raycast AABB tất cả child để lấy node gần nhất
        val ray = screenPointToRay(x, y) ?: return null
        return pickByAabb(ray.first, ray.second)
    }

    // Raycast AABB tất cả ModelNode con (bỏ root và table)
    private fun pickByAabb(rayOrigin: Position, rayDir: Direction): ModelNode? {
        var bestT = Float.POSITIVE_INFINITY
        var bestNode: ModelNode? = null

        fun rayAabbT(ro: Position, rd: Direction, mn: Position, mx: Position): Float? {
            var tmin = (if (rd.x >= 0) (mn.x - ro.x)/rd.x else (mx.x - ro.x)/rd.x)
            var tmax = (if (rd.x >= 0) (mx.x - ro.x)/rd.x else (mn.x - ro.x)/rd.x)
            val tymin = (if (rd.y >= 0) (mn.y - ro.y)/rd.y else (mx.y - ro.y)/rd.y)
            val tymax = (if (rd.y >= 0) (mx.y - ro.y)/rd.y else (mn.y - ro.y)/rd.y)
            if (tmin.isNaN() || tymin.isNaN()) return null
            if (tmin > tymax || tymin > tmax) return null
            if (tymin > tmin) tmin = tymin; if (tymax < tmax) tmax = tymax
            val tzmin = (if (rd.z >= 0) (mn.z - ro.z)/rd.z else (mx.z - ro.z)/rd.z)
            val tzmax = (if (rd.z >= 0) (mx.z - ro.z)/rd.z else (mn.z - ro.z)/rd.z)
            if (tmin > tzmax || tzmin > tmax) return null
            if (tzmin > tmin) tmin = tzmin; if (tzmax < tmax) tmax = tzmax
            return if (tmax < 0f) null else if (tmin >= 0f) tmin else tmax
        }

        val skip = setOf<Node?>(rootModel, tableNode) // bỏ qua root và WoodTable

        fun visit(n: Node?) {
            if (n == null) return
            // Duyệt tất cả con
            n.childNodes.forEach { c ->
                // Lấy AABB thế giới cho bất kỳ Node nào
                getAnyNodeWorldAabb(c)?.let { (mn, mx) ->
                    // Loại trừ root + bàn
                    if (c !in skip) {
                        rayAabbT(rayOrigin, rayDir, mn, mx)?.let { t ->
                            if (t in 0f..bestT) {
                                bestT = t
                                // Trả về ModelNode để phần còn lại không cần sửa nhiều
                                bestNode = when (c) {
                                    is ModelNode -> c
                                    else -> { // RenderableNode: trả về ModelNode cha gần nhất (nếu cần)
                                        // tìm cha là ModelNode
                                        var p = c.parent
                                        var found: ModelNode? = null
                                        while (p != null && found == null) {
                                            found = p as? ModelNode
                                            p = p.parent
                                        }
                                        found ?: rootModel // fallback
                                    }
                                }
                            }
                        }
                    }
                }
                visit(c)
            }
        }

        visit(rootModel)
        return bestNode
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

    // ================== CAMERA (giản lược, đủ dùng) ==================

    private fun updateCamera() {
        // Camera nhìn về center (x,y,z) từ phía trước 1 đoạn "radius"
        val eye = Position(centerX, centerY + 0.3f * radius, centerZ - 1.2f * radius)
        sceneView.cameraNode.position = eye
        sceneView.cameraNode.lookAt(Position(centerX, centerY, centerZ), upDirection = Direction(y = 1f))
        sceneView.invalidate()
    }

    // ================== EXTENSIONS ==================

    private fun Direction.normalized(): Direction {
        val len = kotlin.math.sqrt((x*x + y*y + z*z).toDouble()).toFloat().coerceAtLeast(1e-6f)
        return Direction(x/len, y/len, z/len)
    }
}
