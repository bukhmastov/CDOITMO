package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.CircularTransformation;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.RequestHandle;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class UniversityBuildingsFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "UniversityBuildingsFragment";
    private Bundle savedInstanceState;
    private Activity activity;
    private View container;
    private RequestHandle fragmentRequestHandle = null;
    private JSONObject building_map = null;
    private MapView mapView = null;
    private GoogleMap googleMap = null;
    private ArrayList<Marker> markers = new ArrayList<>();
    private boolean markers_campus = true;
    private boolean markers_dormitory = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        activity = getActivity();
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        this.savedInstanceState = savedInstanceState;
        markers_campus = Storage.pref.get(getContext(), "pref_university_buildings_campus", true);
        markers_dormitory = Storage.pref.get(getContext(), "pref_university_buildings_dormitory", true);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        if (building_map == null) {
            forceLoad();
        }
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
        if (fragmentRequestHandle != null) {
            fragmentRequestHandle.cancel(true);
        }
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        savedInstanceState = outState;
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup cont, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_university_tab, cont, false);
        container = view.findViewById(R.id.university_tab_container);
        return view;
    }

    private void forceLoad() {
        IfmoRestClient.get(getContext(), "building_map", null, new IfmoRestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, JSONObject json, JSONArray responseArr) {
                if (statusCode == 200) {
                    building_map = json;
                    display();
                } else {
                    loadFailed();
                }
            }
            @Override
            public void onProgress(int state) {
                Log.v(TAG, "forceLoad | progress " + state);
                draw(R.layout.state_loading);
                if (activity != null) {
                    TextView loading_message = (TextView) container.findViewById(R.id.loading_message);
                    if (loading_message != null) {
                        switch (state) {
                            case IfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(int state) {
                Log.v(TAG, "forceLoad | failure " + state);
                switch (state) {
                    case IfmoRestClient.FAILED_OFFLINE:
                        draw(R.layout.state_offline);
                        if (activity != null) {
                            View offline_reload = container.findViewById(R.id.offline_reload);
                            if (offline_reload != null) {
                                offline_reload.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        forceLoad();
                                    }
                                });
                            }
                        }
                        break;
                    case IfmoRestClient.FAILED_TRY_AGAIN:
                        draw(R.layout.state_try_again);
                        if (activity != null) {
                            View try_again_reload = container.findViewById(R.id.try_again_reload);
                            if (try_again_reload != null) {
                                try_again_reload.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        forceLoad();
                                    }
                                });
                            }
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
    private void loadFailed(){
        Log.v(TAG, "loadFailed");
        try {
            draw(R.layout.state_try_again);
            TextView try_again_message = (TextView) container.findViewById(R.id.try_again_message);
            if (try_again_message != null) try_again_message.setText(R.string.load_failed);
            View try_again_reload = container.findViewById(R.id.try_again_reload);
            if (try_again_reload != null) {
                try_again_reload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        forceLoad();
                    }
                });
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }

    @Override
    public void onMapReady(GoogleMap gMap) {
        googleMap = gMap;
        displayMarkers();
        googleMap.setOnMarkerClickListener(this);
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(59.93465, 30.3138391)).zoom(10).build()));
    }
    private void display() {
        draw(R.layout.layout_university_buildings);
        MapView mapView = (MapView) container.findViewById(R.id.buildings_map);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.onResume();
            mapView.getMapAsync(this);
            Switch dormitory_switch = (Switch) container.findViewById(R.id.dormitory_switch);
            if (dormitory_switch != null) {
                dormitory_switch.setChecked(markers_dormitory);
                dormitory_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        markers_dormitory = isChecked;
                        displayMarkers();
                        Storage.pref.put(getContext(), "pref_university_buildings_dormitory", markers_dormitory);
                    }
                });
            }
            Switch campus_switch = (Switch) container.findViewById(R.id.campus_switch);
            if (campus_switch != null) {
                campus_switch.setChecked(markers_campus);
                campus_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        markers_campus = isChecked;
                        displayMarkers();
                        Storage.pref.put(getContext(), "pref_university_buildings_campus", markers_campus);
                    }
                });
            }
            View view_list = container.findViewById(R.id.view_list);
            if (view_list != null) {
                view_list.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openList();
                    }
                });
            }
        }
    }

    private void displayMarkers() {
        try {
            JSONArray list = building_map.getJSONArray("list");
            if (list != null) {
                removeAllMarkers();
                for (int i = 0; i < list.length(); i++) {
                    try {
                        JSONObject building = list.getJSONObject(i);
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
                                type = getString(R.string.campus);
                                icon = R.drawable.location_campus;
                                break;
                            case 2:
                                if (!markers_dormitory) {
                                    continue;
                                }
                                type = getString(R.string.dormitory);
                                icon = R.drawable.location_dormitory;
                                break;
                        }
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(new LatLng(building.getDouble("N"), building.getDouble("E")));
                        markerOptions.title(building.getString("title"));
                        markerOptions.zIndex((float) (building.getInt("major") + 1));
                        if (type != null) {
                            markerOptions.snippet(type);
                        }
                        if (icon != -1) {
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(icon));
                        }
                        Marker marker = googleMap.addMarker(markerOptions);
                        marker.setTag(building);
                        markers.add(marker);
                    } catch (Exception e) {
                        Static.error(e);
                    }
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void removeAllMarkers() {
        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();
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
            Static.error(e);
            return false;
        }
    }
    private boolean openMarkerInfo(JSONObject building) {
        try {
            closeMarkerInfo();
            closeList();
            ViewGroup marker_info_container = (ViewGroup) container.findViewById(R.id.marker_info_container);
            if (marker_info_container != null) {
                View marker_info = inflate(R.layout.layout_university_buildings_marker_info);
                String title = escapeText(building.getString("title"));
                String text = escapeText(building.getString("text"));
                String image = building.getString("image");
                ((TextView) marker_info.findViewById(R.id.marker_text)).setText(title);
                ((TextView) marker_info.findViewById(R.id.marker_desc)).setText(text);
                Picasso.with(getContext())
                        .load(image)
                        .error(R.drawable.ic_sentiment_very_satisfied)
                        .transform(new CircularTransformation())
                        .into((ImageView) marker_info.findViewById(R.id.marker_image));
                marker_info.findViewById(R.id.marker_close).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        closeMarkerInfo();
                    }
                });
                marker_info_container.addView(marker_info);
            }
            return true;
        } catch (Exception e) {
            Static.error(e);
            return false;
        }
    }
    private void closeMarkerInfo() {
        try {
            View marker_info_container = container.findViewById(R.id.marker_info_container);
            if (marker_info_container != null) {
                View marker_info = marker_info_container.findViewById(R.id.marker_info);
                if (marker_info != null) {
                    Static.removeView(marker_info);
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void openList() {
        try {
            closeMarkerInfo();
            closeList();
            ViewGroup marker_info_container = (ViewGroup) container.findViewById(R.id.marker_info_container);
            JSONArray list = building_map.getJSONArray("list");
            if (marker_info_container != null && list != null) {
                View layout_university_buildings_list = inflate(R.layout.layout_university_buildings_list);
                layout_university_buildings_list.findViewById(R.id.list_close).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        closeList();
                    }
                });
                ViewGroup list_container = (ViewGroup) layout_university_buildings_list.findViewById(R.id.list_container);
                for (int i = 0; i < list.length(); i++) {
                    try {
                        JSONObject building = list.getJSONObject(i);
                        if (building == null) {
                            continue;
                        }
                        final String title = building.getString("title");
                        View item = inflate(R.layout.layout_university_faculties_divisions_list_item);
                        ((TextView) item.findViewById(R.id.title)).setText(title);
                        item.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                for (Marker marker : markers) {
                                    try {
                                        JSONObject building = (JSONObject) marker.getTag();
                                        if (building == null) {
                                            continue;
                                        }
                                        if (Objects.equals(building.getString("title"), title)) {
                                            closeList();
                                            onMarkerClick(marker);
                                            break;
                                        }
                                    } catch (Exception e) {
                                        // norm
                                    }
                                }
                            }
                        });
                        list_container.addView(item);
                    } catch (Exception e) {
                        Static.error(e);
                    }
                }
                marker_info_container.addView(layout_university_buildings_list);
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void closeList() {
        try {
            View marker_info_container = container.findViewById(R.id.marker_info_container);
            if (marker_info_container != null) {
                View list = marker_info_container.findViewById(R.id.list);
                if (list != null) {
                    Static.removeView(list);
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void zoomToMarker(Marker marker) {
        CameraPosition cameraPosition = new CameraPosition.Builder().target(marker.getPosition()).zoom(15).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
    }

    private String escapeText(String text) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                text = android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString();
            } else {
                text = android.text.Html.fromHtml(text).toString();
            }
        } catch (Exception e) {
            Static.error(e);
        }
        return text.trim();
    }

    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) container);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(@LayoutRes int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
