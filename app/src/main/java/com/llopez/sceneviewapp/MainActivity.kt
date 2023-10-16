package com.llopez.sceneviewapp

import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.filament.gltfio.FilamentAsset
import com.google.ar.core.Config
import com.google.ar.sceneform.lullmodel.VertexAttributeUsage.Position
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.getDescription
import io.github.sceneview.math.Position
import kotlinx.coroutines.launch
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.model.Model
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.coroutineScope

class MainActivity : AppCompatActivity() {
    lateinit var sceneView: ArSceneView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sceneView = findViewById<ArSceneView?>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            depthEnabled = true
            instantPlacementEnabled = true
            onArSessionCreated = { _ ->

            }
        }

        val node = ModelNode(
            engine = sceneView.engine
        )
        lifecycleScope.launch {
            node.loadModelGlb(
                context = sceneView.context,
                glbFileLocation = "PokebolaColorA.glb",
                autoAnimate = false,
                centerOrigin = Position(0f, 0f, 0f),
                scaleToUnits = 0.25f,
                onError = { exception ->
                    Log.i("Error model", "error model ${exception.localizedMessage}")
                }
            )
            sceneView.addChild(node)
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