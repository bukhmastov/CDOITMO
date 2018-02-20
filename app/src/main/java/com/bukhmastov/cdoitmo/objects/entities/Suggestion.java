package com.bukhmastov.cdoitmo.objects.entities;

public class Suggestion {
    public final int icon;
    public final String query;
    public final String title;
    public Suggestion(String query, String title, int icon){
        this.icon = icon;
        this.query = query;
        this.title = title;
    }
}