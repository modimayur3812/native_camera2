package com.example.native_camera2;

import static android.hardware.camera2.CameraCharacteristics.LENS_FACING;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

import static androidx.core.math.MathUtils.clamp;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import androidx.exifinterface.media.ExifInterface;

import android.hardware.camera2.params.MeteringRectangle;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.view.TextureRegistry;

public class NativeCamera {

    private final String TAG = NativeCamera.class.getSimpleName();

    private final TextureRegistry.SurfaceTextureEntry flutterTexture;
    private final DartMessenger dartMessenger;

    private final CameraProperties cameraProperties;
    private final Activity activity;

    CameraDevice cameraDevice;
    ImageReader imageReader;
    CameraCaptureSession captureSession;

    HandlerThread cameraThread;
    Handler cameraHandler;

    HandlerThread imageReaderThread;
    Handler imageReaderHandler;

    boolean pausedPreview = false;
    private Surface surface;

    public NativeCamera(final Activity activity, final TextureRegistry.SurfaceTextureEntry flutterTexture, final DartMessenger dartMessenger, final CameraProperties cameraProperties) {
        if (activity == null) {
            throw new IllegalStateException("No activity available!");
        }
        this.activity = activity;
        this.flutterTexture = flutterTexture;
        this.dartMessenger = dartMessenger;
        this.cameraProperties = cameraProperties;

        startBackgroundThread();
    }

    public void openCameraInitializer() throws CameraAccessException {
        CameraManager cameraManager = CameraUtils.getCameraManager(activity);

        try {
            open(cameraManager, cameraProperties.getCameraName(), cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            dartMessenger.sendCameraErrorEvent(e.getMessage());
        }

        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraProperties.getCameraName());
        int[] capabilities = cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            boolean isManualFocusSupported = IntStream.of(capabilities).anyMatch(x -> x == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR);
            if (isManualFocusSupported) {

            }
        }

        Size[] sizeArray = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
        Size cameraPreviewSize = new Size(1920, 1080);
        if (!Arrays.asList(sizeArray).contains(cameraPreviewSize)) {
            Display activityDisplay = activity.getWindowManager().getDefaultDisplay();
            Point point = new Point();
            activityDisplay.getRealSize(point);
            cameraPreviewSize = new Size(point.x, point.y);
        }

        imageReader = ImageReader.newInstance(cameraPreviewSize.getWidth(), cameraPreviewSize.getHeight(), ImageFormat.JPEG, 1);

        SurfaceTexture surfaceTexture = flutterTexture.surfaceTexture();
        surfaceTexture.setDefaultBufferSize(cameraPreviewSize.getWidth(), cameraPreviewSize.getHeight());

        surface = new Surface(surfaceTexture);

        List<Surface> targets = new ArrayList<>();
        targets.add(surface);
        targets.add(imageReader.getSurface());

