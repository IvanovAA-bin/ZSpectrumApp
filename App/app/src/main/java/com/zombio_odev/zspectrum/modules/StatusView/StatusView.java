package com.zombio_odev.zspectrum.modules.StatusView;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class StatusView extends ScrollView {
    private LinearLayout LL;

    public StatusView(Context context) {
        super(context);
        this.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        LL = new LinearLayout(getContext());
        LL.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        LL.setOrientation(LinearLayout.VERTICAL);
        this.addView(LL);
    }

    private Button getStatusLink(String str)
    {
        Button res = new Button(getContext());
        res.setText(str);
        res.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        res.setClickable(false);
        return res;
    }

    private static int normalizeInt_1(int value) {
        return value & 0x000000FF;
    }

    private static int normalizeInt_2(int value) {
        return value & 0x0000FF00;
    }

    private static int normalizeInt_3(int value) {
        return value & 0x00FF0000;
    }

    private static int normalizeInt_4(int value) {
        return value & 0xFF000000;
    }

    /**
     * @param data cmd 0x0011 data should be send there
     * @return true if error occurred. false if everything ok
     * send Toast in case of error
     */
    public boolean updateStatusData(byte[] data) {
        LL.removeAllViews();
        if (data.length < 16) {
            Toast.makeText(getContext(), "Status cmd len is too short", Toast.LENGTH_SHORT).show();
            return true;
        }
        //int cmdResponce = data[0];
        int cmd = normalizeInt_2(data[1] << 8) + normalizeInt_1(data[2]);
        if (cmd != 0x0011) {
            Toast.makeText(getContext(), "Data cmd is not status cmd (0x0011) ", Toast.LENGTH_SHORT).show();
            return true;
        }
        int flags = normalizeInt_4(data[3] << 24) + normalizeInt_3(data[4] << 16) + normalizeInt_2(data[5] << 8) + normalizeInt_1(data[6]);
        int remainingTime = normalizeInt_4(data[7] << 24) + normalizeInt_3(data[8] << 16) + normalizeInt_2(data[9] << 8) + normalizeInt_1(data[10]);
        int dmaBufSize = normalizeInt_2(data[11] << 8) + normalizeInt_1(data[12]);
        int minFiltrationValue = normalizeInt_2(data[13] << 8) + normalizeInt_1(data[14]);
        int maxFiltrationValue = normalizeInt_2(data[15] << 8) + normalizeInt_1(data[16]);
        if ((flags & 1) > 0)
            LL.addView(getStatusLink("Impulses are being processed"));
        else
            LL.addView(getStatusLink("Impulses are not processing"));
        if ((flags & (1 << 1)) > 0)
            LL.addView(getStatusLink("CPS is sending"));
        else
            LL.addView(getStatusLink("CPS is not sending"));
        if ((flags & (1 << 2)) > 0)
            LL.addView(getStatusLink("uSD attached"));
        else
            LL.addView(getStatusLink("uSD detached"));
        if ((flags & (1 << 3)) > 0)
            LL.addView(getStatusLink("FileSystem initialized"));
        else
            LL.addView(getStatusLink("FileSystem is not initialized"));
        if ((flags & (1 << 4)) > 0)
            LL.addView(getStatusLink("BLE module has errors"));
        else
            LL.addView(getStatusLink("BLE module doesn't have errors"));
        if ((flags & (1 << 5)) > 0) {
            LL.addView(getStatusLink("Timer is set"));
            LL.addView(getStatusLink("Remaining time = " + Integer.toString(remainingTime)));
        }
        else
            LL.addView(getStatusLink("Timer is not set"));
        if ((flags & (1 << 6)) > 0)
            LL.addView(getStatusLink("Comparator value = 1"));
        else
            LL.addView(getStatusLink("Comparator value = 0"));

        LL.addView(getStatusLink("Counts for 1 pulse = " + dmaBufSize));
        LL.addView(getStatusLink("Min filter value = " + minFiltrationValue));
        LL.addView(getStatusLink("Max filter value = " + maxFiltrationValue));

        this.invalidate();
        return false;
    }
}
