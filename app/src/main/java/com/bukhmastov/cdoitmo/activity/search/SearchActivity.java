package com.bukhmastov.cdoitmo.activity.search;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
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
import com.bukhmastov.cdoitmo.adapter.SuggestionsListView;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.object.entity.Suggestion;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.singleton.CtxWrapper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.Transliterate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public abstract class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";
    private static final int REQ_CODE_SPEECH_INPUT = 1337;
    protected final Context context = this;
    private EditText search_edit_text;
    protected final int numberOfSuggestions;
    protected final int maxCountOfSuggestionsToStore;
    protected int currentNumberOfSuggestions = 0;
    protected boolean saveCurrentSuggestion = true;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Theme theme;
    @Inject
    TextUtils textUtils;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({SPEECH_RECOGNITION, CLEAR, NONE})
    private @interface EXTRA_ACTION_MODE {}
    private static final String SPEECH_RECOGNITION = "speech_recognition";
    private static final String CLEAR = "clear";
    private static final String NONE = "none";

    abstract protected String getType();
    abstract protected String getHint();
    abstract protected void onDone(String query);

    private void inject() {
        if (thread == null) {
            AppComponentProvider.getComponent().inject(this);
        }
    }

    public SearchActivity(int numberOfSuggestions, int maxCountOfSuggestionsToStore) {
        this.numberOfSuggestions = numberOfSuggestions;
        this.maxCountOfSuggestionsToStore = maxCountOfSuggestionsToStore;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        inject();
        switch (theme.getAppTheme(this)) {
            case "light":
            default: setTheme(R.style.AppTheme_Search); break;
            case "dark": setTheme(R.style.AppTheme_Search_Dark); break;
            case "white": setTheme(R.style.AppTheme_Search_White); break;
            case "black": setTheme(R.style.AppTheme_Search_Black); break;
        }
        super.onCreate(savedInstanceState);
        log.i(TAG, "Activity created | type=", getType());
        setContentView(R.layout.activity_search);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        log.v(TAG, "onPostCreate | type=", getType());
        findViewById(R.id.search_close).setOnClickListener(v -> {
            log.v(TAG, "type=", getType(), " | search_close clicked");
            finish();
        });
        search_edit_text = findViewById(R.id.search_edittext);
        search_edit_text.setHint(getHint());
        search_edit_text.addTextChangedListener(textWatcher);
        search_edit_text.setOnKeyListener(onKeyListener);
        setMode(SPEECH_RECOGNITION);
        setSuggestions(getSuggestions(""));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log.i(TAG, "Activity destroyed | type=", getType());
    }

    @Override
    protected void attachBaseContext(Context context) {
        inject();
        super.attachBaseContext(CtxWrapper.wrap(context, storagePref, log, textUtils));
    }

    private void setMode(final @EXTRA_ACTION_MODE String mode) {
        thread.runOnUI(() -> {
            log.v(TAG, "setMode | type=", getType(), " | mode=", mode);
            final ViewGroup search_extra_action = findViewById(R.id.search_extra_action);
            final ImageView search_extra_action_image = findViewById(R.id.search_extra_action_image);
            if (search_extra_action != null) {
                search_extra_action.setOnClickListener(null);
                if (!mode.equals(NONE)) {
                    switch (mode) {
                        case SPEECH_RECOGNITION: {
                            if (checkVoiceRecognition()) {
                                log.v(TAG, "type=", getType(), " | voice recognition not supported");
                                setMode(NONE);
                                return;
                            }
                            if (search_extra_action_image != null) {
                                search_extra_action_image.setImageDrawable(getDrawable(R.drawable.ic_keyboard_voice));
                            }
                            search_extra_action.setOnClickListener(v -> {
                                log.v(TAG, "type=", getType(), " | speech_recognition clicked");
                                startRecognition();
                            });
                            break;
                        }
                        case CLEAR: {
                            if (search_extra_action_image != null) {
                                search_extra_action_image.setImageDrawable(getDrawable(R.drawable.ic_close));
                            }
                            search_extra_action.setOnClickListener(v -> {
                                log.v(TAG, "type=", getType(), " | clear clicked");
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
        thread.runOnUI(() -> {
            log.v(TAG, "setSuggestions | type=", getType());
            try {
                ListView search_suggestions = findViewById(R.id.search_suggestions);
                if (search_suggestions != null) {
                    search_suggestions.setDividerHeight(0);
                    search_suggestions.setDivider(null);
                    search_suggestions.setAdapter(new SuggestionsListView(context, suggestions, new SuggestionsListView.OnClickCallback() {
                        @Override
                        public void onClick(Suggestion suggestion) {
                            done(suggestion.query, suggestion.title);
                        }
                        @Override
                        public void onRemove(Suggestion suggestion) {
                            try {
                                if (suggestion.removable) {
                                    JSONArray recent = textUtils.string2jsonArray(storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_" + getType() + "#recent", ""));
                                    for (int i = 0; i < recent.length(); i++) {
                                        String item = recent.getString(i);
                                        if (SearchActivity.equals(item, suggestion.query) || SearchActivity.equals(item, suggestion.title)) {
                                            recent.remove(i);
                                            storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_" + getType() + "#recent", recent.toString());
                                            setSuggestions(getSuggestions(""));
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception ignore) {
                                /* ignore */
                            }
                        }
                    }));
                    search_suggestions.setOnItemClickListener((parent, view, position, id) -> done(suggestions.get(position).query, suggestions.get(position).title));
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }
    private List<Suggestion> getSuggestions(String query) {
        log.v(TAG, "getSuggestions | type=", getType(), " | query=", query);
        try {
            List<Suggestion> suggestions = new ArrayList<>();
            currentNumberOfSuggestions = 0;
            recentSchedulesIterator(getType(), (item) -> {
                if (query.isEmpty() || contains(item, query)) {
                    currentNumberOfSuggestions++;
                    suggestions.add(new Suggestion(item, item, R.drawable.ic_access_time, true));
                }
                return currentNumberOfSuggestions >= numberOfSuggestions;
            });
            cachedSchedulesIterator(getType(), (q, t) -> {
                if (query.isEmpty() || contains(q, query) || contains(t, query)) {
                    suggestions.add(new Suggestion(q, t, R.drawable.ic_save, false));
                }
                return false;
            });
            return suggestions;
        } catch (Exception e) {
            log.exception(e);
            return null;
        }
    }
    private void done(String query, String title) {
        thread.run(() -> {
            log.v(TAG, "done | type=", getType(), " | query=", query, " | title=", title);
            try {
                JSONArray recent = textUtils.string2jsonArray(storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_" + getType() + "#recent", ""));
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
                storage.put(context, Storage.PERMANENT, Storage.USER, "schedule_" + getType() + "#recent", recent.toString());
            } catch (Exception e) {
                log.exception(e);
                storage.delete(context, Storage.PERMANENT, Storage.USER, "schedule_" + getType() + "#recent");
            }
            onDone(query);
            thread.runOnUI(this::finish);
        });
    }
    private static boolean contains(String first, String second) {
        return first != null && second != null && (first.toLowerCase().contains(second.toLowerCase()) || Transliterate.cyr2lat(first).toLowerCase().contains(Transliterate.cyr2lat(second).toLowerCase()));
    }
    private static boolean equals(String first, String second) {
        return first != null && second != null && (first.equalsIgnoreCase(second) || Transliterate.cyr2lat(first).equalsIgnoreCase(Transliterate.cyr2lat(second)));
    }

    private boolean checkVoiceRecognition() {
        log.v(TAG, "checkVoiceRecognition");
        try {
            return getPackageManager().queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).size() == 0;
        } catch (Exception e) {
            return true;
        }
    }
    private void startRecognition() {
        log.v(TAG, "startRecognition");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getHint());
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            log.v(TAG, "voice recognition not supported");
            notificationMessage.toast(context, R.string.speech_recognition_is_not_supported);
        }
    }
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        thread.runOnUI(() -> {
            switch (requestCode) {
                case REQ_CODE_SPEECH_INPUT: {
                    log.v(TAG, "doneRecognition");
                    if (resultCode == RESULT_OK && data != null) {
                        ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (result.size() > 0) {
                            log.v(TAG, "resultRecognition | " + result.get(0));
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
        JSONArray recent = textUtils.string2jsonArray(storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_" + type + "#recent", ""));
        for (int i = 0; i < recent.length(); i++) {
            if (recentSchedulesIterator.onIterate(recent.getString(i))) {
                break;
            }
        }
    }
    protected void cachedSchedulesIterator(String type, CachedSchedulesIterator cachedSchedulesIterator) {
        ArrayList<String> cachedFiles = storage.list(context, Storage.CACHE, Storage.GLOBAL, "schedule_" + type + "#lessons");
        for (String file : cachedFiles) {
            String cachedFile = storage.get(context, Storage.CACHE, Storage.GLOBAL, "schedule_" + type + "#lessons#" + file);
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
                    log.exception(e);
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
            thread.run(() -> {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    setMode(SPEECH_RECOGNITION);
                } else {
                    setMode(CLEAR);
                }
                setSuggestions(getSuggestions(query));
            });
        }
    };
    private final View.OnKeyListener onKeyListener = new View.OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                String query = textUtils.prettifyGroupNumber(search_edit_text.getText().toString().trim());
                if (!query.isEmpty()) {
                    done(query, query);
                    return true;
                }
            }
            return false;
        }
    };
}
