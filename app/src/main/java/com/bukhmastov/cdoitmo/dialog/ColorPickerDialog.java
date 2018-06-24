package com.bukhmastov.cdoitmo.dialog;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

public class ColorPickerDialog extends Dialog {

    private static final String TAG = "ColorPickerDialog";
    private static final String[][] COLORS = {
            {"#F44336", "#FFEBEE", "#FFCDD2", "#EF9A9A", "#E57373", "#EF5350", "#F44336", "#E53935", "#D32F2F", "#C62828", "#B71C1C"}, /* Red */
            {"#E91E63", "#FCE4EC", "#F8BBD0", "#F48FB1", "#F06292", "#EC407A", "#E91E63", "#D81B60", "#C2185B", "#AD1457", "#880E4F"}, /* Pink */
            {"#9C27B0", "#F3E5F5", "#E1BEE7", "#CE93D8", "#BA68C8", "#AB47BC", "#9C27B0", "#8E24AA", "#7B1FA2", "#6A1B9A", "#4A148C"}, /* Purple */
            {"#673AB7", "#EDE7F6", "#D1C4E9", "#B39DDB", "#9575CD", "#7E57C2", "#673AB7", "#5E35B1", "#512DA8", "#4527A0", "#311B92"}, /* Deep Purple */
            {"#3F51B5", "#E8EAF6", "#C5CAE9", "#9FA8DA", "#7986CB", "#5C6BC0", "#3F51B5", "#3949AB", "#303F9F", "#283593", "#1A237E"}, /* Indigo */
            {"#2196F3", "#E3F2FD", "#BBDEFB", "#90CAF9", "#64B5F6", "#42A5F5", "#2196F3", "#1E88E5", "#1976D2", "#1565C0", "#0D47A1"}, /* Blue */
            {"#03A9F4", "#E1F5FE", "#B3E5FC", "#81D4FA", "#4FC3F7", "#29B6F6", "#03A9F4", "#039BE5", "#0288D1", "#0277BD", "#01579B"}, /* Light Blue */
            {"#00BCD4", "#E0F7FA", "#B2EBF2", "#80DEEA", "#4DD0E1", "#26C6DA", "#00BCD4", "#00ACC1", "#0097A7", "#00838F", "#006064"}, /* Cyan */
            {"#009688", "#E0F2F1", "#B2DFDB", "#80CBC4", "#4DB6AC", "#26A69A", "#009688", "#00897B", "#00796B", "#00695C", "#004D40"}, /* Teal */
            {"#4CAF50", "#E8F5E9", "#C8E6C9", "#A5D6A7", "#81C784", "#66BB6A", "#4CAF50", "#43A047", "#388E3C", "#2E7D32", "#1B5E20"}, /* Green */
            {"#8BC34A", "#F1F8E9", "#DCEDC8", "#C5E1A5", "#AED581", "#9CCC65", "#8BC34A", "#7CB342", "#689F38", "#558B2F", "#33691E"}, /* Light Green */
            {"#CDDC39", "#F9FBE7", "#F0F4C3", "#E6EE9C", "#DCE775", "#D4E157", "#CDDC39", "#C0CA33", "#AFB42B", "#9E9D24", "#827717"}, /* Lime */
            {"#FFEB3B", "#FFFDE7", "#FFF9C4", "#FFF59D", "#FFF176", "#FFEE58", "#FFEB3B", "#FDD835", "#FBC02D", "#F9A825", "#F57F17"}, /* Yellow */
            {"#FFC107", "#FFF8E1", "#FFECB3", "#FFE082", "#FFD54F", "#FFCA28", "#FFC107", "#FFB300", "#FFA000", "#FF8F00", "#FF6F00"}, /* Amber */
            {"#FF9800", "#FFF3E0", "#FFE0B2", "#FFCC80", "#FFB74D", "#FFA726", "#FF9800", "#FB8C00", "#F57C00", "#EF6C00", "#E65100"}, /* Orange */
            {"#FF5722", "#FBE9E7", "#FFCCBC", "#FFAB91", "#FF8A65", "#FF7043", "#FF5722", "#F4511E", "#E64A19", "#D84315", "#BF360C"}, /* Deep Orange */
            {"#795548", "#EFEBE9", "#D7CCC8", "#BCAAA4", "#A1887F", "#8D6E63", "#795548", "#6D4C41", "#5D4037", "#4E342E", "#3E2723"}, /* Brown */
            {"#9E9E9E", "#FAFAFA", "#F5F5F5", "#EEEEEE", "#E0E0E0", "#BDBDBD", "#9E9E9E", "#757575", "#616161", "#424242", "#212121"}, /* Grey */
            {"#607D8B", "#ECEFF1", "#CFD8DC", "#B0BEC5", "#90A4AE", "#78909C", "#607D8B", "#546E7A", "#455A64", "#37474F", "#263238"}, /* Blue Grey */
            {"#000000"}, /* Black */
            {"#FFFFFF"}  /* White */
    };

    private final ColorPickerCallback callback;
    private AlertDialog alertDialog = null;
    private GridView container = null;
    private EditText selectedColorInput = null;
    private GridAdapter gridAdapter = null;
    public interface ColorPickerCallback {
        void result(String hex);
        void exception(Exception e);
    }

    public ColorPickerDialog(final Context context, final ColorPickerCallback callback) {
        super(context);
        this.callback = callback;
    }

    public void show() {
        show(null);
    }

