package com.example.smartphysicapplication.main

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.environment.*
import io.github.sceneview.math.Scale

class ModelViewerNativeFragment : Fragment() {

    private lateinit var sceneView: SceneView
    private var rootModel: ModelNode? = null

    private val cubeName = "Cube"
    private val jetName  = "Jet"

    private var height = 6f                     // khoảng cách camera đến mặt phẳng (OY)
    private var centerX = 0f
    private var centerZ = 0f
    private lateinit var scaleDetector: ScaleGestureDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_model_viewer, container, false)
        sceneView = root.findViewById(R.id.sceneView)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ánh sáng cơ bản
        sceneView.mainLightNode = LightNode(
            sceneView.engine,
            sceneView.engine.entityManager.create()   // <-- truyền entity
        ).apply {
            intensity = 100_000f
        }

        // Chỉ cho ZOOM
        enableTopDownOnly(initialHeight = 15f)

        // Nạp GLB từ assets (đường dẫn trong app/src/main/assets/)
        loadGlb("models/Untitled.glb")
        rootModel!!.childNodes.forEach { android.util.Log.d("Nodes", it.name ?: "<no name>") }
        setNodeScale("Cube", 3.0f)
        setNodeScale("Jet",  3.0f)

        setupEnvironment()

        // Nút bật/tắt đối tượng
        view.findViewById<Button>(R.id.btnToggleCube).setOnClickListener {
            toggleChildVisibility(cubeName)
        }
        view.findViewById<Button>(R.id.btnToggleJet).setOnClickListener {
            toggleChildVisibility(jetName)
        }
    }

    private fun loadGlb(assetPath: String) {
        // Tạo instance từ assets
        val instance = sceneView.modelLoader.createModelInstance(assetPath)

        // Gốc của mô hình (chứa toàn bộ các node con bên trong GLB)
        rootModel = ModelNode(
            modelInstance = instance,
            autoAnimate = false,
            scaleToUnits = 1.0f     // tuỳ scene của bạn
        )

        sceneView.addChildNode(rootModel!!)

        // Đặt camera lùi ra một chút
        sceneView.cameraNode.position = Position(0.0f, 0.0f, 4.0f)
    }

    private fun setNodeScale(nodeName: String, uniform: Float) {
        rootModel?.childNodes?.firstOrNull { it.name == nodeName }?.let { n ->
            n.scale = Scale(uniform)         // scale đều X=Y=Z
            // hoặc: n.scale = Scale(x, y, z)
        }
    }

    private fun setupEnvironment() {
        // Tạo skybox + ánh sáng môi trường từ HDR
        val env = sceneView.environmentLoader.createHDREnvironment(
            "environments/studio.hdr"  // đường dẫn trong assets
        )
        if (env != null) {
            sceneView.environment = env
        }

        // Tăng độ sáng ánh sáng gián tiếp nếu cần
        sceneView.environment?.indirectLight?.intensity = 30_000f
    }

    /** Bật/tắt node con theo TÊN (so với Blender) */
    private fun toggleChildVisibility(childName: String) {
        val parent = rootModel ?: return
        val child = parent.childNodes.firstOrNull { it.name == childName } ?: return
        child.isVisible = !child.isVisible
    }

    /** Khoá xoay & pan, chỉ cho pinch-zoom (đổi khoảng cách camera) */
    private fun enableZoomOnly() {
        // Vô hiệu hoá camera manipulator mặc định (nếu có)
        sceneView.cameraManipulator = null

        val scaleDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val cam = sceneView.cameraNode
                    val curZ = cam.position.z
                    // scaleFactor > 1 => phóng to => giảm khoảng cách một chút
                    val nextZ = (curZ / detector.scaleFactor).coerceIn(1.5f, 15f)
                    cam.position = Position(cam.position.x, cam.position.y, nextZ)
                    return true
                }
            }
        )

        sceneView.setOnTouchListener { _: View, event: MotionEvent ->
            scaleDetector.onTouchEvent(event)
            // Trả true để chặn các gesture xoay/pan mặc định
            true
        }
    }

    private fun enableTopDownOnly(
        initialHeight: Float = 6f,
        minHeight: Float = 1.5f,
        maxHeight: Float = 30f
    ) {
        sceneView.cameraManipulator = null      // tự điều khiển camera
        height = initialHeight

        // Pinch = đổi độ cao (zoom)
        scaleDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    height = (height / d.scaleFactor).coerceIn(minHeight, maxHeight)
                    updateTopDownCamera()
                    return true
                }
            }
        )

        // (tuỳ chọn) Pan 2 ngón để dời tâm quan sát trên mặt phẳng OX–OZ
        var lastCx = 0f; var lastCz = 0f
        sceneView.setOnTouchListener { _, ev ->
            scaleDetector.onTouchEvent(ev)

            if (ev.pointerCount == 2 && ev.actionMasked == MotionEvent.ACTION_MOVE) {
                // đơn giản hoá: dùng chênh lệch ngón 0 để tịnh tiến tâm
                val dx = ev.getX(0) - lastCx
                val dy = ev.getY(0) - lastCz
                lastCx = ev.getX(0); lastCz = ev.getY(0)

                val panSpeed = 0.01f            // px -> mét, chỉnh theo cảm giác
                centerX -= dx * panSpeed
                centerZ += dy * panSpeed
                updateTopDownCamera()
            }
            true
        }

        updateTopDownCamera()
    }

    private fun updateTopDownCamera() {
        // Hệ trục Filament/SceneView: +Y là "up".
        // Camera luôn trên trục +Y, nhìn xuống tâm (centerX, 0, centerZ).
        sceneView.cameraNode.position = Position(centerX, height, centerZ)
        sceneView.cameraNode.lookAt(Position(centerX, 0f, centerZ))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sceneView.destroy()
    }
}