package com.bukhmastov.cdoitmo.view.dialog;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

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
    private final List<String> prefThemeTitles;
    private final List<String> prefThemeValues;

    private final static String DEFAULT_THEME = "light";
    private final static String DEFAULT_THEME_DARK = "dark";

    // auto theme enabled
    private boolean autoEnabled;

    // static theme value
    private String staticValue = DEFAULT_THEME;

    // auto theme first value
    private String t1Value = DEFAULT_THEME;
    private int t1Hour = 6;
    private int t1Minutes = 30;

    // auto theme second value
    private String t2Value = DEFAULT_THEME_DARK;
    private int t2Hour = 23;
    private int t2Minutes = 0;

    @Inject
    Log log;
    @Inject
    Thread thread;

    public ThemeDialog(Context context, String value, Callback cb) {
        super(context);
        AppComponentProvider.getComponent().inject(this);
        this.prefThemeTitles = Arrays.asList(context.getResources().getStringArray(R.array.pref_theme_titles));
        this.prefThemeValues = Arrays.asList(context.getResources().getStringArray(R.array.pref_theme_values));
        this.cb = cb;
        this.autoEnabled = value.contains("#");
        if (autoEnabled) {
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
                this.t1Hour = Integer.parseInt(t1_values[0]);
                this.t1Minutes = Integer.parseInt(t1_values[1]);
                if (t1Hour < 0 || t1Hour > 23) t1Hour = 6;
                if (t1Minutes < 0 || t1Minutes > 59) t1Minutes = 0;
                this.t2Hour = Integer.parseInt(t2_values[0]);
                this.t2Minutes = Integer.parseInt(t2_values[1]);
                if (t2Hour < 0 || t2Hour > 23) t2Hour = 23;
                if (t2Minutes < 0 || t2Minutes > 59) t2Minutes = 59;
                if (prefThemeValues.contains(t1_value)) {
                    this.t1Value = t1_value;
                }
                if (prefThemeValues.contains(t2_value)) {
                    this.t2Value = t2_value;
                }
            } catch (Exception ignore) {
                this.staticValue = DEFAULT_THEME;
                this.autoEnabled = false;
            }
        } else {
            if (prefThemeValues.contains(value)) {
                this.staticValue = value;
            } else {
                this.staticValue = DEFAULT_THEME;
            }
        }
    }

    public void show() {
        log.v(TAG, "show");
        thread.runOnUI(() -> {
            ViewGroup themeLayout = (ViewGroup) inflate(R.layout.dialog_theme_picker);
            thread.standalone(() -> {
                ViewGroup themeContainerStatic = themeLayout.findViewById(R.id.theme_container_static);
                ViewGroup themeContainerAuto = themeLayout.findViewById(R.id.theme_container_auto);
                ViewGroup switcherContainer = themeLayout.findViewById(R.id.switcher_container);
                Switch switcher = themeLayout.findViewById(R.id.switcher);
                TextView t1Time = themeLayout.findViewById(R.id.t1_time);
                TextView t2Time = themeLayout.findViewById(R.id.t2_time);
                TextView t1Spinner = themeLayout.findViewById(R.id.t1_spinner);
                TextView t2Spinner = themeLayout.findViewById(R.id.t2_spinner);
                // setup switcher
                switcher.setOnCheckedChangeListener((compoundButton, checked) -> {
                    autoEnabled = checked;
                    log.v(TAG, "switcher clicked | autoEnabled = " + (autoEnabled ? "true" : "false"));
                    if (autoEnabled) {
                        themeContainerStatic.setVisibility(View.GONE);
                        themeContainerAuto.setVisibility(View.VISIBLE);
                    } else {
                        themeContainerStatic.setVisibility(View.VISIBLE);
                        themeContainerAuto.setVisibility(View.GONE);
                    }
                });
                switcherContainer.setOnClickListener(view -> {
                    log.v(TAG, "switcherContainer clicked");
                    switcher.setChecked(!autoEnabled);
                });
                switcher.setChecked(autoEnabled);
                if (autoEnabled) {
                    themeContainerStatic.setVisibility(View.GONE);
                    themeContainerAuto.setVisibility(View.VISIBLE);
                } else {
                    themeContainerStatic.setVisibility(View.VISIBLE);
                    themeContainerAuto.setVisibility(View.GONE);
                }
                // setup static theme selector
                thread.runOnUI(() -> {
                    for (int i = 0; i < prefThemeTitles.size(); i++) {
                        final RadioButton radioButton = (RadioButton) inflate(R.layout.dialog_theme_picker_static_item);
                        radioButton.setText(prefThemeTitles.get(i));
                        radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked) {
                                staticValue = prefThemeValues.get(prefThemeTitles.indexOf(buttonView.getText().toString().trim()));
                            }
                        });
                        themeContainerStatic.addView(radioButton, RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
                        if (prefThemeValues.indexOf(staticValue) == i) {
                            ((RadioGroup) themeContainerStatic).check(radioButton.getId());
                        }
                    }
                });
                // setup automatic theme selector
                t1Time.setText(t1Hour + ":" + StringUtils.ldgZero(t1Minutes));
                t1Time.setOnClickListener(view -> {
                    log.v(TAG, "t1Time clicked");
                    showTimePicker(t1Hour, t1Minutes, (hours, minutes) -> {
                        log.v(TAG, "t1Time showTimePicker done | " + hours + " | " + minutes);
                        t1Hour = hours;
                        t1Minutes = minutes;
                        t1Time.setText(t1Hour + ":" + StringUtils.ldgZero(t1Minutes));
                    });
                });
                t2Time.setText(t2Hour + ":" + StringUtils.ldgZero(t2Minutes));
                t2Time.setOnClickListener(view -> showTimePicker(t2Hour, t2Minutes, (hours, minutes) -> {
                    log.v(TAG, "t2Time showTimePicker done | " + hours + " | " + minutes);
                    t2Hour = hours;
                    t2Minutes = minutes;
                    t2Time.setText(t2Hour + ":" + StringUtils.ldgZero(t2Minutes));
                }));
                t1Spinner.setText(prefThemeTitles.get(prefThemeValues.indexOf(t1Value)));
                t1Spinner.setOnClickListener(view -> showThemePicker(t1Value, theme -> {
                    log.v(TAG, "t1Spinner showThemePicker done | " + theme);
                    t1Value = theme;
                    t1Spinner.setText(prefThemeTitles.get(prefThemeValues.indexOf(t1Value)));
                }));
                t2Spinner.setText(prefThemeTitles.get(prefThemeValues.indexOf(t2Value)));
                t2Spinner.setOnClickListener(view -> showThemePicker(t2Value, theme -> {
                    log.v(TAG, "t2Spinner showThemePicker done | " + theme);
                    t2Value = theme;
                    t2Spinner.setText(prefThemeTitles.get(prefThemeValues.indexOf(t2Value)));
                }));
                // show picker
                thread.runOnUI(() -> new AlertDialog.Builder(context)
                        .setTitle(R.string.theme)
                        .setView(themeLayout)
                        .setPositiveButton(R.string.accept, (dialog, which) -> {
                            log.v(TAG, "show picker accepted");
                            String theme;
                            if (autoEnabled) {
                                theme = t1Hour + ":" + StringUtils.ldgZero(t1Minutes) + "#" + t1Value + "#" +
                                        t2Hour + ":" + StringUtils.ldgZero(t2Minutes) + "#" + t2Value;
                            } else {
                                theme = staticValue;
                            }
                            cb.onDone(theme, getThemeDesc(context, theme));
                        })
                        .create().show());
            }, throwable -> {
                log.exception(throwable);
            });
        });
    }

    private void showThemePicker(String value, ThemePickerCallback callback) {
        thread.runOnUI(() -> {
            log.v(TAG, "showThemePicker | " + value);
            new AlertDialog.Builder(context)
                    .setTitle(R.string.theme)
                    .setSingleChoiceItems(R.array.pref_theme_titles, prefThemeValues.indexOf(value), (dialogInterface, position) -> {
                        callback.onDone(prefThemeValues.get(position));
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create().show();
        });
    }

    private void showTimePicker(int hours, int minutes, TimePickerCallback callback) {
        thread.runOnUI(() -> {
            log.v(TAG, "showTimePicker | " + hours + " | " + minutes);
            new TimePickerDialog(context, (timePicker, hourOfDay, minute) -> callback.onDone(hourOfDay, minute), hours, minutes, true).show();
        });
    }

    public static String getTheme(Context context, StoragePref storagePref, Time time) {
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

    public static String getThemeDesc(Context context,  String value) {
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
                        return values[0] + " " + pref_theme_titles.get(index1) + ", " + values[2] + " " + pref_theme_titles.get(index2);
                    } else if (index1 != -1) {
                        return values[0] + " " + pref_theme_titles.get(index1) + ", " + values[2] + " " + Static.GLITCH;
                    } else {
                        return values[0] + " " + Static.GLITCH + ", " + values[2] + " " + pref_theme_titles.get(index2);
                    }
                } else {
                    return value;
                }
            } catch (Exception ignore) {
                return value;
            }
        } else {
            int index = pref_theme_values.indexOf(value);
            return index != -1 ? pref_theme_titles.get(index) : value;
        }
    }
}
