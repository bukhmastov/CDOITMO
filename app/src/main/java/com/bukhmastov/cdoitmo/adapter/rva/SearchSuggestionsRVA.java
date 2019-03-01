package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.entity.Suggestion;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

public class SearchSuggestionsRVA extends RVA<Suggestion> {

    private static final int TYPE_SUGGESTION = 0;

    private final Context context;

    public SearchSuggestionsRVA(@NonNull Context context, @NonNull Collection<Suggestion> suggestions) {
        this.context = context;
        addItems(entity2dataset(suggestions));
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_SUGGESTION: layout = R.layout.search_suggestion; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_SUGGESTION: {
                bindSuggestion(container, item);
                break;
            }
        }
    }

    private void bindSuggestion(View container, Item<Suggestion> item) {
        try {
            Suggestion suggestion = item.data;
            ((ImageView) container.findViewById(R.id.typeIcon)).setImageDrawable(context.getDrawable(suggestion.icon));
            ((TextView) container.findViewById(R.id.label)).setText(suggestion.title);

            tryRegisterClickListener(container, R.id.click, suggestion);
            if (suggestion.removable) {
                tryRegisterClickListener(container, R.id.remove, suggestion);
            } else {
                container.findViewById(R.id.remove).setVisibility(View.GONE);
            }

        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private Collection<Item> entity2dataset(Collection<Suggestion> suggestions) {
        final Collection<Item> dataset = new ArrayList<>();
        if (CollectionUtils.isEmpty(suggestions)) {
            return dataset;
        }
        for (Suggestion suggestion : suggestions) {
            dataset.add(new Item<>(TYPE_SUGGESTION, suggestion));
        }
        return dataset;
    }
}
