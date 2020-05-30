package com.leduyanh.testar;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.AnimationData;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener, RotationGestureDetector.OnRotationGestureListener {

    ArFragment arFragment;
    AnchorNode anchorNode;
    ModelAnimator animator;
    int nextAnimation = 0;
    int nextAntAnimation = 0;
    FloatingActionButton btnAnim;
    ModelRenderable animationcrab;
    ModelRenderable animationAnt;

    TransformableNode transformableNode;

    ArSceneView arSceneView;
    private GLSurfaceView surfaceView;
    Session session;
    boolean shouldConfigureSession = false;
    private RotationGestureDetector mRotationDetector;
    private GestureDetector gestureDetector;
    private MotionEvent motionEvent;
    private int mPtrCount = 0;
    private final ArrayBlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(16);
    private MyScaleGestures scaleGestureDetector;
    private boolean installRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //arSceneView = (ArSceneView)findViewById(R.id.arView);
        arFragment = (ArFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
        mRotationDetector = new RotationGestureDetector(this);
        // Set up tap listener.
        gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        queuedSingleTaps.offer(motionEvent);
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        GlobalClass.scaleFactor += GlobalClass.scaleFactor;
                        return true;
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        if (mPtrCount < 2) {
                            queuedSingleTaps.offer(motionEvent);
                            return true;
                        } else
                            return false;
                    }
                });


        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        setUpSession();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this,"chưa cấp quyền",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

        initSceneView();
        setUpModel();
    }

    private void initSceneView() {
        //arSceneView.getScene().addOnUpdateListener(this);
        arFragment.getArSceneView().getScene().addOnUpdateListener(this);
    }

    void setUpSession(){
        if(session == null){
            try {
                session = new Session(this);
//                Config config = new Config(session);
//                config.setFocusMode(Config.FocusMode.AUTO);
//
//                session.configure(config);
                CameraConfigFilter filter = new CameraConfigFilter(session);
                filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
                filter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.
                        DO_NOT_USE));
                CameraConfig[] cameraConfigList = session.getSupportedCameraConfigs(filter).toArray(new CameraConfig[0]);
                session.setCameraConfig(cameraConfigList[0]);

            } catch (UnavailableApkTooOldException e) {
                e.printStackTrace();
            } catch (UnavailableDeviceNotCompatibleException e) {
                e.printStackTrace();
            } catch (UnavailableArcoreNotInstalledException e) {
                e.printStackTrace();
            } catch (UnavailableSdkTooOldException e) {
                e.printStackTrace();
            }
            shouldConfigureSession = true;
        }

        if(shouldConfigureSession){
            configSession();
            shouldConfigureSession = false;
            arFragment.getArSceneView().setupSession(session);
        }

        try {
            session.resume();
            //arSceneView.resume();
            arFragment.getArSceneView().resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
            session = null;
            return;
        }
    }

    private void configSession() {
        Config config = new Config(session);
        if(!builDatabase(config)){
            Toast.makeText(MainActivity.this,"lỗi database",Toast.LENGTH_SHORT).show();
        }
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        session.configure(config);
    }

    private boolean builDatabase(Config config) {
        AugmentedImageDatabase augmentedImageDatabase;
        Bitmap bitmapCrab = loadImage("crab.jpeg");
        Bitmap bitmapAnt = loadImage("ant.jpeg");
        Bitmap bitmapDeer = loadImage("deer.jpg");

        if(bitmapCrab == null || bitmapAnt == null || bitmapDeer == null){
            return false;
        }
        augmentedImageDatabase = new AugmentedImageDatabase(session);
        augmentedImageDatabase.addImage("CRAB",bitmapCrab);
        augmentedImageDatabase.addImage("ANT",bitmapAnt);
        augmentedImageDatabase.addImage("DEER", bitmapDeer);
        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }

    private Bitmap loadImage(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    Boolean showCrab = false;
    Boolean showAnt = false;
    MyArNode nodeCrab;
    @Override
    public void onUpdate(FrameTime frameTime) {
        //Toast.makeText(this,"ok",Toast.LENGTH_LONG).show();

            Toast.makeText(this,"Đang quét...",Toast.LENGTH_LONG).show();

        if (nodeCrab!=null){
            Quaternion q1 = nodeCrab.getLocalRotation();
            Quaternion q2 = Quaternion.axisAngle(new Vector3(0, 1f, 0f), .2f);
            nodeCrab.setLocalRotation(Quaternion.multiply(q1, q2));
        }

            //Frame frame = arSceneView.getArFrame();
            Frame frame = arFragment.getArSceneView().getArFrame();
            Collection<AugmentedImage> updateAugmentImg = frame.getUpdatedTrackables(AugmentedImage.class);
            for(AugmentedImage image: updateAugmentImg){
                if(image.getTrackingState() == TrackingState.TRACKING){
                    Toast.makeText(this,image.getName(),Toast.LENGTH_LONG).show();
                    if(!showCrab && image.getName().equals("DEER")){
                        showCrab = true;
                        MediaPlayer mediaPlayer = MediaPlayer.create(this,R.raw.crab);
                        mediaPlayer.start();

                        nodeCrab = new MyArNode(this,R.raw.deer);
                        nodeCrab.setImage(image,animationcrab);
                        //arSceneView.getScene().addChild(node);
                        nodeCrab.setParent(arFragment.getArSceneView().getScene());

//                        nodeCrab.setOnTapListener(new Node.OnTapListener() {
//                            @Override
//                            public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
//                                MediaPlayer mediaPlayer = MediaPlayer.create(MainActivity.this,R.raw.crab);
//                                mediaPlayer.start();
//
//                                arFragment.getArSceneView().getScene().removeChild(nodeCrab);
//                                showCrab = false;
//                            }

//                        transformableNode = new TransformableNode(arFragment.getTransformationSystem());
//                        transformableNode.getScaleController().setMinScale(0.09f);
//                        transformableNode.getScaleController().setMaxScale(0.1f);
//                        transformableNode.setParent(anchorNode);
//                        transformableNode.setRenderable(animationcrab);
//
//                        if(animator == null || !animator.isRunning()){
//                            AnimationData data = animationcrab.getAnimationData(nextAnimation);
//                            nextAnimation = (nextAnimation+1)%animationcrab.getAnimationDataCount();
//                            animator = new ModelAnimator(data,animationcrab);
//                            animator.start();
//                        }
                        //setUpModel();
                    }else if(!showAnt && image.getName().equals("ANT")){
                        showAnt = true;
//                        MediaPlayer mediaPlayer = MediaPlayer.create(this,R.raw.crab);
//                        mediaPlayer.start();

//                        node = new MyArNode(this,R.raw.ant);
//                        node.setImage(image,animationAnt);
//                        //arSceneView.getScene().addChild(node);
//                        node.setParent(arFragment.getArSceneView().getScene());
                    }
                }
            }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        setUpSession();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this,"chưa cấp quyền",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(session != null){
            //arSceneView.pause();
            arFragment.getArSceneView().pause();
            session.pause();
        }
    }

    //    void demoAR(){
//        arFragment = (ArFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
//
//        arFragment.setOnTapArPlaneListener(new BaseArFragment.OnTapArPlaneListener() {
//            @Override
//            public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
//                if(animationcrab == null){
//                    return;
//                }
//                Anchor anchor = hitResult.createAnchor();
//                //if(anchorNode == null){
//                anchorNode = new AnchorNode(anchor);
//                anchorNode.setParent(arFragment.getArSceneView().getScene());
//
//                transformableNode = new TransformableNode(arFragment.getTransformationSystem());
//                transformableNode.getScaleController().setMinScale(0.09f);
//                transformableNode.getScaleController().setMaxScale(0.1f);
//                transformableNode.setParent(anchorNode);
//                transformableNode.setRenderable(animationcrab);
//                //}
//            }
//        });
//
//        arFragment.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
//            @Override
//            public void onUpdate(FrameTime frameTime) {
//                if(anchorNode == null){
//                    if(btnAnim.isEnabled()){
//                        btnAnim.setEnabled(false);
//                    }
//                }else {
//                    if(!btnAnim.isEnabled()){
//                        btnAnim.setEnabled(true);
//                    }
//                }
//            }
//        });
//
//        btnAnim = (FloatingActionButton)findViewById(R.id.btn_anim);
//        btnAnim.setEnabled(false);
//        btnAnim.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(animator == null || !animator.isRunning()){
//                    AnimationData data = animationcrab.getAnimationData(nextAnimation);
//                    nextAnimation = (nextAnimation+1)%animationcrab.getAnimationDataCount();
//                    animator = new ModelAnimator(data,animationcrab);
//                    animator.start();
//                }
//            }
//        });
//
//        setUpModel();
//    }
//
    void setUpModel(){
        ModelRenderable.builder()
                .setSource(this,R.raw.cangrejo)
                .build()
                .thenAccept(renderable->animationcrab = renderable)
                .exceptionally(throwable->{
                    Toast.makeText(this,""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                    return null;
                });

        ModelRenderable.builder()
                .setSource(this,R.raw.ant)
                .build()
                .thenAccept(renderable->animationAnt = renderable)
                .exceptionally(throwable->{
                    Toast.makeText(this,""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                    return null;
                });
        ModelRenderable.builder()
                .setSource(this,R.raw.deer)
                .build()
                .thenAccept(renderable->animationAnt = renderable)
                .exceptionally(throwable->{
                    Toast.makeText(this,""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                    return null;
                });
    }

    @Override
    public void OnRotation(RotationGestureDetector rotationDetector) {
        GlobalClass.rotateF = GlobalClass.rotateF + rotationDetector.getAngle() / 10;
    }
}
