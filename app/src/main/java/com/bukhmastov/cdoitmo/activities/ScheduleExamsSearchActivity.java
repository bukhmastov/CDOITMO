package com.bukhmastov.cdoitmo.activities;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragments.ScheduleExamsFragment;
import com.bukhmastov.cdoitmo.objects.entities.Suggestion;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ScheduleExamsSearchActivity extends SearchActivity {

    private static final int numberOfSuggestions = 3;
    private static final int maxCountOfSuggestionsToStore = 100;

    @Override
    String getHint() {
        return getString(R.string.schedule_exams_search_view_hint);
    }

    @Override
    ArrayList<Suggestion> getSuggestions(String query) {
        try {
            ArrayList<Suggestion> suggestions = new ArrayList<>();
            String recentString = Storage.file.perm.get(this, "schedule_exams#recent");
            JSONArray recent;
            if (recentString.isEmpty()) {
                recent = new JSONArray();
            } else {
                recent = new JSONArray(recentString);
            }
            int counter = 0;
            for (int i = 0; i < recent.length(); i++) {
                String item = recent.getString(i);
                if (query.isEmpty() || item.contains(query)) {
                    counter++;
                    suggestions.add(new Suggestion(item, item, R.drawable.ic_access_time));
                }
                if (counter >= numberOfSuggestions) break;
            }
            ArrayList<String> cachedFiles = Storage.file.cache.list(this, "schedule_exams#lessons");
            for (String file : cachedFiles) {
                String cachedFile = Storage.file.cache.get(this, "schedule_exams#lessons#" + file);
                if (!cachedFile.isEmpty()) {
                    try {
                        JSONObject object = new JSONObject(cachedFile);
                        if (query.isEmpty() || object.getString("scope").contains(query)) {
                            suggestions.add(new Suggestion(object.getString("scope"), object.getString("scope"), R.drawable.ic_save));
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
    void onDone(String query, String label) {
        try {
            String recentString = Storage.file.perm.get(this, "schedule_exams#recent");
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
            Storage.file.perm.put(this, "schedule_exams#recent", recent.toString());
        } catch (Exception e) {
            Static.error(e);
            Storage.file.perm.delete(this, "schedule_exams#recent");
        }
        if (ScheduleExamsFragment.scheduleExams != null) {
            ScheduleExamsFragment.scheduleExams.search(query);
        }
    }
}
