package com.bukhmastov.cdoitmo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RatingFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingFragment";
    private SharedPreferences sharedPreferences;
    public static Rating rating = null;
    private boolean notifyAboutDateUpdate = false;
    private boolean loaded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rating = new Rating(getActivity());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rating, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!loaded) {
            loaded = true;
            forceLoad();
        }
    }

    @Override
    public void onRefresh() {
        forceLoad();
    }

    private void load(){
        if(rating.is()){
            display();
        } else {
            forceLoad();
        }
    }
    private void forceLoad(){
        notifyAboutDateUpdate = true;
        DeIfmoRestClient.get("servlet/distributedCDE?Rule=REP_EXECUTE_PRINT&REP_ID=1441", null, new DeIfmoRestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                if(statusCode == 200){
                    new RatingParse(new RatingParse.response() {
                        @Override
                        public void finish(JSONObject json) {
                            rating.put(json);
                            display();
                        }
                    }).execute(response);
                } else {
                    if(rating.is()){
                        display();
                    } else {
                        loadFailed();
                    }
                }
            }
            @Override
            public void onProgress(int state) {
                draw(R.layout.state_loading);
                TextView loading_message = (TextView) getActivity().findViewById(R.id.loading_message);
                switch(state){
                    case DeIfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                    case DeIfmoRestClient.STATE_AUTHORIZATION: loading_message.setText(R.string.authorization); break;
                    case DeIfmoRestClient.STATE_AUTHORIZED: loading_message.setText(R.string.authorized); break;
                }
            }
            @Override
            public void onFailure(int state) {
                switch(state){
                    case DeIfmoRestClient.FAILED_OFFLINE:
                        draw(R.layout.state_offline);
                        getActivity().findViewById(R.id.offline_reload).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                forceLoad();
                            }
                        });
                        break;
                    case DeIfmoRestClient.FAILED_TRY_AGAIN:
                    case DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN:
                        draw(R.layout.state_try_again);
                        if(state == DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN) ((TextView) getActivity().findViewById(R.id.try_again_message)).setText(R.string.auth_failed);
                        getActivity().findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                forceLoad();
                            }
                        });
                        break;
                    case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED); break;
                    case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED); break;
                }
            }
        });
    }
    private void loadFailed(){
        draw(R.layout.state_try_again);
        getActivity().findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forceLoad();
            }
        });
    }
    void display(){
        try {
            JSONObject data = rating.get();
            if(data == null) throw new NullPointerException("Rating.rating can't be null");
            // получаем список для отображения
            final ArrayList<HashMap<String, String>> courses = new ArrayList<>();
            JSONArray jsonArray = data.getJSONObject("rating").getJSONArray("courses");
            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("name", jsonObject.getString("faculty") + " - " + jsonObject.getInt("course") + " " + getString(R.string.course));
                hashMap.put("position", jsonObject.getString("position"));
                courses.add(hashMap);
            }
            // отображаем интерфейс
            draw(R.layout.rating_layout);
            // работаем со списком
            ListView rl_list_view = (ListView) getActivity().findViewById(R.id.rl_list_view);
            rl_list_view.setAdapter(new RatingListView(getActivity(), courses));
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
            mSwipeRefreshLayout.setColorSchemeColors(getActivity().getResources().getColor(R.color.colorPrimaryLight), getActivity().getResources().getColor(R.color.colorPrimary), getActivity().getResources().getColor(R.color.colorPrimaryDark));
            mSwipeRefreshLayout.setOnRefreshListener(this);
            // показываем снекбар с датой обновления
            if(notifyAboutDateUpdate){
                int shift = (int)((Calendar.getInstance().getTimeInMillis() - data.getLong("timestamp")) / 1000);
                String message;
                if(shift < 21600){
                    if(shift < 3600) {
                        message = shift / 60 + " " + "мин. назад";
                    } else {
                        message = shift / 3600 + " " + "час. назад";
                    }
                } else {
                    message = data.getString("date");
                }
                if(shift >= 60) Snackbar.make(getActivity().findViewById(R.id.protocol_layout), getString(R.string.update_date) + " " + message, Snackbar.LENGTH_SHORT).show();
                notifyAboutDateUpdate = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            loadFailed();
        }
    }
    void gotoLogin(int state){
        LoginActivity.state = state;
        getActivity().finish();
    }
    private void draw(int layoutId){
        ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_rating));
        vg.removeAllViews();
        vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }
}

class Rating {

    private static final String TAG = "Rating";
    private SharedPreferences sharedPreferences;
    private JSONObject rating = null;

    Rating(Context context){
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String protocol = this.sharedPreferences.getString("Rating", "");
        if(!Objects.equals(protocol, "")){
            try {
                this.rating = new JSONObject(protocol);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    void put(JSONObject data){
        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", Calendar.getInstance().getTimeInMillis());
            json.put("date", new SimpleDateFormat("dd.MM.YYYY HH:mm", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
            json.put("rating", data);
            rating = json;
            SharedPreferences.Editor editor = this.sharedPreferences.edit();
            editor.putString("Rating", json.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    JSONObject get(){
        return rating;
    }
    boolean is(){
        return this.rating != null;
    }
}

class RatingParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    RatingParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            String response = params[0].replace("&nbsp;", " ");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(response);
            JSONObject json = new JSONObject();
            TagNode div = root.findElementByAttValue("class", "d_text", true, false);
            TagNode table = div.getElementListByAttValue("class", "d_table", true, false).get(1);
            List<? extends TagNode> rows = table.getAllElementsList(false).get(0).getAllElementsList(false);
            JSONArray courses = new JSONArray();
            for(TagNode row : rows){
                if(row.getText().toString().contains("Позиция")) continue;
                List<? extends TagNode> columns = row.getAllElementsList(false);
                JSONObject course = new JSONObject();
                course.put("faculty", columns.get(0).getText().toString().trim());
                course.put("course", Integer.parseInt(columns.get(1).getText().toString().trim()));
                course.put("position", columns.get(2).getText().toString().trim());
                courses.put(course);
            }
            json.put("courses", courses);
            return json;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}

class RatingListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> courses;

    RatingListView(Activity context, ArrayList<HashMap<String, String>> courses) {
        super(context, R.layout.listview_rating, courses);
        this.context = context;
        this.courses = courses;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> change = courses.get(position);
        View rowView;
        rowView = inflater.inflate(R.layout.listview_rating, null, true);
        TextView lv_rating_name = ((TextView) rowView.findViewById(R.id.lv_rating_name));
        TextView lv_rating_position = ((TextView) rowView.findViewById(R.id.lv_rating_position));
        lv_rating_name.setText(change.get("name"));
        lv_rating_position.setText(change.get("position"));
        return rowView;
    }
}