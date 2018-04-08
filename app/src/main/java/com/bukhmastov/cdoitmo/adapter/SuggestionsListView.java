package com.bukhmastov.cdoitmo.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.object.entity.Suggestion;
import com.bukhmastov.cdoitmo.util.Static;

import java.util.List;

public class SuggestionsListView extends ArrayAdapter<Suggestion> {

    public interface OnClickCallback {
        void onClick(Suggestion suggestion);
        void onRemove(Suggestion suggestion);
    }

    private final Context context;
    private final List<Suggestion> suggestions;
    private final OnClickCallback onClickCallback;

    public SuggestionsListView(@NonNull Context context, @NonNull List<Suggestion> suggestions) {
        this(context, suggestions, null);
    }
    public SuggestionsListView(@NonNull Context context, @NonNull List<Suggestion> suggestions, @Nullable OnClickCallback onClickCallback) {
        super(context, R.layout.layout_search_suggestion, suggestions);
        this.context = context;
        this.suggestions = suggestions;
        this.onClickCallback = onClickCallback;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        try {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.layout_search_suggestion, parent, false);
            }
            Suggestion suggestion = suggestions.get(position);
            ((ImageView) convertView.findViewById(R.id.typeIcon)).setImageDrawable(context.getDrawable(suggestion.icon));
            ((TextView) convertView.findViewById(R.id.label)).setText(suggestion.title);
            if (onClickCallback != null) {
                convertView.setOnClickListener(v -> onClickCallback.onClick(suggestion));
                convertView.setClickable(true);
                convertView.setFocusable(true);
                if (suggestion.removable) {
                    convertView.findViewById(R.id.remove).setOnClickListener(v -> onClickCallback.onRemove(suggestion));
                    convertView.findViewById(R.id.remove).setVisibility(View.VISIBLE);
                } else {
                    convertView.findViewById(R.id.remove).setVisibility(View.GONE);
                }
            } else {
                convertView.findViewById(R.id.remove).setVisibility(View.GONE);
            }
            return convertView;
        } catch (Exception e) {
            Static.error(e);
            return super.getView(position, convertView, parent);
        }
    }

}
