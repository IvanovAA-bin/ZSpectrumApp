package com.zombio_odev.zspectrum.modules.BLEservice.Modules;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.zombio_odev.zspectrum.App;
import com.zombio_odev.zspectrum.MainActivity;
import com.zombio_odev.zspectrum.R;

/**
 * ServiceNotificationManager is used in BleService to control,
 * show and update notifications, that can be used to inform user
 */
public class ServiceNotificationManager {
    //**********<CONSTANT>************
    public static final int CONSTANT_NOTIFICATION_ID = 13;
    //**********</CONSTANT>***********
    private final Context context;
    //private Notification constantNotification;
    private NotificationCompat.Builder constantNotificationBuilder;
    private final NotificationManagerCompat notificationManager;


    public ServiceNotificationManager(Context context) {
        this.context = context;
        notificationManager = NotificationManagerCompat.from(context);
        setConstantNotificationBuilder();
    }

    /**
     * Constant notification is notification, that floats in users notifications
     * and it's can't be removed due to Android specification (foreground service
     * must show, that it is running)
     * @return constant notification
     */
    public Notification getConstantNotification() {
        return constantNotificationBuilder.build();
    }

    /**
     * This function used to update text, that displays in constant notification
     * @param id must be ServiceNotificationManager.CONSTANT_NOTIFICATION_ID
     * @param text text, that will appeal in constant notification
     */
    public void updateConstantNotificationText(int id, String text) {
        constantNotificationBuilder.setContentText(text);
        notificationManager.notify(id, constantNotificationBuilder.build());
    }

    /**
     * This functions defines constant notification setting and values
     * Constant notification is notification, that floats in users notifications
     * and it's can't be removed due to Android specification (foreground service
     * must show, that it is running)
     */
    private void setConstantNotificationBuilder () {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0); // @change РАЗБЕРИСЬ С РЕКВЕСТ КОДОМ
        constantNotificationBuilder = new NotificationCompat.Builder(context, App.NOTIFICATION_CHANNEL1_ID)
                .setContentTitle("ZSpectrum") // @change ВСЕ МЕНЯЙ
                .setContentText("none")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent);
    }
}
