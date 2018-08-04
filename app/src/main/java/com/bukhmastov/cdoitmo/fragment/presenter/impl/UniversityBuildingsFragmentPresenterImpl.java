package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityBuildingsFragmentPresenter;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.view.CircularTransformation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

public class UniversityBuildingsFragmentPresenterImpl implements UniversityBuildingsFragmentPresenter, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "UniversityBuildingsFragment";
    private FragmentActivity activity = null;
    private Fragment fragment = null;
    private View container;
    private Client.Request requestHandle = null;
    private JSONObject building_map = null;
    private GoogleMap googleMap = null;
    private final ArrayList<Marker> markers = new ArrayList<>();
    private boolean markers_campus = true;
    private boolean markers_dormitory = true;
    private long timestamp = 0;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    IfmoRestClient ifmoRestClient;
    @Inject
    Static staticUtil;
    @Inject
    Theme theme;
    @Inject
    Time time;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public UniversityBuildingsFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot("university")) {
            return;
        }
        building_map = null;
    }

    @Override
    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
        markers_campus = storagePref.get(activity, "pref_university_buildings_campus", true);
        markers_dormitory = storagePref.get(activity, "pref_university_buildings_dormitory", true);
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        log.v(TAG, "Fragment resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused");
        if (requestHandle != null) {
            requestHandle.cancel();
        }
    }

    @Override
    public void onCreateView(View container) {
        this.container = container;
        try {
            SupportMapFragment mapFragment = (SupportMapFragment) fragment.getChildFragmentManager().findFragmentById(R.id.buildings_map);
            mapFragment.getMapAsync(this);
        } catch (Exception e) {
            log.exception(e);
            failed();
            load();
        }
        Switch dormitory_switch = container.findViewById(R.id.dormitory_switch);
        if (dormitory_switch != null) {
            dormitory_switch.setChecked(markers_dormitory);
            dormitory_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                markers_dormitory = isChecked;
                displayMarkers();
                storagePref.put(activity, "pref_university_buildings_dormitory", markers_dormitory);
            });
        }
        Switch campus_switch = container.findViewById(R.id.campus_switch);
        if (campus_switch != null) {
            campus_switch.setChecked(markers_campus);
            campus_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                markers_campus = isChecked;
                displayMarkers();
                storagePref.put(activity, "pref_university_buildings_campus", markers_campus);
            });
        }
        View view_list = container.findViewById(R.id.view_list);
        if (view_list != null) {
            view_list.setOnClickListener(v -> openList());
        }
    }

    private void load() {
        thread.run(() -> load(storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)
                ? Integer.parseInt(storagePref.get(activity, "pref_static_refresh", "168"))
                : 0));
    }

    private void load(final int refresh_rate) {
        thread.run(() -> {
            log.v(TAG, "load | refresh_rate=" + refresh_rate);
            if (storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                String cache = storage.get(activity, Storage.CACHE, Storage.GLOBAL, "university#buildings").trim();
                if (!cache.isEmpty()) {
                    try {
                        JSONObject cacheJson = new JSONObject(cache);
                        building_map = cacheJson.getJSONObject("data");
                        timestamp = cacheJson.getLong("timestamp");
                        if (timestamp + refresh_rate * 3600000L < time.getCalendar().getTimeInMillis()) {
                            load(true);
                        } else {
                            load(false);
                        }
                    } catch (JSONException e) {
                        log.exception(e);
                        load(true);
                    }
                } else {
                    load(false);
                }
            } else {
                load(false);
            }
        });
    }

    private void load(final boolean force) {
        thread.run(() -> {
            log.v(TAG, "load | force=" + (force ? "true" : "false"));
            if ((!force || !Client.isOnline(activity)) && building_map != null) {
                display();
                return;
            }
            if (!App.OFFLINE_MODE) {
                loadProvider(new RestResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject json, final JSONArray responseArr) {
                        thread.run(() -> {
                            if (statusCode == 200) {
                                long now = time.getCalendar().getTimeInMillis();
                                if (json != null && storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                                    try {
                                        storage.put(activity, Storage.CACHE, Storage.GLOBAL, "university#buildings", new JSONObject()
                                                .put("timestamp", now)
                                                .put("data", json)
                                                .toString()
                                        );
                                    } catch (JSONException e) {
                                        log.exception(e);
                                    }
                                }
                                building_map = json;
                                timestamp = now;
                                display();
                            } else {
                                loadFailed();
                            }
                        });
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                        thread.run(() -> {
                            log.v(TAG, "forceLoad | failure " + state);
                            switch (state) {
                                case IfmoRestClient.FAILED_OFFLINE:
                                    loadOffline();
                                    break;
                                case IfmoRestClient.FAILED_CORRUPTED_JSON:
                                case IfmoRestClient.FAILED_SERVER_ERROR:
                                case IfmoRestClient.FAILED_TRY_AGAIN:
                                    loadFailed();
                                    break;
                            }
                        });
                    }
                    @Override
                    public void onProgress(final int state) {
                        thread.run(() -> {
                            log.v(TAG, "forceLoad | progress " + state);
                            loading();
                        });
                    }
                    @Override
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                });
            } else {
                loadOffline();
            }
        });
    }

    private void loadProvider(RestResponseHandler handler) {
        log.v(TAG, "loadProvider");
        ifmoRestClient.get(activity, "building_map", null, handler);
    }

    private void display() {
        if (building_map != null) {
            loaded();
            displayMarkers();
        } else {
            loadFailed();
        }
    }

    @Override
    public void onMapReady(final GoogleMap gMap) {
        thread.run(() -> {
            final MapStyleOptions mapStyleOptions;
            switch (theme.getAppTheme(activity)) {
                case "light":
                default: mapStyleOptions = MapStyleOptions.loadRawResourceStyle(activity, R.raw.google_map_light); break;
                case "dark": mapStyleOptions = MapStyleOptions.loadRawResourceStyle(activity, R.raw.google_map_dark); break;
                case "black": mapStyleOptions = MapStyleOptions.loadRawResourceStyle(activity, R.raw.google_map_black); break;
                case "white": mapStyleOptions = MapStyleOptions.loadRawResourceStyle(activity, R.raw.google_map_white); break;
            }
            thread.runOnUI(() -> {
                googleMap = gMap;
                googleMap.setMapStyle(mapStyleOptions);
                googleMap.setOnMarkerClickListener(this);
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(59.93465, 30.3138391)).zoom(10).build()));
                if (building_map == null) {
                    load();
                } else {
                    display();
                }
            });
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        try {
            JSONObject building = (JSONObject) marker.getTag();
            if (building == null) {
                throw new NullPointerException("marker's tag is null");
            }
            zoomToMarker(marker);
            return openMarkerInfo(building);
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    private void displayMarkers() {
        thread.run(() -> {
            try {
                if (building_map == null) return;
                if (googleMap == null) return;
                JSONArray list = building_map.getJSONArray("list");
                if (list != null) {
                    removeAllMarkers();
                    for (int i = 0; i < list.length(); i++) {
                        try {
                            final JSONObject building = list.getJSONObject(i);
                            if (building == null) {
                                continue;
                            }
                            String type = null;
                            int icon = -1;
                            switch (building.getInt("type_id")) {
                                case 1:
                                    if (!markers_campus) {
                                        continue;
                                    }
                                    type = activity.getString(R.string.campus);
                                    icon = R.drawable.location_campus;
                                    break;
                                case 2:
                                    if (!markers_dormitory) {
                                        continue;
                                    }
                                    type = activity.getString(R.string.dormitory);
                                    icon = R.drawable.location_dormitory;
                                    break;
                            }
                            final MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(new LatLng(building.getDouble("N"), building.getDouble("E")));
                            markerOptions.title(building.getString("title"));
                            markerOptions.zIndex((float) (building.getInt("major") + 1));
                            if (type != null) {
                                markerOptions.snippet(type);
                            }
                            if (icon != -1) {
                                markerOptions.icon(tintMarker(activity, icon));
                            }
                            thread.runOnUI(() -> {
                                try {
                                    Marker marker = googleMap.addMarker(markerOptions);
                                    marker.setTag(building);
                                    markers.add(marker);
                                } catch (Exception e) {
                                    log.exception(e);
                                }
                            });
                        } catch (Exception e) {
                            log.exception(e);
                        }
                    }
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void removeAllMarkers() {
        thread.runOnUI(() -> {
            for (Marker marker : markers) {
                marker.remove();
            }
            markers.clear();
        });
    }

    private boolean openMarkerInfo(JSONObject building) {
        try {
            closeMarkerInfo();
            closeList();
            ViewGroup marker_info_container = container.findViewById(R.id.marker_info_container);
            if (marker_info_container != null) {
                View marker_info = inflate(R.layout.layout_university_buildings_marker_info);
                final String title = escapeText(building.getString("title"));
                final int id = building.getInt("id");
                final String image = building.getString("image");
                ((TextView) marker_info.findViewById(R.id.marker_text)).setText(title);
                marker_info.findViewById(R.id.web).setOnClickListener(view -> eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ifmo.ru/ru/map/" + id + "/")))));
                Picasso.with(activity)
                        .load(image)
                        .error(R.drawable.ic_sentiment_very_satisfied)
                        .transform(new CircularTransformation())
                        .into((ImageView) marker_info.findViewById(R.id.marker_image));
                marker_info.findViewById(R.id.marker_close).setOnClickListener(v -> closeMarkerInfo());
                marker_info_container.addView(marker_info);
            }
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    private void closeMarkerInfo() {
        thread.runOnUI(() -> {
            try {
                View marker_info_container = container.findViewById(R.id.marker_info_container);
                if (marker_info_container != null) {
                    View marker_info = marker_info_container.findViewById(R.id.marker_info);
                    if (marker_info != null) {
                        staticUtil.removeView(marker_info);
                    }
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void openList() {
        thread.runOnUI(() -> {
            try {
                closeMarkerInfo();
                closeList();
                ViewGroup marker_info_container = container.findViewById(R.id.marker_info_container);
                if (building_map == null) return;
                JSONArray list = building_map.getJSONArray("list");
                if (marker_info_container != null && list != null) {
                    View layout_university_buildings_list = inflate(R.layout.layout_university_buildings_list);
                    layout_university_buildings_list.findViewById(R.id.list_close).setOnClickListener(v -> closeList());
                    ViewGroup list_container = layout_university_buildings_list.findViewById(R.id.list_container);
                    for (int i = 0; i < list.length(); i++) {
                        try {
                            JSONObject building = list.getJSONObject(i);
                            if (building == null) {
                                continue;
                            }
                            final String title = building.getString("title");
                            View item = inflate(R.layout.layout_university_faculties_divisions_list_item);
                            ((TextView) item.findViewById(R.id.title)).setText(title);
                            item.setOnClickListener(v -> {
                                for (Marker marker : markers) {
                                    try {
                                        JSONObject building1 = (JSONObject) marker.getTag();
                                        if (building1 == null) {
                                            continue;
                                        }
                                        if (title.equals(building1.getString("title"))) {
                                            closeList();
                                            onMarkerClick(marker);
                                            break;
                                        }
                                    } catch (Exception ignore) {
                                        // ignore
                                    }
                                }
                            });
                            list_container.addView(item);
                        } catch (Exception e) {
                            log.exception(e);
                        }
                    }
                    marker_info_container.addView(layout_university_buildings_list);
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void closeList() {
        thread.runOnUI(() -> {
            try {
                View marker_info_container = container.findViewById(R.id.marker_info_container);
                if (marker_info_container != null) {
                    View list = marker_info_container.findViewById(R.id.list);
                    if (list != null) {
                        staticUtil.removeView(list);
                    }
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void zoomToMarker(final Marker marker) {
        thread.runOnUI(() -> {
            CameraPosition cameraPosition = new CameraPosition.Builder().target(marker.getPosition()).zoom(15).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
        });
    }

    private BitmapDescriptor tintMarker(final Context context, @DrawableRes final int icon) {
        try {
            Paint markerPaint = new Paint();
            Bitmap markerBitmap = BitmapFactory.decodeResource(context.getResources(), icon);
            Bitmap resultBitmap = Bitmap.createBitmap(markerBitmap, 0, 0, markerBitmap.getWidth() - 1, markerBitmap.getHeight() - 1);
            markerPaint.setColorFilter(new PorterDuffColorFilter(Color.resolve(activity, R.attr.colorImageMultiply), PorterDuff.Mode.MULTIPLY));
            Canvas canvas = new Canvas(resultBitmap);
            canvas.drawBitmap(resultBitmap, 0, 0, markerPaint);
            return BitmapDescriptorFactory.fromBitmap(resultBitmap);
        } catch (Exception ignore) {
            return BitmapDescriptorFactory.fromResource(icon);
        }
    }

    @SuppressWarnings("deprecation")
    private String escapeText(String text) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                text = android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString();
            } else {
                text = android.text.Html.fromHtml(text).toString();
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return text.trim();
    }

    private void loading() {
        thread.runOnUI(() -> {
            try {
                ViewGroup vg = container.findViewById(R.id.status);
                if (vg != null) {
                    vg.removeAllViews();
                    vg.addView(inflate(R.layout.layout_university_buildings_status_loading));
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void loadFailed() {
        thread.runOnUI(() -> {
            try {
                View view = inflate(R.layout.layout_university_buildings_status_image);
                ImageView imageView = view.findViewById(R.id.image);
                imageView.setImageResource(R.drawable.ic_warning);
                view.setOnClickListener(view1 -> load());
                ViewGroup vg = container.findViewById(R.id.status);
                if (vg != null) {
                    vg.removeAllViews();
                    vg.addView(view);
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void loadOffline() {
        thread.runOnUI(() -> {
            try {
                View view = inflate(R.layout.layout_university_buildings_status_image);
                ImageView imageView = view.findViewById(R.id.image);
                imageView.setImageResource(R.drawable.ic_disconnected);
                ViewGroup vg = container.findViewById(R.id.status);
                if (vg != null) {
                    vg.removeAllViews();
                    vg.addView(view);
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void loaded() {
        thread.runOnUI(() -> {
            try {
                ViewGroup vg = container.findViewById(R.id.status);
                if (vg != null) {
                    vg.removeAllViews();
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void failed() {
        thread.runOnUI(() -> {
            try {
                View view = inflate(R.layout.layout_university_buildings_status_image);
                ImageView imageView = view.findViewById(R.id.image);
                imageView.setImageResource(R.drawable.ic_warning);
                ViewGroup vg = container.findViewById(R.id.status);
                if (vg != null) {
                    vg.removeAllViews();
                    vg.addView(view);
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private View inflate(@LayoutRes int layout) throws InflateException {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
