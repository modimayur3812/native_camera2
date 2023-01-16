package com.example.native_camera2;

import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodChannel;

public class DartMessenger {

    @NonNull
    private final Handler handler;

    @Nullable
    private MethodChannel cameraChannel;

    /** Specifies the different camera related message types. */
    enum CameraEventType {
        /** Indicates that an error occurred while interacting with the camera. */
        ERROR("error"),
        /** Indicates that the camera is closing. */
        CLOSING("camera_closing"),
        /** Indicates that the camera is initialized. */
        INITIALIZED("initialized");

        private final String method;

        /**
         * Converts the supplied method name to the matching {@link CameraEventType}.
         *
         * @param method name to be converted into a {@link CameraEventType}.
         */
        CameraEventType(String method) {
            this.method = method;
        }
    }

    public DartMessenger(BinaryMessenger messenger, long cameraId, @NonNull Handler handler) {
        cameraChannel = new MethodChannel(messenger, "plugins.flutter.io/native_android2/camera" + cameraId);
        this.handler = handler;
    }

    void sendCameraInitializedEvent(Integer previewWidth, Integer previewHeight) {
        assert (previewWidth != null);
        assert (previewHeight != null);
        this.send(CameraEventType.INITIALIZED, new HashMap<String, Object>() {
            {
                put("previewWidth", previewWidth.doubleValue());
                put("previewHeight", previewHeight.doubleValue());
            }
        });
    }

    void sendCameraClosingEvent() {
        send(CameraEventType.CLOSING);
    }

    void sendCameraErrorEvent(@Nullable String description) {
        this.send(CameraEventType.ERROR, new HashMap<String, Object>() {
            {
                if (!TextUtils.isEmpty(description)) {
                    put("description", description);
                }
            }
        });
    }

    private void send(CameraEventType eventType) {
        send(eventType, new HashMap<>());
    }

    private void send(CameraEventType eventType, Map<String, Object> args) {
        if (cameraChannel == null) {
            return;
        }
        handler.post(() -> cameraChannel.invokeMethod(eventType.method, args));
    }

    public void finish(MethodChannel.Result result, Object payload) {
        handler.post(() -> result.success(payload));
    }

    public void error(
            MethodChannel.Result result,
            String errorCode,
            @Nullable String errorMessage,
            @Nullable Object errorDetails) {
        handler.post(() -> result.error(errorCode, errorMessage, errorDetails));
    }
}
