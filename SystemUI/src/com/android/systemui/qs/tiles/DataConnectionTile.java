/*
 * SPRD
 */

package com.android.systemui.qs.tiles;
import android.os.RemoteException;
import java.util.List;
import android.os.UserHandle;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings.Global;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManagerSprd;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.app.ActivityManagerNative;
import com.android.systemui.R;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController.DataUsageInfo;
import com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback;

/** Quick settings tile: DataConnection **/
public class DataConnectionTile extends QSTile<QSTile.BooleanState> {
    private static final Intent CELLULAR_SETTINGS = new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));

    private static final Intent DATA_SETTINGS = new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$SimSettingsActivity"));
    private final NetworkController mController;
    private final MobileDataController mDataController;
    private final CellularDetailAdapter mDetailAdapter;

    private final GlobalSetting mDataSetting;
    private final GlobalSetting mRadioSetting;
    private TelephonyManagerSprd mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;

    public DataConnectionTile(Host host) {
        super(host);
        mController = host.getNetworkController();
        mDataController = mController.getMobileDataController();
        mDetailAdapter = new CellularDetailAdapter();

        mTelephonyManager = (TelephonyManagerSprd) TelephonyManager.from(mContext);
        mSubscriptionManager = SubscriptionManager.from(mContext);

        mDataSetting = new GlobalSetting(mContext, mHandler, Global.MOBILE_DATA) {
            @Override
            protected void handleValueChanged(int value) {
                Log.d(TAG, "mDataSetting handleValueChanged");
                mState.value = mTelephonyManager.getDataEnabled();
                handleRefreshState(value);
                mDetailAdapter.setMobileDataEnabled(mState.value);
            }
        };

        mRadioSetting = new GlobalSetting(mContext, mHandler, Global.RADIO_OPERATION) {
            @Override
            protected void handleValueChanged(int value) {
                Log.d(TAG, "mRadioSetting handleValueChanged");
                handleRefreshState(value);
            }
        };
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    public void setListening(boolean listening) {
        mRadioSetting.setListening(listening);
        mDataSetting.setListening(listening);
    }

    @Override
    protected void handleClick() {
        Log.d(TAG, "handleClick");
        if (mDataController.isMobileDataSupported()) {
            if (mTelephonyManager.isAirplaneModeOn()) {
                mHost.collapsePanels();
                Toast.makeText(mContext, R.string.toggle_data_error_airplane, Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            if (isDefaultDataSimAvailable()) {
                boolean enabled = !mTelephonyManager.getDataEnabled();
                toggleDataConnectionToDesired(enabled);
            }
        }
    }
	@Override
    protected void handleLongClick() {
        // optional
		mHost.startSettingsActivity(DATA_SETTINGS);
		mHost.collapsePanels();
		//
    }
	private void startSettingsActivity(Intent intent) {
        startSettingsActivity(intent, true);
    }
	 private void startSettingsActivity(Intent intent, boolean onlyProvisioned) {
       // if (onlyProvisioned && !getService().isDeviceProvisioned()) return;
        //try {
            // Dismiss the lock screen when Settings starts.
           // ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
       // } catch (RemoteException e) {
       // }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
        mHost.collapsePanels();
    }
	
    private void toggleDataConnectionToDesired(boolean enabled) {
        mState.value = enabled;
        mDataController.setMobileDataEnabled(enabled);
        // disable data for other subscriptions
        if (enabled) {
            int currentSubId = SubscriptionManager.getDefaultDataSubId();
            disableDataForOtherSubscriptions(currentSubId);
        }
    }

    private void disableDataForOtherSubscriptions(int currentSubId) {
        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (subInfo.getSubscriptionId() != currentSubId) {
                    mTelephonyManager.setDataEnabled(subInfo.getSubscriptionId(), false);
                }
            }
        }
    }

    private boolean isDefaultDataSimAvailable() {
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubId();
        int defaultDataPhoneId = SubscriptionManager.getSlotId(defaultDataSubId);
        boolean isDefaultDataSimReady = SubscriptionManager
                .getSimStateForSubscriber(defaultDataSubId) == TelephonyManager.SIM_STATE_READY;
        boolean isDefaultDataStandby = SubscriptionManager.isValidPhoneId(defaultDataPhoneId)
                && mTelephonyManager.isSimStandby(defaultDataPhoneId);
        Log.d(TAG, "defaultDataSubId = " + defaultDataSubId + " isDefaultDataSimReady = "
                + isDefaultDataSimReady + " isDefaultDataStandby = " + isDefaultDataStandby);
        return !mTelephonyManager.isRadioBusy() && isDefaultDataSimReady && isDefaultDataStandby;
    }

    @Override
    public boolean supportsDualTargets() {
        return true;
    }

    @Override
    protected void handleSecondaryClick() {
        Log.d(TAG, "handleSecondaryClick");
        if (mDataController.isMobileDataSupported() && isDefaultDataSimAvailable()) {
            showDetail(true);
        } else {
            mHost.startSettingsActivity(CELLULAR_SETTINGS);
        }
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final boolean dataConnected = mTelephonyManager.getDataEnabled();
        state.value = dataConnected;
        state.visible = true;
        state.label = mContext.getString(R.string.quick_settings_data_connection_label);
        Log.d(TAG, "dataConnected = " + dataConnected);
        if (dataConnected
                && isDefaultDataSimAvailable()
                && !mTelephonyManager.isAirplaneModeOn()) {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_mobile_data_on_sprd);
            state.contentDescription = mContext.getString(
                    R.string.accessibility_quick_settings_data_on);
        } else {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_mobile_data_off_sprd);
            state.contentDescription = mContext.getString(
                    R.string.accessibility_quick_settings_data_off);
        }
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_data_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_data_changed_off);
        }
    }

    private final class CellularDetailAdapter implements DetailAdapter {

        @Override
        public int getTitle() {
            return R.string.quick_settings_cellular_detail_title;
        }

        @Override
        public Boolean getToggleState() {
            // SPRD: fixbug438382 When the flight mode is opened, the data connection cannot be opened.
            return mTelephonyManager.isAirplaneModeOn() ? Boolean.FALSE : mDataController.isMobileDataSupported()
                    ? mDataController.isMobileDataEnabled()
                    : null;
        }

        @Override
        public Intent getSettingsIntent() {
            return CELLULAR_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
            toggleDataConnectionToDesired(state);
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            final DataUsageDetailView v = (DataUsageDetailView) (convertView != null
                    ? convertView
                    : LayoutInflater.from(mContext).inflate(R.layout.data_usage, parent, false));
            final DataUsageInfo info = mDataController.getDataUsageInfo();
            if (info == null) return v;
            v.bind(info);
            return v;
        }

        public void setMobileDataEnabled(boolean enabled) {
            fireToggleStateChanged(enabled);
        }
    }
}
