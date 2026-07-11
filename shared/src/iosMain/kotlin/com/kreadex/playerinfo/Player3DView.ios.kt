package com.kreadex.playerinfo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.SceneKit.*
import platform.UIKit.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlinx.cinterop.*

class GesturesHandler(
    private val scnView: SCNView,
    private val onHit: () -> Unit,
    private val onLongPress: () -> Unit
) : NSObject() {

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    @ObjCAction
    fun handleTap(gesture: UITapGestureRecognizer) {
        val location = gesture.locationInView(scnView)
        val hitResults = scnView.hitTest(location, options = emptyMap<Any?, Any>())
        val firstHit = hitResults.firstOrNull() as? SCNHitTestResult

        if (firstHit != null) {
            val characterNode = scnView.scene?.rootNode?.childNodes?.firstOrNull() as? SCNNode

            if (characterNode != null) {
                val hitY = firstHit.worldCoordinates.useContents { y.toDouble() }
                val modelY = characterNode.position.useContents { y.toDouble() }

                val localHitHeight = hitY - modelY

                if (localHitHeight > 0.35) {
                    onHit()

                    val cameraNode = scnView.pointOfView
                    if (cameraNode != null) {
                        cameraNode.position.useContents {
                            val camX = x.toDouble()
                            val camZ = z.toDouble()

                            characterNode.position.useContents {
                                val modX = x.toDouble()
                                val modZ = z.toDouble()

                                val dx = camX - modX
                                val dz = camZ - modZ

                                val animName = getDirectionalAnimation(dx, dz)
                                val player = characterNode.animationPlayerForKey(animName)
                                player?.play()
                            }
                        }
                    }
                }
            }
        }
    }

    @ObjCAction
    fun handleLongPress(gesture: UILongPressGestureRecognizer) {
        if (gesture.state == UIGestureRecognizerStateBegan) {
            onLongPress()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun Player3DView(
    modifier: Modifier,
    modelType: PlayerModelType,
    skinBytes: ByteArray?,
    onHit: () -> Unit,
    onLongPress: () -> Unit
) {
    val gesturesHandler = remember { mutableStateOf<GesturesHandler?>(null) }

    UIKitView(
        modifier = modifier,
        factory = {
            val view = SCNView()
            view.allowsCameraControl = true
            view.autoenablesDefaultLighting = true
            view.backgroundColor = UIColor.clearColor

            val fileName = when (modelType) {
                PlayerModelType.STEVE -> "steve"
                PlayerModelType.ALEX -> "alex"
                PlayerModelType.OLD -> "old"
            }

            val url = NSBundle.mainBundle.URLForResource(
                name = fileName,
                withExtension = "usdz",
                subdirectory = "models"
            )

            if (url != null) {
                view.scene = SCNScene.sceneWithURL(url, null, null)
            }

            val handler = GesturesHandler(view, onHit, onLongPress)
            gesturesHandler.value = handler

            val tapGesture = UITapGestureRecognizer(
                target = handler,
                action = NSSelectorFromString("handleTap:")
            )
            view.addGestureRecognizer(tapGesture)

            val longPressGesture = UILongPressGestureRecognizer(
                target = handler,
                action = NSSelectorFromString("handleLongPress:")
            )
            view.addGestureRecognizer(longPressGesture)

            view
        },
        update = { view ->
            if (skinBytes != null && skinBytes.isNotEmpty()) {
                val nsData = skinBytes.usePinned { pinned ->
                    NSData.dataWithBytes(pinned.addressOf(0), skinBytes.size.toULong())
                }
                val uiImage = UIImage(data = nsData)

                val characterNode = view.scene?.rootNode?.childNodes?.firstOrNull() as? SCNNode
                val material = characterNode?.geometry?.firstMaterial
                material?.diffuse?.contents = uiImage
            }
        }
    )
}