        createCaptureSession(cameraDevice, targets, cameraHandler, surface);
    }

    private void refreshPreviewCaptureSession(Surface surface) throws CameraAccessException {
        CaptureRequest.Builder captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        captureRequest.addTarget(surface);

        //cameraPreviewSize.getHeight() / 2;
        //        cameraPreviewSize.getWidth() / 2
        new Handler().postDelayed(() -> {
            if (captureSession != null) {
                try {
//                    CameraManager cameraManager = CameraUtils.getCameraManager(activity);
//                    CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraProperties.getCameraName());
//                    Rect rect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
//                    Log.i(TAG, "SENSOR_INFO_ACTIVE_ARRAY_SIZE,,,,,,,,rect.left--->" + rect.left + ",,,rect.top--->" + rect.top + ",,,,rect.right--->" + rect.right + ",,,,rect.bottom---->" + rect.bottom);
//                    Size size = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
//                    Log.i("onAreaTouchEvent", "mCameraCharacteristics,,,,size.getWidth()--->" + size.getWidth() + ",,,size.getHeight()--->" + size.getHeight());
//                    int areaSize = 200;
//                    int right = rect.right;
//                    int bottom = rect.bottom;
//                    int viewWidth = getPreviewSize().getWidth();
//                    int viewHeight = getPreviewSize().getHeight();
//                    int ll, rr;
//                    Rect newRect;
//                    int centerX = (int) viewWidth / 2;
//                    int centerY = (int) viewHeight / 2;
//                    ll = ((centerX * right) - areaSize) / viewWidth;
//                    rr = ((centerY * bottom) - areaSize) / viewHeight;
//                    int focusLeft = clamp(ll, 0, right);
//                    int focusBottom = clamp(rr, 0, bottom);
//                    Log.i(TAG, "focusLeft--->" + focusLeft + ",,,focusTop--->" + focusBottom + ",,,focusRight--->" + (focusLeft + areaSize) + ",,,focusBottom--->" + (focusBottom + areaSize));
//                    newRect = new Rect(focusLeft, focusBottom, focusLeft + areaSize, focusBottom + areaSize);
//                    MeteringRectangle meteringRectangle = new MeteringRectangle(newRect, 500);
//                    MeteringRectangle[] meteringRectangleArr = {meteringRectangle};
//                    captureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
//                    captureRequest.set(CaptureRequest.CONTROL_AF_REGIONS, meteringRectangleArr);
//                    captureRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                    captureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    captureSession.setRepeatingRequest(captureRequest.build(), null, cameraHandler);
                } catch (CameraAccessException | IllegalStateException e) {
                    e.printStackTrace();
                    dartMessenger.sendCameraErrorEvent(e.getMessage());
                }
            }
        }, 400);
    }

    private void open(CameraManager manager, String cameraId, Handler handler) throws CameraAccessException {
        manager.openCamera(cameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.d(TAG, "open | onDisconnected");
                close();
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.d(TAG, "open | onError");

                close();
                String errorDescription;
                switch (error) {
                    case ERROR_CAMERA_IN_USE:
                        errorDescription = "The camera device is in use already.";
                        break;

                    case ERROR_MAX_CAMERAS_IN_USE:
                        errorDescription = "Max cameras in use";
                        break;

                    case ERROR_CAMERA_DISABLED:
                        errorDescription = "The camera device could not be opened due to a device policy.";
                        break;

                    case ERROR_CAMERA_DEVICE:
                        errorDescription = "The camera device has encountered a fatal error";
                        break;

                    case ERROR_CAMERA_SERVICE:
                        errorDescription = "The camera service has encountered a fatal error.";
                        break;

                    default:
                        errorDescription = "Unknown camera error";
                }
                dartMessenger.sendCameraErrorEvent(errorDescription);
            }

            @Override
            public void onClosed(@NonNull CameraDevice camera) {
                Log.d(TAG, "open | onClosed");

                cameraDevice = null;
                closeCaptureSession();
            }
        }, handler);
    }

    private void createCaptureSession(CameraDevice device, List<Surface> targets, Handler handler, Surface surface) throws CameraAccessException {
        device.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {

            boolean captureSessionClosed = false;

            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                Log.d(TAG, "CameraCaptureSession onConfigured");
                if (cameraDevice == null || captureSessionClosed) {
                    dartMessenger.sendCameraErrorEvent("The camera was closed during configuration.");
                    return;
                }
                captureSession = session;
                try {
                    refreshPreviewCaptureSession(surface);
                } catch (CameraAccessException e) {
                    dartMessenger.sendCameraErrorEvent(e.getMessage());
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Log.d(TAG, "CameraCaptureSession onConfigureFailed");
                dartMessenger.sendCameraErrorEvent("Failed to configure camera session.");
            }

            @Override
            public void onClosed(@NonNull CameraCaptureSession session) {
                Log.d(TAG, "CameraCaptureSession onClosed");
                captureSessionClosed = true;
            }

        }, handler);
    }

    private MethodChannel.Result flutterResult;

    public void takePicture(@NonNull final MethodChannel.Result result) throws CameraAccessException {

        captureSession.stopRepeating();

        flutterResult = result;

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireNextImage();

            CameraManager cameraManager = CameraUtils.getCameraManager(activity);
            CameraCharacteristics cameraCharacteristics;
            try {

                cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraProperties.getCameraName());
                int rotation = 90;
                boolean mirrored = cameraCharacteristics.get(LENS_FACING) == LENS_FACING_FRONT;
                int exifOrientation = computeExifOrientation(rotation, mirrored);

                File newFile = createFile("jpg");

                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                FileOutputStream output;

                output = new FileOutputStream(newFile);
                output.write(bytes);
                output.close();

                ExifInterface exif = new ExifInterface(newFile.getAbsolutePath());
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
                exif.saveAttributes();

                flutterResult.success(newFile.getAbsolutePath());

                image.close();

                //openCameraInitializer();
                if (surface != null) {
                    refreshPreviewCaptureSession(surface);
                }

            } catch (CameraAccessException | IOException e) {
                dartMessenger.error(flutterResult, "", e.getMessage(), null);
            }
        }, imageReaderHandler);

        CaptureRequest.Builder captureRequest = captureSession.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureRequest.addTarget(imageReader.getSurface());

        captureSession.capture(captureRequest.build(), new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
                Log.i(TAG, "onCaptureStarted: ");
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                super.onCaptureProgressed(session, request, partialResult);
                Log.i(TAG, "onCaptureProgressed: ");
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                Log.i(TAG, "onCaptureCompleted: ");
                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (afState == null || aeState == null) {
                    return;
                }
                Log.i(TAG, "onCaptureCompleted: " + afState + " , " + aeState);
                if (afState == CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                    Log.i("DngSessionCallback", " aeState.intValue()--->" + aeState);
                    if (aeState == CameraMetadata.CONTROL_AE_STATE_LOCKED || aeState == CameraMetadata.CONTROL_AE_STATE_CONVERGED || aeState == CameraMetadata.CONTROL_AE_STATE_PRECAPTURE || aeState == CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        Log.i("DngSessionCallback", " inner aeState.intValue()--->" + aeState);
                    }
                }
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                Log.i(TAG, "onCaptureFailed: ");
            }

        }, cameraHandler);
    }

    private File createFile(String extension) {
        DateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US);
        return new File(activity.getFilesDir(), "IMG_" + sdf.format(new Date()) + "." + extension);
    }

    private int computeExifOrientation(int rotationDegrees, boolean mirrored) {
        if (rotationDegrees == 0 && !mirrored) {
            return ExifInterface.ORIENTATION_NORMAL;
        } else if (rotationDegrees == 0) {
            return ExifInterface.ORIENTATION_FLIP_HORIZONTAL;
        } else if (rotationDegrees == 180 && !mirrored) {
            return ExifInterface.ORIENTATION_ROTATE_180;
        } else if (rotationDegrees == 180) {
            return ExifInterface.ORIENTATION_FLIP_VERTICAL;
        } else if (rotationDegrees == 270 && mirrored) {
            return ExifInterface.ORIENTATION_TRANSVERSE;
        } else if (rotationDegrees == 90 && !mirrored) {
            return ExifInterface.ORIENTATION_ROTATE_90;
        } else if (rotationDegrees == 90) {
            return ExifInterface.ORIENTATION_TRANSPOSE;
        } else if (rotationDegrees == 270 && mirrored) {
            return ExifInterface.ORIENTATION_ROTATE_270;
        } else if (rotationDegrees == 270) {
            return ExifInterface.ORIENTATION_TRANSVERSE;
        } else {
            return ExifInterface.ORIENTATION_UNDEFINED;
        }
    }
