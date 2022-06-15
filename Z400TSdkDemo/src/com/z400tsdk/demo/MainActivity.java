package com.z400tsdk.demo;

import com.sleepace.sdk.domain.BleDevice;
import com.sleepace.sdk.interfs.IConnectionStateCallback;
import com.sleepace.sdk.interfs.IDeviceManager;
import com.sleepace.sdk.manager.CONNECTION_STATE;
import com.sleepace.sdk.util.SdkLog;
import com.sleepace.sdk.z400t.Z400THelper;
import com.sleepace.sdk.z400t.domain.HistoryData;
import com.z400tsdk.demo.fragment.ControlFragment;
import com.z400tsdk.demo.fragment.DataFragment;
import com.z400tsdk.demo.fragment.DeviceFragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class MainActivity extends BaseActivity {
	
	private RadioGroup rgTab;
	private RadioButton rbDevice, rbRealtimeData, rbHistoryData;
	private FragmentManager fragmentMgr;
	private Fragment deviceFragment, controlFragment, dataFragment;
	private BleDevice device;
	private Z400THelper z400tHelper;
	private ProgressDialog upgradeDialog;
	
	//缓存数据
	public static String deviceName, deviceId, power, version, temp, hum;
	public static boolean realtimeDataOpen;
	public static final int TAB_DEVICE = 0;
	public static final int TAB_REALTIME_DATA = 1;
	public static final int TAB_HISTORY_REPORT = 2;
	private int tabIndex = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		z400tHelper = Z400THelper.getInstance(this);
		findView();
		initListener();
		initUI();
	}
	
	
	@Override
	protected void findView() {
		// TODO Auto-generated method stub
		super.findView();
		rgTab = (RadioGroup) findViewById(R.id.rg_tab);
		rbDevice = (RadioButton) findViewById(R.id.rb_device);
		rbRealtimeData = (RadioButton) findViewById(R.id.rb_control);
		rbHistoryData = (RadioButton) findViewById(R.id.rb_data);
	}


	@Override
	protected void initListener() {
		// TODO Auto-generated method stub
		super.initListener();
		z400tHelper.addConnectionStateCallback(stateCallback);
		rgTab.setOnCheckedChangeListener(checkedChangeListener);
	}


	@Override
	protected void initUI() {
		// TODO Auto-generated method stub
		super.initUI();
		device = (BleDevice) getIntent().getSerializableExtra("device");
		fragmentMgr = getFragmentManager();
		deviceFragment = new DeviceFragment();
		controlFragment = new ControlFragment();
		dataFragment = new DataFragment();
//		rbDevice.setChecked(true);
		ivBack.setImageResource(R.drawable.tab_btn_scenes_home);
		
		upgradeDialog = new ProgressDialog(this);
		upgradeDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);// 设置进度条的形式为圆形转动的进度条
		upgradeDialog.setMessage(getString(R.string.fireware_updateing, ""));
		upgradeDialog.setCancelable(false);
		upgradeDialog.setCanceledOnTouchOutside(false);
		showTab(TAB_DEVICE, null);
	}
	
	public void showTab(int tabIndex, HistoryData historyData) {
		if(this.tabIndex != tabIndex) {
			this.tabIndex = tabIndex;
			switch(tabIndex) {
			case TAB_DEVICE:
				rbDevice.setChecked(true);
				break;
			case TAB_REALTIME_DATA:{
				rbRealtimeData.setChecked(true);
				break;
			}
			case TAB_HISTORY_REPORT:{
				rgTab.setTag("ok");
				rbHistoryData.setChecked(true);
				FragmentTransaction trans = fragmentMgr.beginTransaction();
				Bundle bundle = new Bundle();
				bundle.putSerializable("historyData", historyData);
				dataFragment.setArguments(bundle);
				trans.replace(R.id.content, dataFragment);
				trans.commit();
				break;
			}
			}
		}
	}
	
	
	public void setTitle(int res){
		tvTitle.setText(res);
	}
	
	public BleDevice getDevice() {
		return device;
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v == ivBack){
			exit();
		}
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode == KeyEvent.KEYCODE_BACK){
			exit();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	public void exit(){
		z400tHelper.disconnect();
		clearCache();
		Intent intent = new Intent(this, SplashActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}
	
	
	private void clearCache(){
		realtimeDataOpen = false;
		deviceName = null;
		deviceId = null;
		power = null;
		version = null;
		temp = null;
		hum = null;
	}
	
	private IConnectionStateCallback stateCallback = new IConnectionStateCallback() {
		@Override
		public void onStateChanged(IDeviceManager manager, CONNECTION_STATE state) {
			// TODO Auto-generated method stub
			SdkLog.log(TAG+" onStateChanged state:" + state);
			if(state == CONNECTION_STATE.DISCONNECT){
				realtimeDataOpen = false;
			}
		}
	};
	
	
	private OnCheckedChangeListener checkedChangeListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// TODO Auto-generated method stub
			FragmentTransaction trans = fragmentMgr.beginTransaction();
			if(checkedId == R.id.rb_device){
				trans.replace(R.id.content, deviceFragment);
			}else if(checkedId == R.id.rb_control){
				trans.replace(R.id.content, controlFragment);
			}else if(checkedId == R.id.rb_data){
				trans.replace(R.id.content, dataFragment);
			}
			trans.commit();
		}
	};
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK){
			
		}
	}
	
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		z400tHelper.removeConnectionStateCallback(stateCallback);
	}
	
	public void showUpgradeDialog(){
		upgradeDialog.show();
	}
	
	public void setUpgradeProgress(int progress) {
		if(upgradeDialog != null && upgradeDialog.isShowing()) {
			upgradeDialog.setProgress(progress);
		}
	}
	
	public void hideUpgradeDialog(){
		upgradeDialog.dismiss();
	}
	
}








































