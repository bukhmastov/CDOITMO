package com.bukhmastov.cdoitmo.model.widget.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class WSLTheme extends JsonEntity {

    @JsonProperty("background")
    private String background;

    @JsonProperty("text")
    private String text;

    @JsonProperty("opacity")
    private int opacity;

    public WSLTheme() {
        super();
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getOpacity() {
        return opacity;
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WSLTheme)) return false;
        WSLTheme wslTheme = (WSLTheme) o;
        return opacity == wslTheme.opacity &&
                Objects.equals(background, wslTheme.background) &&
                Objects.equals(text, wslTheme.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(background, text, opacity);
    }

    @Override
    public String toString() {
        return "WSLTheme{" +
                "background='" + background + '\'' +
                ", text='" + text + '\'' +
                ", opacity=" + opacity +
                '}';
    }
}
