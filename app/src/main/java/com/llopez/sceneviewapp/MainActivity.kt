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
import dev.romainguy.kotlin.math.Float3
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

                // Load local 3D object
//                node.loadModelGlb(
//                    context = sceneView.context,
//                    glbFileLocation = "PokebolaColorA.glb",
//                    autoAnimate = false,
//                    centerOrigin = null,
//                    scaleToUnits = 0.25f,
//                    onError = { exception ->
//                        Log.i(TAG, "node.loadModelGlb() - onError: ${exception.localizedMessage}")
//                    }
//                )

                // Load 3D object from server
                node.loadModelGlbAsync(
                    glbFileLocation = "https://firebasestorage.googleapis.com/v0/b/ar-core-test-project.appspot.com/o/PokebolaColorA.glb?alt=media&token=80f1b820-ad2b-446e-8d3b-ca9554d25f7d",
                    autoAnimate = false,
                    centerOrigin = null,
                    scaleToUnits = 0.25f,
                    onError = { exception ->
                        Log.i(TAG, "node.loadModelGlb() - load onError: ${exception.localizedMessage}")
                    },
                    onLoaded = { _ ->
                        Log.i(TAG, "node.loadModelGlb() - load success")
                    }
                )

                node.worldPosition = hitTestResult.hitPose.position
                sceneView.addChild(node)

                node.rotation = Float3( 0f, 270f, 0f)
                // node.scale = Float3(0.75f, 0.75f, 0.75f)
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