    public void show(String colorHex) {
        Log.v(TAG, "show");
        Thread.runOnUI(() -> {
            try {
                ViewGroup layout = (ViewGroup) inflate(R.layout.dialog_color_picker);
                if (layout == null) {
                    return;
                }
                container = layout.findViewById(R.id.colorPickerContainer);
                selectedColorInput = layout.findViewById(R.id.selectedColorInput);
                alertDialog = new AlertDialog.Builder(context)
                        .setTitle(R.string.choose_color)
                        .setView(layout)
                        .setPositiveButton(R.string.apply, (dialogInterface, i) -> {
                            try {
                                if (selectedColorInput == null) {
                                    return;
                                }
                                String selected = selectedColorInput.getText().toString().trim();
                                Log.v(TAG, "apply | selected=" + selected);
                                if (selected.isEmpty() || selected.charAt(0) != '#') {
                                    return;
                                }
                                Color.parseColor(selected);
                                callback.result(selected);
                            } catch (Exception ignore) {/* ignore */}
                        })
                        .setNegativeButton(R.string.do_cancel, null)
                        .create();
                selectedColorInput.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        gridAdapter.notifyDataSetChanged();
                    }
                });
                if (colorHex != null) {
                    select(colorHex);
                }
            } catch (Exception e) {
                callback.exception(e);
            }
        });
    }

    public void select(String colorHex) {
        Log.v(TAG, "select | colorHex=", colorHex);
        Thread.runOnUI(() -> {
            try {
                try {
                    if (selectedColorInput == null || colorHex.isEmpty() || colorHex.charAt(0) != '#') {
                        throw new Exception();
                    }
                    Color.parseColor(colorHex);
                    selectedColorInput.setText(colorHex);
                } catch (Exception ignore) {/* ignore */}
                if (alertDialog != null && !alertDialog.isShowing()) {
                    alertDialog.show();
                    displayColors(-1);
                }
            } catch (Exception e) {
                callback.exception(e);
            }
        });
    }

    public void close() {
        Log.v(TAG, "close");
        Thread.runOnUI(() -> {
            try {
                if (alertDialog != null && alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
            } catch (Exception e) {
                callback.exception(e);
            }
        });
    }

    private void displayColors(final int index) {
        Log.v(TAG, "displayColors | index=" + index);
        Thread.run(() -> {
            try {
                final boolean modeAllColors = index < 0 || index > COLORS.length;
                final String[] colors;
                if (modeAllColors) { // display all colors
                    colors = new String[COLORS.length];
                    for (int i = 0; i < COLORS.length; i++) {
                        colors[i] = COLORS[i][0];
                    }
                } else { // display certain colors
                    colors = new String[COLORS[index].length];
                    System.arraycopy(COLORS[index], 1, colors, 0, COLORS[index].length - 1);
                    colors[COLORS[index].length - 1] = "back";
                }
                Thread.runOnUI(() -> {
                    try {
                        if (gridAdapter == null) {
                            gridAdapter = new GridAdapter(context);
                            container.setAdapter(gridAdapter);
                        }
                        container.setOnItemClickListener((adapterView, view, i, l) -> {
                            Log.v(TAG, "color clicked | i=" + i);
                            Thread.run(() -> {
                                try {
                                    String color = gridAdapter.getItem(i);
                                    if ("back".equals(color)) {
                                        Log.v(TAG, "back clicked");
                                        displayColors(-1);
                                    } else {
                                        Log.v(TAG, "color selected | color=" + color);
                                        Thread.runOnUI(() -> {
                                            if (selectedColorInput != null) {
                                                selectedColorInput.setText(color);
                                            }
                                            if (modeAllColors && COLORS[i].length > 1) {
                                                displayColors(i);
                                            } else {
                                                gridAdapter.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    callback.exception(e);
                                }
                            });
                        });
                        gridAdapter.updateColors(colors);
                        gridAdapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        callback.exception(e);
                    }
                });
            } catch (Exception e) {
                callback.exception(e);
            }
        });
    }

    private class GridAdapter extends BaseAdapter {
        private final Context context;
        private String[] colors;
        private GridAdapter(Context context) {
            this.context = context;
            this.colors = new String[0];
        }
        private void updateColors(String[] colors) {
            this.colors = colors;
        }
        @Override
        public int getCount() {
            return colors.length;
        }
        @Override
        public String getItem(int position) {
            return colors[position];
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(R.layout.dialog_color_picker_item, parent, false);
                }
                String color = getItem(position);
                if ("back".equals(color)) {
                    GradientDrawable sd = (GradientDrawable) convertView.getBackground();
                    sd.setColor(Color.BLACK);
                    convertView.findViewById(R.id.sign_selected).setVisibility(View.GONE);
                    convertView.findViewById(R.id.sign_back).setVisibility(View.VISIBLE);
                    ImageView sign_selected_mark = convertView.findViewById(R.id.sign_back_mark);
                    sign_selected_mark.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                } else {
                    GradientDrawable sd = (GradientDrawable) convertView.getBackground();
                    sd.setColor(Color.parseColor(color));
                    if (selectedColorInput != null && color.toLowerCase().equals(selectedColorInput.getText().toString().toLowerCase())) {
                        convertView.findViewById(R.id.sign_selected).setVisibility(View.VISIBLE);
                        ImageView sign_selected_mark = convertView.findViewById(R.id.sign_selected_mark);
                        sign_selected_mark.setImageTintList(ColorStateList.valueOf(Color.parseColor(color) > Color.parseColor("#757575") ? Color.BLACK : Color.WHITE));
                    } else {
                        convertView.findViewById(R.id.sign_selected).setVisibility(View.GONE);
                    }
                    convertView.findViewById(R.id.sign_back).setVisibility(View.GONE);
                }
                return convertView;
            } catch (Exception e) {
                callback.exception(e);
                return null;
            }
        }
    }
}
