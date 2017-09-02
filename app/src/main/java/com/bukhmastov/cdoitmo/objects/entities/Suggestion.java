package com.bukhmastov.cdoitmo.objects.entities;

public class Suggestion {
    public final int icon;
    public final String query;
    public final String label;
    public Suggestion(String query, String label, int icon){
        this.icon = icon;
        this.query = query;
        this.label = label;
    }
}