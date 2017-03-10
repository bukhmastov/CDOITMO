package com.bukhmastov.cdoitmo.objects;

import android.content.Context;

import com.bukhmastov.cdoitmo.utils.Cache;
import com.bukhmastov.cdoitmo.utils.Static;

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
        String protocol;
        protocol = Cache.get(context, "Rating");
        if(!Objects.equals(protocol, "")){
            try {
                this.rating = new JSONObject(protocol);
            } catch (Exception e) {
                Static.error(e);
            }
        }
        protocol = Cache.get(context, "RatingList");
        if(!Objects.equals(protocol, "")){
            try {
                this.ratingList = new JSONObject(protocol);
            } catch (Exception e) {
                Static.error(e);
            }
        }
    }
    public void put(String type, JSONObject data){
        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", Calendar.getInstance().getTimeInMillis());
            json.put("date", new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
            json.put("rating", data);
            if(Objects.equals(type, "Rating")){
                rating = json;
            } else {
                ratingList = json;
            }
            Cache.put(context, type, json.toString());
        } catch (Exception e) {
            Static.error(e);
        }
    }
    public JSONObject get(String type){
        if(Objects.equals(type, "Rating")){
            return rating;
        } else {
            return ratingList;
        }
    }
    public boolean is(String type){
        if(Objects.equals(type, "Rating")){
            return this.rating != null;
        } else {
            return this.ratingList != null;
        }
    }
}
