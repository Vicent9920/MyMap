package cn.com.vicent.mymap;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class TestActivity extends AppCompatActivity {

    private static final String TAG = "TestActivity";
    boolean isRecordeOver = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        final RecordedButton recordedButton = (RecordedButton) findViewById(R.id.btn);
        recordedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ");
            }
        });
        recordedButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        while (isRecordeOver){
                            Log.d(TAG, "run: ");
                        }
                    }
                }.start();
                return true;
            }
        });
        recordedButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP &&isRecordeOver){
                    isRecordeOver = false;
                }
                return true;
            }
        });
//        recordedButton.setMax(30*1000);//最长录制时间30秒
//        final Timer timer = new Timer();//通过timer来模拟拍摄的进度
//        recordedButton.setOnGestureListener(new RecordedButton.OnGestureListener() {
//            @Override
//            public void onLongClick() {
//                //长按监听
//                final long startTime = System.currentTimeMillis();
//
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                long progress = System.currentTimeMillis()-startTime;
//                                recordedButton.setProgress(progress);//默认的进度值是0，所以这个需要自定义
//                            }
//                        });
//                    }
//                },1000,1000);
//            }
//
//            @Override
//            public void onClick() {
//                timer.cancel();
//            }
//
//            @Override
//            public void onLift() {
//                timer.cancel();
//            }
//
//            @Override
//            public void onOver() {
//                timer.cancel();
//            }
//        });
    }
}
