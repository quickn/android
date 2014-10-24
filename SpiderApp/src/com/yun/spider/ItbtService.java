package com.yun.spider;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ItbtService extends Service {
	CommandReceiver cmdReceiver;
	boolean flag;
	private AppContext appContext;
	private static final String TAG = "ItbtService";

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "onCreate");
		cmdReceiver = new CommandReceiver();
		flag = true;
		SharedPrefsUtil.putValue(getApplicationContext(), MainActivity.isOpen,
				true);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		flag = false;
		this.unregisterReceiver(cmdReceiver);// 取消BroadcastReceiver
		Log.i(TAG, "onDestroy");
		SharedPrefsUtil.putValue(getApplicationContext(), MainActivity.isOpen,
				false);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("onStartCommand", "onStartCommand=");
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("AAAAA");
		registerReceiver(cmdReceiver, intentFilter);
		cmdReceiver.doJob();// 启动线程
		return super.onStartCommand(intent, flags, startId);
	}

	@SuppressLint("NewApi")
	private void addNotification(Context context, String msg, String sender) {
		NotificationManager manager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent intent = new Intent(context, MainActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("msg", msg);
		bundle.putString("sender", sender);
		intent.putExtras(bundle);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
				intent, PendingIntent.FLAG_ONE_SHOT);
		Notification noti = new Notification.Builder(context)
				.setContentTitle(sender).setContentText(msg).setTicker("MSG")
				.setContentIntent(pendingIntent)
				.setSmallIcon(R.drawable.notice).build();
		noti.defaults = Notification.DEFAULT_SOUND;
		// 让声音、振动无限循环，直到用户响应
		noti.flags = Notification.FLAG_INSISTENT;
		manager.notify(1, noti);
	}

	private class CommandReceiver extends BroadcastReceiver {

		public void stopServer() {
			flag = false;// 停止线程
			stopSelf();// 停止服务
			Log.i(TAG, "收到消息停止服务,关闭服务");
			SharedPrefsUtil.putValue(getApplicationContext(),
					MainActivity.isOpen, false);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			int cmd = intent.getIntExtra("cmd", -1);
			if (cmd == MainActivity.CMD_STOP_SERVICE) {// 如果等于0
				stopServer();
			}
		}

		public void doJob() {
			new Thread() {
				@Override
				public void run() {
					Log.i(TAG, "启动线程");
					while (flag) {
						Time time = new Time();
						int hour = time.hour;
						if (hour >= 8 & hour <= 23) {// 在这个区间运行
							try {
								appContext = (AppContext) getApplication();// 全局Context
								if (appContext.isNetworkConnected()) {
									boolean isRun = false;
									int type = appContext.getNetworkType();
									Log.i(TAG, "type" + type);
									boolean isWifi = SharedPrefsUtil.getValue(
											getApplicationContext(),
											MainActivity.frequency, true);
									if (!isWifi) {
										isRun = true;
									} else if (isWifi && type == 1) {// 只WiFi下才可以允许
										isRun = true;
									}
									if (isRun) {// 在wifi条件下才运行
										boolean check = HttpUtils.checkMiao();
										if (check) {
											addNotification(
													getApplicationContext(),
													"秒标提示", "亲，有秒标了");
											Thread.sleep(1000 * 60 * 60);// 隔一个小时后运行
											Log.i(TAG, "有秒标了停一个小时");
										}
									}
									int frequency = SharedPrefsUtil.getValue(
											getApplicationContext(),
											MainActivity.frequency, 2);
									Thread.sleep(1000 * 60 * frequency);// 每两分钟运行一次
									Log.i(TAG, "正常情况下每2分钟扫描一次");
								} else {
									Thread.sleep(1000 * 60 * 5);
									Log.i(TAG, "没有网络停5分钟");
								}
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						} else {
							try {
								Thread.sleep(1000 * 60 * 30);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							Log.i(TAG, "服务在这个区间不运行，暂停半个小时");
						}
					}
				}
			}.start();
		}
	}

}
