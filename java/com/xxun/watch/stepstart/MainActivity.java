package com.xxun.watch.stepstart;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.database.ContentObserver;
import android.os.Handler;

import com.xiaoxun.sdk.ResponseData;
import com.xiaoxun.sdk.IResponseDataCallBack;
import com.xiaoxun.sdk.XiaoXunNetworkManager;
import com.xiaoxun.sdk.IMessageReceiveListener;
import com.xiaoxun.statistics.XiaoXunStatisticsManager;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import android.provider.Settings;

public class MainActivity extends Activity {
    private static final String DEBUG_TAG = "XunStepsCount";
    private GestureDetectorCompat mDetector;
    private ImageView mHeadIcon;
    private TextView mWatchSteps;
    private ProgressBar mWatchStepsProgress;

    private String curSteps = "0";
    private String targetLevel;
    private XiaoXunNetworkManager nerservice;
    private XiaoXunStatisticsManager statisticsManager;
    private String statisticsTime;


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Const.ACTION_BROAST_SENSOR_STEPS_REQ)){
                String sensorSteps = intent.getStringExtra("sensor_steps");

                Log.d(DEBUG_TAG,"sensorSteps:"+sensorSteps);

                //获取当前步数，需要在计算前一天数据之前才可以
                String oldSteps = "0";
                /*
                try {
                    Uri uri = Uri.parse("content://com.xxun.watch.stepCountProvider/user");
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);

                     Log.d(DEBUG_TAG,"cursor:"+cursor);

                    while (cursor.moveToNext()) {
                        oldSteps = cursor.getString(1);
                    }
		    Log.d(DEBUG_TAG,"oldSteps:"+oldSteps);
                    curSteps = StepsCountUtils.getPhoneStepsByFirstSteps(context,oldSteps, sensorSteps);

                }catch(Exception e){
                    e.printStackTrace();
                    curSteps = "0";
                }
                */
               oldSteps = android.provider.Settings.Global.getString(getContentResolver(),"step_local");
               Log.d(DEBUG_TAG,"oldSteps:"+oldSteps);
               curSteps = StepsCountUtils.getPhoneStepsByFirstSteps(context,oldSteps, sensorSteps);
               Log.d(DEBUG_TAG,"curSteps:"+curSteps);

                mWatchSteps.setText(curSteps+"/"+targetLevel);
                mWatchStepsProgress.setProgress(Integer.valueOf(curSteps));
                mWatchStepsProgress.setMax(Integer.valueOf(targetLevel));
                Log.d(DEBUG_TAG,"curSteps:"+curSteps+":"+targetLevel);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nerservice = (XiaoXunNetworkManager)getSystemService("xun.network.Service");
				statisticsManager = (XiaoXunStatisticsManager) getSystemService("xun.statistics.service");
				statisticsManager.stats(XiaoXunStatisticsManager.STATS_MOTION);
        statisticsTime = StepsCountUtils.getTimeStampLocal();
        
        
        targetLevel = android.provider.Settings.System.getString(getContentResolver(),"steps_target_level");
        if(targetLevel== null || targetLevel.equals("")){
            targetLevel = "8000";
        }
        //targetLevel = StepsCountUtils.getStringValue(this,Const.STEPS_TARGET_LEVEL,"8000");
        Log.e("target Level:",targetLevel.toString());
        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        Log.e(DEBUG_TAG, "oncreate");
//        StepsCountUtils.initSensor(this ,"0");
        sendBroForSteps();
        if(targetLevel== null || targetLevel.equals("")){
            targetLevel = "8000";
        }
	
        initView();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Const.ACTION_BROAST_SENSOR_STEPS_REQ);
        registerReceiver(broadcastReceiver,filter);
        RegisterTargetStepObserve();
    }

    private void sendBroForSteps(){
        Intent _intent = new Intent(Const.ACTION_BROAST_SENSOR_STEPS);
        _intent.setPackage("com.xxun.watch.location");
        sendBroadcast(_intent);
    }

    private void RegisterTargetStepObserve(){
        this.getContentResolver().registerContentObserver(
                android.provider.Settings.System.getUriFor("steps_target_level"),
                true, mTargetObserver);
    }

    final private ContentObserver mTargetObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            targetLevel = android.provider.Settings.System.getString(getContentResolver(),"steps_target_level");
            Log.d("target change", targetLevel);
        }
    };


    private void initView(){
        mWatchSteps = (TextView) findViewById(R.id.watch_steps_title);
        mWatchStepsProgress = (ProgressBar) findViewById(R.id.watch_steps_progress);
        mHeadIcon = (ImageView) findViewById(R.id.head_icon);
        mHeadIcon.setOnTouchListener(new View.OnTouchListener() {
            float lastX = 0;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    lastX = motionEvent.getX();
                }else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                    float newX = motionEvent.getX();
                    if((lastX - newX) >= 20){
                        Intent intent = new Intent(MainActivity.this, stepRanksActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_left);
                    }else if((newX - lastX) >= 20){
                        finish();
                        overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_right);
                    }else{
                        Intent intent = new Intent(MainActivity.this, stepRanksActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_left);
                    }
                }
                return true;
            }
        });
        mWatchStepsProgress.setMax(Integer.valueOf(targetLevel));
        mWatchStepsProgress.setProgress(Integer.valueOf(curSteps));
        mWatchSteps.setText(curSteps+"/"+targetLevel);
    }

    private void getNetServiceData(){
        getMapGetValue();
    }

    private void getMapGetValue(){
        JSONObject pl = new JSONObject();
        JSONArray plKeyList = new JSONArray();
        plKeyList.add(Const.STEPS_TARGET_LEVEL);

        pl.put(Const.KEY_NAME_EID, nerservice.getWatchEid());
        pl.put(Const.KEY_NAME_KEYS, plKeyList);
        String sendData = StepsCountUtils.obtainCloudMsgContent(
                Const.CID_MAPGET_MGET,nerservice.getMsgSN(),nerservice.getSID(),pl).toJSONString();
        Log.e("mainActivity"," senddata:mepget:"+sendData);
        nerservice.sendJsonMessage(sendData,
                new ResponseDataCallBack());
    }

    private class ResponseDataCallBack extends IResponseDataCallBack.Stub{
		    @Override
                    public void onSuccess(ResponseData responseData) {
			try{
		                Log.e("MainActivity","responseData:"+responseData.toString());
				JSONObject responseJson = (JSONObject)JSONValue.parse(responseData.getResponseData());
		                int responRc = (int)responseJson.get("RC");
		                if(responRc == 1){
		                    JSONObject pl = (JSONObject) responseJson.get("PL");
		                    targetLevel = (String)pl.get(Const.STEPS_TARGET_LEVEL);
		                    if(targetLevel != null){
		                        StepsCountUtils.setValue(MainActivity.this,Const.STEPS_TARGET_LEVEL,targetLevel);
		                        mWatchStepsProgress.setMax(Integer.valueOf(targetLevel));
		                        mWatchStepsProgress.setProgress(Integer.valueOf(curSteps));
		                    }
		                }
			}catch(Exception e){
				Log.e("error","mainActivity parse error!");					
			}	
                    }

                    @Override
                    public void onError(int i, String s) {
                        Log.e("MainActivity","onError"+i+":"+s);
                    }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        getContentResolver().unregisterContentObserver(mTargetObserver);
        if(statisticsTime != null){
	        String curTimeStamp = StepsCountUtils.getTimeStampLocal();
	        long timeStampSec = StepsCountUtils.calcTwoTimeStampInterval(statisticsTime,curTimeStamp);
	        Log.e("stepscount MainActivity",timeStampSec+":"+(int)timeStampSec);
	        statisticsManager.stats(XiaoXunStatisticsManager.STATS_MOTION_TIME,(int)timeStampSec);
	      }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";
        private int verticalMinDistance = 20;

        private int minVelocity         = 0;
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d(DEBUG_TAG, "onFling: " + event1.toString()+event2.toString());
            if(event1.getX() - event2.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {
                Intent intent = new Intent(MainActivity.this, stepRanksActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_left);
            }else if(event2.getX() - event1.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {
                finish();
                overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_right);
            }
            return true;
        }
    }

}
