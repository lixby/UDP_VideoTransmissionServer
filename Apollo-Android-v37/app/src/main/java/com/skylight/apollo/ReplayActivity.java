package com.skylight.apollo;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kandaovr.sdk.renderer.Renderer;
import com.kandaovr.sdk.renderer.ReplayRenderer;
import com.kandaovr.sdk.renderer.StitchRenderer;
import com.kandaovr.sdk.util.Constants;
import com.kandaovr.sdk.view.RenderView;
import com.skylight.apollo.decoder.DataDecoder;
import com.skylight.apollo.decoder.ImageDataDecoder;
import com.skylight.apollo.decoder.VideoDataDecoder;
import com.skylight.apollo.util.Util;

public class ReplayActivity extends AppCompatActivity{

    private final static String TAG = "ReplayActivity";
    public final static int MODE_HAND = 0;
    public final static int MODE_GYRO = 1;
    private final static int PROJECTION_PERSPECTIVE = 0;
    private final static int PROJECTION_FISHEYE = 1;
    private final static int PROJECTION_PLANET = 2;
    private final static int[] MODE_TEXTS = new int[]{
            R.string.replay_mode_hand, R.string.replay_mode_gyro
    };
    private final static int[] PROJECTION_TEXTS = new int[]{
            R.string.replay_projection_perspective, R.string.replay_projection_fisheye,
            R.string.replay_projection_planet
    };

    RenderView mMainView;
    ReplayRenderer mReplayRenderer;
    DataDecoder mDecoder;
//    SensorTracker mTracker;
    MyOrientationDetector mOrientationDetector;


    private int mMode = MODE_HAND;
    private int mOrientationMode = Surface.ROTATION_0;
    private boolean mPendingWindowModeChange = false;
    private int mProjection = PROJECTION_PERSPECTIVE;

    public class MyOrientationDetector extends OrientationEventListener {
        public MyOrientationDetector( Context context ) {
            super(context );
        }

        @Override
        public void onOrientationChanged(int orientation) {
            int newWindowOrientation = ReplayActivity.this.getWindowManager().getDefaultDisplay().getRotation();
//            int width =
            if (mOrientationMode != newWindowOrientation) {
                mOrientationMode = newWindowOrientation;
                if (mReplayRenderer != null) {
//                    mReplayRenderer.setOrientationMode(mOrientationMode);
//                    mReplayRenderer.updateWindow(mOrientationMode);
                }
                if (mMainView != null) {
//                    mMainView.setOrientationMode(mOrientationMode);
                }
            }
//            ReplayActivity.this.getWindowManager().getDefaultDisplay().getRotation();

//            Log.d(TAG, "dsj onOrientationChanged orientation = " + orientation + " " + ReplayActivity.this.getWindowManager().getDefaultDisplay().getRotation());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "dsj replayActivity create");
        super.onCreate(savedInstanceState);
        Util.verifyStoragePermissions(this);
        setContentView(R.layout.activity_replay);
        setTitle(R.string.activity_replay);

        int mediaType = getIntent().getIntExtra("mediaType", Constants.MEDIA_TYPE_VIDEO);
        String mediaPath = getIntent().getStringExtra("mediaPath");
        if (mediaPath == null) { // TODO for testing
            mediaPath = Util.VIDEO_DIR + "/3K 360 video.mp4";
            mediaPath = Util.PICTURE_DIR + "/1474627572157.jpg";
            mediaType = Constants.MEDIA_TYPE_PICTURE;
        }

        // create decoder
        if (mediaType == Constants.MEDIA_TYPE_VIDEO) {
            mDecoder = new VideoDataDecoder(this, mediaPath);
        } else {
            mDecoder = new ImageDataDecoder(this, mediaPath);
        }

        // create renderer
        mReplayRenderer = new ReplayRenderer(this);
        mReplayRenderer.setTextureSurfaceListener(new Renderer.TextureSurfaceListener() {
            @Override
            public void onSurfaceCreated(Surface surface) {
                mDecoder.setSurface(surface);
            }
        });


        // create SurfaceView
        mMainView = (RenderView) findViewById(R.id.surfaceView);
        mMainView.setRenderer(mReplayRenderer);
        mMainView.setGestureListener(mReplayRenderer);

        mOrientationDetector = new MyOrientationDetector(this);

        // Capture button
        TextView captureButton = (TextView) findViewById(R.id.captureButton);
        captureButton.setClickable(true);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mReplayRenderer.capture(Util.SCREENSHOT_DIR + "/" + System.currentTimeMillis() + ".jpg",
                        new StitchRenderer.CaptureListener() {
                            @Override
                            public void onComplete(final String path) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ReplayActivity.this, "Save Screenshot " + path, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            @Override
                            public void onError(String path, String errorMessage) {
                                Toast.makeText(ReplayActivity.this, "Capture Error:" + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        // Change projection method button
        TextView projectionButton = (TextView) findViewById(R.id.projectionButton);
        projectionButton.setClickable(true);
        projectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView button = (TextView) view;
                mProjection = (mProjection + 1) % PROJECTION_TEXTS.length;
                button.setText(PROJECTION_TEXTS[mProjection]);

                // set projection
                mReplayRenderer.setProjection(mProjection);
            }
        });

        // Change navigation method button
        TextView modeButton = (TextView) findViewById(R.id.modeButton);
        modeButton.setClickable(true);
        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView button = (TextView) view;
                mMode = (mMode + 1) % MODE_TEXTS.length;
                button.setText(MODE_TEXTS[mMode]);
//                mReplayRenderer.setHandOrGyroMode(mMode);
//                mMainView.setMode(mMode);
//                mReplayRenderer.clickUpdateWindow(mOrientationMode);

//                /**
//                 * An app can use this method to check if it is currently allowed to write or modify system
//                 * settings. In order to gain write access to the system settings, an app must declare the
//                 * {@link android.Manifest.permission#WRITE_SETTINGS} permission in its manifest. If it is
//                 * currently disallowed, it can prompt the user to grant it this capability through a
//                 * management UI by sending an Intent with action
//                 * {@link android.provider.Settings#ACTION_MANAGE_WRITE_SETTINGS}.
//                 *
//                 * @param context A context
//                 * @return true if the calling app can write to system settings, false otherwise
//                 */
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//
//                    if (!Settings.System.canWrite(ReplayActivity.this)) {
//                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
//                                Uri.parse("package:" + getPackageName()));
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(intent);
//                    }
//                }

                //set mode
//                if (mMode == MODE_GYRO) {
//                    setConfigurationStatus(getContentResolver(), 0);
//                } else {
//                    setConfigurationStatus(getContentResolver(), 1);
//                }


            }
        });

