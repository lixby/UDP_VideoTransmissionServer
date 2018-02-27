package com.skylight.apollo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.skylight.apollo.baseunit.biz.SendVideoPresenter;
import com.skylight.apollo.decoder.DataDecoder;
import com.skylight.apollo.decoder.UsbDataDecoder;
import com.skylight.apollo.util.Constants;
import com.skylight.apollo.util.Util;
import com.skylight.sd.ReadFrameImpl;
import com.skylight.sd.SkylightUsbDataService;

import java.io.File;
import java.io.FileOutputStream;

import apollo.encoder.EncodeFormat;


public class StitchActivity extends AppCompatActivity implements ReadFrameImpl,UsbVdoApp.OnUsbListener {

    private final static String TAG = "StitchActivity";
    private final static int SAMPLE_RATE = 44100;
    private final static boolean RECORD_RAW_DATA = true && Constants.DEBUG_MODE;
    private final static boolean USE_PHONE_MIC = false;

    private boolean mRecordRawStarted = false;

//    private RenderView mMainView;
//    private StitchRenderer mStitchRenderer;
    private DataDecoder mDecoder;

    private boolean mRecording = false;
    private String mRecordingPath = null;
    private EncodeFormat mRecordingConfig = null;

    private NotificationManager nm;
    private ServiceConnection mServiceConnection;
    private SkylightUsbDataService mSkylightService;
    private boolean mServiceStarted = false;
    private boolean mLensParamSet = false;
    private final Handler mHandler = new Handler();

    // buttons
    private Button mCaptureButton;
    private Button mBrowseButton;
    private Button mRecordButton;
    private Button mStreamButton;
    private Button mAdjustCalibrationButton;
    private int type = 0;  //0:白平衡；1：拼接
    private Button btn_o,btn_t;
    private boolean isReadedStatue = false;
    private Dialog dialog;


