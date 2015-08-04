/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Global;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManagerSprd;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;
import com.android.systemui.R;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTileView;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.sprd.internal.telephony.uicc.IATUtils;

/** Quick settings tile: Lte service **/
public class LteServiceTile extends QSTile<QSTile.BooleanState> {

    private static final String AT_QUERY_LTE_STATE = "AT+SPUECAP?";
    private static final int UPDATE_LTE_STATE = 100;
    private static final String LTE_STATE_ENABLE = "0";

    private final GlobalSetting mRadioSetting;
    private TelephonyManagerSprd mTelephonyManager;
    private boolean mListening;
    private boolean mLteEnabled;
    private boolean mLteAvailable;
    private QSTileView mQSTileView;

    public LteServiceTile(Host host) {
        super(host);
        mTelephonyManager = (TelephonyManagerSprd) TelephonyManager.from(mContext);

        mRadioSetting = new GlobalSetting(mContext, mHandler, Global.RADIO_OPERATION) {
            @Override
            protected void handleValueChanged(int value) {
                Log.d(TAG, "handleValueChanged: value = " + value);
                handleRefreshState(value);
            }
        };

    }

    public QSTileView createTileView(Context context) {
        mQSTileView = new QSTileView(context);
        return mQSTileView;
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleClick() {
        if (mTelephonyManager.isAirplaneModeOn()) {
            mHost.collapsePanels();
            Toast.makeText(mContext, R.string.lte_service_error_airplane, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        boolean isRadioBusy = mRadioSetting.getValue() == 1;
        Log.d(TAG, "handleClick: mLteAvailable = " + mLteAvailable +
                " isRadioBusy = " + isRadioBusy);

        if (isRadioBusy || !mLteAvailable) {
            return;
        } else {
            setLteEnabled();
        }
    }

    private void setLteEnabled() {
        Log.d(TAG, "setLteEnabled: " + !mState.value);
        mLteEnabled = !mState.value;
        mTelephonyManager.setLteEnabled(mLteEnabled);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {

        boolean isRadioBusy = mRadioSetting.getValue() == 1;
        if (!isRadioBusy && !mTelephonyManager.isAirplaneModeOn()) {
            updateLteEnabledState();
        }

        state.value = mLteEnabled;
        state.visible = true;
        state.label = mContext.getString(R.string.quick_settings_lte_service_label);

        int primaryCard = mTelephonyManager.getPrimaryCard();
        boolean isPrimaryCardUsim = mTelephonyManager.hasUsimCard(primaryCard);
        boolean isPrimaryCardReady = mTelephonyManager.isSimStandby(primaryCard) &&
                mTelephonyManager.getSimState(primaryCard) == TelephonyManager.SIM_STATE_READY;
        mLteAvailable = isPrimaryCardUsim && isPrimaryCardReady;

        Log.d(TAG, "handleUpdateState: mLteEnabled = " + mLteEnabled +
                " isPrimaryCardUsim = " + isPrimaryCardUsim +
                " isPrimaryCardReady = " + isPrimaryCardReady);

        if (mLteEnabled && mLteAvailable
                && !mTelephonyManager.isAirplaneModeOn()) {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_4g_on_sprd);
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_lte_on);
        } else {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_4g_off_sprd);
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_lte_off);
        }
    }

    public void updateLteEnabledState() {
        final int primaryCard = mTelephonyManager.getPrimaryCard();
        Log.d(TAG, "updateLteEnabledState: primaryCard = " + primaryCard);

        if (SubscriptionManager.isValidPhoneId(primaryCard)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String response = IATUtils.sendATCmd(AT_QUERY_LTE_STATE, primaryCard);
                    if (response != null) {
                        Message msg = mLteHandler.obtainMessage(UPDATE_LTE_STATE, response);
                        mLteHandler.sendMessage(msg);
                    }
                }
            }).start();
        }
    }

    private Handler mLteHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String mode = handleResponseForLteEnabled((String) msg.obj);
            boolean queryLteEnabledResult = LTE_STATE_ENABLE.equals(mode);
            Log.d(TAG, "query lte enbaled mode = " + mode + " mLteEnabled = " + mLteEnabled);

            if (mLteEnabled != queryLteEnabledResult) {
                mState.value = mLteEnabled = queryLteEnabledResult;
                if (mQSTileView != null) {
                    mQSTileView.onStateChanged(mState);
                }
            }
        }
    };

    private String handleResponseForLteEnabled(String response) {
        String mode = "";
        int startIndex = response.lastIndexOf(":") + 1;
        int endIndex = response.indexOf("\r");
        if (startIndex > 0 && endIndex != -1) {
            mode = response.substring(startIndex, endIndex).trim();
        }
        return mode;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_lte_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_lte_changed_off);
        }
    }

    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (listening) {
            final IntentFilter filter = new IntentFilter();
//            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            mContext.registerReceiver(mReceiver, filter);
        } else {
            mContext.unregisterReceiver(mReceiver);
        }
        mRadioSetting.setListening(listening);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
                refreshState();
            }
        }
    };
}
