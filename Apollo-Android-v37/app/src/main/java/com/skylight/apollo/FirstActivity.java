package com.skylight.apollo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class FirstActivity extends Activity {
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!isTaskRoot()) {
            super.onCreate(savedInstanceState);
            finish();
            return;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent  = new Intent(FirstActivity.this,TestActivity.class);
                startActivity(intent);
                finish();
            }
        },1000);
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Intent intent  = new Intent(FirstActivity.this,StitchActivity.class);
//                intent.putExtra("type", 0);
//                startActivity(intent);
//                startActivity(intent);
//                finish();
//            }
//        },1000);

    }

}
