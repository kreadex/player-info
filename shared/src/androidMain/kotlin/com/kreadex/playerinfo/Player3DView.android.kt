package com.kreadex.playerinfo

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import io.github.sceneview.SceneView
import io.github.sceneview.material.setTexture
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.texture.setBitmap

@Composable
actual fun Player3DView(
    modifier: Modifier,
    modelType: PlayerModelType,
    skinBytes: ByteArray?,
    onHit: () -> Unit,
    onLongPress: () -> Unit
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine).apply {
        position = Position(z = 4.0f)
    }

    val mainLightNode = rememberMainLightNode(engine).apply {
        intensity = 100_000.0f
    }

    val cameraManipulator = rememberCameraManipulator()

    val fileName = when (modelType) {
        PlayerModelType.STEVE -> "steve.glb"
        PlayerModelType.ALEX -> "alex.glb"
        PlayerModelType.OLD -> "old.glb"
    }

    val modelInstance = rememberModelInstance(
        assetFileLocation = "files/models/$fileName",
        modelLoader = modelLoader
    )


    LaunchedEffect(skinBytes, modelInstance) {
        if (skinBytes != null && modelInstance != null) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(skinBytes, 0, skinBytes.size)
                if (bitmap != null) {
                    val texture = Texture.Builder()
                        .width(bitmap.width)
                        .height(bitmap.height)
                        .sampler(Texture.Sampler.SAMPLER_2D)
                        .format(Texture.InternalFormat.SRGB8_A8)
                        .levels(1)
                        .build(engine)
                        .apply { setBitmap(engine, bitmap) }

                    val sampler = TextureSampler(
                        TextureSampler.MinFilter.NEAREST,
                        TextureSampler.MagFilter.NEAREST,
                        TextureSampler.WrapMode.CLAMP_TO_EDGE
                    )

                    modelInstance.materialInstances.forEach { material ->
                        material.setTexture("baseColorMap", texture, sampler)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val touchStart = remember { floatArrayOf(0f, 0f) }

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        cameraNode = cameraNode,
        mainLightNode = mainLightNode,
        cameraManipulator = cameraManipulator,
        isOpaque = false,
        autoCenterContent = true,
        autoFitContent = true,

        onGestureListener = rememberOnGestureListener(
            onLongPress = { motionEvent, node ->
                onLongPress()
            }
        ),

        onTouchEvent = { event, hitResult ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    touchStart[0] = event.x
                    touchStart[1] = event.y
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val dx = kotlin.math.abs(event.x - touchStart[0])
                    val dy = kotlin.math.abs(event.y - touchStart[1])

                    if (dx < 20f && dy < 20f && hitResult != null && hitResult.node != null) {

                        val hitY = hitResult.getWorldPosition().y

                        var currentNode: io.github.sceneview.node.Node? = hitResult.node
                        var rootModelNode: io.github.sceneview.node.ModelNode? = null

                        while (currentNode != null) {
                            if (currentNode is io.github.sceneview.node.ModelNode) {
                                rootModelNode = currentNode
                                break
                            }
                            currentNode = currentNode.parent
                        }

                        if (rootModelNode != null) {
                            val modelY = rootModelNode.worldPosition.y

                            val localHitHeight = hitY - modelY

                            if (localHitHeight > 0.35f) {
                                onHit()

                                val camPos = cameraNode.worldPosition
                                val modPos = rootModelNode.worldPosition
                                val deltaX = (camPos.x - modPos.x).toDouble()
                                val deltaZ = (camPos.z - modPos.z).toDouble()

                                rootModelNode.playAnimation(getDirectionalAnimation(deltaX, deltaZ), loop = false)
                            }
                        }
                    }
                }
            }
            false
        }
    ) {

        if (modelInstance != null) {
            ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 0.75f,
                autoAnimate = false,
                rotation = io.github.sceneview.math.Rotation(y = 180.0f)
            )
        }
    }
}