package com.sleepace.z400tsdk.demo.fragment;

import com.sleepace.sdk.manager.CallbackData;
import com.sleepace.sdk.z400t.Z400THelper;
import com.sleepace.z400tsdk.demo.MainActivity;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public abstract class BaseFragment extends Fragment implements OnClickListener{
	
	protected String TAG = getClass().getSimpleName();
	protected MainActivity mActivity;
	private Z400THelper z400tHelper;
	private boolean isFragmentVisible;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mActivity = (MainActivity) getActivity();
		z400tHelper = Z400THelper.getInstance(mActivity);
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		setFragmentVisible(true);
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		setFragmentVisible(false);
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
	    super.onHiddenChanged(hidden);
		if(hidden){
			 setFragmentVisible(false);
		} else {
			setFragmentVisible(true);
		}
	}

	public Z400THelper getZ400THelper() {
		return z400tHelper;
	}

	protected void findView(View root) {
		// TODO Auto-generated method stub
	}


	protected void initListener() {
		// TODO Auto-generated method stub
	}


	protected void initUI() {
		// TODO Auto-generated method stub
	}
	
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

	public void printLog(int strRes){
		
	}
	
	public boolean checkStatus(CallbackData cd){
		return mActivity.checkStatus(cd);
	}

	public boolean isFragmentVisible() {
		if(isAdded()) {
			return isFragmentVisible;
		}
		return false;
	}

	public void setFragmentVisible(boolean isFragmentVisible) {
		this.isFragmentVisible = isFragmentVisible;
	}
	
	
	
}



