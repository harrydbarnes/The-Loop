package com.example.theloop.models;

public class CalendarEvent {
    private final String title;
    private final long startTime;
    private final long endTime;
    private final String location;

    public CalendarEvent(String title, long startTime, long endTime, String location) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
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
