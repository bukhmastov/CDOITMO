package com.bukhmastov.cdoitmo.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DownloadImage;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

public class UniversityPersonCardActivity extends ConnectedActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityPersonCard";
    private RequestHandle fragmentRequestHandle = null;
    private boolean loaded = false;
    private boolean first_load = true;
    private JSONObject person = null;
    private int pid = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark_TransparentStatusBar);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        try {
            Intent intent = getIntent();
            if (intent == null) throw new NullPointerException("intent is null");
            Bundle extras = intent.getExtras();
            if (extras == null) throw new NullPointerException("extras are null");
            boolean ok = false;
            if (extras.containsKey("person")) {
                try {
                    person = new JSONObject(extras.getString("person"));
                    pid = person.getInt("persons_id");
                    ok = true;
                } catch (Exception e) {
                    ok = false;
                }
            }
            if (!ok && extras.containsKey("pid")) {
                try {
                    person = null;
                    pid = extras.getInt("pid");
                    ok = true;
                } catch (Exception e) {
                    ok = false;
                }
            }
            if (!ok) throw new Exception("failed to get info from extras");
        } catch (Exception e) {
            finish();
        }

        FirebaseAnalyticsProvider.logCurrentScreen(this);
        setContentView(R.layout.activity_university_person_card);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(this);
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
        if (fragmentRequestHandle != null) {
            loaded = false;
            fragmentRequestHandle.cancel(true);
        }
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "refreshed");
        person = null;
        load();
    }

    private void load() {
        if (person != null) {
            display();
            return;
        }
        IfmoRestClient.get(this, "person/" + pid, null, new IfmoRestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, JSONObject json, JSONArray responseArr) {
                try {
                    SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.person_swipe);
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                    String post = json.getString("post");
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        json.put("post", android.text.Html.fromHtml(post, android.text.Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        json.put("post", android.text.Html.fromHtml(post));
                    }
                } catch (Exception e) {
                    // ok
                }
                person = json;
                display();
            }
            @Override
            public void onProgress(int state) {
                Log.v(TAG, "load | progress " + state);
                if (first_load) {
                    draw(R.layout.state_loading);
                    TextView loading_message = (TextView) findViewById(R.id.loading_message);
                    if (loading_message != null) {
                        switch (state) {
                            case IfmoRestClient.STATE_HANDLING:
                                loading_message.setText(R.string.loading);
                                break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(int state) {
                Log.v(TAG, "load | failure " + state);
                switch (state) {
                    case IfmoRestClient.FAILED_OFFLINE:
                        draw(R.layout.state_offline);
                        View offline_reload = findViewById(R.id.offline_reload);
                        if (offline_reload != null) {
                            offline_reload.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    load();
                                }
                            });
                        }
                        break;
                    case IfmoRestClient.FAILED_TRY_AGAIN:
                        draw(R.layout.state_try_again);
                        View try_again_reload = findViewById(R.id.try_again_reload);
                        if (try_again_reload != null) {
                            try_again_reload.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    load();
                                }
                            });
                        }
                        break;
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                fragmentRequestHandle = requestHandle;
            }
        });
    }

    private void display() {
        try {
            first_load = false;
            draw(R.layout.layout_university_person_card);
            findViewById(R.id.person_header).setPadding(0, getStatusBarHeight(), 0, 0);
            // кнопка назад
            findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            // заголовок
            final String name = (person.getString("title_l") + " " + person.getString("title_f") + " " + person.getString("title_m")).trim();
            final String degree = person.getString("degree").trim();
            final String image = person.getString("image");
            ((TextView) findViewById(R.id.name)).setText(name);
            if (!degree.isEmpty()) {
                ((TextView) findViewById(R.id.degree)).setText(Static.capitalizeFirstLetter(degree));
            } else {
                Static.removeView(findViewById(R.id.degree));
            }
            new DownloadImage(new DownloadImage.response() {
                @Override
                public void finish(Bitmap bitmap) {
                    if (bitmap == null) return;
                    try {
                        float destiny = getResources().getDisplayMetrics().density;
                        float dimen = getResources().getDimension(R.dimen.university_person_card_big_avatar);
                        bitmap = Static.createSquaredBitmap(bitmap);
                        bitmap = Static.getResizedBitmap(bitmap, (int) (62 * dimen), (int) (62 * dimen));
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                        drawable.setCornerRadius((dimen / 2) * destiny);
                        ((ImageView) findViewById(R.id.avatar)).setImageDrawable(drawable);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).execute(image);
            // контент
            ViewGroup info_connect_container = (ViewGroup) findViewById(R.id.info_connect_container);
            if (info_connect_container != null) {
                final String[] phones = person.getString("phone").trim().split(";|,");
                final String[] emails = person.getString("email").trim().split(";|,");
                final String[] webs = person.getString("www").trim().split(";|,");
                for (final String phone : phones) {
                    if (!phone.isEmpty()) {
                        info_connect_container.addView(getConnectContainer(R.drawable.ic_phone, phone, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.trim()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }));
                    }
                }
                for (final String email : emails) {
                    if (!email.isEmpty()) {
                        info_connect_container.addView(getConnectContainer(R.drawable.ic_email, email, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                emailIntent.setType("message/rfc822");
                                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email.trim()});
                                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_mail) + "..."));
                            }
                        }));
                    }
                }
                for (final String web : webs) {
                    if (!web.isEmpty()) {
                        info_connect_container.addView(getConnectContainer(R.drawable.ic_web, web, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(web.trim())));
                            }
                        }));
                    }
                }
            }
            ViewGroup info_about_container = (ViewGroup) findViewById(R.id.info_about_container);
            if (info_about_container != null) {
                final String rank = person.getString("rank").trim();
                final String post = person.getString("post").trim();
                final String bio = person.getString("text").trim();
                if (!rank.isEmpty()) {
                    info_about_container.addView(getAboutContainer(getString(R.string.person_rank), Static.capitalizeFirstLetter(rank)));
                }
                if (!post.isEmpty()) {
                    info_about_container.addView(getAboutContainer(getString(R.string.person_post), Static.capitalizeFirstLetter(post)));
                }
                if (!bio.isEmpty()) {
                    info_about_container.addView(getAboutContainer(getString(R.string.person_bio), bio));
                }
                if ((!rank.isEmpty() || !post.isEmpty() || !bio.isEmpty()) && info_connect_container != null && info_connect_container.getChildCount() > 0) {
                    Static.removeView(info_connect_container.getChildAt(info_connect_container.getChildCount() - 1).findViewById(R.id.separator));
                }
            }
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.person_swipe);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
            final ScrollView persons_list_scroll = (ScrollView) findViewById(R.id.persons_list_scroll);
            if (persons_list_scroll != null && persons_list_scroll.getChildCount() > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    persons_list_scroll.getChildAt(0).setOnScrollChangeListener(new View.OnScrollChangeListener() {
                        @Override
                        public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                            Log.d(TAG, scrollX + " " + scrollY);
                        }
                    });
                }
                persons_list_scroll.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        int scrollY = persons_list_scroll.getScrollY(); // For ScrollView
                        int scrollX = persons_list_scroll.getScrollX(); // For HorizontalScrollView
                        Log.d(TAG, scrollX + " " + scrollY);
                    }
                });
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private View getConnectContainer(@DrawableRes int icon, String text, View.OnClickListener listener) {
        View activity_university_person_card_connect = inflate(R.layout.layout_university_person_card_connect);
        ((ImageView) activity_university_person_card_connect.findViewById(R.id.connect_image)).setImageResource(icon);
        ((TextView) activity_university_person_card_connect.findViewById(R.id.connect_text)).setText(text.trim());
        activity_university_person_card_connect.setOnClickListener(listener);
        return activity_university_person_card_connect;
    }
    private View getAboutContainer(String title, String text) {
        View activity_university_person_card_about = inflate(R.layout.layout_university_person_card_about);
        ((TextView) activity_university_person_card_about.findViewById(R.id.title)).setText(title);
        TextView textView = (TextView) activity_university_person_card_about.findViewById(R.id.text);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            textView.setText(android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim());
        } else {
            textView.setText(android.text.Html.fromHtml(text).toString().trim());
        }
        return activity_university_person_card_about;
    }

    private void draw(@LayoutRes int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) findViewById(R.id.person_common_container));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(@LayoutRes int layoutId) throws InflateException {
        return ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    protected int getRootViewId() {
        return R.id.person_common_container;
    }

}
