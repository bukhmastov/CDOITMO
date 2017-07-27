package com.bukhmastov.cdoitmo.firebase;

import android.content.Context;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.google.firebase.messaging.FirebaseMessaging;

public class FirebaseMessagingProvider {

    private static final String TAG = "FirebaseMessagingProvider";

    public static class Channel {
        public static String OWNER_NOTIFICATION = "owner_notification";
    }

    public static void subscribe(final String topic) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    FirebaseMessaging.getInstance().subscribeToTopic(topic);
                    Log.i(TAG, "Firebase Messaging Service: subscribed to '" + topic + "' topic");
                } catch (Throwable e) {
                    Log.i(TAG, "Firebase Messaging Service: failed to subscribed to '" + topic + "' topic");
                    Static.error(e);
                }
            }
        });
    }

    public static void unsubscribe(final String topic) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
                    Log.i(TAG, "Firebase Messaging Service: unsubscribed from '" + topic + "' topic");
                } catch (Throwable e) {
                    Log.i(TAG, "Firebase Messaging Service: failed to unsubscribed from '" + topic + "' topic");
                    Static.error(e);
                }
            }
        });
    }

    public static void checkOwnerNotification(final Context context){
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                if (Storage.pref.get(context, "pref_allow_owner_notifications", true)) {
                    subscribe(Channel.OWNER_NOTIFICATION);
                } else {
                    unsubscribe(Channel.OWNER_NOTIFICATION);
                }
            }
        });
    }
}