    private SendVideoPresenter sendVideoPresenter;
    private boolean setOrientationed = false;
 //   private CameraConnectionReceiver cameraConnectionReceiver;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
//            initService();
//            Log.i(TAG,"initService");
            String s = (String) msg.obj;
            if ("UsbDevice attached".equals(s)){
//                if (mSkylightService != null) {
//                    Constants.USB_SDK = mSkylightService.getSdkVersion();
//                    Constants.FW = mSkylightService.requestFWVersionToJson();
//                }
                switch (type){
                    case 0:
                       // Toast.makeText(StitchActivity.this,"mSkylightService:"+ mSkylightService,Toast.LENGTH_SHORT).show();
                        if (mSkylightService != null) {
                            //setTitle(R.string.wb + "\n" + HandleUtil.handleVersion(Constants.FW,Constants.USB_SDK));
                            Constants.wb_statue = mSkylightService.getWB();
                         //   Toast.makeText(StitchActivity.this,"wb_statue:"+ Constants.wb_statue,Toast.LENGTH_SHORT).show();
                            String statue ;
//                            if("1".equals(Constants.wb_statue)){
//                                statue = "pass";
//                            }else if("-1".equals(Constants.wb_statue)){
//                                statue = "fail";
//                            }else if("0".equals(Constants.wb_statue)){
//                                statue = "未验证";
//                            }else{
//                                statue = null;
//                            }
                            if(Constants.wb_statue == null){
                                statue = "";
                            }else if(Constants.wb_statue.startsWith("1")){
                                statue = "pass";
                            }else if(Constants.wb_statue.startsWith("-1")){
                                statue = "fail";
                            }else if(Constants.wb_statue.startsWith("0")){
                                statue = "未验证";
                            }else{
                                statue = "";
                            }
                            Toast.makeText(StitchActivity.this,"当前白平衡:"+statue,Toast.LENGTH_SHORT).show();
                            Log.i(TAG,"onStatusUpdate.mSkylightService:"+mSkylightService+",wb_statue:"+Constants.wb_statue);
                            if (!TextUtils.isEmpty(Constants.wb_statue) && Constants.wb_statue.startsWith("1")){


                                dialog = new android.support.v7.app.AlertDialog.Builder(StitchActivity.this).setMessage("当前白平衡:"+statue+",是否再次确认").setPositiveButton("cancle", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                }).setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();

                                    }
                                }).create();
                                dialog.setCanceledOnTouchOutside(false);
                                dialog.show();
                            }
                        }else{
                            //setTitle(R.string.wb + "\n" + HandleUtil.handleVersion(null,null));
                        }

                        break;
                    case 1:
                        if (mSkylightService != null) {
                           // setTitle(R.string.cc + "\n" + HandleUtil.handleVersion(Constants.FW,Constants.USB_SDK));
                            Constants.cc_statue = mSkylightService.getCircle_Calib();
                            Log.i(TAG,"onStatusUpdate.mSkylightService:"+mSkylightService+",cc_statue:"+Constants.cc_statue);
                            String statue ;
//                            if("1".equals(Constants.cc_statue)){
//                                statue = "pass";
//                            }else if("-1".equals(Constants.cc_statue)){
//                                statue = "fail";
//                            }else if("0".equals(Constants.cc_statue)){
//                                statue = "未验证";
//                            }else{
//                                statue = null;
//                            }
                            if(Constants.cc_statue == null){
                                statue = "";
                            }else if(Constants.cc_statue.startsWith("1")){
                                statue = "pass";
                            }else if(Constants.cc_statue.startsWith("-1")){
                                statue = "fail";
                            }else if(Constants.cc_statue.startsWith("0")){
                                statue = "未验证";
                            }else{
                                statue = "";
                            }
                            Toast.makeText(StitchActivity.this,"当前拼接:"+statue,Toast.LENGTH_SHORT).show();
                            if (!TextUtils.isEmpty(Constants.cc_statue) && Constants.cc_statue.startsWith("1")){

                                dialog = new android.support.v7.app.AlertDialog.Builder(StitchActivity.this).setMessage("当前拼接:"+statue+",是否再次确认").setPositiveButton("cancle", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                }).setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();


                                    }
                                }).create();
                                dialog.setCanceledOnTouchOutside(false);
                                dialog.show();
                            }
                        }else{
                            //setTitle(R.string.cc + "\n" + HandleUtil.handleVersion(null,null));
                        }
                        break;
                }
                Constants.wb_statue = mSkylightService.getWB();
                Constants.cc_statue = null;
                Constants.FW = null;
                setBtn(true);
            }else if("UsbDevice detached".equals(s)){
                setBtn(false);
                //setTitle(R.string.cc + "\n" + HandleUtil.handleVersion(null,Constants.USB_SDK));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

      //  cameraConnectionReceiver = UsbVdoApp.getInstance().getCameraConnectionReceiver();

        Util.verifyStoragePermissions(this);
        Util.createFolders();
        setContentView(R.layout.activity_stitch);

//        try {
//            Thread.sleep(30);
//            udpFramePresenter.setTargetInfo("192.168.0.104",3364);
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        Intent intents = getIntent();
//        //获取类型
        if(intents != null){
            type = intents.getIntExtra("type",0);
        }

        initBtn();
        setBtn(false);
//        if(type == 0) {
//            setTitle(getResources().getString(R.string.wb) + "-" + Constants.version );
//        }else {
//            setTitle(getResources().getString(R.string.cc) + "-" + Constants.version);
//        }
        sendVideoPresenter = new SendVideoPresenter();
        sendVideoPresenter.startRunTcp();
        // create decoder
        mDecoder = new UsbDataDecoder(this);
       // if(Constants.DEBUG_MODE){
         //   mDecoder = new TestDataDecoder(this); // TODO for testing
//            mDecoder = new ImageDataDecoder(this, Environment.getExternalStorageDirectory() + "/113788818712911662.jpg");
      //  }
        if(mDecoder instanceof UsbDataDecoder){
            initService();
        }

        // create renderer
//        mStitchRenderer = new StitchRenderer(this, 3840, 1920);
//        mStitchRenderer.setFov(120);
//        mStitchRenderer.setThumbnailEnabled(false);
//        mStitchRenderer.setTextureSurfaceListener(new Renderer.TextureSurfaceListener() {
//            @Override
//            public void onSurfaceCreated(Surface surface) {
//                mDecoder.setSurface(surface);
//            }
//        });

        // create SurfaceView
//        mMainView = (RenderView)findViewById(R.id.surfaceView);
//        mMainView.setRenderer(mStitchRenderer);
//        mMainView.setGestureListener(mStitchRenderer);

//        if(mDecoder instanceof TestDataDecoder || RECORD_RAW_DATA || Constants.DEBUG_MODE) {
//            mStitchRenderer.setLensParams(Constants.TEST_LENS_PARAM);
//        }

        initComponent();
        updateLayout(getResources().getConfiguration());

        mRecordingConfig = new EncodeFormat();
        mRecordingConfig.width = 3840;
        mRecordingConfig.height = 1920;
        mRecordingConfig.videoBitrate = 35*1000000; // 1M
        mRecordingConfig.channels = 2; // must be 2
        mRecordingConfig.sampleRate = 44100; // must be 44100
        mRecordingConfig.audioBitrate = 88200;
    }
    private void initBtn() {
        btn_o = (Button) findViewById(R.id.btn_o);
        btn_t = (Button) findViewById(R.id.btn_t);
        btn_o.setVisibility(View.VISIBLE);
        btn_t.setVisibility(View.VISIBLE);
        switch (type){
            case 0:

                btn_o.setText("OK");
                btn_t.setText("NG");
                btn_o.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //TODO 白平衡OK
                        //mSkylightService.setWhiteBalanceMode("","");
                        if (mSkylightService != null) {
                              int response = mSkylightService.setWB("1");
                            Log.i("setwb(1)response", response +".");
                            onResponse(response);
                        }
                    }
                });
                btn_t.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //TODO 白平衡NG
                        if (mSkylightService != null) {
                            int response = mSkylightService.setWB("-1");
                            Log.i("setwb(-1)response", response +".");
                            onResponse(response);
                        }

                    }
                });
                break;
            case 1:
                btn_o.setText("拼接通过");
                btn_t.setText("不良拼接");
                btn_o.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //TODO 拼接通过
                        if (mSkylightService != null) {
                            int response =  mSkylightService.setCircle_Calib("1");
                            Log.i("setcc(1)response : ", response +".");
                            onResponse(response);
                        }
                    }
                });
                btn_t.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //TODO 不良拼接
                        if (mSkylightService != null) {
                            int response = mSkylightService.setCircle_Calib("-1");
                            Log.i("setcc(-1)response : ", response +".");
                            onResponse(response);
                        }
                    }
                });
                break;
        }
    }
    public void onResponse(int response){
        if (0 ==response){
            setBtn(false);
            //获取CC及WB
            if (mSkylightService != null) {
                if (type == 1) {
                    Constants.cc_statue = mSkylightService.getCircle_Calib();

                }else if(type == 0){
                    Constants.wb_statue = mSkylightService.getWB();
                }
            }
            // showMessage("pass");
        }else{
            setBtn(true);
            if (type == 0) {
                Toast.makeText(StitchActivity.this, "set WB fail",Toast.LENGTH_SHORT).show();
                Constants.wb_statue = null;
            }else{
                Toast.makeText(StitchActivity.this, "set CC fail",Toast.LENGTH_SHORT).show();
                Constants.cc_statue = null;
            }
        }
    }

    public void setBtn(boolean enable){
//        switch (type){
//            case 0:
                if(enable){
                    btn_o.setBackgroundColor(Color.GREEN);
                    btn_t.setBackgroundColor(Color.RED);
                    btn_o.setEnabled(true);
                    btn_t.setEnabled(true);
                }else{
                    btn_o.setBackgroundColor(Color.WHITE);
                    btn_t.setBackgroundColor(Color.WHITE);
                    btn_o.setEnabled(false);
                    btn_t.setEnabled(false);
                }
//                break;
//            case 1:
//                if(enable){
//                    btn_o.setTextColor(Color.BLACK);
//                    btn_t.setTextColor(Color.BLACK);
//                    btn_o.setEnabled(true);
//                    btn_t.setEnabled(true);
//                }else{
//                    btn_o.setTextColor(Color.WHITE);
//                    btn_t.setTextColor(Color.WHITE);
//                    btn_o.setEnabled(false);
//                    btn_t.setEnabled(false);
//                }
//                break;
//        }
    }

    private void initComponent(){
        // Capture photo button
        mCaptureButton = (Button)findViewById(R.id.captureButton);
//        mCaptureButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(mRecording){
//                    Toast.makeText(StitchActivity.this, "Recording in progress, cannot capture.", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                mCaptureButton.setEnabled(false);
//                mStitchRenderer.capture(Util.PICTURE_DIR + "/" + System.currentTimeMillis() + ".jpg",
//                        new StitchRenderer.CaptureListener() {
//                            @Override
//                            public void onComplete(final String path) {
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        mCaptureButton.setEnabled(true);
//                                        Toast.makeText(StitchActivity.this, "Saved Picture " + path, Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                            }
//                            @Override
//                            public void onError(String path, final String errorMessage){
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        mCaptureButton.setEnabled(true);
//                                        Toast.makeText(StitchActivity.this, "Capture Error:" + errorMessage, Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                            }
//                        });
//            }
//        });

        // Record video button
        mRecordButton = (Button)findViewById(R.id.recordButton);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Button button = (Button)view;
                if(!mRecording){
                    disableButtons(mRecordButton);
                    button.setText(R.string.stitch_record_stop);

                    mRecording = true;
                    mRecordingPath = Util.VIDEO_DIR + "/" + System.currentTimeMillis() + ".mp4";

                    if(RECORD_RAW_DATA) {
                        mRecordRawStarted = false;
                        openFile("data" + System.currentTimeMillis() + ".h264");
                    }
                }
                else{
                    stopRecording();

                    // TODO remove the following before release
                    if(RECORD_RAW_DATA) {
                        closeFile();
                    }
                }
            }
        });

        // Streaming button
        mStreamButton = (Button)findViewById(R.id.streamButton);
        mStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Button button = (Button)view;
                if(!mRecording){
                    disableButtons(mStreamButton);
                    button.setText(R.string.stitch_record_stop);

                    mRecording = true;
                }
                else{
                    stopRecording();
                }
            }
        });

        // Browse button
        mBrowseButton = (Button)findViewById(R.id.browseButton);
        mBrowseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StitchActivity.this, BrowseActivity.class);
                startActivity(intent);
            }
        });

        Button frontCameraButton = (Button)findViewById(R.id.frontCameraButton);
        frontCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mStitchRenderer.setCameraPosition(StitchRenderer.CAMERA_FRONT);
                Toast.makeText(StitchActivity.this,"1",Toast.LENGTH_SHORT).show();
            }
        });

        Button rearCameraButton = (Button)findViewById(R.id.rearCameraButton);
        rearCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mStitchRenderer.setCameraPosition(StitchRenderer.CAMERA_REAR);
                Toast.makeText(StitchActivity.this,"2",Toast.LENGTH_SHORT).show();
            }
        });

        // Capture photo button
        Button screenshotButton = (Button)findViewById(R.id.screenshotButton);
