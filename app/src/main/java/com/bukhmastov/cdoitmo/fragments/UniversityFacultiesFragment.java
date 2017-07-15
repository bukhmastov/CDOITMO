package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.UniversityPersonCardActivity;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DownloadImage;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
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
    private boolean first_block = true;

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
        String dep_id = "";
        if (stack.size() > 0) {
            dep_id = stack.get(stack.size() - 1);
        }
        IfmoRestClient.get(getContext(), "study_structure" + (dep_id.isEmpty() ? "" : "/" + dep_id), null, new IfmoRestClientResponseHandler() {
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
            first_block = true;
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
            // структура подразделения
            displayStructure(structure);
            // дочерние подразделения
            displayDivisions(divisions);
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) container.findViewById(R.id.swipe_layout);
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
    private void displayStructure(JSONObject structure) throws Exception {
        if (structure != null) {
            boolean exists;
            final JSONObject info = getJsonObject(structure, "info");
            if (info != null) {
                // основная информация
                exists = false;
                ViewGroup structure_common = (ViewGroup) container.findViewById(R.id.structure_common);
                final String address = getString(info, "adres");
                final String phone = getString(info, "phone");
                final String site = getString(info, "site");
                if (address != null && !address.trim().isEmpty()) {
                    exists = true;
                    structure_common.addView(getConnectContainer(R.drawable.ic_location, address.trim(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // @TODO link to the map tab
                        }
                    }));
                }
                if (phone != null && !phone.trim().isEmpty()) {
                    exists = true;
                    structure_common.addView(getConnectContainer(R.drawable.ic_phone, phone.trim(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.trim()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }));
                }
                if (site != null && !site.trim().isEmpty()) {
                    exists = true;
                    structure_common.addView(getConnectContainer(R.drawable.ic_web, site.trim(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(site.trim())));
                        }
                    }));
                }
                if (!exists) {
                    Static.removeView(structure_common);
                } else {
                    if (structure_common.getChildCount() > 2) {
                        Static.removeView(structure_common.getChildAt(structure_common.getChildCount() - 1).findViewById(R.id.separator));
                    }
                    removeFirstSeparator(structure_common);
                }
                // деканат
                exists = false;
                ViewGroup structure_deanery = (ViewGroup) container.findViewById(R.id.structure_deanery);
                final String deanery_address = getString(info, "dekanat_adres");
                final String deanery_phone = getString(info, "dekanat_phone");
                final String deanery_email = getString(info, "dekanat_email");
                if (deanery_address != null && !deanery_address.trim().isEmpty()) {
                    exists = true;
                    structure_deanery.addView(getConnectContainer(R.drawable.ic_location, deanery_address.trim(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // @TODO link to the map tab
                        }
                    }));
                }
                if (deanery_phone != null && !deanery_phone.trim().isEmpty()) {
                    exists = true;
                    structure_deanery.addView(getConnectContainer(R.drawable.ic_phone, deanery_phone.trim(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + deanery_phone.trim()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }));
                }
                if (deanery_email != null && !deanery_email.trim().isEmpty()) {
                    exists = true;
                    structure_deanery.addView(getConnectContainer(R.drawable.ic_email, deanery_email.trim(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent emailIntent = new Intent(Intent.ACTION_SEND);
                            emailIntent.setType("message/rfc822");
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{deanery_email.trim()});
                            startActivity(Intent.createChooser(emailIntent, getString(R.string.send_mail) + "..."));
                        }
                    }));
                }
                if (!exists) {
                    Static.removeView(structure_deanery);
                } else {
                    if (structure_deanery.getChildCount() > 2) {
                        Static.removeView(structure_deanery.getChildAt(structure_deanery.getChildCount() - 1).findViewById(R.id.separator));
                    }
                    removeFirstSeparator(structure_deanery);
                }
                // глава
                exists = false;
                ViewGroup structure_head = (ViewGroup) container.findViewById(R.id.structure_head);
                final String head_post = getString(info, "person_post");
                final String head_lastname = getString(info, "lastname");
                final String head_firstname = getString(info, "firstname");
                final String head_middlename = getString(info, "middlename");
                final String head_avatar = getString(info, "person_avatar");
                final String head_degree = getString(info, "person_degree");
                final String head_email = getString(info, "email");
                final int head_pid = getInt(info, "ifmo_person_id");
                if (head_post != null && !head_post.trim().isEmpty()) {
                    ((TextView) structure_head.findViewById(R.id.structure_head_title)).setText(Static.capitalizeFirstLetter(head_post));
                }
                if (head_lastname != null && !head_lastname.trim().isEmpty() &&
                        head_firstname != null && !head_firstname.trim().isEmpty() &&
                        head_middlename != null && !head_middlename.trim().isEmpty()) {
                    exists = true;
                    final View layout_university_persons_list_item = inflate(R.layout.layout_university_persons_list_item);
                    ((TextView) layout_university_persons_list_item.findViewById(R.id.name)).setText((head_lastname + " " + head_firstname + " " + head_middlename).trim());
                    if (head_degree != null && !head_degree.trim().isEmpty()) {
                        ((TextView) layout_university_persons_list_item.findViewById(R.id.post)).setText(head_degree);
                    } else {
                        Static.removeView(layout_university_persons_list_item.findViewById(R.id.post));
                    }
                    if (head_avatar != null && !head_avatar.trim().isEmpty()) {
                        new DownloadImage(new DownloadImage.response() {
                            @Override
                            public void finish(Bitmap bitmap) {
                                if (bitmap == null) return;
                                try {
                                    float destiny = getResources().getDisplayMetrics().density;
                                    float dimen = getResources().getDimension(R.dimen.university_person_card_big_avatar);
                                    bitmap = Static.createSquaredBitmap(bitmap);
                                    bitmap = Static.getResizedBitmap(bitmap, (int) (dimen * destiny), (int) (dimen * destiny));
                                    RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                                    drawable.setCornerRadius((dimen / 2) * destiny);
                                    ((ImageView) layout_university_persons_list_item.findViewById(R.id.avatar)).setImageDrawable(drawable);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).execute(head_avatar);
                    }
                    layout_university_persons_list_item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getContext(), UniversityPersonCardActivity.class);
                            intent.putExtra("pid", head_pid);
                            startActivity(intent);
                        }
                    });
                    structure_head.addView(layout_university_persons_list_item);
                }
                if (head_email != null && !head_email.trim().isEmpty()) {
                    exists = true;
                    structure_head.addView(getConnectContainer(R.drawable.ic_email, head_email.trim(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent emailIntent = new Intent(Intent.ACTION_SEND);
                            emailIntent.setType("message/rfc822");
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{head_email.trim()});
                            startActivity(Intent.createChooser(emailIntent, getString(R.string.send_mail) + "..."));
                        }
                    }));
                    Static.removeView(structure_head.findViewById(R.id.separator));
                }
                if (!exists) {
                    Static.removeView(structure_head);
                } else {
                    removeFirstSeparator(structure_head);
                }
            } else {
                Static.removeView(container.findViewById(R.id.structure));
            }
        } else {
            Static.removeView(container.findViewById(R.id.structure));
        }
    }
    private void displayDivisions(JSONArray divisions) throws Exception {
        if (divisions != null && divisions.length() > 0) {
            if (stack.size() == 0) {
                Static.removeView(container.findViewById(R.id.divisions_list_title));
            }
            ViewGroup divisions_list = (ViewGroup) container.findViewById(R.id.divisions_list);
            for (int i = 0; i < divisions.length(); i++) {
                final JSONObject division = divisions.getJSONObject(i);
                final String title = getString(division, "name");
                final int cis_dep_id = getInt(division, "cis_dep_id");
                if (title != null && !title.trim().isEmpty()) {
                    View view = inflate(R.layout.layout_university_faculties_divisions_list_item);
                    ((TextView) view.findViewById(R.id.title)).setText(title);
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (cis_dep_id >= 0) {
                                stack.add(String.valueOf(cis_dep_id));
                                forceLoad();
                            }
                        }
                    });
                    divisions_list.addView(view);
                }
            }
            removeFirstSeparator(divisions_list);
        } else {
            Static.removeView(container.findViewById(R.id.divisions));
        }
    }
    private void removeFirstSeparator(ViewGroup section) {
        if (first_block) {
            first_block = false;
            if (section.getChildCount() > 0) {
                Static.removeView(section.getChildAt(0));
            }
        }
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

    private View getConnectContainer(@DrawableRes int icon, String text, View.OnClickListener listener) {
        View layout_university_connect = inflate(R.layout.layout_university_connect);
        ((ImageView) layout_university_connect.findViewById(R.id.connect_image)).setImageResource(icon);
        ((TextView) layout_university_connect.findViewById(R.id.connect_text)).setText(text.trim());
        layout_university_connect.setOnClickListener(listener);
        return layout_university_connect;
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