        // start sensor tracker
        initTracker();
        updateLayout(getResources().getConfiguration());
    }

    private void initTracker(){
//        final TextView infoView = (TextView)findViewById(R.id.label);
//        mTracker = new SensorTracker(this);
//        mTracker.registerListener(new SensorTracker.SensorTrackerListener() {
//            @Override
//            public void onOrientationChange(final float[] eulerAngles) {
//                if(mMode == MODE_GYRO) {
//                    Configuration configuration = getResources().getConfiguration();
//                    String debug = "" + eulerAngles[0] + "\n" + eulerAngles[1] + "\n" + eulerAngles[2] + "\n\n";
//
//
//                    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                        // swap x and y angles
//                        float tmp = eulerAngles[0];
//                        eulerAngles[0] = eulerAngles[1];
//                        eulerAngles[1] = -tmp + 90;
//                    }
//                    mReplayRenderer.setCamera(eulerAngles);
//                    debug += "" + eulerAngles[0] + "\n" + eulerAngles[1] + "\n" + eulerAngles[2] + "\n\n";
//
//                    final String text = debug;
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            infoView.setText(text);
//                        }
//                    });
//                }
//            }

//            @Override
//            public void onMatrixChange(final Matrixf4x4 headViewMatrix) {
//                if(mMode == MODE_GYRO) {
//                    mReplayRenderer.setHeadViewMatrix(headViewMatrix);
////                    (TAG, "onMatrixChange");
//                }
//
//            }
//        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
            Log.d(TAG, "dsj onConfigureadionChanged");
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

//    private void setConfigurationStatus(ContentResolver resolver, int status)
//    {
//        //get uri
//        Uri uri = android.provider.Settings.System.getUriFor("accelerometer_rotation");
//        android.provider.Settings.System.putInt(resolver, "accelerometer_rotation", status);
//        resolver.notifyChange(uri, null);
//    }

    @Override
    protected void onResume(){
        super.onResume();
        mDecoder.startDecoding();
//        mTracker.startTracking();
//        setConfigurationStatus(getContentResolver(), 1);
        mOrientationDetector.enable();
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG, "onPause");
        mDecoder.stopDecoding();
//        mTracker.stopTracking();
        mOrientationDetector.disable();
    }



}
