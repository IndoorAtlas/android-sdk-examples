package com.indooratlas.android.sdk.examples.ar;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.widget.Toast;

import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;

import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IAWayfindingRequest;
import com.indooratlas.android.sdk.IAARObject;

import com.indooratlas.android.sdk.examples.ar.helpers.CameraPermissionHelper;
import com.indooratlas.android.sdk.examples.ar.helpers.DisplayRotationHelper;
import com.indooratlas.android.sdk.examples.ar.helpers.FullScreenHelper;
import com.indooratlas.android.sdk.examples.ar.helpers.SnackbarHelper;
import com.indooratlas.android.sdk.examples.ar.helpers.TrackingStateHelper;
import com.indooratlas.android.sdk.examples.ar.rendering.BitmapSignRenderer;
import com.indooratlas.android.sdk.examples.ar.rendering.BorderEffect;
import com.indooratlas.android.sdk.examples.ar.rendering.ObjectRenderer;
import com.indooratlas.android.sdk.examples.ar.rendering.ObjectRenderer.BlendMode;
import com.indooratlas.android.sdk.examples.ar.wrapper.Api;
import com.indooratlas.android.sdk.examples.ar.wrapper.Implementation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import timber.log.Timber;

/**
 * Third party AR wayfinding example.
 * The wayfinding will always navigate to the first POI found on the map.
 */
@SdkExample(description = R.string.example_thirdpartyar_description)
public class WayfindingAr extends AppCompatActivity implements GLSurfaceView.Renderer {
  private static final boolean ENABLE_BORDER_EFFECT = true; // disable to debug rendering issues

