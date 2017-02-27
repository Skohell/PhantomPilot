package com.dji.FPVDemo;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.battery.DJIBatteryState;
import dji.common.camera.CameraSystemState;
import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.common.product.Model;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.battery.DJIBattery;
import dji.sdk.camera.DJICamera;
import dji.sdk.camera.DJICamera.CameraReceivedVideoDataCallback;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;


public class MainActivity extends Activity implements SurfaceTextureListener,OnClickListener{


    /* ------------------------------ ELEMENTS GRAPHIQUES ------------------------------*/
    // Retour vidéo
    protected TextureView mVideoSurface = null;

    // Médias
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
    private ToggleButton mRecordBtn;
    private TextView recordingTime;

    private Button mdecollerBtn;
    private Button matterrirBtn;
    private Button mswitchControlBtn;

    private TextView mbattery_level;
    private TextView mgps;

    //Joysticks
    private OnScreenJoystick mScreenJoystickRight;
    private OnScreenJoystick mScreenJoystickLeft;

    private Button mleftUp;
    private Button mleftLeft;
    private Button mleftRight;
    private Button mleftDown;
    private Button mrightUp;
    private Button mrightLeft;
    private Button mrightRight;
    private Button mrightDown;

    /* --------------------------------------------------------------------------------*/

    /* ------------------------------- ATTRIBUTS ------------------------------------- */

    private boolean isJoystickVisible = true;

    private static final String TAG = MainActivity.class.getName();

    protected DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec pour la vidéo
    protected DJICodecManager mCodecManager = null;

    // Informations des joysticks
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;

    protected StringBuffer mBatteryStringBuffer;
    protected StringBuffer mGpsStringBuffer;
    protected static final int CHANGE_TEXT_VIEW = 0;



    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;


    protected Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case CHANGE_TEXT_VIEW :
                    mbattery_level.setText(mBatteryStringBuffer.toString());
                    mgps.setText(mGpsStringBuffer.toString());
                    break;

