package com.bukhmastov.cdoitmo.network.provider.impl;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import com.bukhmastov.cdoitmo.network.provider.NetworkUserAgentProvider;
import com.bukhmastov.cdoitmo.util.Log;

public class NetworkUserAgentProviderImpl implements NetworkUserAgentProvider {

    private static final String TAG = "NetworkUserAgentProvider";
    private static final String USER_AGENT_TEMPLATE = "CDOITMO/{versionName}/{versionCode} Java/Android/{sdkInt}";
    private String USER_AGENT = null;

    //@Inject
    private Log log = Log.instance();

    @Override
    public String get(Context context) {
        try {
            if (USER_AGENT == null && context != null) {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                USER_AGENT = USER_AGENT_TEMPLATE
                        .replace("{versionName}", pInfo.versionName)
                        .replace("{versionCode}", String.valueOf(pInfo.versionCode))
                        .replace("{sdkInt}", String.valueOf(Build.VERSION.SDK_INT));
            }
            return USER_AGENT;
        } catch (Exception e) {
            log.w(TAG, "Failed to provide user agent | ", e.getMessage());
            return USER_AGENT_TEMPLATE
                    .replace("{versionName}", "-")
                    .replace("{versionCode}", "-")
                    .replace("{sdkInt}", "-");
        }
    }
}
