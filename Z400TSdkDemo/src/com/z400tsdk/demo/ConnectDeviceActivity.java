package com.z400tsdk.demo;

import com.sleepace.sdk.domain.BleDevice;
import com.sleepace.sdk.interfs.IResultCallback;
import com.sleepace.sdk.manager.CallbackData;
import com.sleepace.sdk.manager.DeviceType;
import com.sleepace.sdk.z400t.Z400THelper;
import com.sleepace.sdk.z400t.domain.LoginBean;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class ConnectDeviceActivity extends BaseActivity {
    private EditText etUserId;
    private TextView tvDeviceCode;
    private Button btnConnect;
    
    private BleDevice device;
    private Z400THelper z400tHelper;
    private SharedPreferences mSetting;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_device);
        z400tHelper = Z400THelper.getInstance(this);
        mSetting = getSharedPreferences("config", Context.MODE_PRIVATE);
        findView();
        initListener();
        initUI();
    }


    public void findView() {
    	super.findView();
    	etUserId = (EditText) findViewById(R.id.et_userid);
    	tvDeviceCode = (TextView) findViewById(R.id.tv_device_code);
    	btnConnect = (Button) findViewById(R.id.btn_connect_device);
    }

    public void initListener() {
    	super.initListener();
    	tvDeviceCode.setOnClickListener(this);
    	btnConnect.setOnClickListener(this);
    }

    public void initUI() {
    	super.initUI();
    	device = (BleDevice) getIntent().getSerializableExtra("device");
        tvTitle.setText(R.string.connect_device);
        printLog(null);
        String uid = mSetting.getString("uid", "1");
        etUserId.setText(uid);
        etUserId.setSelection(etUserId.length());
        
//        String code = DEVICE_CODE[0];
//		tvDeviceCode.setText(code);
//		DeviceType deviceType = SleepUtil.getDeviceTypeFromCode(code);
//		device.setDeviceType(deviceType);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public void onClick(View v) {
    	super.onClick(v);
    	if(v == tvDeviceCode){
//    		new AlertDialog.Builder(this)
//    		.setIcon(android.R.drawable.ic_dialog_info)
//    		.setTitle(R.string.device_code)
//    		.setItems(DEVICE_CODE, new OnClickListener() {
//				@Override
//				public void onClick(DialogInterface dialog, int which) {
//					// TODO Auto-generated method stub
//					String code = DEVICE_CODE[which];
//					tvDeviceCode.setText(code);
////					DeviceType deviceType = SleepUtil.getDeviceTypeFromCode(code);
////					device.setDeviceType(deviceType);
//				}
//			})
//    		.setNegativeButton(android.R.string.cancel, null)
//    		.show();
    	}else if (v == btnConnect) {
    		
//    		final String deviceCode = tvDeviceCode.getText().toString();
//    		if(TextUtils.isEmpty(deviceCode)){
//    			Toast.makeText(this, R.string.device_code, Toast.LENGTH_SHORT).show();
//    			return;
//    		}
    		
        	//printLog(R.string.userid_judgment);
        	
        	String uid = etUserId.getText().toString().trim();
        	if(!TextUtils.isEmpty(uid)){
        		
        		btnConnect.setEnabled(false);
        		//printLog(R.string.non_empty);
        		//printLog(R.string.connecting_device);
        		
//        		mSetting.edit().putString("uid", uid).commit();
            	showLoading();
        		int userId = Integer.valueOf(uid);
        		z400tHelper.login(device.getDeviceName(), device.getAddress(), device.getDeviceType(), userId, 10 * 1000, new IResultCallback<LoginBean>() {
					@Override
					public void onResultCallback(final CallbackData<LoginBean> cd) {
						// TODO Auto-generated method stub
						
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								hideLoading();
								btnConnect.setEnabled(true);
								if(cd.isSuccess()){
									//printLog(R.string.connect_device_successfully);
									LoginBean bean =  cd.getResult();
									device.setDeviceId(bean.getDeviceId());
									Intent intent = new Intent(mContext, MainActivity.class);
									intent.putExtra("device", device);
									startActivity(intent);
								}else{
									//printLog(R.string.failure);
								}
							}
						});
						
					}
				});
        	}else {
        		Toast.makeText(getApplicationContext(), R.string.toast_user_id, Toast.LENGTH_SHORT).show();
        	}
        }
    }
    
}












