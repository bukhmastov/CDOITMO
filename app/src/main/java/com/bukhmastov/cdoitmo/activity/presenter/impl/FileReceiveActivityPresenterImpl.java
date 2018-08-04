package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.FileReceiveActivity;
import com.bukhmastov.cdoitmo.activity.presenter.FileReceiveActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsShareFragment;
import com.bukhmastov.cdoitmo.network.provider.NetworkUserAgentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class FileReceiveActivityPresenterImpl implements FileReceiveActivityPresenter {

    private static final String TAG = "FileReceiveActivity";
    private FileReceiveActivity activity = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    Theme theme;
    @Inject
    NetworkUserAgentProvider networkUserAgentProvider;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public FileReceiveActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setActivity(@NonNull FileReceiveActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.i(TAG, "Activity created");
        firebaseAnalyticsProvider.logCurrentScreen(activity);
        activity.setContentView(R.layout.activity_file_receive);
        Toolbar toolbar = activity.findViewById(R.id.toolbar_file);
        if (toolbar != null) {
            theme.applyToolbarTheme(activity, toolbar);
            activity.setSupportActionBar(toolbar);
        }
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        proceed();
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public boolean onToolbarSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return false;
            default:
                return true;
        }
    }

    @Override
    public boolean onBackPressed() {
        return activity.back();
    }

    private void proceed() {
        thread.run(() -> {
            try {
                final Intent intent = activity.getIntent();
                if (intent == null) {
                    throw new NullPointerException("Intent is null");
                }
                log.v(TAG, "proceed | intent: " + intent.toString());
                final Uri uri = intent.getData();
                if (uri == null) {
                    throw new NullPointerException("Intent's data (uri) is null");
                }
                final String scheme = uri.getScheme();
                if (scheme == null) {
                    throw new NullPointerException("Uri's scheme is null");
                }
                final String file;
                switch (scheme) {
                    case "file":    file = fileFromUri(activity, uri); break;
                    case "http":
                    case "https":   file = fileFromWeb(activity, uri); break;
                    case "content": file = fileFromContent(activity, uri); break;
                    default:        throw new MessageException(activity.getString(R.string.failed_to_handle_file));
                }
                final JSONObject object = (JSONObject) new JSONTokener(file).nextValue();
                switch (object.getString("type")) {
                    case "share_schedule_of_lessons": share_schedule_of_lessons(file, object); break;
                    /* Place for future file types (if any) */
                    default: throw new MessageException(activity.getString(R.string.file_doesnot_supported));
                }
            } catch (MessageException e) {
                log.v(TAG, "proceed | MessageException: " + e.getMessage());
                failure(e.getMessage());
            } catch (Throwable throwable) {
                log.w(TAG, "proceed | Throwable: " + throwable.getMessage());
                failure(activity.getString(R.string.failed_to_handle_file));
            }
        });
    }

    private String fileFromUri(final Context context, final Uri uri) throws Throwable {
        log.v(TAG, "fileFromUri | uri: " + uri.toString());
        Cursor cursor = null;
        try {
            cursor = activity.getContentResolver().query(uri, null, null, null, null);
            if (cursor == null) {
                throw new NullPointerException("fileFromUri | cursor is null");
            }
            final int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            final String filename = cursor.getString(nameIndex);
            if (!Pattern.compile("^.*\\.cdoitmo$").matcher(filename).find()) {
                log.v(TAG, "fileFromUri | filename does not match pattern | filename=" + filename);
                throw new MessageException(context.getString(R.string.error_while_handle_file));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
        if (parcelFileDescriptor != null) {
            InputStream in = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
            final byte[] buffer = new byte[1024];
            final StringBuilder out = new StringBuilder();
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.append(new String(buffer, 0, length));
            }
            return out.toString();
        } else {
            throw new NullPointerException("fileFromUri | ParcelFileDescriptor is null");
        }
    }

    private String fileFromWeb(final Context context, final Uri uri) throws Throwable {
        log.v(TAG, "fileFromWeb | uri: " + uri.toString());
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", networkUserAgentProvider.get(context));
        Request request = new Request.Builder()
                .url(uri.toString())
                .headers(okhttp3.Headers.of(headers))
                .build();
        Response response = new OkHttpClient()
                .newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
                .newCall(request).execute();
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            final char[] buffer = new char[1024];
            final StringBuilder out = new StringBuilder();
            final Reader reader = responseBody.charStream();
            int length;
            while ((length = reader.read(buffer, 0, buffer.length)) != -1) {
                out.append(buffer, 0, length);
            }
            return out.toString();
        } else {
            throw new NullPointerException("fileFromWeb | ResponseBody is null");
        }
    }

    private String fileFromContent(final Context context, final Uri uri) throws Throwable {
        log.v(TAG, "fileFromContent | uri: " + uri.toString());
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in != null) {
            final byte[] buffer = new byte[1024];
            final StringBuilder out = new StringBuilder();
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.append(new String(buffer, 0, length));
            }
            return out.toString();
        } else {
            throw new NullPointerException("fileFromContent | InputStream is null");
        }
    }

    private void share_schedule_of_lessons(final String file, final JSONObject object) {
        thread.run(() -> {
            try {
                log.v(TAG, "share_schedule_of_lessons");
                if (storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", "").trim().isEmpty()) {
                    throw new MessageException(activity.getString(R.string.file_requires_auth));
                }
                if (object.has("content")) {
                    final JSONObject content = object.getJSONObject("content");
                    if (
                            !(content.has("query") && content.get("query") instanceof String) ||
                                    !(content.has("title") && content.get("title") instanceof String) ||
                                    !(content.has("added") && content.get("added") instanceof JSONArray) ||
                                    !(content.has("reduced") && content.get("reduced") instanceof JSONArray)
                            ) {
                        throw new MessageException(activity.getString(R.string.corrupted_file));
                    }
                } else {
                    throw new MessageException(activity.getString(R.string.corrupted_file));
                }
                thread.runOnUI(() -> {
                    Bundle extras = new Bundle();
                    extras.putString("action", "handle");
                    extras.putString("data", file);
                    if (!activity.openFragment(ConnectedActivity.TYPE.ROOT, ScheduleLessonsShareFragment.class, extras)) {
                        failure(activity.getString(R.string.failed_to_display_file));
                    }
                });
            } catch (MessageException e) {
                log.v(TAG, "share_schedule_of_lessons | MessageException: " + e.getMessage());
                failure(e.getMessage());
            } catch (Throwable throwable) {
                log.w(TAG, "share_schedule_of_lessons | Throwable: " + throwable.getMessage());
                failure(activity.getString(R.string.failed_to_decode_file));
            }
        });
    }

    private void failure(final String message) {
        thread.runOnUI(() -> {
            log.v(TAG, "failure | message=" + message);
            View state_failed_without_align = activity.inflate(R.layout.state_failed_text_compact);
            ((TextView) state_failed_without_align.findViewById(R.id.text)).setText(message);
            ViewGroup container = activity.findViewById(activity.getRootViewId());
            container.removeAllViews();
            container.addView(state_failed_without_align);
        });
    }

    private class MessageException extends Exception {
        private MessageException(String message) {
            super(message);
        }
    }
}
