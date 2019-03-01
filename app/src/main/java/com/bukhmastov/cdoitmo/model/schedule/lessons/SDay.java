package com.bukhmastov.cdoitmo.model.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.Objects;

public class SDay extends JsonEntity implements Comparable<SDay> {

    /**
     * Расписание на определенный день
     * 0 - Понедельник
     * 6 - Воскресенье
     * null - Расписание на определенный день
     */
    @JsonProperty("weekday")
    private Integer weekday;

    /**
     * Тип расписания
     * regular - Обычное расписание на определенный день недели. При этом weekday != null
     * date - Расписание на определенный день. При этом weekday == null
     * unknown - deprecated alias for regular
     */
    @JsonProperty("type")
    private String type;

    /**
     * Заголовок дня расписания, учитывается при type=date
     * Формат:
     * dd.MM.yyyy - расписание на определенную дату
     * любая другая строка - расписание с произвольным заголовком
     */
    @JsonProperty("title")
    private String customDay;

    @JsonProperty("lessons")
    private ArrayList<SLesson> lessons;

    public SDay() {
        super();
    }

    public SDay(int weekday, SLesson lesson) {
        super();
        this.weekday = weekday;
        this.type = "regular";
        this.customDay = "";
        this.lessons = new ArrayList<>();
        this.lessons.add(lesson);
    }

    public SDay(String customDay, SLesson lesson) {
        super();
        this.weekday = null;
        this.type = "date";
        this.customDay = customDay;
        this.lessons = new ArrayList<>();
        this.lessons.add(lesson);
    }

    public Integer getWeekday() {
        return weekday;
    }

    public String getTitle() {
        return customDay;
    }

    public String getType() {
        return type;
    }

    public ArrayList<SLesson> getLessons() {
        return lessons;
    }

    public void setLessons(ArrayList<SLesson> lessons) {
        this.lessons = lessons;
    }

    public void addLesson(SLesson lesson) {
        if (this.lessons == null) {
            this.lessons = new ArrayList<>();
        }
        this.lessons.add(lesson);
    }

    public boolean isMatched(Integer weekday, String customDay) {
        if (this.weekday != null) {
            if (weekday != null) {
                return Objects.equals(this.weekday, weekday);
            }
            return false;
        }
        if (this.customDay != null) {
            if (customDay != null) {
                return Objects.equals(this.customDay, customDay);
            }
            return false;
        }
        return false;
    }

    @Override
    public int compareTo(SDay day) {
        if (day == null) {
            return 0;
        }
        int c = 0;
        if (Objects.equals(getType(), day.getType())) {
            if (getWeekday() != null && day.getWeekday() != null) {
                c = Objects.compare(getWeekday(), day.getWeekday(), Integer::compareTo);
            }
            if (c == 0 && getTitle() != null && day.getTitle() != null) {
                if (getTitle().matches("[0-9]+") && !day.getTitle().matches("[0-9]+")) {
                    c = -1;
                } else if (day.getTitle().matches("[0-9]+") && !getTitle().matches("[0-9]+")) {
                    c = 1;
                } else {
                    c = Objects.compare(getTitle(), day.getTitle(), String::compareTo);
                }
            }
        } else {
            if (getWeekday() == null && day.getWeekday() != null) {
                c = 1;
            } else {
                c = -1;
            }
        }
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SDay)) return false;
        SDay sDay = (SDay) o;
        return Objects.equals(weekday, sDay.weekday) &&
                Objects.equals(customDay, sDay.customDay) &&
                Objects.equals(type, sDay.type) &&
                Objects.equals(lessons, sDay.lessons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weekday, customDay, type, lessons);
    }

    @Override
    public String toString() {
        return "SDay{" +
                "weekday=" + weekday +
                ", customDay='" + customDay + '\'' +
                ", type='" + type + '\'' +
                ", lessons=" + lessons +
                '}';
    }
}
