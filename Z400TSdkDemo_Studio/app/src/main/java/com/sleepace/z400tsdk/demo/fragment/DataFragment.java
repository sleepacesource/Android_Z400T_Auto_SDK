package com.sleepace.z400tsdk.demo.fragment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sleepace.sdk.interfs.IConnectionStateCallback;
import com.sleepace.sdk.interfs.IDeviceManager;
import com.sleepace.sdk.interfs.IMonitorManager;
import com.sleepace.sdk.interfs.IResultCallback;
import com.sleepace.sdk.manager.CONNECTION_STATE;
import com.sleepace.sdk.manager.CallbackData;
import com.sleepace.sdk.manager.DeviceType;
import com.sleepace.sdk.util.SdkLog;
import com.sleepace.sdk.util.StringUtil;
import com.sleepace.sdk.util.TimeUtil;
import com.sleepace.sdk.z400t.constants.SleepConfig;
import com.sleepace.sdk.z400t.constants.SleepStatusType;
import com.sleepace.sdk.z400t.domain.Analysis;
import com.sleepace.sdk.z400t.domain.Detail;
import com.sleepace.sdk.z400t.domain.HistoryData;
import com.sleepace.sdk.z400t.domain.Summary;
import com.sleepace.sdk.z400t.util.AnalysisUtil;
import com.sleepace.z400tsdk.demo.R;
import com.sleepace.z400tsdk.demo.bean.CvPoint;
import com.sleepace.z400tsdk.demo.util.DensityUtil;
import com.sleepace.z400tsdk.demo.util.HistoryDataComparator;
import com.sleepace.z400tsdk.demo.util.SleepData;
import com.sleepace.z400tsdk.demo.view.graphview.GraphView;
import com.sleepace.z400tsdk.demo.view.graphview.GraphViewSeries;
import com.sleepace.z400tsdk.demo.view.graphview.GraphViewStyle;
import com.sleepace.z400tsdk.demo.view.graphview.LineGraphView;
import com.sleepace.z400tsdk.demo.view.graphview.LineGraphView.BedBean;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DataFragment extends BaseFragment {

	private LayoutInflater inflater;
	private LinearLayout reportLayout;
	private Button btnAnalysis, btnShort, btnLong;
	private HistoryData shortData, longData;
	private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
	private DateFormat timeFormat = new SimpleDateFormat("HH:mm");
	private ProgressDialog progressDialog;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		this.inflater = inflater;
		View view = inflater.inflate(R.layout.fragment_data, null);
		// SdkLog.log(TAG + " onCreateView-----------");
		findView(view);
		initListener();
		initUI();
		return view;
	}

	protected void findView(View root) {
		// TODO Auto-generated method stub
		super.findView(root);
		btnAnalysis = (Button) root.findViewById(R.id.btn_sleep_analysis);
		btnShort = (Button) root.findViewById(R.id.btn_sleep_short);
		btnLong = (Button) root.findViewById(R.id.btn_sleep_long);
		reportLayout = (LinearLayout) root.findViewById(R.id.layout_chart);
	}

	protected void initListener() {
		// TODO Auto-generated method stub
		super.initListener();
		getZ400THelper().addConnectionStateCallback(stateCallback);
		btnAnalysis.setOnClickListener(this);
		btnShort.setOnClickListener(this);
		btnLong.setOnClickListener(this);
	}

	protected void initUI() {
		// TODO Auto-generated method stub
		mActivity.setTitle(R.string.report_);
		btnAnalysis.setEnabled(getZ400THelper().isConnected());
		initDemoData();

		progressDialog = new ProgressDialog(mActivity);
		progressDialog.setIcon(android.R.drawable.ic_dialog_info);
		progressDialog.setMessage(getString(R.string.sleep_analysis));
		progressDialog.setCancelable(false);
		progressDialog.setCanceledOnTouchOutside(false);
		
		Bundle bundle = getArguments();
		if(bundle != null) {
			HistoryData historyData = (HistoryData) bundle.getSerializable("historyData");
			if(historyData != null && historyData.getAnalysis() != null) {
				if (historyData.getAnalysis().getReportFlag() == 1) {// 长报告
					initLongReportView(historyData);
				} else {
					initShortReportView(historyData);
				}
			}
		}
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
						btnAnalysis.setEnabled(false);
						// printLog(R.string.connection_broken);
					} else if (state == CONNECTION_STATE.CONNECTED) {

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

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		super.onClick(v);
		if (v == btnAnalysis) {
			progressDialog.show();
			stopRealTimeData();
		} else if (v == btnShort) {
			// printLog(R.string.simulation_short_report);
			initShortReportView(shortData);
		} else if (v == btnLong) {
			// printLog(R.string.simulation_long_report);
			initLongReportView(longData);
		}
	}
	
	
	private void stopRealTimeData() {
		// 下载历史数据前，建议停止原始数据上报，实时数据上报
		getZ400THelper().stopRealTimeData(3000, new IResultCallback<Void>() {
			@Override
			public void onResultCallback(CallbackData<Void> cd) {
				// TODO Auto-generated method stub
				if (!isFragmentVisible()) {
					return;
				}
				if (cd.getCallbackType() == IMonitorManager.METHOD_REALTIME_DATA_CLOSE) {
					SdkLog.log(TAG + " stopRealTimeData cd:" + cd);
				}
				
				stopCollection();
			}
		});
	}
	
	private void stopCollection() {
		// 下载历史数据前，建议停止原始数据上报，实时数据上报
		getZ400THelper().stopCollection(3000, new IResultCallback<Void>() {
			@Override
			public void onResultCallback(CallbackData<Void> cd) {
				// TODO Auto-generated method stub
				if (!isFragmentVisible()) {
					return;
				}
				if (cd.getCallbackType() == IMonitorManager.METHOD_COLLECT_STOP) {
					SdkLog.log(TAG + " stopCollection cd:" + cd);
				}
				downloadHistoryData();
			}
		});
	}
	
	
	private void downloadHistoryData() {
		// printLog(R.string.data_analyzed);
		Calendar cal = Calendar.getInstance();
		int endTime = (int) (cal.getTimeInMillis() / 1000);
		cal.set(Calendar.MONTH, 1);
		cal.set(Calendar.DAY_OF_MONTH, 28);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		int startTime = (int) (cal.getTimeInMillis() / 1000);
		getZ400THelper().historyDownload(startTime, endTime, 1, new IResultCallback<List<HistoryData>>() {
			@Override
			public void onResultCallback(final CallbackData<List<HistoryData>> cd) {
				// TODO Auto-generated method stub
				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (!isFragmentVisible()) {
							return;
						}

						progressDialog.dismiss();
						if (checkStatus(cd)) {
							List<HistoryData> list = cd.getResult();
							SdkLog.log(TAG + " historyDownload size:" + list.size());
							if (list.size() > 0) {
								Collections.sort(list, new HistoryDataComparator());
								HistoryData historyData = list.get(0);
								Detail detail = historyData.getDetail();
								SdkLog.log(TAG + " historyDownload status:" + Arrays.toString(detail.getStatus()));
								SdkLog.log(TAG + " historyDownload statusVal:" + Arrays.toString(detail.getStatusValue()));
								SdkLog.log(TAG + " historyDownload first data duration:" + historyData.getSummary().getRecordCount() + ",algorithmVer:" + historyData.getAnalysis().getAlgorithmVer());
								if (historyData.getAnalysis().getReportFlag() == 1) {// 长报告
									initLongReportView(historyData);
								} else {
									initShortReportView(historyData);
								}
							} else {
								Toast.makeText(mActivity, R.string.hint_analyze_fail, Toast.LENGTH_LONG).show();
							}
						} else {
							SdkLog.log(TAG + " historyDownload fail cd:" + cd);
						}
					}
				});
			}
		});
	}
	

	private void initDemoData() {
		// TODO Auto-generated method stub
		shortData = createShortReportData(1483056407, 166);
		longData = createLongReportData(1701133867, 348);
//		longData = createLongReportData1(1642868610, 430);
//		longData = createLongReportData1(1642782105, 409, 
//				SleepData.Data_1642782105_409.data1, SleepData.Data_1642782105_409.data2, 
//				SleepData.Data_1642782105_409.data3, SleepData.Data_1642782105_409.data4);
//		longData = createLongReportData1(1643039573, 511, SleepData.Data_1643039573_511.data2, SleepData.Data_1643039573_511.data3);
//		longData = createLongReportData1(1629651612, 493, SleepData.Data_1629651612_493.data1, SleepData.Data_1629651612_493.data3);
//		longData = createLongReportData1(1657043276, 348, SleepData.Data_1657043276_348.data1, SleepData.Data_1657043276_348.data3);
	}

	private void initShortReportView(HistoryData historyData) {
		reportLayout.removeAllViews();
		String ver = null;
		Analysis analysis = historyData.getAnalysis();
		if (analysis != null) {
			ver = analysis.getAlgorithmVer();
		}
		if (ver == null)
			ver = "";
		SdkLog.log(TAG + " initShortReportView ver:" + ver);

		View view = inflater.inflate(R.layout.layout_short_report, null);
		TextView tvCollectDate = (TextView) view.findViewById(R.id.tv_collect_date);
		TextView tvSleepTime = (TextView) view.findViewById(R.id.tv_sleep_time);
		TextView tvSleepDuration = (TextView) view.findViewById(R.id.tv_sleep_duration);
		TextView tvAvgHeartRate = (TextView) view.findViewById(R.id.tv_avg_heartrate);
		TextView tvAvgBreathRate = (TextView) view.findViewById(R.id.tv_avg_breathrate);
		TextView tvTemp = (TextView) view.findViewById(R.id.tv_temp);
		TextView tvHumidity = (TextView) view.findViewById(R.id.tv_humidity);
		TextView tvAlgorithmVersion = (TextView) view.findViewById(R.id.tv_algorithm_version);
		
		if (analysis != null) {
			int starttime = historyData.getSummary().getStartTime();
			int endtime = starttime + analysis.getFinalLen() * 60;
			tvCollectDate.setText(dateFormat.format(new Date(starttime * 1000l)));
			tvSleepTime.setText(timeFormat.format(new Date(starttime * 1000l)) + "(" + getString(R.string.starting_point) + ")-" + timeFormat.format(new Date(endtime * 1000l)) + "(" + getString(R.string.end_point) + ")");
			int duration = historyData.getSummary().getRecordCount();
			int hour = duration / 60;
			int minute = duration % 60;
			tvSleepDuration.setText(hour + getString(R.string.unit_h) + minute + getString(R.string.unit_m));
			tvAvgHeartRate.setText(analysis.getAvgHeartRate() + getString(R.string.unit_heart));
			tvAvgBreathRate.setText(analysis.getAvgBreathRate() + getString(R.string.unit_respiration));
		}
		
		int minTemp = 50, maxTemp = 0, minHum = 100, maxHum = 0;
		if(historyData.getDetail().getTemp() != null&&historyData.getDetail().getHumidity()!=null)
		for(int i=0; i<historyData.getDetail().getTemp().length; i++) {
			int temp = historyData.getDetail().getTemp()[i];
			int hum = historyData.getDetail().getHumidity()[i];
			if(temp > maxTemp) {
				maxTemp = temp;
			}
			if(temp < minTemp) {
				minTemp = temp;
			}
			if(hum > maxHum) {
				maxHum = hum;
			}
			if(hum < minHum) {
				minHum = hum;
			}
		}
		
		if(maxTemp < minTemp) maxTemp = minTemp;
		if(maxHum < minHum) maxHum = minHum;
		tvTemp.setText(minTemp + "~" + maxTemp + "℃");
		tvHumidity.setText(minHum + "~" + maxHum + "%");
		tvAlgorithmVersion.setText(ver);
		reportLayout.addView(view);
	}

	private void initLongReportView(HistoryData historyData) {
		reportLayout.removeAllViews();
		String ver = null;
		Analysis analysis = historyData.getAnalysis();
		if (analysis != null) {
			ver = analysis.getAlgorithmVer();
		}
		if (ver == null)
			ver = "";
		// printLog(getString(R.string.generate_sleep_report, ver));

		View view = inflater.inflate(R.layout.layout_long_report, null);
		LinearLayout mainGraph = (LinearLayout) view.findViewById(R.id.layout_chart);
		GraphView.GraphViewData[] mainData = getSleepGraphData(historyData.getDetail(), historyData.getAnalysis(), 60, DeviceType.DEVICE_TYPE_PILLOW);

		int think = (int) (DensityUtil.dip2px(mActivity, 1) * 0.8);
		final LineGraphView main_graph = new LineGraphView(mActivity, "");
		if (mainData == null) {
			mainData = new GraphView.GraphViewData[0];
		}

		GraphViewSeries series = new GraphViewSeries("", new GraphViewSeries.GraphViewSeriesStyle(getResources().getColor(R.color.COLOR_2), think), mainData);
		main_graph.addSeries(series);
		main_graph.isMySelft = true;
		if (mainData.length > 0) {
			main_graph.setViewPort(mainData[0].getX(), mainData[mainData.length - 1].getX());
		} else {
			main_graph.setViewPort(0, 10);
		}

		main_graph.setMinMaxY(-3, 2);
		main_graph.setVerticalLabels(new String[] { "", getString(R.string.wake_), getString(R.string.light_), getString(R.string.mid_), getString(R.string.deep_), "" });

		main_graph.setBeginAndOffset(historyData.getSummary().getStartTime(), TimeUtil.getTimeZoneHour(), 0);
		main_graph.setScalable(false);
		main_graph.setScrollable(false);
		main_graph.setShowLegend(false);
		main_graph.setMainPoint(points);
		main_graph.setDrawBackground(true);
		main_graph.testVLabel = "wake";
		main_graph.setPauseData(apneaPauseList, heartPauseList);

		// 说明没有 数据
		if (mainData.length == 0) {
			main_graph.setHorizontalLabels(new String[] { "1", "2", "3", "4", "5", "6", "7" });
		}

		GraphViewStyle gvs = main_graph.getGraphViewStyle();
		gvs.setVerticalLabelsAlign(Paint.Align.CENTER);
		gvs.setTextSize(DensityUtil.sp2px(mActivity, 12));
		gvs.setGridColor(Color.parseColor("#668492a6"));
		gvs.setHorizontalLabelsColor(getResources().getColor(R.color.COLOR_3));
		gvs.setVerticalLabelsColor(getResources().getColor(R.color.COLOR_3));
		gvs.setLegendBorder(DensityUtil.dip2px(mActivity, 12));
		gvs.setNumVerticalLabels(4);
		gvs.setVerticalLabelsWidth(DensityUtil.dip2px(mActivity, 40));
		gvs.setNumHorizontalLabels(7);
		gvs.setLegendWidth(DensityUtil.dip2px(mActivity, 30));
		main_graph.setBedBeans(bedBeans);
		main_graph.setSleepUpIn(SleepInUP);
		mainGraph.removeAllViews();
		mainGraph.addView(main_graph);
		// main_graph.setOnHeartClickListener(heartClick);
		// main_graph.setOnGraphViewScrollListener(new
		// GraphView.OnGraphViewScrollListener() {
		// @Override
		// public void onTouchEvent(MotionEvent event, GraphView v) {
		// main_graph.onMyTouchEvent(event);
		// }
		// });
		// main_graph.setTouchDisallowByParent(true);

		TextView tvCollectDate = (TextView) view.findViewById(R.id.tv_collect_date);
		TextView tvSleepScore = (TextView) view.findViewById(R.id.tv_sleep_score);
		LinearLayout layoutDeductionPoints = (LinearLayout) view.findViewById(R.id.layout_deduction_points);
		TextView tvSleepTime = (TextView) view.findViewById(R.id.tv_sleep_time);
		TextView tvSleepDuration = (TextView) view.findViewById(R.id.tv_sleep_duration);
		TextView tvAsleepDuration = (TextView) view.findViewById(R.id.tv_fall_asleep_duration);
		TextView tvAvgHeartRate = (TextView) view.findViewById(R.id.tv_avg_heartrate);
		TextView tvAvgBreathRate = (TextView) view.findViewById(R.id.tv_avg_breathrate);
		
		TextView tvAHI = (TextView) view.findViewById(R.id.tv_ahi);
		TextView tvAHIInfo = (TextView) view.findViewById(R.id.tv_ahi_info);
		TextView tvApneaEventAllDuration = (TextView) view.findViewById(R.id.tv_head_apnea_duration);
		TextView tvApneaEventAllTimes = (TextView) view.findViewById(R.id.tv_head_apnea_events);
		TextView tvCsaDuration = (TextView) view.findViewById(R.id.tv_csa_duration);
		TextView tvEventsOfCsa = (TextView) view.findViewById(R.id.tv_events_of_csa);
		TextView tvMaxCsaDuration = (TextView) view.findViewById(R.id.tv_max_csa_duration);
		TextView tvOsaDuration = (TextView) view.findViewById(R.id.tv_osa_duration);
		TextView tvEventsOfOsa = (TextView) view.findViewById(R.id.tv_events_of_osa);
		TextView tvMaxOsaDuration = (TextView) view.findViewById(R.id.tv_max_osa_duration);
		TextView tvAlgorithmVersion = (TextView) view.findViewById(R.id.tv_algorithm_version);
		
		TextView tvDeepSleepPer = (TextView) view.findViewById(R.id.tv_deep_sleep_proportion);
		TextView tvMidSleepPer = (TextView) view.findViewById(R.id.tv_medium_sleep_proportion);
		TextView tvLightSleepPer = (TextView) view.findViewById(R.id.tv_light_sleep_proportion);
		TextView tvWakeSleepPer = (TextView) view.findViewById(R.id.tv_Sober_proportion);
		TextView tvWakeTimes = (TextView) view.findViewById(R.id.tv_wake_times);
		TextView tvTurnTimes = (TextView) view.findViewById(R.id.tv_turn_times);
		TextView tvBodyTimes = (TextView) view.findViewById(R.id.tv_Body_times);
		TextView tvOutTimes = (TextView) view.findViewById(R.id.tv_out_times);
		TextView tvTemp = (TextView) view.findViewById(R.id.tv_temp);
		TextView tvHumidity = (TextView) view.findViewById(R.id.tv_humidity);

		if (analysis != null) {
			tvCollectDate.setText(dateFormat.format(new Date(historyData.getSummary().getStartTime() * 1000l)));
			tvSleepScore.setText(String.valueOf(analysis.getSleepScore()));

			int duration = historyData.getAnalysis().getDuration();
			int fallSleep = analysis.getFallsleepTimeStamp();
			 int wakeUp = analysis.getWakeupTimeStamp();
//			int wakeUp = fallSleep + duration * 60;
			 SdkLog.log(TAG+" fallSleep:" +fallSleep+",date:"+ StringUtil.DATE_FORMAT.format(new Date(fallSleep * 1000l))+",duration:" + duration+",wakeUp:"+wakeUp+",date:" + StringUtil.DATE_FORMAT.format(new Date(wakeUp * 1000l)));
			tvSleepTime.setText(timeFormat.format(new Date(fallSleep * 1000l)) + "(" + getString(R.string.asleep_point) + ")-" + timeFormat.format(new Date(wakeUp * 1000l)) + "(" + getString(R.string.awake_point) + ")");
			int hour = duration / 60;
			int minute = duration % 60;
			tvSleepDuration.setText(hour + getString(R.string.unit_h) + minute + getString(R.string.unit_m));
			tvAvgHeartRate.setText(analysis.getAvgHeartRate() + getString(R.string.unit_heart));
			tvAvgBreathRate.setText(analysis.getAvgBreathRate() + getString(R.string.unit_respiration));

			List<DeductItems> list = new ArrayList<DataFragment.DeductItems>();

			if (analysis.getMd_heart_high_decrease_scale() > 0 && analysis.getMd_heart_low_decrease_scale() > 0) {// 心率不齐
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.heartrate_not_near);
				item.score = analysis.getMd_heart_high_decrease_scale() + analysis.getMd_heart_low_decrease_scale();
				list.add(item);
			} else if (analysis.getMd_heart_high_decrease_scale() > 0) {// 心率过速
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.heartrate_too_fast);
				item.score = analysis.getMd_heart_high_decrease_scale();
				list.add(item);
			} else if (analysis.getMd_heart_low_decrease_scale() > 0) {// 心率过缓
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.heartrate_too_low);
				item.score = analysis.getMd_heart_low_decrease_scale();
				list.add(item);
			}

			if (analysis.getMd_breath_high_decrease_scale() > 0) {// 呼吸过速
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.deduction_breathe_fast);
				item.score = analysis.getMd_breath_high_decrease_scale();
				list.add(item);
			}

			if (analysis.getMd_breath_low_decrease_scale() > 0) {// 呼吸过缓
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.deduction_breathe_slow);
				item.score = analysis.getMd_breath_low_decrease_scale();
				list.add(item);
			}

			if (analysis.getMd_body_move_decrease_scale() != 0) {// 躁动不安
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.restless);
				item.score = analysis.getMd_body_move_decrease_scale();
				list.add(item);
			}

			if (analysis.getMd_leave_bed_decrease_scale() > 0) {// 起夜过多
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.up_night_more);
				item.score = analysis.getMd_leave_bed_decrease_scale();
				list.add(item);
			}

			if (analysis.getMd_sleep_time_increase_scale() > 0) {// 睡眠时间过长
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.actual_sleep_long);
				item.score = analysis.getMd_sleep_time_increase_scale();
				list.add(item);
			}

			if (analysis.getMd_sleep_time_decrease_scale() > 0) {// 睡眠时间过短
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.actual_sleep_short);
				item.score = analysis.getMd_sleep_time_decrease_scale();
				list.add(item);
			}

			if (analysis.getMd_perc_deep_decrease_scale() > 0) {// 深睡眠时间不足
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.deep_sleep_time_too_short);
				item.score = analysis.getMd_perc_deep_decrease_scale();
				list.add(item);
			}

			if (analysis.getMd_fall_asleep_time_decrease_scale() > 0) {// 入睡时间长
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.fall_asleep_hard);
				item.score = analysis.getMd_fall_asleep_time_decrease_scale();
				list.add(item);
			}
			
			if (analysis.getBreathPauseTimes() > 0 && analysis.getMd_breath_stop_decrease_scale() > 0) {// 呼吸暂停
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.abnormal_breathing);
				item.score = analysis.getMd_breath_stop_decrease_scale();
				list.add(item);
			}

			if (analysis.getMd_heart_stop_decrease_scale() > 0) {// 心跳骤停
				DeductItems item = new DeductItems();
				// item.desc = getString(R.string.heart_pause);
				item.score = analysis.getMd_heart_stop_decrease_scale();
				list.add(item);
			}

			if (analysis.getMd_start_time_decrease_scale() > 0) {// 上床时间较晚
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.start_sleep_time_too_latter);
				item.score = analysis.getMd_start_time_decrease_scale();
				list.add(item);
			}

			if (analysis.getMd_wake_cnt_decrease_scale() > 0) {// 清醒次数较多
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.wake_times_too_much);
				item.score = analysis.getMd_wake_cnt_decrease_scale();
				list.add(item);
			}

			if (analysis.getMd_perc_effective_sleep_decrease_scale() > 0) {// 良性睡眠扣分
				DeductItems item = new DeductItems();
				item.desc = getString(R.string.benign_sleep);
				item.score = analysis.getMd_perc_effective_sleep_decrease_scale();
				list.add(item);
			}

			int size = list.size();
			if (size > 0) {
				for (int i = 0; i < size; i++) {
					View pointView = inflater.inflate(R.layout.layout_deduction_points, null);
					TextView tvDesc = (TextView) pointView.findViewById(R.id.tv_deduction_desc);
					TextView tvScore = (TextView) pointView.findViewById(R.id.tv_deduction_score);
					tvDesc.setText((i + 1) + "." + list.get(i).desc);
					tvScore.setText("-" + Math.abs(list.get(i).score));
					layoutDeductionPoints.addView(pointView);
				}
			}

			hour = analysis.getFallAlseepAllTime() / 60;
			minute = analysis.getFallAlseepAllTime() % 60;
			tvAsleepDuration.setText(hour + getString(R.string.unit_h) + minute + getString(R.string.unit_m));
			
			if(analysis.getAhIndex() == 0) {
				tvAHI.setText(R.string.nothing);
			}else {
				tvAHI.setText(analysis.getAhIndex() + getString(R.string.unit_ahi));
			}
			
			short[] ahiArr = analysis.getAhiAry();
			if (ahiArr != null && ahiArr.length > 3) {
				short count = ahiArr[2];
				byte sHour = (byte) ((ahiArr[0] >> 8) & 0xFF);
				byte sMinute = (byte) (ahiArr[0] & 0xFF);
				byte eHour = (byte) ((ahiArr[1] >> 8) & 0xFF);
				byte eMinute = (byte) (ahiArr[1] & 0xFF);
				Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.HOUR_OF_DAY, sHour);
				calendar.set(Calendar.MINUTE, sMinute);
				calendar.set(Calendar.SECOND, 0);

				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < count; i++) {
					String sTime = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
					String eTime = null;
					if (i == 0) {
						if (sMinute == 0) {
							calendar.add(Calendar.MINUTE, 60);
						} else {
							calendar.add(Calendar.MINUTE, 60 - sMinute);
						}
						eTime = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
					} else if (i == (count - 1)) {
						eTime = String.format("%02d:%02d", eHour, eMinute);
					} else {
						calendar.add(Calendar.MINUTE, 60);
						eTime = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
					}

					sb.append(sTime);
					sb.append("~");
					sb.append(eTime);
					sb.append("\t\t\t");
					sb.append(ahiArr[i + 3]);
					sb.append(getString(R.string.unit_times));

					if (i != (count - 1)) {
						sb.append("\n");
					}
				}
				tvAHIInfo.setText(sb.toString());
			}else {
				tvAHIInfo.setText(R.string.nothing);
			}
			
			tvApneaEventAllDuration.setText(analysis.getBreathPauseAllTime()+getString(R.string.unit_s));
			tvApneaEventAllTimes.setText(analysis.getBreathPauseTimes()+getString(R.string.unit_times));
			tvCsaDuration.setText(analysis.getCsaDur()+getString(R.string.unit_s));
			tvEventsOfCsa.setText(analysis.getCsaCnt()+getString(R.string.unit_times));
			tvMaxCsaDuration.setText(analysis.getCsaMaxDur()+getString(R.string.unit_s));
			tvOsaDuration.setText(analysis.getOsaDur()+getString(R.string.unit_s));
			tvEventsOfOsa.setText(analysis.getOsaCnt()+getString(R.string.unit_times));
			tvMaxOsaDuration.setText(analysis.getOsaMaxDur()+getString(R.string.unit_s));
			
