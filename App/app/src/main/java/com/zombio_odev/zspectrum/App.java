package com.zombio_odev.zspectrum;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class App extends Application {
    public static final String NOTIFICATION_CHANNEL1_ID = "BLE_NOTIFICATION_CHANNEL";
    public static final String BLESSING_LOG_STRING = "MEOW";
    // Add more channels to make more notifications (https://www.youtube.com/watch?v=FbpD5RZtbCc)

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // If the API level of app if Oreo (26) or higher
            NotificationChannel ch1 = new NotificationChannel(
                    NOTIFICATION_CHANNEL1_ID,
                    "Bluetooth Service", // @change КОНСТАНТА**********************
                    NotificationManager.IMPORTANCE_HIGH
            );

            ch1.setDescription("This notification is used to notify user, that BLE is on and data flow is going"); // @change CHANGE FOR CONST VALUE****************
            // Add more channels if needed

            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.createNotificationChannel(ch1);
        }
    }
}