//        screenshotButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mStitchRenderer.screenshot(Util.SCREENSHOT_DIR + "/" + System.currentTimeMillis() + ".jpg",
//                        new StitchRenderer.CaptureListener() {
//                            @Override
//                            public void onComplete(final String path) {
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        Toast.makeText(StitchActivity.this, "Saved Screnshot " + path, Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                            }
//                            @Override
//                            public void onError(String path, final String errorMessage){
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        Toast.makeText(StitchActivity.this, "Screnshot Error:" + errorMessage, Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                            }
//                        });
//            }
//        });

        mAdjustCalibrationButton = (Button)findViewById(R.id.adjustCalibrationButton);
//        mAdjustCalibrationButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
                Toast.makeText(StitchActivity.this,"3",Toast.LENGTH_SHORT).show();
//                boolean enabled = !mStitchRenderer.isAdjustCalibrationEnabled();
//                mStitchRenderer.setAdjustCalibrationEnabled(enabled);
//                mAdjustCalibrationButton.setText(enabled ? "Stop Auto Calibration" : "Start Auto Calibration");
//            }
//        });
    }

    private void stopRecording(){
        final Button button = mRecordButton.isEnabled() ? mRecordButton : mStreamButton;
        mRecording = false;
        button.setEnabled(false);
        // delay the UI change by 1 second so that the encoder has sufficient time to shutdown
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                enableButtons();
                button.setText(button == mStreamButton ? R.string.stitch_stream : R.string.stitch_record);
            }
        }, 1000);
    }

    private void disableButtons(Button excluded){
        mRecordButton.setEnabled(mRecordButton == excluded);
        mBrowseButton.setEnabled(mBrowseButton == excluded);
        mCaptureButton.setEnabled(mCaptureButton == excluded);
        mStreamButton.setEnabled(mStreamButton == excluded);
    }

    private void enableButtons(){
        mRecordButton.setEnabled(true);
        mBrowseButton.setEnabled(true);
        mCaptureButton.setEnabled(true);
        mStreamButton.setEnabled(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayout(newConfig);
    }

    private void updateLayout(Configuration configuration){
        // Checks the orientation of the screen
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getSupportActionBar().hide();
        } else if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            getSupportActionBar().show();
        }
    }

    private void initService(){
        mSkylightService  = UsbVdoApp.getInstance().getUsbService();
        UsbVdoApp.getInstance().setOnReadFrameListener(StitchActivity.this);
//        mSkylightService.setReadFrameImpl(StitchActivity.this);
//        mSkylightService.setOnStatusChangedListener(StitchActivity.this);
//        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        mServiceConnection = new ServiceConnection() {
//            @Override
//            public void onServiceDisconnected(ComponentName name) {
//                // TODO Auto-generated method stub
//                if (mSkylightService != null) {
//                    mSkylightService.destroy();
//
//                }
//            }
//
//            @Override
//            public void onServiceConnected(ComponentName name, IBinder binder) {
//                Log.d(TAG, "onServiceConnected");
//                mSkylightService = ((SkylightUsbDataService.MyBinder) binder).getSkylightUsbDataService();
                if (mSkylightService != null) {
//                    showMessage("Skylight service connected");
//                    Log.d(TAG, "Skylight service is not null");
//                    mSkylightService.setReadFrameImpl(StitchActivity.this);
//                    mSkylightService.setOnStatusChangedListener(StitchActivity.this);
//                    mSkylightService.init();
//                    if(!mServiceStarted){
                        mSkylightService.obtainStream();
                        setBtn(true);
                        Toast.makeText(StitchActivity.this,"Start to obtainStream",Toast.LENGTH_SHORT).show();
//                    }
                }
                else{
//                    showMessage("Skylight service is null");
                    Toast.makeText(StitchActivity.this,"Skylight service is null",Toast.LENGTH_SHORT).show();
                }
//            }
//        };
//        Intent intent = new Intent(this, SkylightUsbDataService.class);
//        boolean bindSuccess = bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
//        Toast.makeText(StitchActivity.this,"bindService:"+bindSuccess,Toast.LENGTH_SHORT).show();
    }

    private void showMessage(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView label = (TextView) findViewById(R.id.label);
                label.setText(message);
            }
        });
    }
    private boolean isFirstIFrame = false;
    @Override
    public int onReadFrame(int channel, byte[] data, int length, long pts) {
        if(!setOrientationed) {
//            mStitchRenderer.setOrientation(90, 0);
            setOrientationed = true;
        }
        if (!isReadedStatue){
            synchronized ("1") {
                isReadedStatue = true;
                Message msg = new Message();
                msg.obj = "UsbDevice attached";
                handler.sendMessage(msg);
            }
        }
//        showMessage("onReadFrame " + channel + " length = " + length);
//        Log.i(TAG,"length:"+length);

        if(channel == 10) {
            try {
                byte[] frame = mSkylightService.frameDataFilter(data, length);

                sendVideoPresenter.onReadFrame(frame, length,channel);
//                if (mDecoder instanceof UsbDataDecoder) {
//                    Log.i(TAG,"2:");
//                    ((UsbDataDecoder) mDecoder).onReadFrame(frame, frame.length);
//
//
//                }
                // TODO remove the following before release
//                if (RECORD_RAW_DATA) {
//                    Log.i(TAG,"3:");
//                    if(InfoExtractor.isIFrame(frame)){
//                        Log.i(TAG,"4:");
//                        mRecordRawStarted = true;
//                    }
//                    if(mRecordRawStarted) {
//                        Log.i(TAG,"5:");
//                        recordData(frame);
//                    }
//                }
            }
            catch(final Exception ex){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(StitchActivity.this, "exception : " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                ex.printStackTrace();
            }
        }
//        else if(channel == 20){
//
//            if (RECORD_RAW_DATA) {
////                recordData(data);
//            }
//            try {
////                mStitchRenderer.onAudioFrame(data, length, SAMPLE_RATE);
//            }
//            catch(final Exception ex){
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(StitchActivity.this, "exception : " + ex.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//                ex.printStackTrace();
//            }
//        }
//
//        // set lens param if it has not been set
//        if(!mLensParamSet){
//            final String lensParam = mSkylightService.getLensParam();
//        CmLog.e("----------Camera Log:"+lensParam);
//            if(Util.isLenParamValid(lensParam)){
//                mLensParamSet = true;
////                Toast.makeText(StitchActivity.this,"setLensParams:true",Toast.LENGTH_SHORT).show();
//                Log.i(TAG,"setLensParams:true");
//                if(!Constants.DEBUG_MODE) {
//
//                    mStitchRenderer.setLensParams(lensParam);
//                }
//            }
//        }

        return 0;
    }

    @Override
    public void onStatusChanged(final String s) {
        Log.d(TAG, "usb status = " + s);
        if ("UsbDevice detached".equals(s) ){
            mLensParamSet = false;
            isReadedStatue = false;
            setOrientationed = false;
            setBtn(false);
            if (dialog != null && dialog.isShowing()){
                dialog.dismiss();
            }

        }else if("UsbDevice attached".equals(s)|| "sdk initialized".equals(s)) {
            setBtn(true);
            isReadedStatue = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                        Message msg = new Message();
                        msg.obj = s;
                        attachedHandler.sendMessage(msg);

                }
            }).start();
        }
    }
    private Handler attachedHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (mSkylightService != null){
                Toast.makeText(StitchActivity.this,"attachedHandler.obtainStream",Toast.LENGTH_SHORT).show();
                mSkylightService.obtainStream();
            }
        }
    };

    @Override
    protected void onResume(){
        super.onResume();

        if(mSkylightService != null) {
            mSkylightService.obtainStream();
            mServiceStarted = true;
        }
       // Log.i(TAG,"startDecoding");
        mDecoder.startDecoding();

        if(USE_PHONE_MIC){
            startAudio();
        }
    }

    @Override
    protected void onPause(){
        // stop recording if it is currently recording
        if(mRecording){
            stopRecording();
        }
        // release stream and decoder
        if(mSkylightService != null) {
            mSkylightService.releaseStream();
        }
        mDecoder.stopDecoding();

        if(USE_PHONE_MIC){
            stopAudio();
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stitch, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.res:
                Log.d(TAG, "xdf setting thumbnail");
//                mStitchRenderer.setThumbnailEnabled(!mStitchRenderer.isThumbnailEnabled());
//                showSettingOptions(item);
//                showBitrateOptions(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showSettingOptions(final MenuItem item){

        final CharSequence labels[] = new CharSequence[] {"Current Lens Params", "Read Camera Lens", "Set Camera Lens"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick an action");
        builder.setItems(labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0){
//                    Toast.makeText(StitchActivity.this, mStitchRenderer.getLensParams(), Toast.LENGTH_LONG).show();
                }
                else if(which == 1){
                    Toast.makeText(StitchActivity.this, mSkylightService.getLensParam(), Toast.LENGTH_LONG).show();
                }
                else if(which == 2){
//                    mSkylightService.setLensParam(mStitchRenderer.getLensParams());
                    Toast.makeText(StitchActivity.this, "Lens Set Successfully", Toast.LENGTH_SHORT).show();
                    Toast.makeText(StitchActivity.this, mSkylightService.getLensParam(), Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.show();
    }

    /**
     * Display a list of bit rate options
     * @param item
     */
    private void showBitrateOptions(final MenuItem item){

        final CharSequence labels[] = new CharSequence[] {"512Kbps", "1Mbps", "2Mbps", "3Mbps", "4Mbps", "5Mbps", "8Mbps", "10Mbps"};
        final int[] bitrates = new int[]{512000, 1000000, 2000000, 3000000, 4000000, 5000000, 8000000, 10000000};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick a bitrate");
        builder.setItems(labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mRecordingConfig.videoBitrate = bitrates[which];
                item.setTitle(labels[which]);
            }
        });
        builder.show();
    }

    // for recording
    private FileOutputStream outputStream = null;
    public void openFile(String fileName)
    {
        String sdStatus = Environment.getExternalStorageState();
        if (!sdStatus.equals("mounted"))
        {
            Log.d("TestFile", "SD card is not avaiable/writeable right now.");
            return;
        }
        try
        {
            File file = new File(Environment.getExternalStorageDirectory(),
                    fileName);
            if (!file.exists())
            {
                Log.d("TestFile", "Create the file:" + fileName);
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file, true);
        }
        catch (Exception e)
        {
            Log.e("TestFile", "Error on writeFilToSD.");
            e.printStackTrace();
        }
    }

    public void recordData(byte[] data){
        Log.i(TAG,"6:");
        if(outputStream == null){
            return;
        }

        try
        {
            Log.i(TAG,"7:");
            outputStream.write(data, 0 ,data.length);
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error on writeFilToSD.");
            e.printStackTrace();
        }
    }

    public void closeFile(){
        try
        {
            if(outputStream != null){
                outputStream.close();
            }
        }
        catch(Exception e){
            Log.e("UsbActivity", "Error closing file: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            outputStream = null;
        }
    }

    private AudioRecord mic = null;
    private Thread aworker = null;
    private boolean aloop = false;
    private void startAudio(){
        try {
            mic = chooseAudioRecord();
            if (mic == null) {
                Toast.makeText(this, "Cannot open mic for audio recording", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        catch(Exception e){
            Toast.makeText(this, "Cannot open mic for audio recording", Toast.LENGTH_SHORT).show();
            return;
        }

        aworker = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(5);//android.os.Process.THREAD_PRIORITY_AUDIO);
                mic.startRecording();

                byte pcmBuffer[] = new byte[2048];
                while (aloop && !Thread.interrupted()) {
                    int size = mic.read(pcmBuffer, 0, pcmBuffer.length);
                    if (size <= 0) {
                        break;
                    }
//                    if(mStitchRenderer != null) {
//                        mStitchRenderer.onAudioFrame(pcmBuffer, size, SAMPLE_RATE);
//                    }
                }
            }
        });
        aloop = true;
        aworker.start();
    }

    private void stopAudio(){
        aloop = false;
        if (aworker != null) {
            aworker.interrupt();
            try {
                aworker.join();
            } catch (InterruptedException e) {
                aworker.interrupt();
            }
            aworker = null;
        }

        if (mic != null) {
            mic.setRecordPositionUpdateListener(null);
            mic.stop();
            mic.release();
            mic = null;
        }
    }

    private AudioRecord chooseAudioRecord() {
        int sampleRate = SAMPLE_RATE;
        int minBufferSize = 2048;// AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord mic = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        if (mic.getState() != AudioRecord.STATE_INITIALIZED) {
            mic = null;
        }

        return mic;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        sendVideoPresenter.onClose();
        super.onBackPressed();

    }
}
