package com.jassani.sms_gateway;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class BackgroundService extends Service {

    private final String CHANNEL_ID = "personal_notifications";
    private final int NOTIFICATION_ID = 001;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            onTaskRemoved(intent);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(App.getContext(),CHANNEL_ID);
            mBuilder.setSmallIcon(R.mipmap.logojassani);
            mBuilder.setContentTitle("HoucineSMS");
            mBuilder.setContentText("Service is running");
            mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(App.getContext());
            notificationManagerCompat.notify(NOTIFICATION_ID,mBuilder.build());

            // Toast.makeText(this, "Service", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            MainActivity.log(TypeLog.ERROR, "Exception on onStartCommand :" + e.getMessage());
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        MainActivity.log(TypeLog.ERROR, "onBind Not yet implemented !");
        throw new UnsupportedOperationException("onBind Not yet implemented !");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        try {
            Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
            restartServiceIntent.setPackage(getPackageName());
            startService(restartServiceIntent);
            super.onTaskRemoved(rootIntent);
        } catch (Exception e) {
            MainActivity.log(TypeLog.ERROR, "onTaskRemoved Exception:" + e.getMessage());
        }
    }
}
