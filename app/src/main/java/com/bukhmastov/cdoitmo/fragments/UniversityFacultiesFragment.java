package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.FacultiesRecyclerViewAdapter;
import com.bukhmastov.cdoitmo.adapters.RecyclerViewOnScrollListener;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UniversityFacultiesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityFacultiesFragment";
    private Activity activity;
    private View container;
    private RequestHandle fragmentRequestHandle = null;
    private boolean loaded = false;
    private ArrayList<String> stack = new ArrayList<>();
    private FacultiesRecyclerViewAdapter facultiesRecyclerViewAdapter = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        activity = getActivity();
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
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
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        if (!loaded) {
            loaded = true;
            forceLoad();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup cont, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_university_tab, cont, false);
        container = view.findViewById(R.id.university_tab_container);
        return view;
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "refreshed");
        forceLoad();
    }

    private void forceLoad() {
        loadProvider(new IfmoRestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, JSONObject json, JSONArray responseArr) {
                if (statusCode == 200) {
                    display(json);
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
    private void loadProvider(IfmoRestClientResponseHandler handler) {
        loadProvider(handler, 0);
    }
    private void loadProvider(final IfmoRestClientResponseHandler handler, final int attempt) {
        Log.v(TAG, "loadProvider | attempt=" + attempt);
        String dep_id = "";
        if (stack.size() > 0) {
            dep_id = stack.get(stack.size() - 1);
        }
        IfmoRestClient.getPlain(getContext(), "study_structure" + (dep_id.isEmpty() ? "" : "/" + dep_id), null, new IfmoClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                try {
                    if (statusCode == 200) {
                        handler.onSuccess(statusCode, new JSONObject(response), null);
                    } else {
                        handler.onFailure(IfmoRestClient.FAILED_TRY_AGAIN);
                    }
                } catch (Exception e) {
                    if (attempt < 3) {
                        loadProvider(handler, attempt + 1);
                    } else {
                        handler.onFailure(IfmoRestClient.FAILED_TRY_AGAIN);
                    }
                }
            }
            @Override
            public void onProgress(int state) {
                handler.onProgress(state);
            }
            @Override
            public void onFailure(int state) {
                handler.onFailure(state);
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                handler.onNewHandle(requestHandle);
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

    private void display(JSONObject json) {
        try {
            JSONObject structure = getJsonObject(json, "structure");
            JSONArray divisions = getJsonArray(json, "divisions");
            draw(R.layout.layout_university_faculties);
            // заголовок
            if (stack.size() == 0 || structure == null) {
                ((ImageView) ((ViewGroup) container.findViewById(R.id.back)).getChildAt(0)).setImageResource(R.drawable.ic_refresh);
                container.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        forceLoad();
                    }
                });
                ((TextView) container.findViewById(R.id.title)).setText(R.string.division_general);
                Static.removeView(container.findViewById(R.id.link));
            } else {
                container.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stack.remove(stack.size() - 1);
                        forceLoad();
                    }
                });
                final String name = getString(structure, "name");
                final String link = getString(structure, "link");
                if (name != null && !name.trim().isEmpty()) {
                    ((TextView) container.findViewById(R.id.title)).setText(name.trim());
                } else {
                    Static.removeView(container.findViewById(R.id.title));
                }
                if (link != null && !link.trim().isEmpty()) {
                    ((TextView) container.findViewById(R.id.link)).setText(link.trim());
                    container.findViewById(R.id.departament_link).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link.trim())));
                        }
                    });
                } else {
                    Static.removeView(container.findViewById(R.id.link));
                }
            }
            // список
            facultiesRecyclerViewAdapter = new FacultiesRecyclerViewAdapter(getContext());
            final RecyclerView list = (RecyclerView) container.findViewById(R.id.list);
            list.setLayoutManager(new LinearLayoutManager(getContext()));
            list.setAdapter(facultiesRecyclerViewAdapter);
            list.addOnScrollListener(new RecyclerViewOnScrollListener(container));
            displayContent(structure, divisions);
            // добавляем отступ
            container.findViewById(R.id.top_panel).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        int height = container.findViewById(R.id.top_panel).getHeight();
                        RecyclerView list = (RecyclerView) container.findViewById(R.id.list);
                        list.setPadding(0, height, 0, 0);
                        list.scrollToPosition(0);
                        LinearLayout list_info = (LinearLayout) container.findViewById(R.id.list_info);
                        if (list_info.getChildCount() > 0) {
                            list_info.setPadding(0, height, 0, 0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) container.findViewById(R.id.list_swipe);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
        } catch (Exception e) {
            Static.error(e);
            loadFailed();
        }
    }
    private void displayContent(final JSONObject structure, final JSONArray divisions) throws Exception {
        if (facultiesRecyclerViewAdapter != null) {
            ArrayList<FacultiesRecyclerViewAdapter.Item> items = new ArrayList<>();
            if (structure != null) {
                final JSONObject info = getJsonObject(structure, "info");
                if (info != null) {
                    // основная информация
                    final String address = getString(info, "adres");
                    final String phone = getString(info, "phone");
                    final String site = getString(info, "site");
                    if (isValid(address) || isValid(phone) || isValid(site)) {
                        FacultiesRecyclerViewAdapter.Item item = new FacultiesRecyclerViewAdapter.Item();
                        item.type = FacultiesRecyclerViewAdapter.TYPE_UNIT_STRUCTURE_COMMON;
                        item.data = new JSONObject()
                                .put("header", getString(R.string.faculty_section_general))
                                .put("address", isValid(address) ? address : null)
                                .put("phone", isValid(phone) ? phone : null)
                                .put("site", isValid(site) ? site : null);
                        items.add(item);
                    }
                    // деканат
                    final String deanery_address = getString(info, "dekanat_adres");
                    final String deanery_phone = getString(info, "dekanat_phone");
                    final String deanery_email = getString(info, "dekanat_email");
                    if (isValid(deanery_address) || isValid(deanery_phone) || isValid(deanery_email)) {
                        FacultiesRecyclerViewAdapter.Item item = new FacultiesRecyclerViewAdapter.Item();
                        item.type = FacultiesRecyclerViewAdapter.TYPE_UNIT_STRUCTURE_DEANERY;
                        item.data = new JSONObject()
                                .put("header", getString(R.string.faculty_section_deanery))
                                .put("deanery_address", isValid(deanery_address) ? deanery_address : null)
                                .put("deanery_phone", isValid(deanery_phone) ? deanery_phone : null)
                                .put("deanery_email", isValid(deanery_email) ? deanery_email : null);
                        items.add(item);
                    }
                    // глава
                    final String head_post = getString(info, "person_post");
                    final String head_lastname = getString(info, "lastname");
                    final String head_firstname = getString(info, "firstname");
                    final String head_middlename = getString(info, "middlename");
                    final String head_avatar = getString(info, "person_avatar");
                    final String head_degree = getString(info, "person_degree");
                    final int head_pid = getInt(info, "ifmo_person_id");
                    final String head_email = getString(info, "email");
                    if (isValid(head_lastname) || isValid(head_firstname) || isValid(head_middlename) || isValid(head_email)) {
                        FacultiesRecyclerViewAdapter.Item item = new FacultiesRecyclerViewAdapter.Item();
                        item.type = FacultiesRecyclerViewAdapter.TYPE_UNIT_STRUCTURE_HEAD;
                        item.data = new JSONObject()
                                .put("header", isValid(head_post) ? head_post : getString(R.string.faculty_section_head))
                                .put("head_lastname", isValid(head_lastname) ? head_lastname : null)
                                .put("head_firstname", isValid(head_firstname) ? head_firstname : null)
                                .put("head_middlename", isValid(head_middlename) ? head_middlename : null)
                                .put("head_avatar", isValid(head_avatar) ? head_avatar : null)
                                .put("head_degree", isValid(head_degree) ? head_degree : null)
                                .put("head_pid", isValid(head_pid) ? head_pid : -1)
                                .put("head_email", isValid(head_email) ? head_email : null);
                        items.add(item);
                    }
                }
            }
            if (divisions != null && divisions.length() > 0) {
                // дивизионы
                JSONArray d = new JSONArray();
                for (int i = 0; i < divisions.length(); i++) {
                    final JSONObject division = divisions.getJSONObject(i);
                    d.put(new JSONObject()
                            .put("title", getString(division, "name"))
                            .put("id", getInt(division, "cis_dep_id"))
                    );
                }
                FacultiesRecyclerViewAdapter.Item item = new FacultiesRecyclerViewAdapter.Item();
                item.type = FacultiesRecyclerViewAdapter.TYPE_UNIT_DIVISIONS;
                item.data = new JSONObject()
                        .put("header", stack.size() == 0 ? null : getString(R.string.faculty_section_divisions))
                        .put("divisions", d);
                items.add(item);
                facultiesRecyclerViewAdapter.setOnDivisionClickListener(new FacultiesRecyclerViewAdapter.OnDivisionClickListener() {
                    @Override
                    public void onClick(int dep_id) {
                        stack.add(String.valueOf(dep_id));
                        forceLoad();
                    }
                });
            }
            facultiesRecyclerViewAdapter.addItem(items);
        }
    }
    private boolean isValid(String text) {
        return text != null && !text.trim().isEmpty();
    }
    private boolean isValid(int number) {
        return number >= 0;
    }

    private JSONObject getJsonObject(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (JSONObject) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    private JSONArray getJsonArray(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (JSONArray) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    private String getString(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (String) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    private int getInt(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            try {
                return json.getInt(key);
            } catch (Exception e) {
                return -1;
            }
        } else {
            return -1;
        }
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
