package com.bukhmastov.cdoitmo.activities;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.objects.entities.Suggestion;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ScheduleLessonsSearchActivity extends SearchActivity {

    private static final String TAG = "SLSearchActivity";
    private static final int numberOfSuggestions = 3;
    private static final int maxCountOfSuggestionsToStore = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(this);
    }

    @Override
    String getHint() {
        Log.v(TAG, "getHint");
        return self.getString(R.string.schedule_lessons_search_view_hint);
    }

    @Override
    ArrayList<Suggestion> getSuggestions(String query) {
        Log.v(TAG, "getSuggestions | query=" + query);
        try {
            ArrayList<Suggestion> suggestions = new ArrayList<>();
            String recentString = Storage.file.perm.get(this, "schedule_lessons#recent");
            JSONArray recent;
            if (recentString.isEmpty()) {
                recent = new JSONArray();
            } else {
                recent = new JSONArray(recentString);
            }
            int counter = 0;
            for (int i = 0; i < recent.length(); i++) {
                String item = recent.getString(i);
                if (query.isEmpty() || contains(item, query)) {
                    counter++;
                    suggestions.add(new Suggestion(item, item, R.drawable.ic_access_time));
                }
                if (counter >= numberOfSuggestions) break;
            }
            ArrayList<String> cachedFiles = Storage.file.cache.list(this, "schedule_lessons#lessons");
            for (String file : cachedFiles) {
                String cachedFile = Storage.file.cache.get(this, "schedule_lessons#lessons#" + file);
                if (!cachedFile.isEmpty()) {
                    try {
                        JSONObject object = new JSONObject(cachedFile);
                        if (query.isEmpty() || contains(object.getString("label"), query)) {
                            suggestions.add(new Suggestion(object.getString("query"), object.getString("label"), R.drawable.ic_save));
                        }
                    } catch (Exception e) {
                        Static.error(e);
                    }
                }
            }
            return suggestions;
        } catch (Exception e) {
            Static.error(e);
            return null;
        }
    }

    @Override
    void onDone(final String query, final String label) {
        final ScheduleLessonsSearchActivity self = this;
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "onDone | query=" + query + " | label=" + label);
                try {
                    String recentString = Storage.file.perm.get(self, "schedule_lessons#recent");
                    JSONArray recent;
                    if (recentString.isEmpty()) {
                        recent = new JSONArray();
                    } else {
                        recent = new JSONArray(recentString);
                    }
                    for (int i = 0; i < recent.length(); i++) {
                        if (recent.getString(i).equals(label)) {
                            recent.remove(i);
                            break;
                        }
                    }
                    for (int i = recent.length() - 1; i >= 0; i--) {
                        recent.put(i + 1, recent.getString(i));
                    }
                    recent.put(0, label);
                    if (recent.length() > maxCountOfSuggestionsToStore) {
                        for (int i = maxCountOfSuggestionsToStore; i < recent.length(); i++) {
                            recent.remove(i);
                        }
                    }
                    Storage.file.perm.put(self, "schedule_lessons#recent", recent.toString());
                } catch (Exception e) {
                    Static.error(e);
                    Storage.file.perm.delete(self, "schedule_lessons#recent");
                }
                ScheduleLessonsFragment.searchAndClear(query);
            }
        });
    }

    private boolean contains(String first, String second) {
        return first.toLowerCase().contains(second.toLowerCase()) || Static.Translit.cyr2lat(first).toLowerCase().contains(Static.Translit.cyr2lat(second).toLowerCase());
    }
}
