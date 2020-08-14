package my.boxman.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

  	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		init( intent,startId);
		return  START_NOT_STICKY;
	}

	private void init(Intent intent,int startId)
	{
		Log.e("service","startService");
		Notification notification = new Notification();
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
		startForeground(101, notification);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		stopForeground(true);
	}
}
