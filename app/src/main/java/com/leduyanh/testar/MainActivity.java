package com.leduyanh.testar;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
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
import com.google.ar.sceneform.rendering.AnimationData;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
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

public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener {

    ArFragment arFragment;
    AnchorNode anchorNode;
    ModelAnimator animator;
    int nextAnimation;
    ModelRenderable crabRenderable;
    ModelRenderable animationTrex;
    ModelRenderable beeRenderable;
    ModelRenderable sharkRenderable;

    TransformableNode transformableNode;

    Session session;
    boolean shouldConfigureSession = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);

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
        arFragment.getArSceneView().getScene().addOnUpdateListener(this);
    }

    void setUpSession(){
        if(session == null){
            try {
                session = new Session(this);
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
        Bitmap bitmapDinosaur = loadImage("trex.jpeg");
        Bitmap bitmapBee = loadImage("bee.jpeg");

        if(bitmapCrab == null){
            return false;
        }
        augmentedImageDatabase = new AugmentedImageDatabase(session);
        augmentedImageDatabase.addImage("CRAB",bitmapCrab);
        augmentedImageDatabase.addImage("TREX",bitmapDinosaur);
        augmentedImageDatabase.addImage("BEE",bitmapBee);
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
    Boolean showAntTrex = false;
    Boolean showBee = false;
    MyArNode nodeCrab;

    @Override
    public void onUpdate(FrameTime frameTime) {

        Toast.makeText(this,"Đang quét...",Toast.LENGTH_LONG).show();

        Frame frame = arFragment.getArSceneView().getArFrame();
        Collection<AugmentedImage> updateAugmentImg = frame.getUpdatedTrackables(AugmentedImage.class);
        for(AugmentedImage image: updateAugmentImg){
            if(image.getTrackingState() == TrackingState.TRACKING){
                Toast.makeText(this,image.getName(),Toast.LENGTH_LONG).show();
                if(!showCrab && image.getName().equals("CRAB")){
                    showCrab = true;
                    showModel(R.raw.cangrejo, crabRenderable,R.raw.crab,R.raw.crab,true,image);

                }else if(!showBee && image.getName().equals("BEE")){
                    showBee = true;
                    showModel(R.raw.bee,beeRenderable,R.raw.trexsound,R.raw.destrex,false,image);
                }
            }
        }
    }

    // hiển thị con vât, nhận vào renderable, tiếng kêu, mô tả
    void showModel(int model,ModelRenderable renderable,int shout, int description, Boolean animation,AugmentedImage image){
        MediaPlayer mediaPlayer = MediaPlayer.create(this,shout);
        mediaPlayer.start();

        nodeCrab = new MyArNode(this,model);
        nodeCrab.setImage(image,renderable);
        nodeCrab.setParent(arFragment.getArSceneView().getScene());

        nodeCrab.setOnTapListener(new Node.OnTapListener() {
            @Override
            public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                MediaPlayer mediaPlayer = MediaPlayer.create(MainActivity.this,description);
                mediaPlayer.start();
            }
        });

        if(animation){
            transformableNode = new TransformableNode(arFragment.getTransformationSystem());
            transformableNode.getScaleController().setMinScale(0.09f);
            transformableNode.getScaleController().setMaxScale(0.1f);
            transformableNode.setParent(anchorNode);
            transformableNode.setRenderable(renderable);
            transformableNode.getRotationController().setEnabled(true);

            if(animator == null || !animator.isRunning()){
                AnimationData data = renderable.getAnimationData(0);
                animator = new ModelAnimator(data,renderable);
                animator.start();
            }
        }
    }

    void setUpModel(){
        ModelRenderable.builder()
                .setSource(this,R.raw.cangrejo)
                .build()
                .thenAccept(renderable-> crabRenderable = renderable)
                .exceptionally(throwable->{
                    Toast.makeText(this,""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                    return null;
                });

        ModelRenderable.builder()
                .setSource(this,R.raw.trex)
                .build()
                .thenAccept(renderable->animationTrex = renderable)
                .exceptionally(throwable->{
                    Toast.makeText(this,""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                    return null;
                });

        ModelRenderable.builder()
                .setSource(this,R.raw.bee)
                .build()
                .thenAccept(renderable->beeRenderable = renderable)
                .exceptionally(throwable->{
                    Toast.makeText(this,""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                    return null;
                });

        ModelRenderable.builder()
                .setSource(this,R.raw.shark)
                .build()
                .thenAccept(renderable->sharkRenderable = renderable)
                .exceptionally(throwable->{
                    Toast.makeText(this,""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                    return null;
                });
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
            arFragment.getArSceneView().pause();
            session.pause();
        }
    }
}
