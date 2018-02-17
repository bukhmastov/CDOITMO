package com.bukhmastov.cdoitmo.activities;

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

import java.util.ArrayList;
import java.util.Locale;

public abstract class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";
    protected final SearchActivity self = this;
    private static final int REQ_CODE_SPEECH_INPUT = 1337;
    private enum EXTRA_ACTION_MODE {Speech_recognition, Clear, None}
    private EditText search_edit_text;

    abstract String getHint();
    abstract ArrayList<Suggestion> getSuggestions(String query);
    abstract void onDone(String query, String label);

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
        Log.i(TAG, "Activity created");
        setContentView(R.layout.activity_search);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Log.v(TAG, "onPostCreate");
        findViewById(R.id.search_close).setOnClickListener(v -> {
            Log.v(TAG, "search_close clicked");
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
        Log.i(TAG, "Activity destroyed");
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CtxWrapper.wrap(context));
    }

    private void done(String query, String label) {
        Log.v(TAG, "done | query=" + query + " | label=" + label);
        onDone(query, label);
        finish();
    }

    private void setMode(final EXTRA_ACTION_MODE mode) {
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "setMode | mode=" + mode.toString());
            final ViewGroup search_extra_action = findViewById(R.id.search_extra_action);
            final ImageView search_extra_action_image = findViewById(R.id.search_extra_action_image);
            if (search_extra_action != null) {
                search_extra_action.setOnClickListener(null);
                if (mode != EXTRA_ACTION_MODE.None) {
                    switch (mode) {
                        case Speech_recognition: {
                            if (checkVoiceRecognition()) {
                                Log.v(TAG, "voice recognition not supported");
                                setMode(EXTRA_ACTION_MODE.None);
                                return;
                            }
                            search_extra_action_image.setImageDrawable(getDrawable(R.drawable.ic_keyboard_voice));
                            search_extra_action.setOnClickListener(v -> {
                                Log.v(TAG, "speech_recognition clicked");
                                startRecognition();
                            });
                            break;
                        }
                        case Clear: {
                            search_extra_action_image.setImageDrawable(getDrawable(R.drawable.ic_close));
                            search_extra_action.setOnClickListener(v -> {
                                Log.v(TAG, "clear clicked");
                                search_edit_text.setText("");
                            });
                            break;
                        }
                    }
                    search_extra_action_image.setVisibility(View.VISIBLE);
                } else {
                    search_extra_action_image.setVisibility(View.GONE);
                }
            }
        });
    }
    private void setSuggestions(final ArrayList<Suggestion> suggestions) {
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "setSuggestions");
            try {
                ListView search_suggestions = findViewById(R.id.search_suggestions);
                if (search_suggestions == null) throw new Exception("search_suggestions listview is null");
                search_suggestions.setDividerHeight(0);
                search_suggestions.setDivider(null);
                search_suggestions.setAdapter(new SuggestionsListView(self, suggestions));
                search_suggestions.setOnItemClickListener((parent, view, position, id) -> done(suggestions.get(position).query, suggestions.get(position).label));
            } catch (Exception e) {
                Static.error(e);
            }
        });
    }

    public boolean checkVoiceRecognition() {
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
            Static.toast(getApplicationContext(), R.string.speech_recognition_is_not_supported);
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
                String query = search_edit_text.getText().toString().trim();
                if (!query.isEmpty()) {
                    done(query, query);
                    return true;
                }
            }
            return false;
        }
    };
}
