package com.bukhmastov.cdoitmo.model.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.bukhmastov.cdoitmo.util.singleton.TimeUtil;

import java.util.Calendar;
import java.util.Objects;

public class SLesson extends JsonEntity implements Comparable<SLesson> {

    @JsonProperty(value = "subject", order = 1)
    private String subject;

    @JsonProperty(value = "note", order = 2)
    private String note;

    /**
     * lecture, practice, lab, iws
     */
    @JsonProperty(value = "type", order = 3)
    private String type;

    /**
     * 0 - Нечетная
     * 1 - Четная
     * 2 - Обе недели
     */
    @JsonProperty(value = "week", order = 4)
    private int parity;

    @JsonProperty(value = "timeStart", order = 5)
    private String timeStart;

    @JsonProperty(value = "timeEnd", order = 6)
    private String timeEnd;

    @JsonProperty(value = "group", order = 7)
    private String group;

    @JsonProperty(value = "teacher", order = 8)
    private String teacherName;

    @JsonProperty(value = "teacher_id", order = 9)
    private String teacherId;

    @JsonProperty(value = "room", order = 10)
    private String room;

    @JsonProperty(value = "building", order = 11)
    private String building;

    @JsonProperty(value = "cdoitmo_type", order = 12)
    private String cdoitmoType;

    public SLesson() {
        super();
    }

    public SLesson(SLesson lesson) {
        super();
        setSubject(lesson.getSubject());
        setNote(lesson.getNote());
        setType(lesson.getType());
        setParity(lesson.getParity());
        setTimeStart(lesson.getTimeStart());
        setTimeEnd(lesson.getTimeEnd());
        setGroup(lesson.getGroup());
        setTeacherName(lesson.getTeacherName());
        setTeacherId(lesson.getTeacherId());
        setRoom(lesson.getRoom());
        setBuilding(lesson.getBuilding());
        setCdoitmoType(lesson.getCdoitmoType());
    }

    public String getSubjectWithNote() {
        if (StringUtils.isNotBlank(note)) {
            return subject + ": " + note;
        }
        return subject;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getParity() {
        return parity;
    }

    public void setParity(int parity) {
        this.parity = parity;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getCdoitmoType() {
        return cdoitmoType;
    }

    public void setCdoitmoType(String cdoitmoType) {
        this.cdoitmoType = cdoitmoType;
    }

    @Override
    public int compareTo(SLesson lesson) {
        try {
            if (lesson == null) {
                return 0;
            }
            int c = Objects.compare(
                    TimeUtil.time2calendar(Calendar.getInstance(), getTimeStart()),
                    TimeUtil.time2calendar(Calendar.getInstance(), lesson.getTimeStart()),
                    Calendar::compareTo
            );
            if (c == 0) {
                c = Objects.compare(getSubject(), lesson.getSubject(), String::compareTo);
            }
            if (c == 0) {
                c = Objects.compare(getNote(), lesson.getNote(), String::compareTo);
            }
            if (c == 0) {
                c = Objects.compare(getType(), lesson.getType(), String::compareTo);
            }
            if (c == 0) {
                c = Objects.compare(getParity(), lesson.getParity(), Integer::compareTo);
            }
            if (c == 0) {
                c = Objects.compare(getGroup(), lesson.getGroup(), String::compareTo);
            }
            if (c == 0) {
                c = Objects.compare(getTeacherName(), lesson.getTeacherName(), String::compareTo);
            }
            if (c == 0) {
                c = Objects.compare(getRoom(), lesson.getRoom(), String::compareTo);
            }
            if (c == 0) {
                c = Objects.compare(getBuilding(), lesson.getBuilding(), String::compareTo);
            }
            if (c == 0) {
                c = Objects.compare(getCdoitmoType(), lesson.getCdoitmoType(), String::compareTo);
            }
            return c;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SLesson)) return false;
        SLesson sLesson = (SLesson) o;
        return parity == sLesson.parity &&
                Objects.equals(subject, sLesson.subject) &&
                Objects.equals(note, sLesson.note) &&
                Objects.equals(type, sLesson.type) &&
                Objects.equals(timeStart, sLesson.timeStart) &&
                Objects.equals(timeEnd, sLesson.timeEnd) &&
                Objects.equals(group, sLesson.group) &&
                Objects.equals(teacherName, sLesson.teacherName) &&
                Objects.equals(teacherId, sLesson.teacherId) &&
                Objects.equals(room, sLesson.room) &&
                Objects.equals(building, sLesson.building) &&
                Objects.equals(cdoitmoType, sLesson.cdoitmoType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, note, type, parity, timeStart, timeEnd, group, teacherName, teacherId, room, building, cdoitmoType);
    }

    @Override
    public String toString() {
        return "SLesson{" +
                "subject='" + subject + '\'' +
                ", note='" + note + '\'' +
                ", type='" + type + '\'' +
                ", parity=" + parity +
                ", timeStart='" + timeStart + '\'' +
                ", timeEnd='" + timeEnd + '\'' +
                ", group='" + group + '\'' +
                ", teacherName='" + teacherName + '\'' +
                ", teacherId='" + teacherId + '\'' +
                ", room='" + room + '\'' +
                ", building='" + building + '\'' +
                ", cdoitmoType='" + cdoitmoType + '\'' +
                '}';
    }
}
