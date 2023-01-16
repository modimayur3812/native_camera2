import 'package:flutter/material.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'native_camera2_method_channel.dart';
import 'types/camera_description.dart';

abstract class NativeCamera2Platform extends PlatformInterface {
  
  NativeCamera2Platform() : super(token: _token);

  static final Object _token = Object();

  static NativeCamera2Platform _instance = MethodChannelNativeCamera2();

  static NativeCamera2Platform get instance => _instance;
  
  static set instance(NativeCamera2Platform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<List<NativeCameraDescription>> availableCameras() {
    throw UnimplementedError('availableCameras() has not been implemented.');
  }

  Future<int> createCamera(NativeCameraDescription cameraDescription) {
    throw UnimplementedError('createCamera() has not been implemented.');
  }

  Future<void> initializeCamera(int cameraId) {
    throw UnimplementedError('initializeCamera() has not been implemented.');
  }

  Future<Size?> previewSize() {
    throw UnimplementedError('previewSize() has not been implemented.');
  }

  Future<void> dispose() {
    throw UnimplementedError('dispose() has not been implemented.');
  }

  Future<String?> takePicture() {
    throw UnimplementedError('takePicture() has not been implemented.');
  }
  
}
