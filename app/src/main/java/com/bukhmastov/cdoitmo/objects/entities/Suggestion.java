package com.bukhmastov.cdoitmo.objects.entities;

public class Suggestion {
    public int icon;
    public String query;
    public String label;
    public Suggestion(String query, String label, int icon){
        this.icon = icon;
        this.query = query;
        this.label = label;
    }
}