package com.bukhmastov.cdoitmo.activities.search;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.SuggestionsListView;
import com.bukhmastov.cdoitmo.objects.entities.Suggestion;
import com.bukhmastov.cdoitmo.utils.CtxWrapper;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";
    private static final int REQ_CODE_SPEECH_INPUT = 1337;
    protected final SearchActivity activity = this;
    private enum EXTRA_ACTION_MODE {Speech_recognition, Clear, None}
    private EditText search_edit_text;
    protected final int numberOfSuggestions;
    protected final int maxCountOfSuggestionsToStore;
    protected int currentNumberOfSuggestions = 0;
    protected boolean saveCurrentSuggestion = true;

    abstract protected String getType();
    abstract protected String getHint();
    abstract protected void onDone(String query);

    public SearchActivity(int numberOfSuggestions, int maxCountOfSuggestionsToStore) {
        this.numberOfSuggestions = numberOfSuggestions;
        this.maxCountOfSuggestionsToStore = maxCountOfSuggestionsToStore;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        switch (Static.getAppTheme(this)) {
            case "light":
            default: setTheme(R.style.AppTheme_Search); break;
            case "dark": setTheme(R.style.AppTheme_Search_Dark); break;
            case "white": setTheme(R.style.AppTheme_Search_White); break;
            case "black": setTheme(R.style.AppTheme_Search_Black); break;
        }
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created | type=", getType());
        setContentView(R.layout.activity_search);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Log.v(TAG, "onPostCreate | type=", getType());
        findViewById(R.id.search_close).setOnClickListener(v -> {
            Log.v(TAG, "type=", getType(), " | search_close clicked");
            finish();
        });
        search_edit_text = findViewById(R.id.search_edittext);
        search_edit_text.setHint(getHint());
        search_edit_text.addTextChangedListener(textWatcher);
        search_edit_text.setOnKeyListener(onKeyListener);
        setMode(EXTRA_ACTION_MODE.Speech_recognition);
        setSuggestions(getSuggestions(""));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed | type=", getType());
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CtxWrapper.wrap(context));
    }

    private void setMode(final EXTRA_ACTION_MODE mode) {
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "setMode | type=", getType(), " | mode=", mode.toString());
            final ViewGroup search_extra_action = findViewById(R.id.search_extra_action);
            final ImageView search_extra_action_image = findViewById(R.id.search_extra_action_image);
            if (search_extra_action != null) {
                search_extra_action.setOnClickListener(null);
                if (mode != EXTRA_ACTION_MODE.None) {
                    switch (mode) {
                        case Speech_recognition: {
                            if (checkVoiceRecognition()) {
                                Log.v(TAG, "type=", getType(), " | voice recognition not supported");
                                setMode(EXTRA_ACTION_MODE.None);
                                return;
                            }
                            if (search_extra_action_image != null) {
                                search_extra_action_image.setImageDrawable(getDrawable(R.drawable.ic_keyboard_voice));
                            }
                            search_extra_action.setOnClickListener(v -> {
                                Log.v(TAG, "type=", getType(), " | speech_recognition clicked");
                                startRecognition();
                            });
                            break;
                        }
                        case Clear: {
                            if (search_extra_action_image != null) {
                                search_extra_action_image.setImageDrawable(getDrawable(R.drawable.ic_close));
                            }
                            search_extra_action.setOnClickListener(v -> {
                                Log.v(TAG, "type=", getType(), " | clear clicked");
                                search_edit_text.setText("");
                            });
                            break;
                        }
                    }
                    if (search_extra_action_image != null) {
                        search_extra_action_image.setVisibility(View.VISIBLE);
                    }
                } else if (search_extra_action_image != null) {
                    search_extra_action_image.setVisibility(View.GONE);
                }
            }
        });
    }
    private void setSuggestions(final List<Suggestion> suggestions) {
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "setSuggestions | type=", getType());
            try {
                ListView search_suggestions = findViewById(R.id.search_suggestions);
                if (search_suggestions != null) {
                    search_suggestions.setDividerHeight(0);
                    search_suggestions.setDivider(null);
                    search_suggestions.setAdapter(new SuggestionsListView(activity, suggestions));
                    search_suggestions.setOnItemClickListener((parent, view, position, id) -> done(suggestions.get(position).query, suggestions.get(position).title));
                }
            } catch (Exception e) {
                Static.error(e);
            }
        });
    }
    private List<Suggestion> getSuggestions(String query) {
        Log.v(TAG, "getSuggestions | type=", getType(), " | query=", query);
        try {
            List<Suggestion> suggestions = new ArrayList<>();
            currentNumberOfSuggestions = 0;
            recentSchedulesIterator(getType(), (item) -> {
                if (query.isEmpty() || contains(item, query)) {
                    currentNumberOfSuggestions++;
                    suggestions.add(new Suggestion(item, item, R.drawable.ic_access_time));
                }
                return currentNumberOfSuggestions >= numberOfSuggestions;
            });
            cachedSchedulesIterator(getType(), (q, t) -> {
                if (query.isEmpty() || contains(q, query) || contains(t, query)) {
                    suggestions.add(new Suggestion(q, t, R.drawable.ic_save));
                }
                return false;
            });
            return suggestions;
        } catch (Exception e) {
            Static.error(e);
            return null;
        }
    }
    private void done(String query, String title) {
        Static.T.runThread(() -> {
            Log.v(TAG, "done | type=", getType(), " | query=", query, " | title=", title);
            try {
                JSONArray recent = Static.string2jsonArray(Storage.file.perm.get(this, "schedule_" + getType() + "#recent", ""));
                for (int i = 0; i < recent.length(); i++) {
                    String item = recent.getString(i);
                    if (equals(item, query) || equals(item, title)) {
                        recent.remove(i);
                        break;
                    }
                }
                saveCurrentSuggestion = true;
                cachedSchedulesIterator(getType(), (q, t) -> {
                    if (equals(t, query) || equals(q, query)) {
                        saveCurrentSuggestion = false;
                        return true;
                    }
                    return false;
                });
                if (saveCurrentSuggestion) {
                    for (int i = recent.length() - 1; i >= 0; i--) {
                        recent.put(i + 1, recent.getString(i));
                    }
                    recent.put(0, title);
                    if (recent.length() > maxCountOfSuggestionsToStore) {
                        for (int i = maxCountOfSuggestionsToStore; i < recent.length(); i++) {
                            recent.remove(i);
                        }
                    }
                }
                Storage.file.perm.put(this, "schedule_" + getType() + "#recent", recent.toString());
            } catch (Exception e) {
                Static.error(e);
                Storage.file.perm.delete(this, "schedule_" + getType() + "#recent");
            }
            onDone(query);
            Static.T.runOnUiThread(this::finish);
        });
    }
    private boolean contains(String first, String second) {
        return first != null && second != null && (first.toLowerCase().contains(second.toLowerCase()) || Static.Translit.cyr2lat(first).toLowerCase().contains(Static.Translit.cyr2lat(second).toLowerCase()));
    }
    private boolean equals(String first, String second) {
        return first != null && second != null && (first.equalsIgnoreCase(second) || Static.Translit.cyr2lat(first).equalsIgnoreCase(Static.Translit.cyr2lat(second)));
    }

    private boolean checkVoiceRecognition() {
        Log.v(TAG, "checkVoiceRecognition");
        try {
            return getPackageManager().queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).size() == 0;
        } catch (Exception e) {
            return true;
        }
    }
    private void startRecognition() {
        Log.v(TAG, "startRecognition");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getHint());
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Log.v(TAG, "voice recognition not supported");
            Static.toast(activity, R.string.speech_recognition_is_not_supported);
        }
    }
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Static.T.runOnUiThread(() -> {
            switch (requestCode) {
                case REQ_CODE_SPEECH_INPUT: {
                    Log.v(TAG, "doneRecognition");
                    if (resultCode == RESULT_OK && data != null) {
                        ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (result.size() > 0) {
                            Log.v(TAG, "resultRecognition | " + result.get(0));
                            search_edit_text.setText(result.get(0));
                        }
                    }
                    break;
                }
            }
        });
    }

    protected interface RecentSchedulesIterator {
        boolean onIterate(String item);
    }
    protected interface CachedSchedulesIterator {
        boolean onIterate(String query, String title);
    }
    protected void recentSchedulesIterator(String type, RecentSchedulesIterator recentSchedulesIterator) throws JSONException {
        JSONArray recent = Static.string2jsonArray(Storage.file.perm.get(this, "schedule_" + type + "#recent", ""));
        for (int i = 0; i < recent.length(); i++) {
            if (recentSchedulesIterator.onIterate(recent.getString(i))) {
                break;
            }
        }
    }
    protected void cachedSchedulesIterator(String type, CachedSchedulesIterator cachedSchedulesIterator) {
        ArrayList<String> cachedFiles = Storage.file.general.cache.list(this, "schedule_" + type + "#lessons");
        for (String file : cachedFiles) {
            String cachedFile = Storage.file.general.cache.get(this, "schedule_" + type + "#lessons#" + file);
            if (!cachedFile.isEmpty()) {
                try {
                    JSONObject cached = new JSONObject(cachedFile);
                    if (cached.has("query") && cached.has("title")) {
                        String query = cached.getString("query");
                        String title = cached.getString("title");
                        if (cachedSchedulesIterator.onIterate(query, title)) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        }
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(final Editable s) {
            Static.T.runThread(() -> {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    setMode(EXTRA_ACTION_MODE.Speech_recognition);
                } else {
                    setMode(EXTRA_ACTION_MODE.Clear);
                }
                setSuggestions(getSuggestions(query));
            });
        }
    };
    private final View.OnKeyListener onKeyListener = new View.OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                String query = Static.prettifyGroupNumber(search_edit_text.getText().toString().trim());
                if (!query.isEmpty()) {
                    done(query, query);
                    return true;
                }
            }
            return false;
        }
    };
}
