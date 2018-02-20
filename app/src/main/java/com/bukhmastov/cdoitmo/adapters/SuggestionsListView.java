package com.bukhmastov.cdoitmo.adapters;

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
import com.bukhmastov.cdoitmo.objects.entities.Suggestion;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.List;

public class SuggestionsListView extends ArrayAdapter<Suggestion> {

    private final Context context;
    private final List<Suggestion> suggestions;

    public SuggestionsListView(Context context, List<Suggestion> suggestions) {
        super(context, R.layout.layout_search_suggestion, suggestions);
        this.context = context;
        this.suggestions = suggestions;
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
            ((ImageView) convertView.findViewById(R.id.icon)).setImageDrawable(context.getDrawable(suggestion.icon));
            ((TextView) convertView.findViewById(R.id.label)).setText(suggestion.title);
            return convertView;
        } catch (Exception e) {
            Static.error(e);
            return super.getView(position, convertView, parent);
        }
    }

}
