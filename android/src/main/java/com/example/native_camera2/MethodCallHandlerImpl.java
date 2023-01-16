package com.example.native_camera2;

import android.app.Activity;
import android.hardware.camera2.CameraAccessException;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.view.TextureRegistry;

public class MethodCallHandlerImpl implements MethodChannel.MethodCallHandler {

    private final Activity activity;
    private final BinaryMessenger messenger;
    private final TextureRegistry textureRegistry;
    private final MethodChannel methodChannel;
    private @Nullable NativeCamera nativeCamera;

    private final String TAG = MethodCallHandlerImpl.class.getSimpleName();

    public MethodCallHandlerImpl(Activity activity, BinaryMessenger messenger, TextureRegistry textureRegistry) {
        this.activity = activity;
        this.messenger = messenger;
        this.textureRegistry = textureRegistry;
        methodChannel = new MethodChannel(messenger, "plugins.flutter.io/native_android2");
        methodChannel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {

            case "availableCameras":
                try {
                    result.success(CameraUtils.getAvailableCameras(activity));
                } catch (Exception e) {
                    handleException(e, result);
                }
                break;

            case "create":
                try {
                    if (nativeCamera != null) {
                        nativeCamera.close();
                    }
                    instantiateCamera(call, result);
                } catch (Exception e) {
                    handleException(e, result);
                }
                break;

            case "initialize":
                if (nativeCamera != null) {

                    try {
                        nativeCamera.openCameraInitializer();
                        result.success(null);
                    } catch (Exception e) {
                        handleException(e, result);
                    }

                } else {
                    result.error("cameraNotFound", "Camera not found. Please call the 'create' method before calling 'initialize'.", null);
                }
                break;

            case "PreviewSize":
                if (nativeCamera != null) {
                    try {

                        Map<String, String> previewSizeParams = new HashMap<>();
                        previewSizeParams.put("Width", String.valueOf(nativeCamera.getPreviewSize().getWidth()));
                        previewSizeParams.put("Height", String.valueOf(nativeCamera.getPreviewSize().getHeight()));
                        result.success(previewSizeParams);

                    } catch (Exception e) {
                        handleException(e, result);
                    }
                }
                break;

            case "takePicture":
                if (nativeCamera != null) {
                    try {
                        nativeCamera.takePicture(result);
                    } catch (CameraAccessException e) {
                        handleException(e, result);
                    }
                }
                break;

            case "pausePreview":
                if (nativeCamera != null) {
                    try {
                        nativeCamera.pausePreview();
                        result.success(null);
                    } catch (Exception e) {
                        handleException(e, result);
                    }
                }
                break;

            case "resumePreview":
                if (nativeCamera != null) {
                    try {
                        nativeCamera.resumePreview();
                    } catch (CameraAccessException e) {
                        handleException(e, result);
                    }
                    result.success(null);
                }
                break;

            case "dispose":
                if (nativeCamera != null) {
                    nativeCamera.dispose();
                }
                result.success(null);
                break;

            default:
                result.notImplemented();
                break;
        }
    }

    private void instantiateCamera(MethodCall call, MethodChannel.Result result) throws CameraAccessException {
        String cameraName = call.argument("cameraName");

        TextureRegistry.SurfaceTextureEntry flutterSurfaceTexture = textureRegistry.createSurfaceTexture();
        DartMessenger dartMessenger = new DartMessenger(messenger, flutterSurfaceTexture.id(), new Handler(Looper.getMainLooper()));
        CameraProperties cameraProperties = new CameraPropertiesImpl(cameraName, CameraUtils.getCameraManager(activity));

        nativeCamera = new NativeCamera(activity, flutterSurfaceTexture, dartMessenger, cameraProperties);

        Map<String, Object> reply = new HashMap<>();
        reply.put("cameraId", flutterSurfaceTexture.id());
        result.success(reply);
    }

    private void handleException(Exception exception, MethodChannel.Result result) {
        if (exception instanceof CameraAccessException) {
            result.error("CameraAccess", exception.getMessage(), null);
            return;
        }
        throw (RuntimeException) exception;
    }

    void stopListening() {
        methodChannel.setMethodCallHandler(null);
    }
}
