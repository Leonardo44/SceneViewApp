package com.llopez.sceneviewapp

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Config
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.node.CursorNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.utils.colorOf
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var sceneView: ArSceneView
    private lateinit var cursorNode: CursorNode
    private lateinit var cursorModelNode: ModelNode
    private val TAG: String = "SceneViewApp-MainActivity.kt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sceneView = findViewById<ArSceneView?>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            depthEnabled = true
            instantPlacementEnabled = true
            onArSessionCreated = { _ ->
            }
            configureSession { _, _ ->
            }
            cursorNode = CursorNode(
                engine = engine,
                modelFileLocation = "PokebolaColorA.glb",
                autoAnimate = false,
                centerOrigin = null,
                scaleToUnits = 0.1f
            )
            cursorNode.colorNotTracking = colorOf(rgb = 0f)
            cursorNode.colorTracking = colorOf(rgb = 0f)
            cursorNode.colorClicked = colorOf(rgb = 0f)
            addChild(cursorNode)
        }
        sceneView.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL

        sceneView.onTapAr = { hitTestResult, motionEvent ->
            Log.i(TAG, "onTapAr: ${hitTestResult.hitPose.position} - ${cursorNode.worldPosition}")

            lifecycleScope.launch {
                val node = ModelNode(
                    engine = sceneView.engine
                )

                node.loadModelGlb(
                    context = sceneView.context,
                    glbFileLocation = "PokebolaColorA.glb",
                    autoAnimate = false,
                    centerOrigin = null,
                    scaleToUnits = 0.25f,
                    onError = { exception ->
                        Log.i(TAG, "node.loadModelGlb() - onError: ${exception.localizedMessage}")
                    }
                )
                node.worldPosition = hitTestResult.hitPose.position
                sceneView.addChild(node)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!checkPermission(android.Manifest.permission.CAMERA)) {
            AlertDialog.Builder(this)
                .setMessage("Need camera permission to capture image. Please provide permission to access your camera.")
                .setPositiveButton("OK") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.CAMERA),
                        201
                    )
                }
                .setNegativeButton("Cancel") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 201) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onPause() {
        super.onPause()
    }
}