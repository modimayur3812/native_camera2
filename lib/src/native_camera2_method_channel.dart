import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'native_camera2_platform_interface.dart';
import 'types/camera_description.dart';
import 'types/camera_exception.dart';
import 'utils/utils.dart';


class MethodChannelNativeCamera2 extends NativeCamera2Platform {
  
  @visibleForTesting
  final methodChannel = const MethodChannel('plugins.flutter.io/native_android2');

  @override
  Future<List<NativeCameraDescription>> availableCameras() async {
    try {
      final List<Map<dynamic, dynamic>>? cameras = await methodChannel.invokeListMethod<Map<dynamic, dynamic>>('availableCameras');

      if (cameras == null) {
        return <NativeCameraDescription>[];
      }

      return cameras.map((Map<dynamic, dynamic> camera) {
        return NativeCameraDescription(
          name: camera['name']! as String,
          lensDirection: parseCameraLensDirection(camera['lensFacing']! as String),
          sensorOrientation: camera['sensorOrientation']! as int,
        );
      }).toList();
    } on PlatformException catch (e) {
      throw CameraException(e.code, e.message);
    }
  }

  @override
  Future<int> createCamera(NativeCameraDescription cameraDescription) async {
    try {
      final Map<String, dynamic>? reply = await methodChannel.invokeMapMethod<String, dynamic>('create', <String, dynamic>{'cameraName': cameraDescription.name,});
      return reply!['cameraId']! as int;
    } on PlatformException catch (e) {
      throw CameraException(e.code, e.message);
    }
  }

  @override
  Future<void> initializeCamera(int cameraId) {
    final Completer<void> completer = Completer<void>();
    methodChannel.invokeMapMethod<String, dynamic>('initialize').catchError((Object error, StackTrace stackTrace) {
        if (error is! PlatformException) {
          throw error;
        }
        completer.completeError(CameraException(error.code, error.message), stackTrace,);
      },
    );

    return completer.future;
  }

  @override
  Future<Size?> previewSize() async {
    final Map<String, dynamic>? reply = await methodChannel.invokeMapMethod<String, dynamic>('PreviewSize');
    if (reply != null) {
      return Size(double.tryParse(reply['Width']) ?? 0.0, double.tryParse(reply['Height']) ?? 0.0);
    }
    return null;
  }

  @override
  Future<void> dispose() async {
    await methodChannel.invokeMethod<void>('dispose');
  }

  @override
  Future<String?> takePicture() async {
    return await methodChannel.invokeMethod<String>('takePicture');
  }
}