                default:
                    break;
            }
            return false;
        }
    });

    /* ----------------------------------------------------------------------------------*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        mBatteryStringBuffer = new StringBuffer();
        mGpsStringBuffer = new StringBuffer();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new CameraReceivedVideoDataCallback() {

            @Override
            public void onResult(byte[] videoBuffer, int size) {
                if(mCodecManager != null){
                    // Send the raw H264 video data to codec manager for decoding
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }else {
                    Log.e(TAG, "mCodecManager is null");
                }
            }
        };

        DJICamera camera = FPVDemoApplication.getCameraInstance();

        if (camera != null) {

            camera.setDJICameraUpdatedSystemStateCallback(new DJICamera.CameraUpdatedSystemStateCallback() {
                @Override
                public void onResult(CameraSystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                recordingTime.setText(timeString);

                                /*
                                 * Update recordingTime TextView visibility and mRecordBtn's check state
                                 */
                                if (isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else
                                {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });

        }

        try {
            FPVDemoApplication.getProductInstance().getBattery().setBatteryStateUpdateCallback(
                    new DJIBattery.DJIBatteryStateUpdateCallback() {
                        @Override
                        public void onResult(DJIBatteryState djiBatteryState) {
                            mBatteryStringBuffer.delete(0, mBatteryStringBuffer.length());

                            mBatteryStringBuffer.append("Battery: ").
                                    append(djiBatteryState.getBatteryEnergyRemainingPercent()).
                                    append("%\n");

                            mHandler.sendEmptyMessage(CHANGE_TEXT_VIEW);
                        }
                    }
            );

            FPVDemoApplication.getAircraftInstance().getFlightController().setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                @Override
                public void onResult(DJIFlightControllerCurrentState djiFlightControllerCurrentState) {
                    mGpsStringBuffer.delete(0, mGpsStringBuffer.length());

                    mGpsStringBuffer.append("Altitude : ").
                            append(djiFlightControllerCurrentState.getAircraftLocation().getAltitude()).
                            append("m. ").
                            append("Latitude : ").
                            append(String.format("%.5f",djiFlightControllerCurrentState.getAircraftLocation().getLatitude())).
                            append("Longitude :").
                            append(String.format("%.5f",djiFlightControllerCurrentState.getAircraftLocation().getLongitude()));


                            mHandler.sendEmptyMessage(CHANGE_TEXT_VIEW);
                }
            });



        } catch (Exception exception) {

        }


        FPVDemoApplication.getAircraftInstance().getFlightController().enableVirtualStickControlMode(
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null){
                            showToast(djiError.getDescription());
                        }else
                        {
                            showToast("Sticks activés");
                        }
                    }
                }
        );

    }

    protected void onProductChange() {
        initPreviewer();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initUI() {

        // Retour vidéo
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        // Bouttons médias
        recordingTime = (TextView) findViewById(R.id.timer);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
        /*
        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);*/

        mdecollerBtn = (Button) findViewById(R.id.btn_decoller);
        matterrirBtn = (Button) findViewById(R.id.btn_atterrir);

        mleftDown = (Button) findViewById(R.id.button_left_down);
        mleftRight = (Button) findViewById(R.id.button_left_right);
        mleftLeft = (Button) findViewById(R.id.button_left_left);
        mleftUp = (Button) findViewById(R.id.button_left_up);
        mrightDown = (Button) findViewById(R.id.button_right_down);
        mrightLeft = (Button) findViewById(R.id.button_left_left);
        mrightUp = (Button) findViewById(R.id.button_right_up);
        mrightRight = (Button) findViewById(R.id.button_right_right);


        mswitchControlBtn = (Button) findViewById(R.id.switchControlBtn);

        mbattery_level = (TextView) findViewById(R.id.battery_level);
        mgps = (TextView) findViewById(R.id.gps);

        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mdecollerBtn.setOnClickListener(this);
        matterrirBtn.setOnClickListener(this);
        mswitchControlBtn.setOnClickListener(this);
        mleftDown.setOnClickListener(this);
        mleftLeft.setOnClickListener(this);
        mleftRight.setOnClickListener(this);
        mleftUp.setOnClickListener(this);
        mrightDown.setOnClickListener(this);
        mrightLeft.setOnClickListener(this);
        mrightRight.setOnClickListener(this);
        mrightUp.setOnClickListener(this);
       /* mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);*/

        recordingTime.setVisibility(View.INVISIBLE);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });

        // Joysticks
        mScreenJoystickRight = (OnScreenJoystick)findViewById(R.id.directionJoystickRight);
        mScreenJoystickLeft = (OnScreenJoystick)findViewById(R.id.directionJoystickLeft);

        mScreenJoystickLeft.setJoystickListener(new OnScreenJoystickListener(){

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if(Math.abs(pX) < 0.02 ){
                    pX = 0;
                }

                if(Math.abs(pY) < 0.02 ){
                    pY = 0;
                }
                float pitchJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;
                float rollJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;

                mPitch = (float)(pitchJoyControlMaxSpeed * pY);

                mRoll = (float)(rollJoyControlMaxSpeed * pX);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }

            }

        });

        mScreenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if(Math.abs(pX) < 0.02 ){
                    pX = 0;
                }

                if(Math.abs(pY) < 0.02 ){
                    pY = 0;
                }
                float verticalJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity;
                float yawJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;

                mYaw = (float)(yawJoyControlMaxSpeed * pX);
                mThrottle = (float)(verticalJoyControlMaxSpeed * pY);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);

                }

            }
        });
    }

    private void initPreviewer() {

        DJIBaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UnknownAircraft)) {
                DJICamera camera = product.getCamera();
                if (camera != null){
                    // Set the callback
                    camera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    private void uninitPreviewer() {
        DJICamera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            FPVDemoApplication.getCameraInstance().setDJICameraReceivedVideoDataCallback(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_capture:{
                captureAction();
                break;
            }
            case R.id.btn_decoller:{
                decollerAction();
                break;
            }
            case R.id.btn_atterrir:{
                atterrirAction();
                break;
            }
            case R.id.switchControlBtn:{
                switchControl();
                break;
            }

            /*
            case R.id.btn_shoot_photo_mode:{
                switchCameraMode(DJICameraSettingsDef.CameraMode.ShootPhoto);
                break;
            }
            case R.id.btn_record_video_mode:{
                switchCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
                break;
            }*/
            default:
                break;
        }
    }

    private void decollerAction(){

        if(FPVDemoApplication.getAircraftInstance().getFlightController() != null){
            FPVDemoApplication.getAircraftInstance().getFlightController().takeOff(
                    new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast(djiError.getDescription());
                    } else {
                        showToast("Décollage");
                    }
                }
            });
        }
        else{
            showToast("Erreur lors du décollage.");
        }
    }


    private void atterrirAction(){

        if(FPVDemoApplication.getAircraftInstance().getFlightController() != null){
            FPVDemoApplication.getAircraftInstance().getFlightController().autoLanding(
                    new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                showToast("Atterrissage");
                            }
                        }
                    });
        }
        else{
            showToast("Erreur lors de l'atterrissage.");
        }
    }

    private void switchCameraMode(DJICameraSettingsDef.CameraMode cameraMode){

        DJICamera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setCameraMode(cameraMode, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
            }

    }

    // Method for taking photo
    private void captureAction(){

        DJICameraSettingsDef.CameraMode cameraMode = DJICameraSettingsDef.CameraMode.ShootPhoto;

        final DJICamera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {

            DJICameraSettingsDef.CameraShootPhotoMode photoMode = DJICameraSettingsDef.CameraShootPhotoMode.Single; // Set the camera capture mode as Single mode
            camera.startShootPhoto(photoMode, new DJICommonCallbacks.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        showToast("Photo enregistrée.");
                    } else {
                        showToast(error.getDescription());
                    }
                }

            }); // Execute the startShootPhoto API
        }
    }

    // Method for starting recording
    private void startRecord(){

        DJICameraSettingsDef.CameraMode cameraMode = DJICameraSettingsDef.CameraMode.RecordVideo;
        final DJICamera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new DJICommonCallbacks.DJICompletionCallback(){
                @Override
                public void onResult(DJIError error)
                {
                    if (error == null) {
                        showToast("Début enregistrement.");
                    }else {
                        showToast(error.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        DJICamera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new DJICommonCallbacks.DJICompletionCallback(){

                @Override
                public void onResult(DJIError error)
                {
                    if(error == null) {
                        showToast("Vidéo capturée.");
                    }else {
                        showToast(error.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }

    }

    private void switchControl()
    {

        if(isJoystickVisible)
        {
            mScreenJoystickLeft.setVisibility(View.GONE);
            mScreenJoystickRight.setVisibility(View.GONE);
            isJoystickVisible = false;
            mrightUp.setVisibility(View.VISIBLE);
            mrightRight.setVisibility(View.VISIBLE);
            mrightLeft.setVisibility(View.VISIBLE);
            mrightDown.setVisibility(View.VISIBLE);
            mleftLeft.setVisibility(View.VISIBLE);
            mleftRight.setVisibility(View.VISIBLE);
            mleftDown.setVisibility(View.VISIBLE);
            mleftUp.setVisibility(View.VISIBLE);

        }
        else
        {
            mScreenJoystickLeft.setVisibility(View.VISIBLE);
            mScreenJoystickRight.setVisibility(View.VISIBLE);
            isJoystickVisible = true;
            mrightUp.setVisibility(View.GONE);
            mrightRight.setVisibility(View.GONE);
            mrightLeft.setVisibility(View.GONE);
            mrightDown.setVisibility(View.GONE);
            mleftLeft.setVisibility(View.GONE);
            mleftRight.setVisibility(View.GONE);
            mleftDown.setVisibility(View.GONE);
            mleftUp.setVisibility(View.GONE);
        }
    }



    class SendVirtualStickDataTask extends TimerTask {

        @Override
        public void run() {
            if (FPVDemoApplication.isFlightControllerAvailable()) {
                FPVDemoApplication.getAircraftInstance().
                        getFlightController().sendVirtualStickFlightControlData(
                        new DJIVirtualStickFlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new DJICommonCallbacks.DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {

                            }
                        }
                );
            }
        }
    }









}
