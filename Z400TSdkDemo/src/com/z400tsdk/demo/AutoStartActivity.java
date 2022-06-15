package com.z400tsdk.demo;

import com.sleepace.sdk.z400t.Z400THelper;
import com.z400tsdk.demo.R;
import com.z400tsdk.demo.view.wheelview.NumericWheelAdapter;
import com.z400tsdk.demo.view.wheelview.OnItemSelectedListener;
import com.z400tsdk.demo.view.wheelview.WheelAdapter;
import com.z400tsdk.demo.view.wheelview.WheelView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class AutoStartActivity extends BaseActivity {
    private WheelView wvHour, wvMinute;
    private WheelAdapter hourAdapter, minuteAdapter;
    private Button btnSave;
    private Z400THelper z400tHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        z400tHelper = Z400THelper.getInstance(this);
        setContentView(R.layout.activity_autostart);
        findView();
        initListener();
        initUI();
    }


    public void findView() {
    	super.findView();
        wvHour = (WheelView) findViewById(R.id.hour);
        wvMinute = (WheelView) findViewById(R.id.minute);
        btnSave = (Button) findViewById(R.id.btn_save);
    }

    public void initListener() {
    	super.initListener();
    	btnSave.setOnClickListener(this);
    }

    public void initUI() {
    	super.initUI();
//        tvTitle.setText(R.string.set_auto_monitor);
        
        wvHour.setAdapter(new NumericWheelAdapter(0, 23));
        wvHour.setTextSize(20);
        wvHour.setCyclic(true);
        wvHour.setOnItemSelectedListener(onHourItemSelectedListener);

        wvMinute.setAdapter(new NumericWheelAdapter(0, 59));
        wvMinute.setTextSize(20);
        wvMinute.setCyclic(true);
        wvMinute.setOnItemSelectedListener(onMiniteItemSelectedListener);

        wvHour.setRate(5 / 4.0f);
        wvMinute.setRate(1 / 2.0f);
        
        wvHour.setCurrentItem(22);
        wvMinute.setCurrentItem(0);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }


    @Override
    public void onClick(View v) {
    	super.onClick(v);
    	if(v == btnSave){
    		final int hour = wvHour.getCurrentItem();
			final int minute = wvMinute.getCurrentItem();
			//printLog(getString(R.string.writing_automatically_monitor_device, String.format("%02d:%02d", hour,minute)));
			int repeat = 127; //转车二进制 ：01111111，从右到左，分别表示周一，周二，周三，如果该位是1，表示当天重复，否则不重复。故127表示，周一到周日重复
    	}
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
    }



    //更新控件快速滑动
    private OnItemSelectedListener onHourItemSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(int index) {
            
        }
    };

    private OnItemSelectedListener onMiniteItemSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(int index) {
            
        }
    };
    
}