//			int idx = 0;
//			StringBuffer sb = new StringBuffer();
//			if (analysis.getBreathPauseTimes() > 0) {
//				idx = 0;
//				int stime = historyData.getSummary().getStartTime();
//				Detail detail = historyData.getDetail();
//				int[] status = detail.getStatus();
//				int len = status.length;
//				for (int i = 0; i < len; i++) {
//					if (analysis.getBreathRateStatusAry()[i] > 0) {
//						idx++;
//						sb.append(getString(R.string.sequence, String.valueOf(idx)) + "\t\t\t");
//						int time = stime + i * 60;
//						sb.append(timeFormat.format(new Date(time * 1000l)) + "\t\t\t");
//						sb.append(analysis.getBreathRateStatusAry()[i] + getString(R.string.unit_s) + "\n");
//					}
//				}
//
//				if (sb.length() > 0) {
//					sb.delete(sb.lastIndexOf("\n"), sb.length());
//				}
//				tvBreathPause.setText(sb.toString());
//			} else {
//				tvBreathPause.setText(R.string.nothing);
//			}

			tvDeepSleepPer.setText(analysis.getDeepSleepPerc() + "%");
			tvMidSleepPer.setText(analysis.getInSleepPerc() + "%");
			tvLightSleepPer.setText(analysis.getLightSleepPerc() + "%");
			tvWakeSleepPer.setText(analysis.getWakeSleepPerc() + "%");
			tvWakeTimes.setText(analysis.getWakeTimes() + getString(R.string.unit_times));
			tvTurnTimes.setText(analysis.getTrunOverTimes() + getString(R.string.unit_times));
			tvBodyTimes.setText(analysis.getBodyMovementTimes() + getString(R.string.unit_times));
			tvOutTimes.setText(analysis.getLeaveBedTimes() + getString(R.string.unit_times));
		}
		
		int minTemp = 50, maxTemp = 0, minHum = 100, maxHum = 0;
		if(historyData.getDetail().getTemp() != null&&historyData.getDetail().getHumidity()!=null)
		for(int i=0; i<historyData.getDetail().getTemp().length; i++) {
			int temp = historyData.getDetail().getTemp()[i];
			int hum = historyData.getDetail().getHumidity()[i];
			if(temp > maxTemp) {
				maxTemp = temp;
			}
			if(temp < minTemp) {
				minTemp = temp;
			}
			if(hum > maxHum) {
				maxHum = hum;
			}
			if(hum < minHum) {
				minHum = hum;
			}
		}
		
		if(maxTemp < minTemp) maxTemp = minTemp;
		if(maxHum < minHum) maxHum = minHum;
		tvTemp.setText(minTemp + "~" + maxTemp + "℃");
		tvHumidity.setText(minHum + "~" + maxHum + "%");

		tvAlgorithmVersion.setText(ver);
		reportLayout.addView(view);

	}

	class DeductItems {
		String desc;
		int score;
	}

	private HistoryData createShortReportData(int starttime, int count) {
		HistoryData historyData = new HistoryData();
		Summary summ = new Summary();
		summ.setStartTime(starttime);
		summ.setRecordCount(count);
		historyData.setSummary(summ);

		Detail detail = new Detail();
		int[] heartRate = new int[] { 60, 62, 64, 62, 64, 63, 66, 68, 68, 68, 68, 59, 64, 64, 65, 63, 67, 67, 67, 63, 69, 70, 71, 72, 68, 70, 72, 71, 71, 69, 71, 66, 65, 67, 68, 65, 63, 62, 70, 66, 65, 57, 65, 66, 64, 68, 67, 66, 65, 67, 68, 66, 68, 68, 68, 66, 68, 66, 67, 67, 68, 67, 67, 67, 66,
				68, 67, 67, 67, 66, 67, 69, 69, 63, 73, 69, 74, 71, 72, 74, 74, 75, 74, 73, 73, 72, 76, 72, 70, 70, 72, 73, 73, 68, 70, 71, 66, 70, 74, 73, 76, 67, 72, 71, 65, 65, 65, 71, 69, 64, 68, 65, 64, 67, 66, 61, 60, 65, 66, 68, 67, 60, 63, 64, 63, 66, 76, 76, 75, 79, 78, 67, 66, 67, 66, 70,
				66, 64, 66, 72, 61, 64, 70, 64, 62, 66, 68, 73, 70, 66, 63, 61, 62, 72, 64, 74, 75, 72, 65, 71, 65, 58, 70, 74, 69, 74 };

		int[] breathRate = new int[] { 12, 14, 14, 14, 14, 14, 14, 15, 14, 15, 15, 15, 14, 14, 14, 13, 13, 13, 14, 16, 13, 15, 15, 15, 14, 12, 14, 12, 12, 14, 14, 13, 11, 14, 13, 13, 14, 12, 11, 12, 13, 12, 12, 16, 16, 15, 14, 15, 14, 14, 15, 15, 15, 14, 14, 14, 14, 14, 15, 14, 15, 15, 15, 14, 15,
				16, 15, 14, 15, 14, 15, 15, 15, 15, 13, 16, 13, 13, 12, 13, 13, 13, 12, 13, 12, 13, 14, 13, 13, 13, 14, 13, 12, 13, 12, 13, 13, 14, 15, 13, 15, 15, 16, 14, 19, 11, 12, 13, 12, 12, 13, 16, 17, 15, 14, 14, 16, 15, 15, 14, 13, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 15, 14, 13, 15,
				14, 13, 13, 13, 13, 13, 13, 14, 14, 15, 15, 13, 14, 13, 16, 16, 16, 14, 15, 15, 12, 14, 16, 15, 13, 18, 20, 20, 18, 16 };

		int[] status = new int[] { 72, 72, 72, 12, 8, 12, 78, 72, 72, 72, 78, 78, 72, 72, 72, 78, 72, 8, 8, 14, 8, 8, 8, 14, 8, 14, 8, 8, 8, 14, 14, 12, 78, 76, 72, 72, 76, 76, 78, 72, 8, 12, 78, 72, 78, 72, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40,
				46, 110, 104, 72, 78, 78, 72, 76, 72, 72, 72, 72, 8, 8, 8, 8, 8, 76, 72, 72, 72, 104, 104, 104, 104, 104, 104, 72, 72, 72, 78, 78, 78, 72, 72, 72, 72, 78, 76, 78, 78, 12, 12, 12, 8, 14, 14, 12, 12, 12, 12, 12, 12, 14, 14, 12, 14, 14, 14, 12, 12, 14, 12, 12, 12, 12, 8, 12, 8, 12, 12,
				12, 8, 14, 12, 8, 14, 8, 12, 12, 14, 14, 12, 14, 72, 76, 78, 76, 76, 12, 14, 12, 8, 12 };
		int[] statusValue = new int[] { 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 3, 1, 1, 0, 0, 1, 1, 2, 0, 0, 3, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 0, 0, 0, 0, 1, 4, 1, 1, 5, 3, 4, 0, 1, 3, 3, 3, 5, 5, 5, 3, 1, 1, 8, 1, 1, 2, 2, 1, 1, 4, 2, 1, 1, 0, 1, 0, 3, 3, 1, 0, 1, 6, 0, 1, 0, 7, 8, 3, 1, 3, 1, 0, 1, 1, 5, 1, 1, 2, 2, 0, 1 };

		int[] temp = new int[] { 
				26, 26, 26, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 
				27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 26, 26, 
				26, 26, 26, 26, 26, 26, 26, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 
				25, 26, 26, 26, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 
				25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 
				27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27};

		int[] humidity = new int[] { 
				44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 
				44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 
				44, 44, 44, 44, 44, 44, 44, 47, 47, 47, 47, 47, 47, 47, 47, 47, 46, 46, 46, 45, 45, 45, 48, 48, 48, 47, 47, 47, 46, 46, 
				46, 44, 44, 44, 46, 46, 46, 48, 48, 48, 46, 46, 46, 45, 45, 45, 48, 48, 48, 48, 48, 48, 46, 46, 46, 47, 47, 47, 48, 48, 
				48, 48, 48, 48, 47, 47, 47, 47, 47, 47, 45, 45, 45, 45, 45, 45, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 
				44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44
		};
		
		detail.setHeartRate(heartRate);
		detail.setBreathRate(breathRate);
		detail.setStatus(status);
		detail.setStatusValue(statusValue);
		detail.setTemp(temp);
		detail.setHumidity(humidity);
		historyData.setDetail(detail);

//		Analysis analysis = AnalysisUtil.analysData(summ, detail, 1);
		Analysis analysis = new Analysis();
		analysis.setAvgHeartRate(64);
		analysis.setAvgBreathRate(14);
		analysis.setAlgorithmVer("2.0.17");
		historyData.setAnalysis(analysis);
		return historyData;
	}

	private HistoryData createLongReportData(int starttime, int count) {
		HistoryData historyData = new HistoryData();
		Summary summ = new Summary();
		summ.setStartTime(starttime);
		summ.setRecordCount(count);
		historyData.setSummary(summ);

		Detail detail = new Detail();
		int[] heartRate = new int[] { 0, 66, 60, 61, 60, 62, 61, 66, 62, 68, 72, 72, 72, 72, 0, 0, 0, 66, 57, 60, 61, 61, 57, 61, 63, 60, 61, 60, 61, 62, 58, 59, 56, 61, 57, 62, 56, 56, 58, 55, 56, 59, 58, 56, 57, 58, 54, 57, 57, 63, 57, 60, 55, 59, 55, 58, 56, 52, 54, 60, 57, 55, 54, 59, 59, 59,
				57, 54, 60, 54, 56, 57, 61, 58, 58, 56, 60, 57, 60, 52, 56, 55, 60, 54, 56, 55, 58, 61, 57, 54, 58, 51, 57, 53, 55, 55, 53, 52, 57, 54, 54, 55, 60, 57, 55, 61, 57, 54, 54, 60, 61, 56, 60, 57, 56, 54, 57, 56, 56, 59, 66, 61, 59, 62, 63, 56, 55, 53, 54, 57, 58, 62, 54, 60, 64, 61, 61,
				60, 54, 53, 57, 57, 61, 62, 68, 56, 60, 58, 60, 57, 55, 57, 53, 60, 61, 60, 59, 57, 55, 57, 54, 54, 57, 57, 59, 56, 54, 57, 58, 56, 54, 55, 56, 53, 54, 55, 54, 54, 61, 53, 53, 56, 55, 59, 54, 57, 54, 55, 54, 57, 54, 52, 57, 54, 57, 52, 56, 58, 68, 61, 60, 62, 61, 57, 58, 59, 63, 57,
				63, 55, 57, 59, 53, 56, 53, 61, 59, 61, 58, 63, 55, 58, 64, 68, 66, 55, 55, 60, 60, 59, 56, 56, 57, 57, 55, 60, 59, 67, 56, 56, 58, 57, 53, 63, 52, 55, 62, 57, 60, 59, 64, 61, 62, 62, 56, 58, 58, 57, 62, 63, 57, 60, 61, 60, 62, 58, 62, 61, 59, 57, 60, 56, 60, 64, 66, 63, 61, 63, 58,
				57, 58, 58, 56, 56, 57, 54, 63, 59, 67, 58, 54, 54, 60, 63, 60, 60, 56, 68, 0, 0, 0, 66, 59, 60, 58, 55, 55, 56, 61, 55, 54, 55, 55, 56, 56, 54, 59, 60, 57, 56, 58, 55, 54, 57, 60, 54, 57, 53, 56, 56, 55, 58, 56, 56, 58, 55, 58, 56, 56, 57, 60, 65, 55, 56, 54, 59, 57, 56 };

		int[] breathRate = new int[] { 0, 13, 12, 12, 13, 13, 14, 15, 15, 0, 0, 0, 0, 0, 0, 0, 0, 15, 12, 14, 12, 14, 18, 17, 20, 21, 22, 19, 21, 20, 19, 20, 20, 18, 22, 18, 21, 18, 20, 18, 20, 19, 18, 19, 21, 20, 19, 20, 19, 18, 12, 18, 17, 16, 15, 17, 18, 19, 19, 18, 19, 16, 20, 15, 18, 16, 18,
				17, 16, 13, 15, 16, 17, 16, 18, 16, 18, 16, 17, 15, 17, 16, 15, 16, 19, 15, 16, 15, 17, 18, 20, 20, 19, 16, 17, 18, 18, 18, 16, 17, 18, 18, 19, 18, 17, 15, 19, 17, 19, 19, 17, 19, 17, 16, 17, 18, 17, 19, 18, 20, 22, 19, 22, 22, 22, 22, 21, 21, 18, 21, 20, 22, 20, 21, 22, 21, 19, 22,
				17, 22, 21, 19, 15, 17, 16, 16, 20, 18, 16, 18, 16, 16, 18, 17, 18, 15, 18, 18, 16, 20, 19, 18, 18, 16, 15, 17, 18, 18, 18, 18, 15, 19, 19, 19, 18, 17, 18, 17, 16, 17, 17, 18, 17, 17, 17, 18, 18, 18, 19, 18, 16, 17, 17, 17, 17, 17, 16, 20, 18, 17, 0, 15, 15, 19, 17, 17, 20, 13, 12,
				0, 13, 16, 16, 16, 0, 16, 14, 13, 18, 22, 16, 19, 22, 18, 19, 16, 12, 16, 17, 13, 0, 17, 18, 17, 18, 18, 13, 19, 17, 17, 18, 18, 17, 18, 18, 17, 14, 13, 17, 16, 15, 15, 17, 13, 12, 17, 17, 16, 19, 15, 17, 12, 16, 16, 17, 12, 19, 18, 12, 17, 16, 16, 20, 15, 20, 21, 19, 19, 19, 20, 19,
				18, 20, 21, 20, 18, 18, 17, 19, 17, 19, 17, 13, 13, 17, 15, 16, 17, 0, 0, 0, 16, 16, 17, 14, 19, 20, 21, 21, 21, 19, 18, 19, 19, 18, 19, 19, 17, 19, 20, 18, 13, 15, 15, 17, 17, 15, 19, 18, 18, 18, 19, 19, 18, 16, 19, 19, 17, 17, 19, 15, 18, 16, 16, 19, 18, 19, 18 };

		int[] status = new int[] { 0, 1, 1, 1, 1, 1, 1, 1, 1, 17, 17, 17, 17, 17, 0, 0, 0, 35, 34, 34, 35, 34, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 34, 1, 1, 1, 1, 1, 1, 1, 35, 1, 1, 35, 1, 1, 1, 34, 34, 34, 1, 34, 1, 1, 1, 1, 1, 1, 1, 1, 35, 34, 34, 1, 35, 1, 1, 1, 35, 1, 34, 34, 1, 1, 1, 1, 1, 34,
				34, 1, 34, 35, 1, 35, 1, 35, 1, 1, 34, 1, 1, 1, 1, 1, 1, 1, 35, 1, 1, 35, 35, 34, 34, 1, 1, 35, 1, 35, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 35, 1, 1, 35, 35, 1, 1, 34, 34, 1, 1, 1, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 35, 1, 35, 1, 17, 34, 1, 1, 34, 1, 1, 1, 1, 17, 1, 1, 1, 1, 17, 1, 1, 34, 35, 1, 1, 35, 1, 1, 1, 1, 1, 1, 1, 35, 17, 35, 34, 1, 1, 34, 1, 34, 1, 1, 1, 34, 1, 1, 35, 1, 1, 1, 1, 1, 1, 1, 1, 1, 35, 1, 34,
				35, 1, 34, 1, 34, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 35, 1, 35, 34, 35, 34, 34, 35, 0, 0, 0, 35, 34, 35, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 35, 1, 1, 1, 1, 34, 35, 34, 35, 1, 34, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 35, 1, 34, 34, 1, 34, 1,
				1, 1, 1, 1 };

		int[] statusValue = new int[] { 24, 0, 0, 0, 0, 0, 0, 0, 0, 39, 60, 60, 60, 60, 42, 60, 41, 1, 1, 2, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 2, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1,
				0, 1, 0, 2, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 2, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 12, 1, 0, 0, 1, 0, 0, 0, 0, 13, 0, 0, 0, 0, 11, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 12, 1, 1, 0, 0, 2, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 55, 60, 38, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 1, 2, 0, 2, 0, 0, 0, 0, 0 };

		int[] temp = new int[] { 26, 26, 26, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 26, 26, 26, 26, 26, 26, 26, 26, 26,
				25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 26, 26, 26, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 26, 26, 26, 26, 26, 26, 26, 26,
				26, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27,
				27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27,
				27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 25, 25, 25, 26, 26, 26, 26, 26, 26, 26, 26 };

		int[] humidity = new int[] { 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44,
				44, 47, 47, 47, 47, 47, 47, 47, 47, 47, 46, 46, 46, 45, 45, 45, 48, 48, 48, 47, 47, 47, 46, 46, 46, 44, 44, 44, 46, 46, 46, 48, 48, 48, 46, 46, 46, 45, 45, 45, 48, 48, 48, 48, 48, 48, 46, 46, 46, 47, 47, 47, 48, 48, 48, 48, 48, 48, 47, 47, 47, 47, 47, 47, 45, 45, 45, 45, 45, 45, 44,
				44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44,
				44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44,
				44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 45, 45, 45, 44, 44, 44, 45, 45, 45, 44, 44, 44, 46, 46, 46, 45, 45, 45, 44, 44, 44, 45, 45 };

		detail.setHeartRate(heartRate);
		detail.setBreathRate(breathRate);
		detail.setStatus(status);
		detail.setStatusValue(statusValue);
		historyData.setDetail(detail);
		detail.setTemp(temp);
		detail.setHumidity(humidity);

		Analysis analysis = new Analysis();
		analysis.setAlgorithmVer("2.0.17");
		analysis.setAvgBreathRate(17);
		analysis.setAvgHeartRate(58);
		analysis.setFallAlseepAllTime(1);
		analysis.setWakeAndLeaveBedBeforeAllTime(344);
		analysis.setLeaveBedTimes(2);
		analysis.setBreathPauseTimes(192);
		analysis.setDeepSleepPerc(7);
		analysis.setInSleepPerc(37);
		analysis.setLightSleepPerc(51);
		analysis.setWakeSleepPerc(5);
		analysis.setDuration(342);
		analysis.setWakeTimes(3);
		analysis.setLightSleepAllTime(170);
		analysis.setInSleepAllTime(127);
		analysis.setDeepSleepAllTime(27);
		analysis.setWake(3);
		analysis.setBreathPauseAllTime(5506);
		analysis.setMaxHeartBeatRate(72);
		analysis.setMaxBreathRate(22);
		analysis.setMinHeartBeatRate(51);
		analysis.setMinBreathRate(12);
		analysis.setBreathRateSlowAllTime(11);
		analysis.setSleepScore(43);
		analysis.setReportFlag(1); // 长报告

		analysis.setFallsleepTimeStamp(1701133927);
		analysis.setWakeupTimeStamp(1701154507);

		analysis.setAhiAry(new short[] {2316,3639,6,4,34,29,68,34,20});
		analysis.setBodyMovementTimes(77);
		analysis.setOsaMaxDur(56);
		analysis.setOsaCnt(165);
		analysis.setOsaDur(3512);
		analysis.setCsaMaxDur(155);
		analysis.setCsaCnt(27);
		analysis.setCsaDur(1994);
		analysis.setAhiMaxDur(155);
		analysis.setAhIndex(33);

		analysis.setMd_body_move_decrease_scale((short) 2); // 躁动不安扣分项
		analysis.setMd_leave_bed_decrease_scale((short) 2);
		analysis.setMd_wake_cnt_decrease_scale((short) 2); // 清醒次数扣分项
		analysis.setMd_sleep_time_decrease_scale((short) 10);
		analysis.setMd_breath_stop_decrease_scale((short) 30);
		analysis.setMd_breath_low_decrease_scale((short) 1);
		analysis.setMd_perc_effective_sleep_decrease_scale((short) 10); // 良性睡眠扣分项

		// int fallsleepTimeStamp = summ.getStartTime() +
		// analysis.getFallAlseepAllTime() * 60;
		// int wakeupTimeStamp = fallsleepTimeStamp + analysis.getDuration() * 60;
		// analysis.setFallsleepTimeStamp(fallsleepTimeStamp);
		// analysis.setWakeupTimeStamp(wakeupTimeStamp);

		analysis.setSleepCurveArray(new float[] { 0, 0, 0.90709543f, 1.0710061f, 1.1770587f, 1.2434365f, 1.2875129f, 1.3291332f, 1.4370048f, 1.4920353f, 1.5267947f, 1.5501891f, 1.5662099f, 1.4261433f, 0, 0, 0, 0, 0, 0.037264824f, 0.14261603f, 0.29783726f, 0.47608972f, 0.6465516f, 0.7797487f,
				0.8526497f, 0.8526497f, 0.7797487f, 0.6465516f, 0.47608995f, 0.2978375f, 0.14261627f, 0.037264824f, 0, 0, 0, 0.14438295f, 0.2832799f, 0.41167784f, 0.52957654f, 0.63396525f, 0.7166753f, 0.7982397f, 0.87707186f, 0.952605f, 1.0249429f, 1.0948639f, 1.1616948f, 1.2257993f, 1.2878717f,
				1.3482281f, 1.4057442f, 1.4604199f, 1.5103093f, 1.5551188f, 1.5941383f, 1.6270739f, 1.6542197f, 1.675698f, 1.692158f, 1.7036608f, 1.7094963f, 1.7106683f, 1.7064668f, 1.6963043f, 1.6804746f, 1.6598105f, 1.6357323f, 1.6095984f, 1.5819966f, 1.5532202f, 1.5223882f, 1.4897944f,
				1.4560261f, 1.421377f, 1.3864341f, 1.3520786f, 1.3180165f, 1.2854228f, 1.2540035f, 1.2234653f, 1.1943953f, 1.1662061f, 1.1383107f, 1.1107087f, 1.0834005f, 1.0575604f, 1.0323077f, 1.0070549f, 0.9818022f, 0.95537496f, 0.927773f, 0.8992903f, 0.8699267f, 0.8408568f, 0.81266737f,
				0.7859218f, 0.7617698f, 0.7413862f, 0.72503996f, 0.7121558f, 0.7024529f, 0.6967871f, 0.6951833f, 0.6976168f, 0.7049687f, 0.71635795f, 0.7314906f, 0.7503178f, 0.77226496f, 0.7973068f, 0.8248813f, 0.8544252f, 0.88706446f, 0.92336154f, 0.9644418f, 1.0105869f, 1.0609525f, 1.1161014f,
				1.1757523f, 1.2393423f, 1.3074344f, 1.3794657f, 1.4543108f, 1.5311253f, 1.6093466f, 1.6889749f, 1.7694472f, 1.8496381f, 1.9284223f, 2.0049555f, 2.0792377f, 2.1515503f, 2.2218933f, 2.2899854f, 2.3558264f, 2.418291f, 2.477942f, 2.5342164f, 2.5865514f, 2.633717f, 2.675045f, 2.7110987f,
				2.74121f, 2.7633758f, 2.777596f, 2.7825356f, 2.7781944f, 2.7645721f, 2.7416692f, 2.7100482f, 2.6702719f, 2.6223402f, 2.5668159f, 2.505387f, 2.4392846f, 2.3691757f, 2.2950606f, 2.217607f, 2.138818f, 2.0586936f, 1.9799047f, 1.902451f, 1.8263329f, 1.7528857f, 1.6834446f, 1.6180097f,
				1.5579164f, 1.5041708f, 1.4577787f, 1.4207522f, 1.3951033f, 1.3808322f, 1.3779386f, 1.3874286f, 1.4103084f, 1.4452422f, 1.4902184f, 1.5452367f, 1.6069499f, 1.6740224f, 1.7464544f, 1.8219043f, 1.8993661f, 1.977834f, 2.0542898f, 2.1267219f, 2.1951296f, 2.2595136f, 2.3188674f,
				2.3721852f, 2.4194672f, 2.4607131f, 2.495923f, 2.5271091f, 2.553076f, 2.5726287f, 2.5867734f, 2.5949125f, 2.5952532f, 2.5882041f, 2.5737655f, 2.5519369f, 2.5227184f, 2.4873054f, 2.4456978f, 2.397895f, 2.3465075f, 2.2927296f, 2.2365618f, 2.178601f, 2.1200428f, 2.0596921f, 1.9981464f,
				1.9371982f, 1.877445f, 1.8182893f, 1.7597313f, 1.7011732f, 1.64142f, 1.5804718f, 1.5183284f, 1.4537951f, 1.3862741f, 1.3169606f, 1.2452568f, 1.171163f, 1.097069f, 1.0229752f, 0.9494786f, 0.87777495f, 0.8084612f, 0.7415378f, 0.6776016f, 0.61665344f, 0.55869293f, 0.5037198f,
				0.45292974f, 0.40452957f, 0.35732484f, 0.3119123f, 0.2712798f, 0.23363543f, 0.20085192f, 0.17301035f, 0.14891553f, 0.12856793f, 0.11400223f, 0.10521865f, 0.1028955f, 0.108389616f, 0.12170076f, 0.14223146f, 0.17185545f, 0.2098937f, 0.25558782f, 0.30730653f, 0.36564708f, 0.42737985f,
				0.49182606f, 0.56034255f, 0.6329293f, 0.70822954f, 0.7876003f, 0.87036276f, 0.9558387f, 1.0426714f, 1.1315392f, 1.2210854f, 1.3126668f, 1.4076401f, 1.50397f, 1.6003001f, 1.6973084f, 1.7936385f, 1.8865768f, 1.9754446f, 2.0588853f, 2.1347563f, 2.2028422f, 2.2622495f, 2.314012f,
				2.357236f, 2.3924918f, 2.4189942f, 2.4367428f, 2.446416f, 2.4493706f, 2.4047482f, 2.2923949f, 2.1319323f, 1.931616f, 1.7012529f, 1.4514353f, 1.1837181f, 0.90121365f, 0.60781336f, 0.30663085f, 0, 0, 0, 0, 0, 0.008953333f, 0.035660505f, 0.07966447f, 0.1402123f, 0.21626782f,
				0.30653024f, 0.4094541f, 0.52327967f, 0.6460581f, 0.77568984f, 0.9099555f, 1.0465592f, 1.1831626f, 1.3174286f, 1.44706f, 1.5698388f, 1.6836641f, 1.7865883f, 1.8768504f, 1.9529061f, 2.013454f, 2.057458f, 2.084165f, 2.0931187f, 2.089427f, 2.0796432f, 2.0684357f, 2.0553737f, 2.0399628f,
				2.0208204f, 1.9960473f, 1.9631743f, 1.9194175f, 1.8615419f, 1.7176932f, 1.5730374f, 1.4275742f, 1.287342f, 1.1797023f, 1.054427f, 0.8782296f, 0, -0.0052378178f, -0.042135954f, -0.04435301f });
		analysis.setSleepCurveStatusArray(new short[] { 4, 1, 0, 0, 0, 0, 0, 0, 0, 8, 8, 8, 8, 8, 4, 4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0 });

		int len = summ.getRecordCount();
		int[] breathRateStatusAry = new int[len];
		int[] heartRateStatusAry = new int[len];
		int[] leftBedStatusAry = new int[len];
		int[] turnOverStatusAry = new int[len];
		for (int i = 0; i < len; i++) {
			byte state = (byte) (detail.getStatus()[i] & 7);
			if (state == SleepStatusType.SLEEP_B_STOP) { // 呼吸暂停点
				breathRateStatusAry[i] = detail.getStatusValue()[i];
			} else if (state == SleepStatusType.SLEEP_H_STOP) { // 心跳暂停点
				heartRateStatusAry[i] = detail.getStatusValue()[i];
			} else if (state == SleepStatusType.SLEEP_LEAVE && detail.getStatusValue()[i] > 0) { // 有效离床
				leftBedStatusAry[i] = detail.getStatusValue()[i];
			} else if (state == SleepStatusType.SLEEP_TURN_OVER) { // 翻身
				turnOverStatusAry[i] = detail.getStatusValue()[i];
			}
		}
		analysis.setBreathRateStatusAry(breathRateStatusAry);
		analysis.setHeartRateStatusAry(heartRateStatusAry);
		analysis.setLeftBedStatusAry(leftBedStatusAry);
		analysis.setTurnOverStatusAry(turnOverStatusAry);

		historyData.setAnalysis(analysis);
		return historyData;
	}

	private HistoryData createLongReportData1(int starttime, int count, int[] data1, int[] data3) {
		HistoryData historyData = new HistoryData();
		Summary summ = new Summary();
		summ.setStartTime(starttime);
		summ.setRecordCount(count);
		summ.setFirmwareVers("02.00.01");
		historyData.setSummary(summ);
		Detail detail = new Detail();
		int[] data24 = new int[count];
		for(int i=0;i<count;i++) {
			data24[i] = -1;
		}
		detail.setData1(data1);
		detail.setData2(data24);
		detail.setData3(data3);
		detail.setData4(data24);
		historyData.setDetail(detail);
		Analysis analysis = AnalysisUtil.analysData(summ, detail, 1);
		SdkLog.log(TAG + " createLongReportData analysis:" + analysis);
		historyData.setAnalysis(analysis);
		return historyData;
	}

	private List<CvPoint> points = new ArrayList<CvPoint>();
	private List<LineGraphView.BedBean> bedBeans = new ArrayList<LineGraphView.BedBean>();
	private List<LineGraphView.BedBean> SleepInUP = new ArrayList<LineGraphView.BedBean>();
	/**
	 * 描述：呼吸暂停的集合
	 */
	private List<GraphView.GraphViewData> apneaPauseList = new ArrayList<GraphView.GraphViewData>();
	/**
	 * 描述：心跳暂停的集合
	 */
	private List<GraphView.GraphViewData> heartPauseList = new ArrayList<GraphView.GraphViewData>();

	/**
	 * 新的睡眠曲线中离床的起始点，单位是分钟
	 */
	private int leaveBedStart = 0;

	/**
	 * <h3>新版 算出 睡眠周期图的数据结构</h3>
	 * 
	 * @param analysis
	 * @param timeStep
	 * @return
	 */
	private GraphView.GraphViewData[] getNewSleepGraphData(Detail detail, Analysis analysis, int timeStep, DeviceType deviceType) {
		GraphView.GraphViewData[] mainData = new GraphView.GraphViewData[analysis.getSleepCurveArray().length + 1];
		// 是手机监测的新版
		for (int i = 0; i < analysis.getSleepCurveArray().length; i++) {
			// 清醒，潜睡，中睡，深睡 手机给的是 0,1,2,3； ron画图的列表是: 1,0,-1,-2
			mainData[i] = new GraphView.GraphViewData(i * timeStep, 1 - analysis.getSleepCurveArray()[i]);
		}

		mainData[analysis.getSleepCurveArray().length] = new GraphView.GraphViewData(analysis.getSleepCurveArray().length * timeStep, 1);
		SleepInUP.clear();
		heartPauseList.clear();
		apneaPauseList.clear();
		bedBeans.clear();
		int len = analysis.getSleepCurveStatusArray().length;
		for (int i = 0; i < len; i++) {

			if ((analysis.getSleepCurveStatusArray()[i] & SleepConfig.NewSleepInPoint) == SleepConfig.NewSleepInPoint) { // 入睡点
				LineGraphView.BedBean sleepIn = new LineGraphView.BedBean();
				sleepIn.setData(new GraphView.GraphViewData(i * timeStep, 0));
				sleepIn.setX(i * timeStep);
				sleepIn.setStatus(BedBean.SLEEPIN);
				sleepIn.setY(0);
				SleepInUP.add(sleepIn);
			}

			if ((analysis.getSleepCurveStatusArray()[i] & SleepConfig.NewWakeUpPoint) == SleepConfig.NewWakeUpPoint) { // 清醒点
				// 上一个点是非清醒点
				if (i > 0 && ((analysis.getSleepCurveStatusArray()[i - 1] & SleepConfig.NewWakeUpPoint) != SleepConfig.NewWakeUpPoint)) {
					LineGraphView.BedBean waleUp = new LineGraphView.BedBean();
					waleUp.setData(new GraphView.GraphViewData(i * timeStep, 0));
					waleUp.setX(i * timeStep);
					waleUp.setStatus(BedBean.SLEEPUP);
					waleUp.setY(0);
					SleepInUP.add(waleUp);
				}
			}

			// 纽扣没有呼吸暂停和心率暂停
			if (deviceType != DeviceType.DEVICE_TYPE_SLEEPDOT) {
//				if (analysis.getHeartRateStatusAry()[i] > 0) { // 心率暂停点
//					GraphView.GraphViewData heartPause = new GraphView.GraphViewData(i * timeStep, mainData[i].valueY);
//					heartPause.setApneaRate(detail.getBreathRate()[i]);
//					heartPause.setHeartRate(detail.getHeartRate()[i]);
//					heartPause.setStatus(3);
//					heartPause.setStatusValue(analysis.getHeartRateStatusAry()[i]);
//					heartPauseList.add(heartPause);
//				}
//				if (analysis.getBreathRateStatusAry()[i] > 0) { // 呼吸暂停点
//					GraphView.GraphViewData breathPause = new GraphView.GraphViewData(i * timeStep, mainData[i].valueY);
//					breathPause.setApneaRate(detail.getBreathRate()[i]);
//					breathPause.setHeartRate(detail.getHeartRate()[i]);
//					breathPause.setStatus(2);
//					breathPause.setStatusValue(analysis.getBreathRateStatusAry()[i]);
//					apneaPauseList.add(breathPause);
//				}
			}

			if (analysis.getLeftBedStatusAry()[i] > 0) { // 离床点
				if (i > 0) {
					if (analysis.getLeftBedStatusAry()[i - 1] == 0) {
						LineGraphView.BedBean wakeUp = new LineGraphView.BedBean();
						wakeUp.setX(i * timeStep);
						wakeUp.setY((float) mainData[i].getY());
						wakeUp.setData(new GraphView.GraphViewData(i * timeStep, (float) mainData[i].getY()));
						wakeUp.setWake(true);
						bedBeans.add(wakeUp);
						leaveBedStart = i;
					}
				}

				if (i + 1 < len) {
					if (analysis.getLeftBedStatusAry()[i + 1] == 0) {
						LineGraphView.BedBean wakeIn = new LineGraphView.BedBean();
						wakeIn.setX(i * timeStep);
						wakeIn.setY((float) mainData[i].getY());
						wakeIn.setData(new GraphView.GraphViewData(i * timeStep, (float) mainData[i].getY()));
						wakeIn.setWake(false);
						wakeIn.setStatusValue((i - leaveBedStart) * 60);
						bedBeans.add(wakeIn);
					}
				}

				// 如果本身就是最后一个离床点，判断前一个是否是离床点，如果有连续两个离床点则认为是离床
				// 0,0,0,0,0,0,4,4,0,0,0,0,4,4,4,4,4,0,0,0,0,0,0,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4不加这个的话对这种情况会少一个离床点
				if (i + 1 == len) {
					if (analysis.getLeftBedStatusAry()[i - 1] > 0) {
						LineGraphView.BedBean wakeIn = new LineGraphView.BedBean();
						wakeIn.setX(i * timeStep);
						wakeIn.setY((float) mainData[i].getY());
						wakeIn.setData(new GraphView.GraphViewData(i * timeStep, (float) mainData[i].getY()));
						wakeIn.setWake(false);
						wakeIn.setStatusValue((i - leaveBedStart) * 60);
						bedBeans.add(wakeIn);
					}
				}
			}
		}

		return mainData;
	}

	/**
	 * <p>
	 * 分析detail
	 * </p>
	 * 
	 * @param analysis
	 * @param timeStep
	 */
	public GraphView.GraphViewData[] getSleepGraphData(Detail detail, Analysis analysis, int timeStep, DeviceType deviceType) {
		if (analysis == null || analysis.getSleepCurveArray() == null || analysis.getSleepCurveArray().length == 0 || analysis.getSleepCurveStatusArray() == null || analysis.getSleepCurveStatusArray().length == 0) {
			return null;
		}

		return getNewSleepGraphData(detail, analysis, timeStep, deviceType);
	}

	/**
	 * <p>
	 * 由于datas是按照x轴为时间轴的， 保证第一个数 是小于 提供值x的值，就是最近的值
	 * </p>
	 */
	public static GraphView.GraphViewData findNear(GraphView.GraphViewData[] datas, int x) {
		if (datas == null) {
			return null;
		}
		if (datas.length == 0)
			return null;

		if (datas[0].getX() > x)
			return null;

		for (int i = 0; i < datas.length; i++) {
			if (datas[i].getX() >= x)
				return datas[i];
		}
		return null;
	}

	/**
	 * <p>
	 * 由于datas是按照x轴为时间轴的， 保证第一个数 是小于 提供值x的值，就是最近的值
	 * </p>
	 */
	public static GraphView.GraphViewData findNear(GraphView.GraphViewData[] datas, int x, List<GraphView.GraphViewData> dt) {
		if (datas == null) {
			return null;
		}
		if (datas.length == 0)
			return null;

		if (datas[0].getX() > x)
			return null;

		for (int i = 0; i < datas.length; i++) {
			if (datas[i].getX() >= x) {
				if (dt != null)
					for (GraphView.GraphViewData gv : dt) {
						if (gv.getX() == datas[i].getX()) {
							if (i + 1 < datas.length) {
								return datas[i + 1];
							}
						}
					}
				return datas[i];
			}
		}
		return null;
	}

}
