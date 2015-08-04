
package com.android.systemui.statusbar.policy;

import android.app.AddonManager;
import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.systemui.R;

public class SystemUIPluginsHelper {
    static SystemUIPluginsHelper mInstance;

    public static final String TAG = "SystemUIPluginsHelper";

    public static int[] SIM_CARD_ID = TelephonyIcons.SIM_CARD_ID;
    public static final int[][] TELEPHONY_SIGNAL_STRENGTH_COLOR_ONE = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_COLOR_ONE;
    public static final int[][] TELEPHONY_SIGNAL_STRENGTH_COLOR_TWO = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_COLOR_TWO;

    public static final int ABSENT_SIM_COLOR = 0xFFd9d9d9;
    public static final int[] COLORS = {
            0xFF18FFFF, 0xFFFFEB3B
    };

    public SystemUIPluginsHelper() {
    }

    public static SystemUIPluginsHelper getInstance() {
        if (mInstance != null)
            return mInstance;
        mInstance = (SystemUIPluginsHelper) AddonManager.getDefault()
                .getAddon(R.string.feature_display_for_operator, SystemUIPluginsHelper.class);
        return mInstance;
    }

    public String updateNetworkName(Context context, boolean showSpn, String spn, boolean showPlmn,
            String plmn, int phoneId) {
        return "";
    }

    public int getSubscriptionInfoColor(Context context, int subId) {
        SubscriptionManager subManager = SubscriptionManager.from(context);
        SubscriptionInfo subInfo = subManager.getActiveSubscriptionInfo(subId);
        Log.d(TAG, "getSubscriptionInfoColor");
        return subInfo == null ? ABSENT_SIM_COLOR : subInfo.getIconTint();
    }

    public int[][] getColorfulSignalStrengthIcons(int phoneId) {
        return null;
    }

    public int getNoSimIconId() {
        return 0;
    }

    public int getNoServiceIconId() {
        return R.drawable.stat_sys_signal_null;
    }

    public int getSimCardIconId(int subId) {
        return 0;
    }

    public int getSimStandbyIconId() {
        return R.drawable.stat_sys_no_sim_sprd;
    }
}
