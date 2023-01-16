import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:native_camera2/native_android_camera2.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([DeviceOrientation.portraitDown, DeviceOrientation.portraitUp]);
  runApp(
    const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: NativeCameraPreview(),
    )
  );
}

class NativeCameraPreview extends StatefulWidget {

  const NativeCameraPreview({Key? key}) : super(key: key);

  @override
  State<NativeCameraPreview> createState() => _NativeCameraPreviewState();
}

class _NativeCameraPreviewState extends State<NativeCameraPreview> with WidgetsBindingObserver {
  
  List<NativeCameraDescription> _nativeCameraDescriptionList = [];
  NativeCameraDescription? _selectedCameraDescription;

  Size? previewSize;

  /// The id of a camera that hasn't been initialized.
  @visibleForTesting
  static const int kUninitializedCameraId = -1;
  int _cameraId = kUninitializedCameraId;

  bool _isInitialized = false, _isDisposed = false;
  ValueNotifier<bool> cameraInitializeNotifier = ValueNotifier(false);

  final ValueNotifier<List<File>> _capturedIMGNotifier = ValueNotifier(<File>[]);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    WidgetsBinding.instance.addPostFrameCallback((timeStamp) async {
      _nativeCameraDescriptionList = await availableCameras();
      if (_nativeCameraDescriptionList.isNotEmpty) {
        _selectedCameraDescription = _nativeCameraDescriptionList.first;
        bool? cameraPermission = await androidCameraPermissionReq();
        if (cameraPermission != null && cameraPermission) {
          Future.delayed(const Duration(seconds: 2))
          .whenComplete(() => initialize());
        }
      }
    });
  }

  @override
  void dispose() {
    stopUsingCameraInBG();
    cameraInitializeNotifier.dispose();
    _capturedIMGNotifier.dispose();
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      initialize();
    } else if (state == AppLifecycleState.inactive) {
      stopUsingCameraInBG(); 
    }
  }
  
  @override
  Widget build(BuildContext context) {
    final screenSize = MediaQuery.of(context).size;

    double cameraAspectRatio = 1.0;
    double screenAspectRatio = screenSize.aspectRatio;

    if (previewSize != null) {
      cameraAspectRatio = previewSize!.width / previewSize!.height;
    } 

    var scale = screenAspectRatio * cameraAspectRatio;
    if (scale < 1) {
      scale = 1 / scale;
    }

    return Scaffold(
      body: Stack(
        children: [

          Center(
            child: ValueListenableBuilder<bool>(
              valueListenable: cameraInitializeNotifier,
              builder: (context, value, child) {
                if (value) {
                  return AspectRatio(
                    aspectRatio: 1 / cameraAspectRatio,
                    child: Stack(
                      children: [
                        
                        Transform.scale(
                          scale: scale,
                          alignment: Alignment.center,
                          child: Texture(textureId: _cameraId),
                        )

                      ],
                    ),
                  );
                }
                return const SizedBox.shrink();
              },
            ),
          ),

          Positioned(
            bottom: 40.0,
            left: 0.0,
            right: 0.0,
            child: GestureDetector(
              onTap: () async {
                String? capturedIMGPath = await NativeCamera2Platform.instance.takePicture();
                if (capturedIMGPath != null) {
                  _capturedIMGNotifier.value.add(File(capturedIMGPath));
                  debugPrint("CapturedIMGPath: $capturedIMGPath");
                  setState(() {});
                }
              },
              child: Container(
                height: 100.0,
                width: 100.0,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white
                ),
              ),
            ),
          ),

          Positioned(
            bottom: 180.0,
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 8.0),
              child: ValueListenableBuilder<List<File>>(
                valueListenable: _capturedIMGNotifier,
                builder: (context, value, child) {
                  return Row(
                    mainAxisSize: MainAxisSize.max,
                    children: value.map((e) {
                      if (e.existsSync()) {
                        return Container(
                          margin: const EdgeInsets.symmetric(horizontal: 8.0),
                          child: GestureDetector(
                            onTap: () async {
                              await Navigator.push(context, MaterialPageRoute(builder: (context) => CameraIMGPreview(capturedIMG: value)));
                              _capturedIMGNotifier.value.clear();
                              setState(() {
                                
                              });
                            },
                            child: SizedBox(
                              height: 80.0,
                              width: 80.0,
                              child: Image.file(e, fit: BoxFit.fill,),
                            ),
                          ),
                        );
                      } else {
                        return const SizedBox.shrink();
                      }
                    }).toList(),
                  );
                },
              ),
            ),
          ),

        ],
      ),
    );
  }

  Future<List<NativeCameraDescription>> availableCameras() async {
    return NativeCamera2Platform.instance.availableCameras();
  }

  /// Checks whether [CameraController.dispose] has completed successfully.
  ///
  /// This is a no-op when asserts are disabled.
  void debugCheckIsDisposed() {
    assert(_isDisposed);
  }

  /// The camera identifier with which the controller is associated.
  int get cameraId => _cameraId;

  Future<void> initialize() async {
    try {

      if (_isDisposed) {
        throw CameraException('Disposed CameraController', 'initialize was called on a disposed CameraController',);
      }
      _cameraId = await NativeCamera2Platform.instance.createCamera(_selectedCameraDescription!,);
      previewSize = await NativeCamera2Platform.instance.previewSize();
      debugPrint("Preview Size: $previewSize");
      _isInitialized = true;
      debugPrint("isInitialized: $_isInitialized");
      cameraInitializeNotifier.value = _isInitialized;
      if (!mounted) return;
      setState(() {});
      await NativeCamera2Platform.instance.initializeCamera(_cameraId,);
    } on PlatformException catch (e) {
      debugPrint("Exception: $e");
      throw CameraException(e.code, e.message);
    } on CameraException catch (e) {
      throw CameraException(e.code, e.description);
    }
  }

  Future<void> stopUsingCameraInBG() async {
    await NativeCamera2Platform.instance.dispose();
    if (!mounted) return;
    setState(() {});
  }

  Future<bool?> androidCameraPermissionReq() async {
    var cameraPermissionStatus = await Permission.camera.request();
    if (cameraPermissionStatus == PermissionStatus.denied) {
      return false;
    } else if (cameraPermissionStatus == PermissionStatus.permanentlyDenied) {
      return null;
    } else {
      return true;
    }
  }
}

class CameraIMGPreview extends StatefulWidget {

  final List<File> capturedIMG;

  const CameraIMGPreview({Key? key, required this.capturedIMG}) : super(key: key);

  @override
  State<CameraIMGPreview> createState() => _CameraIMGPreviewState();
}

class _CameraIMGPreviewState extends State<CameraIMGPreview> {

  late PageController _pageController;

  @override
  void initState() {
    super.initState();
    _pageController = PageController(
      initialPage: 0,
      keepPage: true,
      viewportFraction: 1.0
    );
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final screenSize = MediaQuery.of(context).size;
    return Scaffold(
      body: PageView.builder(
        allowImplicitScrolling: false,
        controller: _pageController,
        itemCount: widget.capturedIMG.length,
        padEnds: false,
        pageSnapping: true,
        scrollDirection: Axis.horizontal,
        itemBuilder: (context, index) {
          if (widget.capturedIMG[index].existsSync()) {
            return Center(
              child: Image.file(
                widget.capturedIMG[index],
                fit: BoxFit.fill,
              ),
            );
          }
          return const SizedBox.shrink();
        },
      ),
    );
  }
}