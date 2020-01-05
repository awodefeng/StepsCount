package com.xxun.watch.stepstart;

import android.support.v4.view.GestureDetectorCompat;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import com.xiaoxun.sdk.ResponseData;
import com.xiaoxun.sdk.IResponseDataCallBack;
import com.xiaoxun.sdk.XiaoXunNetworkManager;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.lang.ref.WeakReference;

public class stepRanksActivity extends Activity{

    private GestureDetectorCompat mDetector;

    private TextView mTotalRanks;
    private TextView mFriendsRanks;
    private TextView mCityRanks;
    private TextView mAreaName;
    private XiaoXunNetworkManager nerservice;

    private static class MyHandler extends Handler  {
        private final WeakReference<stepRanksActivity> mActivity;

        public MyHandler(stepRanksActivity activity){
            mActivity = new WeakReference<stepRanksActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg){
            if(mActivity.get() == null){
                return ;
            }
            if(msg.what==1){
                try {
                    String rankData = (String) msg.obj;
                    Log.e("rankData:", rankData);
                    mActivity.get().SetStepRankView(rankData);
                }catch(Exception e){
                    Log.e("exception:",e.toString());
                }
            }
        }
    }

    public void SetStepRankView(String rankData){
        String[] rankArray = rankData.split("_");
        mFriendsRanks.setText(rankArray[0]);
        mAreaName.setText(rankArray[1]);
        mCityRanks.setText(rankArray[2]);
        mTotalRanks.setText(rankArray[3]);
    }

    private final Handler handle = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_ranks);
        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        initView();
        nerservice = (XiaoXunNetworkManager)getSystemService("xun.network.Service");
        getNetServiceRanksData();
    }
    private void initView(){
        mFriendsRanks = (TextView) findViewById(R.id.ranks_text_friend);
        mCityRanks = (TextView) findViewById(R.id.ranks_text_area);
        mTotalRanks = (TextView) findViewById(R.id.ranks_text_all);
        mAreaName = (TextView) findViewById(R.id.ranks_area_name);
    }

    private void getNetServiceRanksData(){
        JSONObject pl = new JSONObject();
        String sendData = StepsCountUtils.obtainCloudMsgContent(
                Const.CID_GETSTEPS_RANKS,nerservice.getMsgSN(),nerservice.getSID(),pl).toJSONString();
        Log.e("stepRanks sendData:",sendData);
        nerservice.sendJsonMessage(sendData,
                new ResponseDataCallBack());
    }

    public class ResponseDataCallBack extends IResponseDataCallBack.Stub{
	    @Override
            public void onSuccess(ResponseData responseData) {
                Log.e("recevice:", responseData.toString());
		        JSONObject responJson = (JSONObject)JSONValue.parse(responseData.getResponseData());
                int responRc = (int)responJson.get("RC");
                try{
                    if(responRc == 1) {
                        JSONObject pl = (JSONObject) responJson.get("PL");
                        String cityName = (String)pl.get(Const.STEPS_RANKS_CITYNAME);
                        int total = (int) pl.get(Const.STEPS_RANKS_TOTAL);
                        int city = (int) pl.get(Const.STEPS_RANKS_CITY);
                        int cityUserCount = (int) pl.get(Const.STEPS_RANKS_CITYUSERCOUNT);
                        int friends = (int)pl.get(Const.STEPS_RANKS_FRIENDS);
                        int globalUserCount = (int)pl.get(Const.STEPS_RANKS_GLOBALUSERCOUNT);
                        String rankData = "第"+friends+"名"+"_"+cityName+"_"+"前"+(city*100/cityUserCount)+"%"
                                +"_"+"前"+(total*100/globalUserCount)+"%";
                        Log.e("rankData:",rankData);
                        Message msg = Message.obtain();
                        msg.obj = rankData;
                        msg.what = 1;
                        handle.sendMessage(msg);
                    }else{
                        String rankData = "暂无排名"+"_城市_"+"暂无排名"
							+"_"+"暂无排名";
						Log.e("rankData error:",rankData);
						Message msg = Message.obtain();
						msg.obj = rankData;
						msg.what = 1;
						handle.sendMessage(msg);
                    }
                }catch(Exception e){
                     Log.e("stepsRanks:","parse error!");
                }
            }

            @Override
            public void onError(int i, String s) {
				try{
					String rankData = "暂无排名"+"_城市_"+"暂无排名"
							+"_"+"暂无排名";
					Log.e("rankData error:",rankData);
					Message msg = Message.obtain();
					msg.obj = rankData;
					msg.what = 1;
					handle.sendMessage(msg);
				}catch(Exception e){
					Log.e("stepsRanks:","网络错误");
				}
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

            }else if(event2.getX() - event1.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {
                finish();
                overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_right);
            }
            return true;
        }
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        handle.removeCallbacksAndMessages(null);
    }
}
