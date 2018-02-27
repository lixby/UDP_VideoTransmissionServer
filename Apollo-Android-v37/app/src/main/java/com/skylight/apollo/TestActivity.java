package com.skylight.apollo;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.skylight.apollo.baseunit.util.Utils;
import com.skylight.apollo.util.Constants;
import com.skylight.apollo.util.HandleUtil;
import com.skylight.sd.SkylightUsbDataService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TestActivity extends AppCompatActivity implements View.OnClickListener ,UsbVdoApp.OnUsbListener{

    private Button ccBtn,wbBtn;
    private TextView csTv,sTv,vTv;

    private String TAG = "TestActivity";
    private SkylightUsbDataService mSkylightService;
    private EditText repeatTime_et,tcpBuffer_et,tcpReadSize_et;
    private Button repeatTime_btn,tcpBuffer_btn,tcpReadSize_btn;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        sharedPreferences = getSharedPreferences("tcp",MODE_PRIVATE);
        editor = sharedPreferences.edit();
        Constants.tcpBufferSize = sharedPreferences.getInt("buffersize",Constants.tcpBufferSize);
        Constants.sendPacketSize = sharedPreferences.getInt("readsize",Constants.sendPacketSize);
        initView();
        initListener();
        //192.168.0.108
        String ip = null;
        try {
            ip = Utils.getLocalAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, ip+".",Toast.LENGTH_SHORT).show();



    }
    public void doClick(View v){
        handler.sendEmptyMessage(0);
        initStatue();
    }

    private void initService(){
        Constants.cc_statue = null;
        Constants.wb_statue = null;
        Constants.FW        = null;
        mSkylightService = SkylightUsbDataService.instance(UsbVdoApp.getInstance());
        UsbVdoApp.getInstance().setSkylightUsbDataService(mSkylightService);
        UsbVdoApp.getInstance().setOnReadFrameListener(this);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                handler.sendEmptyMessage(0);
//            }
//        }).start();
//        mServiceConnection = new ServiceConnection() {
//            @Override
//            public void onServiceDisconnected(ComponentName name) {
//                // TODO Auto-generated method stub
//                if (mSkylightService != null) {
//                    mSkylightService.destroy();
//
//                }
//
//            }
//
//            @Override
//            public void onServiceConnected(ComponentName name, IBinder binder) {
//                Log.d(TAG, "onServiceConnected");
//                mSkylightService = ((SkylightUsbDataService.MyBinder) binder).getSkylightUsbDataService();
//                if (mSkylightService != null) {
//                    Log.d(TAG, "Skylight service is not null");
                 //   Constants.FW = mSkylightService.requestFWVersion();
//                    mSkylightService.setOnStatusChangedListener(TestActivity.this);
//                    mSkylightService.setReadFrameImpl(new ReadFrameImpl() {
//                        @Override
//                        public int onReadFrame(int i, byte[] bytes, int i1, long l) {
//                            Log.i(TAG,"data:"+i1);
//                            return 0;
//                        }
//                    });
//                    mSkylightService.init();
                //    mSkylightService.obtainStream();
//                    Constants.wb_statue = mSkylightService.getWB();
//                    Constants.cc_statue = mSkylightService.getCircle_Calib();
//                    Constants.FW = mSkylightService.requestFWVersion();
//                    initStatue();
//                }
//                else{
//                    Log.d(TAG, "Skylight service is null");
//                    Toast.makeText(TestActivity.this,"Skylight service is null",Toast.LENGTH_SHORT).show();
//                }
//            }
//        };
//        Intent intent = new Intent(this, SkylightUsbDataService.class);
//        boolean bindSuccess = bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        //Toast.makeText(TestActivity.this,"bindService:"+bindSuccess,Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onResume() {
        super.onResume();

      //  initStatue();
    }


    @Override
    protected void onStart() {
        super.onStart();

        initService();
        handler.sendEmptyMessage(0);
    }


    private void initStatue(){
       // Toast.makeText(TestActivity.this,"wb:"+Constants.wb_statue+",cc:"+Constants.cc_statue,Toast.LENGTH_SHORT).show();
        String wb_statue = null ,cc_statue = null;
        if(Constants.wb_statue != null && Constants.wb_statue.startsWith("1")){
            wb_statue = "pass";
        }else if(Constants.wb_statue != null && Constants.wb_statue.startsWith("-1")){
            wb_statue = "fail";
//        }else if("".equals(Constants.wb_statue)){
//            wb_statue = "未验证";
        }else if(Constants.wb_statue != null && Constants.wb_statue.startsWith("0") ){
            wb_statue = "未验证";
        }else{
            wb_statue = "";
        }
        if(Constants.cc_statue != null && Constants.cc_statue.startsWith("1")){
            cc_statue = "pass";
        }else if(Constants.cc_statue != null && Constants.cc_statue.startsWith("-1")){
            cc_statue = "fail";
        }else if(Constants.cc_statue != null && Constants.cc_statue.startsWith("0") ) {
            cc_statue = "未验证";
        }else{
            cc_statue = "";
        }
        vTv.setText( HandleUtil.handleVersion(Constants.FW,Constants.USB_SDK));
        sTv.setText("当前设备的白平衡状态为:" + wb_statue + "\n" + "当前设备的拼接状态为:" + cc_statue);
        // sTv.setText("wb:" + Constants.wb_statue + "\n" + "cc:" + Constants.cc_statue);
    }

    private void initView() {
        ccBtn = (Button) findViewById(R.id.cc_btn);
        wbBtn = (Button) findViewById(R.id.wb_btn);
        csTv  = (TextView) findViewById(R.id.connect_statue_tv);
        sTv   = (TextView) findViewById(R.id.statue_tv);
        vTv   = (TextView) findViewById(R.id.version_tv);

        repeatTime_btn = (Button) findViewById(R.id.repeatTime_btn);
        repeatTime_et  = (EditText) findViewById(R.id.repeatTime_tv);
        tcpBuffer_btn = (Button) findViewById(R.id.tcpBuffer_btn);
        tcpBuffer_et  = (EditText) findViewById(R.id.tcpBuffer_tv);
        tcpReadSize_btn = (Button) findViewById(R.id.readContextSize_btn);
        tcpReadSize_et  = (EditText) findViewById(R.id.readContextSize_tv);

        repeatTime_et.setText(Constants.repeatTime + "");
        tcpBuffer_et.setText(Constants.tcpBufferSize+"");
        tcpReadSize_et.setText(Constants.sendPacketSize+"");

    }

    private void initListener() {
        ccBtn.setOnClickListener(this);
        wbBtn.setOnClickListener(this);
        repeatTime_btn.setOnClickListener(this);
        tcpBuffer_btn.setOnClickListener(this);
        tcpReadSize_btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        final Intent intent = new Intent(TestActivity.this,StitchActivity.class);
//        Toast.makeText(TestActivity.this, "wb:"+Constants.wb_statue +",cc:"+Constants.cc_statue,Toast.LENGTH_SHORT).show();
        switch (v.getId()){
            case R.id.wb_btn: //白平衡
                if (TextUtils.isEmpty(Constants.cc_statue) || Constants.cc_statue.startsWith("0")
                        || Constants.cc_statue.startsWith("-1")) {  //拼接参数获取到并且已确认
                    Toast.makeText(TestActivity.this, "拼接确认未通过，请返回拼接确认", Toast.LENGTH_SHORT).show();
                    return;
                }

                    if (!TextUtils.isEmpty(Constants.wb_statue) && Constants.wb_statue.startsWith("1") ) {//|| Constants.wb_statue.startsWith("-1")
                        //TODO tips
                        Dialog dialog = null;
                        dialog = new AlertDialog.Builder(TestActivity.this).setMessage("是否再次进入白平衡确认").setPositiveButton("cancle", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                intent.putExtra("type", 0);
                                startActivity(intent);
                            }
                        }).create();
                        dialog.show();
                    } else if(!TextUtils.isEmpty(Constants.wb_statue) &&
                            (Constants.wb_statue.startsWith("0") || Constants.wb_statue.startsWith("-1"))){
                        intent.putExtra("type", 0);
                        startActivity(intent);
                    }else{
                        Toast.makeText(TestActivity.this,"未获取到白平衡参数",Toast.LENGTH_SHORT).show();
                    }


               break;
            case R.id.cc_btn: //拼接
                Toast.makeText(TestActivity.this,"repeatTime:"+Constants.repeatTime+
                        "\nbuffer:"+Constants.tcpBufferSize+
                        "\nread size:"+Constants.tcpReadSize,Toast.LENGTH_SHORT).show();
                if (!TextUtils.isEmpty(Constants.cc_statue) && (Constants.cc_statue.startsWith("1")
                        )) {//|| Constants.cc_statue.startsWith("-1")
                    //TODO tips
                    Dialog dialog = null;
                    dialog = new AlertDialog.Builder(TestActivity.this).setMessage("是否再次进入拼接确认").setPositiveButton("cancle", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            intent.putExtra("type",1);
                            startActivity(intent);
                        }
                    }).create();
                    dialog.show();
                }else if(!TextUtils.isEmpty(Constants.cc_statue) &&
                        (Constants.cc_statue.startsWith("0") || Constants.cc_statue.startsWith("-1"))){
                    intent.putExtra("type",1);
                    startActivity(intent);
                }else {
                    Toast.makeText(TestActivity.this,"未获取到拼接参数",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.repeatTime_btn:
                String repeatTime = repeatTime_et.getText().toString().trim();
                long time = Constants.repeatTime = Long.valueOf(repeatTime);
                if (time <= 50){
                    Constants.timeUnit = TimeUnit.MILLISECONDS;
                }else if(time <= 1000){
                    Constants.timeUnit = TimeUnit.MICROSECONDS;
                }else {
                    Constants.timeUnit = TimeUnit.NANOSECONDS;
                }
                String timeUnit  = null;
                if (Constants.timeUnit == TimeUnit.MILLISECONDS){
                    timeUnit = "MILLISECONDS";
                }else if(Constants.timeUnit == TimeUnit.MICROSECONDS){
                    timeUnit = "MICROSECONDS";
                }else if (Constants.timeUnit == TimeUnit.NANOSECONDS){
                    timeUnit = "NANOSECONDS";
                }
                Toast.makeText(TestActivity.this, "repeatTime:"+Constants.repeatTime+" "+timeUnit ,Toast.LENGTH_SHORT).show();
               break;
            case R.id.tcpBuffer_btn:
                String tcpSizeString = tcpBuffer_et.getText().toString().trim();
                if (tcpSizeString != null) {
                    int tcpSize = Integer.valueOf(tcpSizeString);
                    if (editor != null) {
                        Constants.tcpBufferSize = tcpSize;
                        editor.putInt("buffersize", tcpSize);
                    }else {
                        editor = sharedPreferences.edit();
                        Toast.makeText(TestActivity.this,"err,try again",Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(TestActivity.this,"null data",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.readContextSize_btn:
                String tcpReadSizeString = tcpReadSize_et.getText().toString().trim();
                if (tcpReadSizeString != null) {
                    int readSize = Integer.valueOf(tcpReadSizeString);
                    if (editor != null) {
                        Constants.sendPacketSize = readSize;
                        editor.putInt("readsize", readSize);
                    }else {
                        editor = sharedPreferences.edit();
                        Toast.makeText(TestActivity.this,"err,try again",Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(TestActivity.this,"null data",Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }



    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                if (mSkylightService != null) {

                    Constants.wb_statue = mSkylightService.getWB();
                    Constants.cc_statue = mSkylightService.getCircle_Calib();
                    Constants.FW = mSkylightService.requestFWVersionToJson();
                    Constants.USB_SDK = mSkylightService.getSdkVersion();
                    initStatue();
                } else {
                   // Toast.makeText(TestActivity.this, "服务未启动,请重启app", Toast.LENGTH_SHORT).show();
                }
                    break;
            }
        }
    };


    private boolean isRefreshData = false;
    @Override
    public int onReadFrame(int i, byte[] bytes, int i1, long l) {
        if (!isRefreshData){
            synchronized ("1") {
                isRefreshData = true;
                handler.sendEmptyMessage(0);
            }
        }
        return 0;
    }

    @Override
    public void onStatusChanged(String s) {
        Log.d(TAG, "usb status = " + s);
//        Toast.makeText(TestActivity.this,"usb status:" + s,Toast.LENGTH_SHORT).show();
        if ("UsbDevice attached".equals(s) || "sdk initialized".equals(s)){
            handler.sendEmptyMessage(0);
        }else if("UsbDevice detached".equals(s)){
            isRefreshData = false;
            Toast.makeText(TestActivity.this,"usb拔出",Toast.LENGTH_SHORT).show();
            Constants.wb_statue = null;
            Constants.cc_statue = null;
            Constants.FW = null;
            initStatue();
        }
    }
}