//    public void open() throws CameraAccessException {
//        CameraManager cameraManager = CameraUtils.getCameraManager(activity);
//        cameraManager.openCamera(
//                cameraProperties.getCameraName(),
//                new CameraDevice.StateCallback() {
//                    @Override
//                    public void onOpened(@NonNull CameraDevice camera) {
//                        cameraDevice = camera;
//                        try {
//                            startPreview(cameraManager);
//                            dartMessenger.sendCameraInitializedEvent(getPreviewSize().getWidth(), getPreviewSize().getHeight());
//                        } catch (Exception e) {
//                            dartMessenger.sendCameraErrorEvent(e.getMessage());
//                        }
//                    }
//
//                    @Override
//                    public void onClosed(@NonNull CameraDevice camera) {
//                        Log.i(TAG, "open | onClosed");
//
//                        cameraDevice = null;
//                        closeCaptureSession();
//                        dartMessenger.sendCameraClosingEvent();
//                    }
//
//                    @Override
//                    public void onDisconnected(@NonNull CameraDevice camera) {
//                        Log.i(TAG, "open | onDisconnected");
//
//                        close();
//                        dartMessenger.sendCameraErrorEvent("The camera was disconnected.");
//                    }
//
//                    @Override
//                    public void onError(@NonNull CameraDevice camera, int error) {
//                        Log.i(TAG, "open | onError");
//
//                        close();
//                        String errorDescription;
//                        switch (error) {
//                            case ERROR_CAMERA_IN_USE:
//                                errorDescription = "The camera device is in use already.";
//                                break;
//                            case ERROR_MAX_CAMERAS_IN_USE:
//                                errorDescription = "Max cameras in use";
//                                break;
//                            case ERROR_CAMERA_DISABLED:
//                                errorDescription = "The camera device could not be opened due to a device policy.";
//                                break;
//                            case ERROR_CAMERA_DEVICE:
//                                errorDescription = "The camera device has encountered a fatal error";
//                                break;
//                            case ERROR_CAMERA_SERVICE:
//                                errorDescription = "The camera service has encountered a fatal error.";
//                                break;
//                            default:
//                                errorDescription = "Unknown camera error";
//                        }
//                        dartMessenger.sendCameraErrorEvent(errorDescription);
//                    }
//                }, cameraHandler);
//    }

    private void startBackgroundThread() {
        if (cameraThread == null && cameraHandler == null) {
            cameraThread = new HandlerThread("CameraThread");
            cameraThread.start();

            cameraHandler = new Handler(cameraThread.getLooper());
        }

        if (imageReaderThread == null && imageReaderHandler == null) {
            imageReaderThread = new HandlerThread("CameraThread");
            imageReaderThread.start();

            imageReaderHandler = new Handler(imageReaderThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (cameraThread != null) {
            cameraThread.quitSafely();
        }
        cameraThread = null;
        cameraHandler = null;

        if (imageReaderThread != null) {
            imageReaderThread.quitSafely();
        }
        imageReaderThread = null;
        imageReaderHandler = null;
    }

    public Size getPreviewSize() throws CameraAccessException {
        CameraManager cameraManager = CameraUtils.getCameraManager(activity);
        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraProperties.getCameraName());

        Size[] sizeArray = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
        Size cameraPreviewSize = new Size(1920, 1080);
        if (!Arrays.asList(sizeArray).contains(cameraPreviewSize)) {
            Display activityDisplay = activity.getWindowManager().getDefaultDisplay();
            Point point = new Point();
            activityDisplay.getRealSize(point);
            cameraPreviewSize = new Size(point.x, point.y);
        }
        return cameraPreviewSize;
    }

    /** Pause the preview from dart. */
    public void pausePreview() throws CameraAccessException {
        this.pausedPreview = true;
        this.captureSession.stopRepeating();
    }

    /** Resume the preview from dart. */
    public void resumePreview() throws CameraAccessException {
        this.pausedPreview = false;
        //this.refreshPreviewCaptureSession(null);
    }

    private void closeCaptureSession() {
        if (captureSession != null) {
            Log.d(TAG, "closeCaptureSession");

            captureSession.close();
            captureSession = null;
        }
    }

    public void close() {
        Log.d(TAG, "close");

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;

            captureSession = null;
        } else {
            closeCaptureSession();
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        stopBackgroundThread();
    }

    public void dispose() {
        Log.d(TAG, "dispose");

        close();
        flutterTexture.release();
    }
}
