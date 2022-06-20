package com.sleepace.z400tsdk.demo;

import com.sleepace.sdk.constant.StatusCode;
import com.sleepace.sdk.manager.CallbackData;
import com.sleepace.z400tsdk.demo.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

public class BaseActivity extends Activity implements OnClickListener {
	protected final String TAG = this.getClass().getSimpleName();
	protected ImageView ivBack;
	protected TextView tvTitle;
	protected ImageView ivRight;
	protected BaseActivity mContext;
	private ProgressDialog loadingDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mContext = this;
	}

	protected void findView() {
		ivBack = (ImageView) findViewById(R.id.iv_back);
		tvTitle = (TextView) findViewById(R.id.tv_title);
	};

	protected void initListener() {
		if(ivBack != null){
			ivBack.setOnClickListener(this);
		}
	};
	
	protected void initUI() {
		
	}
	
	public void showLoading() {
		if(loadingDialog == null) {
			loadingDialog = new ProgressDialog(this);
			loadingDialog.setCancelable(false);
			loadingDialog.setCanceledOnTouchOutside(false);
		}
		loadingDialog.show();
	}
	
	public void hideLoading() {
		if(loadingDialog != null) {
			loadingDialog.dismiss();
		}
	}

	@Override
	public void onClick(View v) {
		if(v == ivBack){
			finish();
		}
	}
	
	public void printLog(int strRes){
		String log = getString(strRes);
		printLog(log);
	}
	
	public void printLog(String log){
		
	}

	public void printLog(int strRes, TextView tvLog){
		String log = getString(strRes);
		printLog(log, tvLog);
	}
	
	private static final int MAX_LOG_ROW_COUNT = 50;
	private static final int CACHE_LOG_ROW_COUNT = 100;
	
	public void printLog(String log, final TextView tvLog){
		
	}
	
	public boolean checkStatus(CallbackData cd){
		if(!cd.isSuccess()){
			return false;
		}
		return true;
	}
	
	public void showMessage(int titleRes, int msgRes) {
		showMessage(getString(titleRes), getString(msgRes));
	}
	
	public void showMessage(String title, String message) {
        final AlertDialog dialog = new AlertDialog.Builder(this)
        		.setPositiveButton(android.R.string.ok, null)
        		.create();
        dialog.setIcon(android.R.drawable.ic_dialog_info);
        dialog.setTitle(title);
        dialog.setMessage(message);
//        Window win = dialog.getWindow();
//        WindowManager.LayoutParams lp = win.getAttributes();
//        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
//        win.setAttributes(lp);

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
	
	public String getErrMsg(int status){
		if(status == StatusCode.NOT_ENABLE){
			return getString(R.string.not_bluetooth);
		}
		
		if(status == StatusCode.DISCONNECT){
			//return getString(R.string.reconnect_device);
		}
		
		if(status == StatusCode.TIMEOUT){
			//return getString(R.string.time_out);
		}
		
		if(status == StatusCode.FAIL){
			//return getString(R.string.failure);
		}
		
		return "";
	}
}


















