package com.example.native_camera2;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CameraUtils {

    static CameraManager getCameraManager(Context context) {
        return (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    /**
     * Gets all the available cameras for the device.
     *
     * @param activity The current Android activity.
     * @return A map of all the available cameras, with their name as their key.
     * @throws CameraAccessException when the camera could not be accessed.
     */
    public static List<Map<String, Object>> getAvailableCameras(Activity activity)
            throws CameraAccessException {
        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        String[] cameraNames = cameraManager.getCameraIdList();
        List<Map<String, Object>> cameras = new ArrayList<>();
        for (String cameraName : cameraNames) {
            int cameraId;
            try {
                cameraId = Integer.parseInt(cameraName, 10);
            } catch (NumberFormatException e) {
                cameraId = -1;
            }
            if (cameraId < 0) {
                continue;
            }

            HashMap<String, Object> details = new HashMap<>();
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraName);
            details.put("name", cameraName);
            int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            details.put("sensorOrientation", sensorOrientation);

            int lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            switch (lensFacing) {
                case CameraMetadata.LENS_FACING_FRONT:
                    details.put("lensFacing", "front");
                    break;
                case CameraMetadata.LENS_FACING_BACK:
                    details.put("lensFacing", "back");
                    break;
                case CameraMetadata.LENS_FACING_EXTERNAL:
                    details.put("lensFacing", "external");
                    break;
            }
            cameras.add(details);
        }
        return cameras;
    }
}
