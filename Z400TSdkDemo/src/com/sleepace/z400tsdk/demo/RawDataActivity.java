package com.sleepace.z400tsdk.demo;


import com.sleepace.sdk.interfs.IMonitorManager;
import com.sleepace.sdk.interfs.IResultCallback;
import com.sleepace.sdk.manager.CallbackData;
import com.sleepace.sdk.z400t.Z400THelper;
import com.sleepace.sdk.z400t.domain.OriginalData;
import com.sleepace.z400tsdk.demo.R;
import com.sleepace.z400tsdk.demo.util.DensityUtil;
import com.sleepace.z400tsdk.demo.view.RealTimeView;

import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

/**
 * <ul>
 * <li>显示实时波形的activity</li>
 * </ul>
 */
public class RawDataActivity extends BaseActivity {
	
	private LinearLayout heartLayout, breathLayout;
    private RealTimeView heartView, breathView;
    private Z400THelper z400tHelper;
    private int space = 10;
    private int x;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.acitivty_realtime_graph);
		z400tHelper = Z400THelper.getInstance(this);
		findView();
		initListener();
		initUI();
	}
	
	@Override
	protected void findView() {
		// TODO Auto-generated method stub
		super.findView();
	}
	
	@Override
	protected void initListener() {
		// TODO Auto-generated method stub
		super.initListener();
	}

    protected void initUI() {
    	tvTitle.setText(R.string.signal_strength);
        space = DensityUtil.dip2px(this, 0.5f);
        heartLayout = (LinearLayout) findViewById(R.id.layout_heart);
        breathLayout = (LinearLayout) findViewById(R.id.layout_breath);
        createGraph();
    }
    
    
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    
    private IResultCallback<OriginalData> rawDataCB = new IResultCallback<OriginalData>() {
		@Override
		public void onResultCallback(final CallbackData<OriginalData> cd) {
			// TODO Auto-generated method stub
//			SdkLog.log(TAG+" rawDataCB " + cd);
			if(cd.getCallbackType() == IMonitorManager.METHOD_RAW_DATA_OPEN){//接口执行结果回调
				
			}else if(cd.getCallbackType() == IMonitorManager.METHOD_RAW_DATA){//原始数据回调
				if(cd.isSuccess()){
					OriginalData data = cd.getResult();
					int len = data.getHeartRate() == null ? 0 : data.getHeartRate().length;
					for(int i=0;i<len;i++){
						if(i % 2 == 0){
							continue;
						}
//						SdkLog.log(TAG+" rawDataCB i:" + i +",x:" + x+",heartRate:" +data.getHeartRate()[i]+",breathRate:" + data.getBreathRate()[i]);
						heartView.add(new PointF(x, data.getHeartRate()[i]));
						breathView.add(new PointF(x, data.getBreathRate()[i]));
						x+=space;
					}
				}
			}
		}
	};

    private void createGraph() {
        heartView = new RealTimeView(this);
        heartView.setBondValue(-1, 1);
        heartView.setGraphLineColor(getResources().getColor(R.color.COLOR_2));
        heartLayout.addView(heartView);

        breathView = new RealTimeView(this);
        breathView.setBondValue(-1, 1);
        breathView.setGraphLineColor(getResources().getColor(R.color.COLOR_2));
        breathLayout.addView(breathView);
    }


    @Override
    public void onClick(View v) {
    	super.onClick(v);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}


