  private WayfindingSession wayfindingSession;
  private Api arWrapper;

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;

  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);

  static class ArPOIRenderer {
    IAARObject object;
    BitmapSignRenderer renderer;
    float scale;
  }

  private List<ArPOIRenderer> arPois = new ArrayList<>();

  private final ObjectRenderer destinationObject = new ObjectRenderer();
  private final ObjectRenderer arrowObject = new ObjectRenderer();
  private final ObjectRenderer poiObject = new ObjectRenderer();
  private final BorderEffect borderEffect = new BorderEffect();
  private final BitmapSignRenderer.Cache bitmapSignRendererCache = new BitmapSignRenderer.Cache();

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private final float[] anchorMatrix = new float[16];
  private static final float[] TARGET_COLOR = new float[]{255, 255, 255, 255};
  private static final float[] ARROW_COLOR = new float[]{50, 128, 247, 255};
  private static final float[] WAYPOINT_COLOR = new float[]{95, 209, 195, 255};

  private static final String NO_CONVERGENCE_MESSAGE = "Walk 20 meters to any direction so we can orient you. Avoid pointing the camera at blank walls.";
  final float[] colorCorrectionRgba = new float[]{ 1, 1, 1, 1 };

  private IALocationManager mLocationManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ar);
    mLocationManager = IALocationManager.create(this);

    surfaceView = findViewById(R.id.surfaceview);
    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    // Set up renderer.
    surfaceView.setPreserveEGLContextOnPause(true);
    surfaceView.setEGLContextClientVersion(2);
    surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
    surfaceView.setRenderer(this);
    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    surfaceView.setWillNotDraw(false);

    arWrapper = Implementation.createArWrapper();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mLocationManager.destroy();
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (wayfindingSession != null) {
      wayfindingSession.destroy();
    }

    wayfindingSession = new WayfindingSession(
            mLocationManager,
            new WayfindingSession.Callbacks() {
              @Override
              public void onArPoisUpdated(List<WayfindingSession.ArPOI> pois) {
                // note: atomic in Java
                List<ArPOIRenderer> renderers = new ArrayList<>();
                for (WayfindingSession.ArPOI poi : pois) {
                  ArPOIRenderer r = new ArPOIRenderer();
                  r.renderer = bitmapSignRendererCache.get(poi.textureName, WayfindingAr.this);
                  r.object = wayfindingSession.getArSdk().createArPOI(
                          poi.iaPoi.getLocation().latitude,
                          poi.iaPoi.getLocation().longitude,
                          poi.iaPoi.getFloor(),
                          poi.heading,
                          poi.elevation);
                  r.scale = poi.scale;
                  renderers.add(r);
                }
                arPois = renderers;
                if (!pois.isEmpty()) {
                  WayfindingSession.ArPOI target = pois.get(0);
                  IAWayfindingRequest request = new IAWayfindingRequest.Builder()
                          .withFloor(target.iaPoi.getFloor())
                          .withLatitude(target.iaPoi.getLocation().latitude)
                          .withLongitude(target.iaPoi.getLocation().longitude)
                          .build();
                  wayfindingSession.setWayfindingTarget(request);
                }
              }
            });

    Api.ResumeResult result = arWrapper.handleInstallFlowAndResume(this);
    switch (result.status) {
      case ERROR:
        messageSnackbarHelper.showError(this, result.errorMessage);
        return;
      case PENDING: return;
      case SUCCESS: break;
    }

    surfaceView.onResume();
    displayRotationHelper.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();

    if (wayfindingSession != null) {
      wayfindingSession.destroy();
      wayfindingSession = null;
    }

    if (arWrapper.isRunning()) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      arWrapper.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
              .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  private void setupObject(ObjectRenderer obj, String modelName, String textureName) throws IOException {
    obj.createOnGlThread(/*context=*/ this, modelName, textureName);
    obj.setBlendMode(BlendMode.AlphaBlending);
    arWrapper.setupObject(this, obj);
    obj.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
    try {
      arWrapper.onSurfaceCreated(this);
      borderEffect.createOnGlThread(this);

      setupObject(destinationObject, "models/finish.obj", "models/finish.png");
      setupObject(arrowObject, "models/arrow_stylish.obj", "models/white2x2pixels.png");
      setupObject(poiObject, "models/andy.obj", "models/andy.png");

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    GLES20.glViewport(0, 0, width, height);
    borderEffect.onSurfaceChanged(width, height);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.0f); // set alpha to 0 to help post-processing
    // Clear screen to notify driver it should not load any pixels from previous frame.
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    List<ArPOIRenderer> curArPois = arPois; // atomic get

    for (ArPOIRenderer r : curArPois) {
      r.renderer.createOnGlThread(this); // no-op if already initialized
    }

    if (!arWrapper.isRunning()) {
      return;
    }

    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    displayRotationHelper.updateSessionIfNeeded(arWrapper);

    try {
      arWrapper.onFrame();

      // ARCore depth API stuff
      float[] uvTransforms = arWrapper.getUpdatedUvTransformMatrix();
      if (uvTransforms != null) {
        destinationObject.setUvTransformMatrix(uvTransforms);
        arrowObject.setUvTransformMatrix(uvTransforms);
      }

      // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
      trackingStateHelper.updateKeepScreenOnFlag(arWrapper.shouldKeepScreenOn());

      // If not tracking, don't draw 3D objects, show tracking failure reason instead.
      String trackingFailureReason = arWrapper.getTrackingFailureReasonString();
      if (trackingFailureReason != null) {
        messageSnackbarHelper.showMessage(this, trackingFailureReason);
        return;
      }

      // Get projection matrix.
      float[] projmtx = new float[16];
      arWrapper.getProjectionMatrix(projmtx, 0.1f, 100.0f);

      // Get camera matrix and draw.
      float[] viewmtx = new float[16];
      arWrapper.getViewMatrix(viewmtx);

      // Compute lighting from average intensity of the image.
      // The first three components are color scaling factors.
      // The last one is the average pixel intensity in gamma space.
      arWrapper.getColorCorrection(colorCorrectionRgba);

      // No tracking error at this point. If we detected any plane, then hide the
      // message UI, otherwise show searchingPlane message.
      if (wayfindingSession.converged()) {
        messageSnackbarHelper.hide(this);
      } else {
        messageSnackbarHelper.showMessage(this, NO_CONVERGENCE_MESSAGE);
      }

      float [] invViewMat = new float[16];
      float [] imuToWorld = new float[16];
      arWrapper.getCameraToWorldMatrix(invViewMat);
      arWrapper.getImuToWorldMatrix(imuToWorld);
      wayfindingSession.onArFrame(imuToWorld, invViewMat, arWrapper.getHorizontalPlanes());

      if (!wayfindingSession.converged()) {
        return;
      }

      if (ENABLE_BORDER_EFFECT) {
        borderEffect.beginCapture();
      }

      float scaleFactor;
      for (IAARObject wp : wayfindingSession.getWaypoints()) {
        scaleFactor = 0.4f;
        if (wp.updateModelMatrix(anchorMatrix)) {
          arrowObject.updateModelMatrix(anchorMatrix, scaleFactor);
          arrowObject.draw(viewmtx, projmtx, colorCorrectionRgba, WAYPOINT_COLOR);
        }
      }

      for (ArPOIRenderer arPoi : curArPois) {
        if (!arPoi.object.updateModelMatrix(anchorMatrix)) {
          arPoi.renderer.draw(viewmtx, projmtx, anchorMatrix, arPoi.scale);
        }
      }

      if (wayfindingSession.getArSdk().getWayfindingTarget().updateModelMatrix(anchorMatrix)) {
        scaleFactor = 1;
        destinationObject.updateModelMatrix(anchorMatrix, scaleFactor);
        destinationObject.draw(viewmtx, projmtx, colorCorrectionRgba, TARGET_COLOR);
      }

      if (wayfindingSession.getArSdk().getWayfindingCompassArrow().updateModelMatrix(anchorMatrix)) {
        scaleFactor = 0.3f;
        arrowObject.updateModelMatrix(anchorMatrix, scaleFactor);
        arrowObject.draw(viewmtx, projmtx, colorCorrectionRgba, ARROW_COLOR);
      }

      if (ENABLE_BORDER_EFFECT) {
        borderEffect.endCapture();
        borderEffect.render();
      }

    } catch (Throwable t) {
      // Avoid crashing the application due to unhandled exceptions.
      Timber.e(t, "Exception on the OpenGL thread");
    }
  }
}
