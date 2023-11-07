package com.llopez.sceneviewapp

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.PixelCopy
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.filament.gltfio.Animator
import com.google.ar.core.Config
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.node.CursorNode
import io.github.sceneview.model.model
import io.github.sceneview.node.ModelNode
import io.github.sceneview.utils.colorOf
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class MainActivity : AppCompatActivity() {
    // Views
    private lateinit var sceneView: ArSceneView
    private lateinit var generateImageBtn: Button
    private lateinit var mainContainer: ConstraintLayout

    // 3D model
    private lateinit var cursorNode: CursorNode
    private val TAG: String = "SceneViewApp-MainActivity.kt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        generateImageBtn = findViewById(R.id.generateImageBtn)
        mainContainer = findViewById(R.id.mainContainer)
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
            // addChild(cursorNode)
        }
        sceneView.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL

        sceneView.onTapAr = { hitTestResult, _ ->
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
                    glbFileLocation = "https://storage.googleapis.com/ese-plus-ar-assets/assets/c6dfec73-02e8-4dfe-962a-dd7a33e7bc85/c6dfec73.glb",
                    autoAnimate = true,
                    centerOrigin = null,
                    scaleToUnits = 20f,
                    onError = { exception ->
                        Log.i(TAG, "node.loadModelGlb() - load onError: ${exception.localizedMessage}")
                    },
                    onLoaded = { _ ->
                        Log.i(TAG, "node.loadModelGlb() - load success")

                        node.playingAnimations.forEach { subNode ->
                            node.playAnimation(subNode.key, loop = true)
                        }
                    }
                )
                
                node.worldPosition = hitTestResult.hitPose.position
                sceneView.addChild(node)
                // node.rotation = Float3( 0f, 270f, 0f)
                // node.scale = Float3(0.75f, 0.75f, 0.75f)
            }
        }

        generateImageBtn.setOnClickListener {
            createPixelCopy()
            openIntentArViewer()
        }
    }

    private fun openIntentArViewer() {
        val intent = Intent(Intent.ACTION_VIEW)
        val intentUri = Uri.parse("https://arvr.google.com/scene-viewer/1.0")
            .buildUpon()
            .appendQueryParameter("file", "https://storage.googleapis.com/ese-plus-ar-assets/assets/c6dfec73-02e8-4dfe-962a-dd7a33e7bc85/c6dfec73.gltf")
            .appendQueryParameter("mode", "ar_preferred")
            .build()

        intent.data = intentUri
        intent.setPackage("com.google.ar.core")

        startActivity(intent)
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

    private fun saveImageInDevice(
        context: Context,
        image: Bitmap,
        compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
    ): File {
        val extension = if (compressFormat == Bitmap.CompressFormat.PNG) ".png" else ".jpg"
        val imageName = "example_image_"

        val outputFile =
            File.createTempFile(imageName, extension, context.cacheDir)
                .apply {
                    createNewFile()
                }

        val outputStream = FileOutputStream(outputFile)

        image.compress(compressFormat, 100, outputStream)
        outputStream.close()

        saveImageInGallery(image, imageName, extension, Bitmap.CompressFormat.PNG)

        return outputFile
    }
    
    private fun saveImageInGallery(image: Bitmap, imageName: String, imageExtension: String, compressFormat: Bitmap.CompressFormat) {
        val imagesFolderName = "SceneViewApp"

        val fos: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver: ContentResolver = this.contentResolver
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageName)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/$imagesFolderName")

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            resolver.openOutputStream(imageUri!!)
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + File.separator + imagesFolderName
            val file = File(imagesDir)
            val image = File(imagesDir, "${imageName}${imageExtension}")

            if (!file.exists()) {
                file.mkdir()
            }

            FileOutputStream(image)
        }

        if (fos != null) {
            image.compress(compressFormat, 100, fos)
        }

        fos?.flush()
        fos?.close()
    }

    private fun createPixelCopy() {
        val bitmap = Bitmap.createBitmap(sceneView.width, sceneView.height, Bitmap.Config.ARGB_8888)

        PixelCopy.request(
            sceneView, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) {
                    saveImageInDevice(this, bitmap)
                }
            },
            Handler(
                HandlerThread("screenshot").apply { start() }.looper
            )
        )
    }
}