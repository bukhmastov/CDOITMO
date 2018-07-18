package com.bukhmastov.cdoitmo.dialog;

import android.app.TimePickerDialog;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class ThemeDialog extends Dialog {

    private static final String TAG = "ThemeDialog";
    public interface Callback {
        void onDone(String theme, String desc);
    }
    private interface ThemePickerCallback {
        void onDone(String theme);
    }
    private interface TimePickerCallback {
        void onDone(int hours, int minutes);
    }
    private final Callback cb;
    private final List<String> pref_theme_titles;
    private final List<String> pref_theme_values;

    private final static String DEFAULT_THEME = "light";
    private final static String DEFAULT_THEME_DARK = "dark";

    // auto theme enabled
    private boolean auto_enabled = false;

    // static theme value
    private String static_value = DEFAULT_THEME;

    // auto theme first value
    private String t1_value = DEFAULT_THEME;
    private int t1_hour = 6;
    private int t1_minutes = 30;

    // auto theme second value
    private String t2_value = DEFAULT_THEME_DARK;
    private int t2_hour = 23;
    private int t2_minutes = 0;

    //@Inject
    private Log log = Log.instance();
    //@Inject
    private Thread thread = Thread.instance();
    //@Inject
    private TextUtils textUtils = TextUtils.instance();

    public ThemeDialog(Context context, String value, Callback cb) {
        super(context);
        this.pref_theme_titles = Arrays.asList(context.getResources().getStringArray(R.array.pref_theme_titles));
        this.pref_theme_values = Arrays.asList(context.getResources().getStringArray(R.array.pref_theme_values));
        this.cb = cb;
        this.auto_enabled = value.contains("#");
        if (auto_enabled) {
            try {
                final String[] values = value.split("#");
                if (values.length != 4) {
                    throw new Exception("Invalid value");
                }
                final String[] t1_values = values[0].split(":");
                final String t1_value = values[1];
                final String[] t2_values = values[2].split(":");
                final String t2_value = values[3];
                if (t1_values.length != 2 || t2_values.length != 2) {
                    throw new Exception("Invalid value");
                }
                this.t1_hour = Integer.parseInt(t1_values[0]);
                this.t1_minutes = Integer.parseInt(t1_values[1]);
                if (t1_hour < 0 || t1_hour > 23) t1_hour = 6;
                if (t1_minutes < 0 || t1_minutes > 59) t1_minutes = 0;
                this.t2_hour = Integer.parseInt(t2_values[0]);
                this.t2_minutes = Integer.parseInt(t2_values[1]);
                if (t2_hour < 0 || t2_hour > 23) t2_hour = 23;
                if (t2_minutes < 0 || t2_minutes > 59) t2_minutes = 59;
                if (pref_theme_values.contains(t1_value)) {
                    this.t1_value = t1_value;
                }
                if (pref_theme_values.contains(t2_value)) {
                    this.t2_value = t2_value;
                }
            } catch (Exception ignore) {
                this.static_value = DEFAULT_THEME;
                this.auto_enabled = false;
            }
        } else {
            if (pref_theme_values.contains(value)) {
                this.static_value = value;
            } else {
                this.static_value = DEFAULT_THEME;
            }
        }
    }

    public void show() {
        log.v(TAG, "show");
        thread.runOnUI(() -> {
            final ViewGroup theme_layout = (ViewGroup) inflate(R.layout.dialog_theme_picker);
            thread.run(() -> {
                try {
                    final ViewGroup theme_container_static = theme_layout.findViewById(R.id.theme_container_static);
                    final ViewGroup theme_container_auto = theme_layout.findViewById(R.id.theme_container_auto);
                    final ViewGroup switcher_container = theme_layout.findViewById(R.id.switcher_container);
                    final Switch switcher = theme_layout.findViewById(R.id.switcher);
                    final TextView t1_time = theme_layout.findViewById(R.id.t1_time);
                    final TextView t2_time = theme_layout.findViewById(R.id.t2_time);
                    final TextView t1_spinner = theme_layout.findViewById(R.id.t1_spinner);
                    final TextView t2_spinner = theme_layout.findViewById(R.id.t2_spinner);
                    // setup switcher
                    switcher.setOnCheckedChangeListener((compoundButton, checked) -> {
                        auto_enabled = checked;
                        log.v(TAG, "switcher clicked | auto_enabled = " + (auto_enabled ? "true" : "false"));
                        if (auto_enabled) {
                            theme_container_static.setVisibility(View.GONE);
                            theme_container_auto.setVisibility(View.VISIBLE);
                        } else {
                            theme_container_static.setVisibility(View.VISIBLE);
                            theme_container_auto.setVisibility(View.GONE);
                        }
                    });
                    switcher_container.setOnClickListener(view -> {
                        log.v(TAG, "switcher_container clicked");
                        switcher.setChecked(!auto_enabled);
                    });
                    switcher.setChecked(auto_enabled);
                    if (auto_enabled) {
                        theme_container_static.setVisibility(View.GONE);
                        theme_container_auto.setVisibility(View.VISIBLE);
                    } else {
                        theme_container_static.setVisibility(View.VISIBLE);
                        theme_container_auto.setVisibility(View.GONE);
                    }
                    // setup static theme selector
                    thread.runOnUI(() -> {
                        for (int i = 0; i < pref_theme_titles.size(); i++) {
                            final RadioButton radioButton = (RadioButton) inflate(R.layout.dialog_theme_picker_static_item);
                            radioButton.setText(pref_theme_titles.get(i));
                            radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                if (isChecked) {
                                    static_value = pref_theme_values.get(pref_theme_titles.indexOf(buttonView.getText().toString().trim()));
                                }
                            });
                            theme_container_static.addView(radioButton, RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
                            if (pref_theme_values.indexOf(static_value) == i) {
                                ((RadioGroup) theme_container_static).check(radioButton.getId());
                            }
                        }
                    });
                    // setup automatic theme selector
                    t1_time.setText(t1_hour + ":" + textUtils.ldgZero(t1_minutes));
                    t1_time.setOnClickListener(view -> {
                        log.v(TAG, "t1_time clicked");
                        showTimePicker(t1_hour, t1_minutes, (hours, minutes) -> {
                            log.v(TAG, "t1_time showTimePicker done | " + hours + " | " + minutes);
                            t1_hour = hours;
                            t1_minutes = minutes;
                            t1_time.setText(t1_hour + ":" + textUtils.ldgZero(t1_minutes));
                        });
                    });
                    t2_time.setText(t2_hour + ":" + textUtils.ldgZero(t2_minutes));
                    t2_time.setOnClickListener(view -> showTimePicker(t2_hour, t2_minutes, (hours, minutes) -> {
                        log.v(TAG, "t2_time showTimePicker done | " + hours + " | " + minutes);
                        t2_hour = hours;
                        t2_minutes = minutes;
                        t2_time.setText(t2_hour + ":" + textUtils.ldgZero(t2_minutes));
                    }));
                    t1_spinner.setText(pref_theme_titles.get(pref_theme_values.indexOf(t1_value)));
                    t1_spinner.setOnClickListener(view -> showThemePicker(t1_value, theme -> {
                        log.v(TAG, "t1_spinner showThemePicker done | " + theme);
                        t1_value = theme;
                        t1_spinner.setText(pref_theme_titles.get(pref_theme_values.indexOf(t1_value)));
                    }));
                    t2_spinner.setText(pref_theme_titles.get(pref_theme_values.indexOf(t2_value)));
                    t2_spinner.setOnClickListener(view -> showThemePicker(t2_value, theme -> {
                        log.v(TAG, "t2_spinner showThemePicker done | " + theme);
                        t2_value = theme;
                        t2_spinner.setText(pref_theme_titles.get(pref_theme_values.indexOf(t2_value)));
                    }));
                    // show picker
                    thread.runOnUI(() -> new AlertDialog.Builder(context)
                            .setTitle(R.string.theme)
                            .setView(theme_layout)
                            .setPositiveButton(R.string.accept, (dialog, which) -> thread.run(() -> {
                                log.v(TAG, "show picker accepted");
                                String theme;
                                if (auto_enabled) {
                                    theme = t1_hour + ":" + textUtils.ldgZero(t1_minutes) + "#" + t1_value + "#" + t2_hour + ":" + textUtils.ldgZero(t2_minutes) + "#" + t2_value;
                                } else {
                                    theme = static_value;
                                }
                                cb.onDone(theme, getThemeDesc(context, theme));
                            }))
                            .create().show());
                } catch (Exception e) {
                    log.exception(e);
                }
            });
        });
    }

    private void showThemePicker(final String value, final ThemePickerCallback callback) {
        thread.runOnUI(() -> {
            log.v(TAG, "showThemePicker | " + value);
            new AlertDialog.Builder(context)
                    .setTitle(R.string.theme)
                    .setSingleChoiceItems(R.array.pref_theme_titles, pref_theme_values.indexOf(value), (dialogInterface, position) -> {
                        callback.onDone(pref_theme_values.get(position));
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create().show();
        });
    }

    private void showTimePicker(final int hours, final int minutes, final TimePickerCallback callback) {
        thread.runOnUI(() -> {
            log.v(TAG, "showTimePicker | " + hours + " | " + minutes);
            new TimePickerDialog(context, (timePicker, hourOfDay, minute) -> callback.onDone(hourOfDay, minute), hours, minutes, true).show();
        });
    }

    public static String getTheme(final Context context, final StoragePref storagePref, final Time time) {
        final String theme = storagePref.get(context, "pref_theme", DEFAULT_THEME);
        if (theme.contains("#")) {
            try {
                final String[] values = theme.split("#");
                if (values.length != 4) {
                    throw new Exception("Invalid value");
                }
                final String[] t1_values = values[0].split(":");
                final String t1_value = values[1];
                final String[] t2_values = values[2].split(":");
                final String t2_value = values[3];
                if (t1_values.length != 2 || t2_values.length != 2) {
                    throw new Exception("Invalid value");
                }
                final int t1_hour = Integer.parseInt(t1_values[0]);
                final int t1_minutes = Integer.parseInt(t1_values[1]);
                final int t2_hour = Integer.parseInt(t2_values[0]);
                final int t2_minutes = Integer.parseInt(t2_values[1]);
                if (t1_hour < 0 || t1_hour > 23 || t1_minutes < 0 || t1_minutes > 59 || t2_hour < 0 || t2_hour > 23 || t2_minutes < 0 || t2_minutes > 59) throw new Exception("Invalid value");
                final Calendar calendar = time.getCalendar();
                final int now_hours = calendar.get(Calendar.HOUR_OF_DAY);
                final int now_minutes = calendar.get(Calendar.MINUTE);
                if (t1_hour == t2_hour) {
                    if (now_hours == t1_hour) {
                        if (t2_minutes >= t1_minutes) {
                            if (t1_minutes <= now_minutes && now_minutes <= t2_minutes) {
                                return t1_value;
                            } else {
                                return t2_value;
                            }
                        } else {
                            if (t2_minutes <= now_minutes && now_minutes <= t1_minutes) {
                                return t2_value;
                            } else {
                                return t1_value;
                            }
                        }
                    } else {
                        return t2_minutes < t1_minutes ? t1_value : t2_value;
                    }
                } else {
                    if ((t1_hour <= now_hours && (t1_hour < now_hours || t1_minutes <= now_minutes) && now_hours <= t2_hour && (now_hours < t2_hour || now_minutes <= t2_minutes)) || (t1_hour >= t2_hour && (now_hours >= t1_hour && (now_hours > t1_hour || now_minutes >= t1_minutes) || now_hours <= t2_hour && (now_hours < t2_hour || now_minutes <= t2_minutes)))) {
                        return t1_value;
                    } else {
                        return t2_value;
                    }
                }
            } catch (Exception ignore) {
                storagePref.put(context, "pref_theme", DEFAULT_THEME);
                return DEFAULT_THEME;
            }
        } else {
            return theme;
        }
    }

    public static String getThemeDesc(final Context context, final String value) {
        final List<String> pref_theme_titles = Arrays.asList(context.getResources().getStringArray(R.array.pref_theme_titles));
        final List<String> pref_theme_values = Arrays.asList(context.getResources().getStringArray(R.array.pref_theme_values));
        if (pref_theme_titles.contains(value)) {
            return value;
        }
        if (value.contains("#")) {
            try {
                final String[] values = value.split("#");
                if (values.length == 4) {
                    int index1 = pref_theme_values.indexOf(values[1]);
                    int index2 = pref_theme_values.indexOf(values[3]);
                    if (index1 != -1 && index2 != -1) {
                        return pref_theme_titles.get(index1) + ", " + pref_theme_titles.get(index2);
                    } else if (index1 != -1) {
                        return pref_theme_titles.get(index1) + ", " + Static.GLITCH;
                    } else {
                        return Static.GLITCH + ", " + pref_theme_titles.get(index2);
                    }
                } else {
                    return value;
                }
            } catch (Exception ignore) {
                return value;
            }
        } else {
            int index = pref_theme_values.indexOf(value);
            return index != -1 ? pref_theme_titles.get(index) : null;
        }
    }
}
