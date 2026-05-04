package com.mvpief;

import lombok.Value;

@Value
public class LogEntry
{
    String date;
    String course;
    int laps;
    int marks;
}