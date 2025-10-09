package com.example.theloop.models;

public class CalendarEvent {
    private final long id;
    private final String title;
    private final long startTime;
    private final long endTime;
    private final String location;

    public CalendarEvent(long id, String title, long startTime, long endTime, String location) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getLocation() {
        return location;
    }
}
