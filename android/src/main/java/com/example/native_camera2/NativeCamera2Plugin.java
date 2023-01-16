package com.example.native_camera2;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.view.TextureRegistry;

/** NativeCamera2Plugin */
public class NativeCamera2Plugin implements FlutterPlugin, ActivityAware {

  private @Nullable
  FlutterPluginBinding flutterPluginBinding;
  private @Nullable MethodCallHandlerImpl methodCallHandler;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    this.flutterPluginBinding = binding;
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    this.flutterPluginBinding = null;
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    maybeStartListening(binding.getActivity(), flutterPluginBinding.getBinaryMessenger(), flutterPluginBinding.getTextureRegistry());
  }

  @Override
  public void onDetachedFromActivity() {
    // Could be on too low of an SDK to have started listening originally.
    if (methodCallHandler != null) {
      methodCallHandler.stopListening();
      methodCallHandler = null;
    }
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }

  private void maybeStartListening(Activity activity, BinaryMessenger messenger, TextureRegistry textureRegistry) {
    methodCallHandler = new MethodCallHandlerImpl(activity, messenger, textureRegistry);
  }

}
