package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import dagger.Lazy;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class FileReceiveActivityPresenterImpl implements FileReceiveActivityPresenter {

    private static final String TAG = "FileReceiveActivity";
    private FileReceiveActivity activity = null;
    private Uri storedUri = null;

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
    @Inject
    Lazy<NotificationMessage> notificationMessage;

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
                activity.setSupportActionBar(toolbar);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setHomeButtonEnabled(true);
            }
            proceed();
        });
    }

    @Override
    public void onDestroy() {}

    @Override
    public boolean onToolbarSelected(MenuItem item) {
        return false;
    }

    @Override
    public boolean onBackPressed() {
        return activity.back();
    }

    @Override
    public void onReadExternalStorageGranted() {
        if (storedUri != null) {
            proceed(storedUri);
        }
    }

    private void proceed() {
        thread.standalone(() -> {
            Intent intent = activity.getIntent();
            if (intent == null) {
                throw new NullPointerException("Intent is null");
            }
            log.v(TAG, "proceed | intent: ", intent.toString());
            proceed(intent.getData());
        }, throwable -> {
            log.w(TAG, "proceed | Throwable: ", throwable.getMessage());
            failure(activity.getString(R.string.failed_to_handle_file));
        });
    }

    private void proceed(Uri uri) {
        thread.standalone(() -> {
            if (uri == null) {
                throw new NullPointerException("Uri is null");
            }
            String scheme = uri.getScheme();
            if (scheme == null) {
                throw new NullPointerException("Uri's scheme is null");
            }
            String file;
            switch (scheme) {
                case "http": case "https":   file = fileFromWeb(uri); break;
                case "file": case "content": file = fileFromContentResolver(uri); break;
                default: throw new MessageException(activity.getString(R.string.failed_to_handle_file));
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
            } else if (throwable instanceof FileNotFoundException) {
                log.v(TAG, "proceed | FileNotFoundException: ", throwable.getMessage());
                failure(activity.getString(R.string.failed_to_handle_file_not_found));
            } else {
                log.w(TAG, "proceed | Throwable: ", throwable.getMessage());
                failure(activity.getString(R.string.failed_to_handle_file));
            }
        });
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

    private String fileFromContentResolver(Uri uri) throws Throwable {
        log.v(TAG, "fileFromContentResolver | uri=", uri.toString());
        try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new NullPointerException("fileFromContentResolver | InputStream is null");
            }
            byte[] buffer = new byte[1024];
            StringBuilder out = new StringBuilder();
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.append(new String(buffer, 0, length));
            }
            return out.toString();
        } catch (FileNotFoundException fileNotFoundException) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                throw fileNotFoundException;
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                throw fileNotFoundException;
            }
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                notificationMessage.get().toast(activity, R.string.failed_to_handle_required_permission);
            }
            storedUri = uri;
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, FileReceiveActivity.PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
            throw fileNotFoundException;
        }
    }

    private void handleScheduleOfLessons(String file) throws Throwable {
        thread.assertNotUI();
        log.v(TAG, "handleScheduleOfLessons");
        FSLessons fsLessons;
        try {
            fsLessons = new FSLessons().fromJsonString(file);
        } catch (Exception e) {
            throw new CorruptedFileException();
        }
        if (fsLessons == null) {
            throw new CorruptedFileException();
        }
        if (storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", "").trim().isEmpty()) {
            throw new MessageException(activity.getString(R.string.file_requires_auth));
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
