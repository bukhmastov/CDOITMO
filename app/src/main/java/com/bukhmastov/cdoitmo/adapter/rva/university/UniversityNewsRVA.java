package com.bukhmastov.cdoitmo.adapter.rva.university;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.rva.RVAUniversity;
import com.bukhmastov.cdoitmo.model.university.news.UNewsItem;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class UniversityNewsRVA extends UniversityRVA {

    public UniversityNewsRVA(Context context) {
        super(context);
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: layout = R.layout.layout_university_update_time; break;
            case TYPE_MAIN: layout = R.layout.layout_university_news_card; break;
            case TYPE_MINOR: layout = R.layout.layout_university_news_card_compact; break;
            case TYPE_STATE: layout = R.layout.layout_university_list_item_state; break;
            case TYPE_NO_DATA: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: bindInfoAboutUpdateTime(container, item); break;
            case TYPE_MAIN: bindMain(container, item); break;
            case TYPE_MINOR: bindMinor(container, item); break;
            case TYPE_STATE: bindState(container, item); break;
            case TYPE_NO_DATA: bindNoData(container); break;
        }
    }

    private void bindMain(View container, Item<UNewsItem> item) {
        try {
            UNewsItem newsItem = item.data;
            if (StringUtils.isBlank(newsItem.getTitle())) {
                return;
            }
            tryRegisterClickListener(container, R.id.news_click, new RVAUniversity(newsItem));
            View imageContainer = container.findViewById(R.id.news_image_container);
            if (imageContainer != null) {
                String img = newsItem.getImg();
                if (StringUtils.isBlank(img)) {
                    img = newsItem.getImgSmall();
                }
                if (StringUtils.isNotBlank(img)) {
                    View imageView = container.findViewById(R.id.news_image);
                    if (imageView != null) {
                        Picasso.with(context)
                                .load(img)
                                .into(container.findViewById(R.id.news_image), new Callback() {
                                    @Override
                                    public void onSuccess() {}
                                    @Override
                                    public void onError() {
                                        staticUtil.removeView(imageContainer);
                                    }
                                });
                    }
                } else {
                    staticUtil.removeView(imageContainer);
                }
            }
            View titleView = container.findViewById(R.id.title);
            if (titleView != null) {
                ((TextView) titleView).setText(textUtils.escapeString(newsItem.getTitle()));
            }
            View categoriesView = container.findViewById(R.id.categories);
            if (categoriesView != null) {
                boolean categoryParentExists = StringUtils.isNotBlank(newsItem.getCategoryParent());
                boolean categoryChildExists = StringUtils.isNotBlank(newsItem.getCategoryChild());
                if (categoryParentExists || categoryChildExists) {
                    if (Objects.equals(newsItem.getCategoryParent(), newsItem.getCategoryChild())) {
                        categoryChildExists = false;
                    }
                    String category = "";
                    if (categoryParentExists) {
                        category += newsItem.getCategoryParent();
                        if (categoryChildExists) {
                            category += " ► ";
                        }
                    }
                    if (categoryChildExists) {
                        category += newsItem.getCategoryChild();
                    }
                    if (!category.isEmpty()) {
                        category = "● " + category;
                        TextView categories = (TextView) categoriesView;
                        categories.setText(category);
                        if (StringUtils.isNotBlank(newsItem.getColorHex())) {
                            categories.setTextColor(Color.parseColor(newsItem.getColorHex()));
                        }
                    } else {
                        staticUtil.removeView(categoriesView);
                    }
                } else {
                    staticUtil.removeView(categoriesView);
                }
            }
            View anonsView = container.findViewById(R.id.anons);
            if (anonsView != null) {
                if (StringUtils.isNotBlank(newsItem.getAnons())) {
                    ((TextView) anonsView).setText(textUtils.escapeString(newsItem.getAnons()));
                } else {
                    staticUtil.removeView(anonsView);
                }
            }
            View dateView = container.findViewById(R.id.date);
            View countViewContainerView = container.findViewById(R.id.count_view_container);
            View countView = container.findViewById(R.id.count_view);
            View infoContainerView = container.findViewById(R.id.info_container);
            boolean dateExists = StringUtils.isNotBlank(newsItem.getDatePublication());
            boolean countExists = newsItem.getViewCount() >= 0;
            if (dateExists || countExists) {
                if (dateView != null) {
                    if (dateExists) {
                        ((TextView) dateView).setText(textUtils.cuteDate(context, storagePref, "yyyy-MM-dd HH:mm:ss", newsItem.getDatePublication()));
                    } else {
                        staticUtil.removeView(dateView);
                    }
                }
                if (countExists) {
                    if (countView != null) {
                        ((TextView) countView).setText(String.valueOf(newsItem.getViewCount()));
                    }
                } else {
                    if (countViewContainerView != null) {
                        staticUtil.removeView(countViewContainerView);
                    }
                }
            } else {
                if (infoContainerView != null) {
                    staticUtil.removeView(infoContainerView);
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindMinor(View container, Item<UNewsItem> item) {
        try {
            UNewsItem newsItem = item.data;
            if (StringUtils.isBlank(newsItem.getTitle())) {
                return;
            }
            tryRegisterClickListener(container, R.id.news_click, new RVAUniversity(newsItem));
            View imageContainer = container.findViewById(R.id.news_image_container);
            if (imageContainer != null) {
                String img = newsItem.getImgSmall();
                if (StringUtils.isBlank(img)) {
                    img = newsItem.getImg();
                }
                if (StringUtils.isNotBlank(img)) {
                    View imageView = container.findViewById(R.id.news_image);
                    if (imageView != null) {
                        Picasso.with(context)
                                .load(img)
                                .into(container.findViewById(R.id.news_image), new Callback() {
                                    @Override
                                    public void onSuccess() {}
                                    @Override
                                    public void onError() {
                                        staticUtil.removeView(imageContainer);
                                    }
                                });
                    }
                } else {
                    staticUtil.removeView(imageContainer);
                }
            }
            View titleView = container.findViewById(R.id.title);
            if (titleView != null) {
                ((TextView) titleView).setText(textUtils.escapeString(newsItem.getTitle()));
            }
            View categoriesView = container.findViewById(R.id.categories);
            if (categoriesView != null) {
                boolean categoryParentExists = StringUtils.isNotBlank(newsItem.getCategoryParent());
                boolean categoryChildExists = StringUtils.isNotBlank(newsItem.getCategoryChild());
                if (categoryParentExists || categoryChildExists) {
                    if (Objects.equals(newsItem.getCategoryParent(), newsItem.getCategoryChild())) {
                        categoryChildExists = false;
                    }
                    String category = "";
                    if (categoryParentExists) {
                        category += newsItem.getCategoryParent();
                        if (categoryChildExists) {
                            category += " ► ";
                        }
                    }
                    if (categoryChildExists) {
                        category += newsItem.getCategoryChild();
                    }
                    if (!category.isEmpty()) {
                        category = "● " + category;
                        TextView categories = (TextView) categoriesView;
                        categories.setText(category);
                        if (StringUtils.isNotBlank(newsItem.getColorHex())) {
                            categories.setTextColor(Color.parseColor(newsItem.getColorHex()));
                        }
                    } else {
                        staticUtil.removeView(categoriesView);
                    }
                } else {
                    staticUtil.removeView(categoriesView);
                }
            }
            View dateView = container.findViewById(R.id.date);
            View countViewContainerView = container.findViewById(R.id.count_view_container);
            View countView = container.findViewById(R.id.count_view);
            View infoContainerView = container.findViewById(R.id.info_container);
            boolean dateExists = StringUtils.isNotBlank(newsItem.getDatePublication());
            boolean countExists = newsItem.getViewCount() >= 0;
            if (dateExists || countExists) {
                if (dateView != null) {
                    if (dateExists) {
                        ((TextView) dateView).setText(textUtils.cuteDate(context, storagePref, "yyyy-MM-dd HH:mm:ss", newsItem.getDatePublication()));
                    } else {
                        staticUtil.removeView(dateView);
                    }
                }
                if (countExists) {
                    if (countView != null) {
                        ((TextView) countView).setText(String.valueOf(newsItem.getViewCount()));
                    }
                } else {
                    if (countViewContainerView != null) {
                        staticUtil.removeView(countViewContainerView);
                    }
                }
            } else {
                if (infoContainerView != null) {
                    staticUtil.removeView(infoContainerView);
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindState(View container, Item item) {
        try {
            tryRegisterClickListener(container, (int) item.extras.get(DATA_STATE_KEEP), null);
            if (container instanceof ViewGroup) {
                ViewGroup containerGroup = (ViewGroup) container;
                for (int i = containerGroup.getChildCount() - 1; i >= 0; i--) {
                    View child = containerGroup.getChildAt(i);
                    if (child.getId() == (int) item.extras.get(DATA_STATE_KEEP)) {
                        child.setVisibility(View.VISIBLE);
                    } else {
                        child.setVisibility(View.GONE);
                    }
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNoData(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_news);
        } catch (Exception e) {
            log.exception(e);
        }
    }
}
