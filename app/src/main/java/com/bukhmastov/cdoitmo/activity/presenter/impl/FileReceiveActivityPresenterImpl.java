package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.app.ActionBar;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toolbar;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.FileReceiveActivity;
import com.bukhmastov.cdoitmo.activity.presenter.FileReceiveActivityPresenter;
import com.bukhmastov.cdoitmo.exception.CorruptedFileException;
import com.bukhmastov.cdoitmo.exception.MessageException;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsShareFragment;
import com.bukhmastov.cdoitmo.model.fileshare.FShare;
import com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons.FSLessons;
import com.bukhmastov.cdoitmo.network.provider.NetworkUserAgentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

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
        thread.runOnUI(() -> {
            log.i(TAG, "Activity created");
            firebaseAnalyticsProvider.logCurrentScreen(activity);
            activity.setContentView(R.layout.activity_file_receive);
            Toolbar toolbar = activity.findViewById(R.id.toolbar_file);
            if (toolbar != null) {
                theme.applyToolbarTheme(activity, toolbar);
                activity.setActionBar(toolbar);
            }
            ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
            proceed();
        });
    }

    @Override
    public void onDestroy() {}

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
            Intent intent = activity.getIntent();
            if (intent == null) {
                throw new NullPointerException("Intent is null");
            }
            log.v(TAG, "proceed | intent: ", intent.toString());
            Uri uri = intent.getData();
            if (uri == null) {
                throw new NullPointerException("Intent's data (uri) is null");
            }
            String scheme = uri.getScheme();
            if (scheme == null) {
                throw new NullPointerException("Uri's scheme is null");
            }
            String file;
            switch (scheme) {
                case "file":    file = fileFromUri(uri); break;
                case "http":
                case "https":   file = fileFromWeb(uri); break;
                case "content": file = fileFromContent(uri); break;
                default:        throw new MessageException(activity.getString(R.string.failed_to_handle_file));
            }
            if (StringUtils.isBlank(file)) {
                throw new MessageException(activity.getString(R.string.failed_to_handle_file));
            }
            FShare fShare;
            try {
                fShare = new FShare().fromJsonString(file);
            } catch (Exception e) {
                log.w(TAG, "proceed | FShare throws ", e.getMessage());
                throw new MessageException(activity.getString(R.string.failed_to_handle_file));
            }
            if (StringUtils.isBlank(fShare.getType())) {
                throw new MessageException(activity.getString(R.string.file_doesnot_supported));
            }
            switch (fShare.getType()) {
                case "share_schedule_of_lessons": handleScheduleOfLessons(file); break;
                /* Place for future file types (if any) */
                default: throw new MessageException(activity.getString(R.string.file_doesnot_supported));
            }
        }, throwable -> {
            if (throwable instanceof CorruptedFileException) {
                log.v(TAG, "proceed | CorruptedFileException");
                failure(activity.getString(R.string.corrupted_file));
            } else if (throwable instanceof MessageException) {
                log.v(TAG, "proceed | MessageException: ", throwable.getMessage());
                failure(throwable.getMessage());
            } else {
                log.w(TAG, "proceed | Throwable: ", throwable.getMessage());
                failure(activity.getString(R.string.failed_to_handle_file));
            }
        });
    }

    private String fileFromUri(Uri uri) throws Throwable {
        log.v(TAG, "fileFromUri | uri=", uri.toString());
        try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor == null) {
                throw new NullPointerException("fileFromUri | cursor is null");
            }
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            String filename = cursor.getString(nameIndex);
            if (!Pattern.compile("^.*\\.cdoitmo$").matcher(filename).find()) {
                log.v(TAG, "fileFromUri | filename does not match pattern | filename=", filename);
                throw new MessageException(activity.getString(R.string.error_while_handle_file));
            }
        }
        try (ParcelFileDescriptor parcelFileDescriptor = activity.getContentResolver().openFileDescriptor(uri, "r")) {
            if (parcelFileDescriptor == null) {
                throw new NullPointerException("fileFromUri | ParcelFileDescriptor is null");
            }
            try (InputStream in = new FileInputStream(parcelFileDescriptor.getFileDescriptor())) {
                byte[] buffer = new byte[1024];
                StringBuilder out = new StringBuilder();
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.append(new String(buffer, 0, length));
                }
                return out.toString();
            }
        }
    }

    private String fileFromWeb(Uri uri) throws Throwable {
        log.v(TAG, "fileFromWeb | uri=", uri.toString());
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", networkUserAgentProvider.get(activity));
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
        if (responseBody == null) {
            throw new NullPointerException("fileFromWeb | ResponseBody is null");
        }
        char[] buffer = new char[1024];
        StringBuilder out = new StringBuilder();
        Reader reader = responseBody.charStream();
        int length;
        while ((length = reader.read(buffer, 0, buffer.length)) != -1) {
            out.append(buffer, 0, length);
        }
        return out.toString();
    }

    private String fileFromContent(Uri uri) throws Throwable {
        log.v(TAG, "fileFromContent | uri=", uri.toString());
        try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new NullPointerException("fileFromContent | InputStream is null");
            }
            byte[] buffer = new byte[1024];
            StringBuilder out = new StringBuilder();
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.append(new String(buffer, 0, length));
            }
            return out.toString();
        }
    }

    private void handleScheduleOfLessons(String file) throws Throwable {
        thread.assertNotUI();
        log.v(TAG, "handleScheduleOfLessons");
        if (storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", "").trim().isEmpty()) {
            throw new MessageException(activity.getString(R.string.file_requires_auth));
        }
        FSLessons fsLessons;
        try {
            fsLessons = new FSLessons().fromJsonString(file);
        } catch (Exception e) {
            throw new CorruptedFileException();
        }
        if (fsLessons == null) {
            throw new CorruptedFileException();
        }
        thread.runOnUI(() -> {
            Bundle extras = new Bundle();
            extras.putString("action", "handle");
            extras.putSerializable("data", fsLessons);
            if (!activity.openFragment(ConnectedActivity.TYPE.ROOT, ScheduleLessonsShareFragment.class, extras)) {
                failure(activity.getString(R.string.failed_to_display_file));
            }
        }, throwable -> {
            log.w(TAG, "handleScheduleOfLessons.runOnUI | Throwable: ", throwable.getMessage());
            failure(activity.getString(R.string.failed_to_handle_file));
        });
    }

    private void failure(String message) {
        thread.runOnUI(() -> {
            log.v(TAG, "failure | message=", message);
            View failed = activity.inflate(R.layout.state_failed_text_compact);
            ((TextView) failed.findViewById(R.id.text)).setText(message);
            ViewGroup container = activity.findViewById(activity.getRootViewId());
            container.removeAllViews();
            container.addView(failed);
        });
    }
}
