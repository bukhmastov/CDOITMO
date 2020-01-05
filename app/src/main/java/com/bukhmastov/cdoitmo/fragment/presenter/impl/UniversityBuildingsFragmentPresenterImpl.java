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
import com.bukhmastov.cdoitmo.model.university.buildings.UBuilding;
import com.bukhmastov.cdoitmo.model.university.buildings.UBuildings;
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
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import static com.bukhmastov.cdoitmo.util.Thread.UB;

public class UniversityBuildingsFragmentPresenterImpl implements UniversityBuildingsFragmentPresenter, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "UniversityBuildingsFragment";
    private FragmentActivity activity = null;
    private Fragment fragment = null;
    private View container;
    private Client.Request requestHandle = null;
    private UBuildings uBuildings = null;
    private GoogleMap googleMap = null;
    private final List<Marker> markers = new ArrayList<>();
    private boolean markersCampusEnabled = true;
    private boolean markersDormitoryEnabled = true;

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
        if (event.isNot(ClearCacheEvent.UNIVERSITY)) {
            return;
        }
        uBuildings = null;
    }

    @Override
    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.initialize(UB);
        thread.run(UB, () -> {
            log.v(TAG, "onCreate");
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            markersCampusEnabled = storagePref.get(activity, "pref_university_buildings_campus", true);
            markersDormitoryEnabled = storagePref.get(activity, "pref_university_buildings_dormitory", true);
        });
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "onDestroy");
        thread.interrupt(UB);
    }

    @Override
    public void onResume() {
        log.v(TAG, "onResume");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
    }

    @Override
    public void onPause() {
        log.v(TAG, "onPause");
        thread.standalone(() -> {
            if (requestHandle != null) {
                requestHandle.cancel();
            }
        });
    }

    @Override
    public void onCreateView(View container) {
        thread.runOnUI(UB, () -> {
            this.container = container;
            if (isNotAddedToActivity()) {
                log.w(TAG, "onCreateView | fragment not added to activity");
                return;
            }
            Switch dormitorySwitch = container.findViewById(R.id.dormitory_switch);
            if (dormitorySwitch != null) {
                dormitorySwitch.setChecked(markersDormitoryEnabled);
                dormitorySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> thread.run(UB, () -> {
                    markersDormitoryEnabled = isChecked;
                    storagePref.put(activity, "pref_university_buildings_dormitory", markersDormitoryEnabled);
                    displayMarkers();
                }));
            }
            Switch campusSwitch = container.findViewById(R.id.campus_switch);
            if (campusSwitch != null) {
                campusSwitch.setChecked(markersCampusEnabled);
                campusSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> thread.run(UB, () -> {
                    markersCampusEnabled = isChecked;
                    storagePref.put(activity, "pref_university_buildings_campus", markersCampusEnabled);
                    displayMarkers();
                }));
            }
            View list = container.findViewById(R.id.view_list);
            if (list != null) {
                list.setOnClickListener(v -> openList());
            }
            // its ok
            SupportMapFragment mapFragment = (SupportMapFragment) fragment.getChildFragmentManager().findFragmentById(R.id.buildings_map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        }, throwable -> {
            log.exception(throwable);
            indicatorFailed();
            load();
        });
    }


    private void load() {
        thread.run(UB, () -> {
            load(storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)
                    ? Integer.parseInt(storagePref.get(activity, "pref_static_refresh", "168"))
                    : 0);
        });
    }

    private void load(int refresh_rate) {
        thread.run(UB, () -> {
            log.v(TAG, "load | refresh_rate=", refresh_rate);
            if (!(storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false))) {
                load(false);
                return;
            }
            UBuildings cache = getFromCache();
            if (cache == null) {
                load(false);
                return;
            }
            uBuildings = cache;
            if (cache.getTimestamp() + refresh_rate * 3600000L < time.getTimeInMillis()) {
                load(true);
            } else {
                load(false);
            }
        }, throwable -> {
            indicatorLoadFailed();
        });
    }

    private void load(boolean force) {
        thread.run(UB, () -> {
            log.v(TAG, "load | force=", force);
            if ((!force || Client.isOffline(activity)) && uBuildings != null) {
                display();
                return;
            }
            if (App.OFFLINE_MODE) {
                indicatorLoadOffline();
                return;
            }
            loadProvider(new RestResponseHandler<UBuildings>() {
                @Override
                public void onSuccess(int code, Client.Headers headers, UBuildings response) throws Exception {
                    if (code == 200 && response != null) {
                        response.setTimestamp(time.getTimeInMillis());
                        if (storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                            storage.put(activity, Storage.CACHE, Storage.GLOBAL, "university#buildings", response.toJsonString());
                        }
                        uBuildings = response;
                        display();
                        return;
                    }
                    indicatorLoadFailed();
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    log.v(TAG, "forceLoad | failure ", state);
                    if (state == Client.FAILED_OFFLINE) {
                        indicatorLoadOffline();
                        return;
                    }
                    indicatorLoadFailed();
                }
                @Override
                public void onProgress(int state) {
                    log.v(TAG, "forceLoad | progress ", state);
                    indicatorLoading();
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
                @Override
                public UBuildings newInstance() {
                    return new UBuildings();
                }
            });
        }, throwable -> {
            indicatorLoadFailed();
        });
    }

    private void loadProvider(RestResponseHandler<UBuildings> handler) {
        log.v(TAG, "loadProvider");
        ifmoRestClient.get(activity, "building_map", null, handler);
    }

    private void display() {
        if (uBuildings == null) {
            indicatorLoadFailed();
            return;
        }
        indicatorLoaded();
        displayMarkers();
    }

    private void displayMarkers() {
        thread.run(UB, () -> {
            if (uBuildings == null || googleMap == null) {
                return;
            }
            if (uBuildings.getBuildings() == null) {
                return;
            }
            removeAllMarkers();
            for (UBuilding building : uBuildings.getBuildings()) {
                try {
                    if (building == null) {
                        continue;
                    }
                    String type = null;
                    int icon = -1;
                    switch (building.getTypeId()) {
                        case 1:
                            if (!markersCampusEnabled) {
                                continue;
                            }
                            type = activity.getString(R.string.campus);
                            icon = R.drawable.location_campus;
                            break;
                        case 2:
                            if (!markersDormitoryEnabled) {
                                continue;
                            }
                            type = activity.getString(R.string.dormitory);
                            icon = R.drawable.location_dormitory;
                            break;
                    }
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(new LatLng(building.getN(), building.getE()));
                    markerOptions.title(building.getTitle());
                    markerOptions.zIndex((float) (building.getMajor() + 1));
                    if (type != null) {
                        markerOptions.snippet(type);
                    }
                    if (icon != -1) {
                        markerOptions.icon(tintMarker(activity, icon));
                    }
                    thread.runOnUI(UB, () -> {
                        Marker marker = googleMap.addMarker(markerOptions);
                        marker.setTag(building);
                        markers.add(marker);
                    }, throwable -> {
                        log.exception(throwable);
                    });
                } catch (Exception e) {
                    log.exception(e);
                }
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private UBuildings getFromCache() {
        thread.assertNotUI();
        String cache = storage.get(activity, Storage.CACHE, Storage.GLOBAL, "university#buildings").trim();
        if (StringUtils.isBlank(cache)) {
            return null;
        }
        try {
            return new UBuildings().fromJsonString(cache);
        } catch (Exception e) {
            storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "university#buildings");
            return null;
        }
    }


    @Override
    public void onMapReady(GoogleMap gMap) {
        thread.run(UB, () -> {
            MapStyleOptions mapStyleOptions;
            switch (theme.getAppTheme(activity)) {
                case "light":
                default: mapStyleOptions = MapStyleOptions.loadRawResourceStyle(activity, R.raw.google_map_light); break;
                case "dark": mapStyleOptions = MapStyleOptions.loadRawResourceStyle(activity, R.raw.google_map_dark); break;
                case "black": mapStyleOptions = MapStyleOptions.loadRawResourceStyle(activity, R.raw.google_map_black); break;
                case "white": mapStyleOptions = MapStyleOptions.loadRawResourceStyle(activity, R.raw.google_map_white); break;
            }
            thread.runOnUI(UB, () -> {
                googleMap = gMap;
                googleMap.setMapStyle(mapStyleOptions);
                googleMap.setOnMarkerClickListener(this);
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(59.93465, 30.3138391)).zoom(10).build()));
                if (uBuildings == null) {
                    load();
                } else {
                    display();
                }
            }, throwable -> {
                indicatorFailed();
            });
        }, throwable -> {
            indicatorFailed();
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        try {
            UBuilding building = (UBuilding) marker.getTag();
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

    private void removeAllMarkers() {
        thread.runOnUI(UB, () -> {
            for (Marker marker : markers) {
                marker.remove();
            }
            markers.clear();
        });
    }

    private boolean openMarkerInfo(UBuilding building) {
        try {
            closeMarkerInfo();
            closeList();
            ViewGroup markerContainer = container.findViewById(R.id.marker_info_container);
            if (markerContainer != null) {
                View marker = inflate(R.layout.layout_university_buildings_marker_info);
                ((TextView) marker.findViewById(R.id.marker_text)).setText(escapeText(building.getTitle()));
                marker.findViewById(R.id.web).setOnClickListener(view -> thread.run(UB, () -> {
                    Uri uri = Uri.parse("http://www.ifmo.ru/ru/map/" + building.getId() + "/");
                    eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, uri)));
                }));
                new Picasso.Builder(activity).build()
                        .load(building.getImage())
                        .error(R.drawable.ic_sentiment_very_satisfied)
                        .transform(new CircularTransformation())
                        .into((ImageView) marker.findViewById(R.id.marker_image));
                marker.findViewById(R.id.marker_close).setOnClickListener(v -> closeMarkerInfo());
                markerContainer.addView(marker);
            }
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    private void closeMarkerInfo() {
        thread.runOnUI(UB, () -> {
            View markerContainer = container.findViewById(R.id.marker_info_container);
            if (markerContainer != null) {
                View marker = markerContainer.findViewById(R.id.marker_info);
                if (marker != null) {
                    staticUtil.removeView(marker);
                }
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void openList() {
        thread.runOnUI(UB, () -> {
            closeMarkerInfo();
            closeList();
            if (uBuildings == null) {
                return;
            }
            ViewGroup markerContainer = container.findViewById(R.id.marker_info_container);
            if (markerContainer == null || uBuildings.getBuildings() == null) {
                return;
            }
            View list = inflate(R.layout.layout_university_buildings_list);
            list.findViewById(R.id.list_close).setOnClickListener(v -> closeList());
            ViewGroup listContainer = list.findViewById(R.id.list_container);
            for (UBuilding building : uBuildings.getBuildings()) {
                try {
                    if (building == null) {
                        continue;
                    }
                    View item = inflate(R.layout.layout_university_faculties_divisions_list_item);
                    ((TextView) item.findViewById(R.id.title)).setText(building.getTitle());
                    item.setOnClickListener(v -> {
                        thread.runOnUI(UB, () -> {
                            for (Marker marker : markers) {
                                UBuilding b = (UBuilding) marker.getTag();
                                if (b == null) {
                                    continue;
                                }
                                if (Objects.equals(building.getTitle(), b.getTitle())) {
                                    closeList();
                                    onMarkerClick(marker);
                                    break;
                                }
                            }
                        });
                    });
                    listContainer.addView(item);
                } catch (Exception e) {
                    log.exception(e);
                }
            }
            markerContainer.addView(list);
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void closeList() {
        thread.runOnUI(UB, () -> {
            View markerContainer = container.findViewById(R.id.marker_info_container);
            if (markerContainer != null) {
                View list = markerContainer.findViewById(R.id.list);
                if (list != null) {
                    staticUtil.removeView(list);
                }
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void zoomToMarker(Marker marker) {
        thread.runOnUI(UB, () -> {
            CameraPosition cameraPosition = new CameraPosition.Builder().target(marker.getPosition()).zoom(15).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
        });
    }

    private BitmapDescriptor tintMarker(Context context, @DrawableRes int icon) {
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


    private void indicatorLoading() {
        thread.runOnUI(UB, () -> {
            ViewGroup vg = container.findViewById(R.id.status);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(R.layout.layout_university_buildings_status_loading));
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void indicatorLoadFailed() {
        thread.runOnUI(UB, () -> {
            View view = inflate(R.layout.layout_university_buildings_status_image);
            view.setOnClickListener(v -> load());
            ImageView image = view.findViewById(R.id.image);
            image.setImageResource(R.drawable.ic_warning);
            ViewGroup vg = container.findViewById(R.id.status);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(view);
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void indicatorLoadOffline() {
        thread.runOnUI(UB, () -> {
            View view = inflate(R.layout.layout_university_buildings_status_image);
            ImageView image = view.findViewById(R.id.image);
            image.setImageResource(R.drawable.ic_disconnected);
            ViewGroup vg = container.findViewById(R.id.status);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(view);
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void indicatorFailed() {
        thread.runOnUI(UB, () -> {
            View view = inflate(R.layout.layout_university_buildings_status_image);
            ImageView image = view.findViewById(R.id.image);
            image.setImageResource(R.drawable.ic_warning);
            ViewGroup vg = container.findViewById(R.id.status);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(view);
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void indicatorLoaded() {
        thread.runOnUI(UB, () -> {
            ViewGroup vg = container.findViewById(R.id.status);
            if (vg != null) {
                vg.removeAllViews();
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }


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

    private boolean isNotAddedToActivity() {
        try {
            if (!thread.assertUI()) {
                return true;
            }
            if (fragment == null) {
                return true;
            }
            FragmentManager fragmentManager = fragment.getFragmentManager();
            if (fragmentManager == null) {
                return true;
            }
            fragmentManager.executePendingTransactions();
            return !fragment.isAdded();
        } catch (Throwable throwable) {
            if (!(throwable instanceof IllegalStateException)) {
                log.exception(throwable);
            }
            return false;
        }
    }

    @NonNull
    private View inflate(@LayoutRes int layout) throws InflateException {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(layout, null);
    }
}
