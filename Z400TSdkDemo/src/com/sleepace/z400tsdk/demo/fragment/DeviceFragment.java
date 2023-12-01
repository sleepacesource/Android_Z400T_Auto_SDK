package com.sleepace.z400tsdk.demo.fragment;

import java.io.IOException;
import java.io.InputStream;

import com.sleepace.sdk.interfs.IConnectionStateCallback;
import com.sleepace.sdk.interfs.IDeviceManager;
import com.sleepace.sdk.interfs.IResultCallback;
import com.sleepace.sdk.manager.CONNECTION_STATE;
import com.sleepace.sdk.manager.CallbackData;
import com.sleepace.sdk.util.SdkLog;
import com.sleepace.sdk.z400t.domain.BatteryBean;
import com.sleepace.z400tsdk.demo.MainActivity;
import com.sleepace.z400tsdk.demo.R;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceFragment extends BaseFragment {
	private Button btnDeviceName, btnDeviceId, btnPower, /* btnEnvirData, */ btnVersion, btnUpgrade;
	private TextView tvDeviceName, tvDeviceId, tvPower, /* tvEnvirData, */ tvVersion, tvUpgrade;
	private Button btnDisconnect;
	// private View envirView;
	private boolean upgrading = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View root = inflater.inflate(R.layout.fragment_device, null);
		// LogUtil.log(TAG+" onCreateView-----------");
		findView(root);
		initListener();
		initUI();
		return root;
	}

	protected void findView(View root) {
		// TODO Auto-generated method stub
		super.findView(root);
		tvDeviceName = (TextView) root.findViewById(R.id.tv_device_name);
		tvDeviceId = (TextView) root.findViewById(R.id.tv_device_id);
		tvPower = (TextView) root.findViewById(R.id.tv_device_battery);
		tvVersion = (TextView) root.findViewById(R.id.tv_device_version);
		tvUpgrade = (TextView) root.findViewById(R.id.tv_upgrade_fireware);
		btnDeviceName = (Button) root.findViewById(R.id.btn_get_device_name);
		btnDeviceId = (Button) root.findViewById(R.id.btn_get_device_id);
		btnPower = (Button) root.findViewById(R.id.btn_get_device_battery);
		// btnEnvirData = (Button) root.findViewById(R.id.btn_get_envir_data);
		// envirView = root.findViewById(R.id.layout_envir_data);
		// tvEnvirData = (TextView) root.findViewById(R.id.tv_envir_data);
		btnVersion = (Button) root.findViewById(R.id.btn_device_version);
		btnUpgrade = (Button) root.findViewById(R.id.btn_upgrade_fireware);
		btnDisconnect = (Button) root.findViewById(R.id.btn_disconnect);
	}

	protected void initListener() {
		// TODO Auto-generated method stub
		super.initListener();
		getZ400THelper().addConnectionStateCallback(stateCallback);
		btnDeviceName.setOnClickListener(this);
		btnDeviceId.setOnClickListener(this);
		btnPower.setOnClickListener(this);
		// btnEnvirData.setOnClickListener(this);
		btnVersion.setOnClickListener(this);
		btnUpgrade.setOnClickListener(this);
		btnDisconnect.setOnClickListener(this);
	}

	protected void initUI() {
		// TODO Auto-generated method stub
		mActivity.setTitle(R.string.device_);
		btnDisconnect.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
		setPageEnable(getZ400THelper().isConnected());
		tvDeviceName.setText(MainActivity.deviceName);
		tvDeviceId.setText(MainActivity.deviceId);
		tvPower.setText(MainActivity.power);
		// if(mActivity.getDevice() != null) {
		// if(DeviceType.isP3(mActivity.getDevice().getDeviceType())) {
		// envirView.setVisibility(View.VISIBLE);
		// if(!TextUtils.isEmpty(MainActivity.temp)) {
		// tvEnvirData.setText(getString(R.string.temp)+":" + MainActivity.temp + " " +
		// getString(R.string.hum) +":" + MainActivity.hum);
		// }
		// }else {
		// envirView.setVisibility(View.GONE);
		// }
		// }else {
		// envirView.setVisibility(View.GONE);
		// }
		tvVersion.setText(MainActivity.version);
	}

	private void setPageEnable(boolean enable) {
		btnDeviceName.setEnabled(enable);
		btnDeviceId.setEnabled(enable);
		btnPower.setEnabled(enable);
		// btnEnvirData.setEnabled(enable);
		btnVersion.setEnabled(enable);
		btnUpgrade.setEnabled(enable);
		btnDisconnect.setEnabled(enable);
	}

	private IConnectionStateCallback stateCallback = new IConnectionStateCallback() {
		@Override
		public void onStateChanged(IDeviceManager manager, final CONNECTION_STATE state) {
			// TODO Auto-generated method stub
			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					if (!isFragmentVisible()) {
						return;
					}

					if (state == CONNECTION_STATE.DISCONNECT) {

						if (upgrading) {
							upgrading = false;
							mActivity.hideUpgradeDialog();
							// printLog(R.string.update_completed);
							// tvUpgrade.setText(R.string.update_completed);
							Toast.makeText(mActivity, R.string.update_success, Toast.LENGTH_LONG).show();
						}

						setPageEnable(false);
						// printLog(R.string.connection_broken);

					} else if (state == CONNECTION_STATE.CONNECTED) {

						if (upgrading) {
							upgrading = false;
							btnUpgrade.setEnabled(true);
							mActivity.hideUpgradeDialog();
							// printLog(R.string.update_completed);
							// tvUpgrade.setText(R.string.update_completed);
							Toast.makeText(mActivity, R.string.update_success, Toast.LENGTH_LONG).show();
						}

					}
				}
			});
		}
	};

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
		getZ400THelper().removeConnectionStateCallback(stateCallback);
	}

	private void upgrade(FirmwareBean bean) {
		if (bean == null) {
			return;
		}
		btnUpgrade.setEnabled(false);
		mActivity.showUpgradeDialog();
		upgrading = true;
		getZ400THelper().stopRealTimeData(3000, new IResultCallback<Void>() {
			@Override
			public void onResultCallback(CallbackData<Void> cd) {
				// TODO Auto-generated method stub
				SdkLog.log(TAG + " upgrade stopRealTimeData cd:" + cd);
			}
		});
		getZ400THelper().upgradeDevice(bean.crcDes, bean.crcBin, bean.is, new IResultCallback() {
			@Override
			public void onResultCallback(final CallbackData cd) {
				// TODO Auto-generated method stub
				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (!isFragmentVisible()) {
							return;
						}

						if (checkStatus(cd)) {
							int progress = (Integer) cd.getResult();
							mActivity.setUpgradeProgress(progress);
							// tvUpgrade.setText(progress+"%");
							if (progress == 100) {
								// printLog(getString(R.string.reboot_device, getString(R.string.device_name)));
								// tvUpgrade.setText(getString(R.string.reboot_device,
								// getString(R.string.device_name)));
							}
						} else {
							upgrading = false;
							btnUpgrade.setEnabled(true);
							mActivity.hideUpgradeDialog();
							Toast.makeText(mActivity, R.string.update_failed, Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		});
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		super.onClick(v);
		if (v == btnUpgrade) {
			try {
				FirmwareBean bean = new FirmwareBean();
				bean.is = getResources().getAssets().open("Z400T-2(Z400T&SW)-v1.39(v2.01.02b)-g-20231110.des");
				bean.crcBin = 1578783682l;
				bean.crcDes = 3266595119l;
				upgrade(bean);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (v == btnDeviceName) {
			// printLog(R.string.getting_device_name);
			MainActivity.deviceName = mActivity.getDevice().getDeviceName();
			tvDeviceName.setText(MainActivity.deviceName);
			// printLog(getString(R.string.receive_device_name,
			// mActivity.getDevice().getDeviceName()));
		} else if (v == btnDeviceId) {
			// printLog(R.string.getting_device_id);
			MainActivity.deviceId = mActivity.getDevice().getDeviceId();
			tvDeviceId.setText(MainActivity.deviceId);
			// printLog(getString(R.string.get_device_id,
			// mActivity.getDevice().getDeviceId()));
		} else if (v == btnPower) {
			// printLog(R.string.getting_power);
			getZ400THelper().getBattery(1000, new IResultCallback<BatteryBean>() {
				@Override
				public void onResultCallback(final CallbackData<BatteryBean> cd) {
					// TODO Auto-generated method stub
					mActivity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							if (!isFragmentVisible()) {
								return;
							}

							if (checkStatus(cd)) {
								BatteryBean bean = cd.getResult();
								MainActivity.power = bean.getQuantity() + "%";
								tvPower.setText(MainActivity.power);
								// printLog(getString(R.string.get_power, MainActivity.power));
							}
						}
					});
				}
			});
		} /*
			 * else if(v == btnEnvirData){ printLog(R.string.getting_envir_data);
			 * getPillowHelper().getEnvironmentalData(1000, new
			 * IResultCallback<EnvironmentData>() {
			 * 
			 * @Override public void onResultCallback(final CallbackData<EnvironmentData>
			 * cd) { // TODO Auto-generated method stub mActivity.runOnUiThread(new
			 * Runnable() {
			 * 
			 * @Override public void run() { // TODO Auto-generated method stub
			 * SdkLog.log(TAG+" getEnvironmentalData cd:" + cd); if(checkStatus(cd)){
			 * EnvironmentData bean = cd.getResult(); MainActivity.temp =
			 * bean.getTemperature()/100 + "℃"; MainActivity.hum = bean.getHumidity() + "%";
			 * tvEnvirData.setText(getString(R.string.temp)+":" + MainActivity.temp + "  " +
			 * getString(R.string.hum) +":" + MainActivity.hum);
			 * printLog(getString(R.string.get_envir_data)+ ":" +
			 * tvEnvirData.getText().toString()); } } }); } }); }
			 */else if (v == btnVersion) {
			// printLog(R.string.getting_current_version);
			getZ400THelper().getDeviceVersion(1000, new IResultCallback<String>() {
				@Override
				public void onResultCallback(final CallbackData<String> cd) {
					// TODO Auto-generated method stub
					mActivity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							if (!isFragmentVisible()) {
								return;
							}

							if (checkStatus(cd)) {
								MainActivity.version = cd.getResult();
								tvVersion.setText(MainActivity.version);
								// printLog(getString(R.string.get_the_current_version, MainActivity.version));
							}
						}
					});
				}
			});
		} else if (v == btnDisconnect) {
			// printLog(R.string.disconnected_successfully);
			setPageEnable(false);

			mActivity.exit();
		}
	}

	class FirmwareBean {
		InputStream is;
		long crcBin;
		long crcDes;
	}

}
