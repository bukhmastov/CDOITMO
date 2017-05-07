package com.bukhmastov.cdoitmo.objects;

import android.content.Context;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class Rating {

    private static final String TAG = "Rating";
    private Context context;
    private JSONObject rating = null;
    private JSONObject ratingList = null;

    public Rating(Context context){
        this.context = context;
        String rating = Storage.file.cache.get(context, "rating#core");
        if (!rating.isEmpty()) {
            try {
                this.rating = new JSONObject(rating);
            } catch (Exception e) {
                Static.error(e);
            }
        }
        String rating_list = Storage.file.cache.get(context, "rating#list");
        if (!rating_list.isEmpty()) {
            try {
                this.ratingList = new JSONObject(rating_list);
            } catch (Exception e) {
                Static.error(e);
            }
        }
    }
    public void put(String type, JSONObject data){
        Log.v(TAG, "put");
        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", Calendar.getInstance().getTimeInMillis());
            json.put("date", new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
            json.put("rating", data);
            if (Objects.equals(type, "Rating")) {
                rating = json;
                Storage.file.cache.put(context, "rating#core", rating.toString());
            } else {
                ratingList = json;
                Storage.file.cache.put(context, "rating#list", ratingList.toString());
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    public JSONObject get(String type){
        Log.v(TAG, "get");
        if(Objects.equals(type, "Rating")){
            return rating;
        } else {
            return ratingList;
        }
    }
    public boolean is(String type){
        Log.v(TAG, "is");
        if(Objects.equals(type, "Rating")){
            return this.rating != null;
        } else {
            return this.ratingList != null;
        }
    }
}
