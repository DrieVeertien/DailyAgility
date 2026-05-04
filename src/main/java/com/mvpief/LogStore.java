package com.mvpief;

import net.runelite.client.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class LogStore
{
    private static final String KEY_KNOWN_COURSES = "knownCourses";
    private static final String KEY_KNOWN_DATES = "knownDates";

    @Inject private ConfigManager configManager;
    @Inject private DailyAgilityConfig config;

    // region Public API

    /**
     * Increments the lap count for the given course on today's date.
     * Also registers the course and date if it hasn't been seen before.
     */
    public void recordLap(String course)
    {
        if (!config.logEntries()) return;
        String date = LocalDate.now().toString();
        LogEntry current = getEntry(date, course);
        saveEntry(date, course, current.getLaps() + 1, current.getMarks());
        registerCourse(course);
        registerDate(date);
    }

    /**
     * Increments the mark count for the given course on today's date.
     */
    public void recordMarks(String course, int count)
    {
        if (!config.logEntries()) return;
        String date = LocalDate.now().toString();
        LogEntry current = getEntry(date, course);
        saveEntry(date, course, current.getLaps(), current.getMarks() + count);
    }

    public List<LogEntry> getEntries(String courseFilter)
    {
        return getEntries(courseFilter, null);
    }

    public List<LogEntry> getEntries(String courseFilter, LocalDate dateFilter)
    {
        List<String> courses = courseFilter != null
                ? Collections.singletonList(courseFilter)
                : getKnownCourses();

        List<String> dates = dateFilter != null
                ? Collections.singletonList(dateFilter.toString())
                : getKnownDates();

        List<LogEntry> entries = new ArrayList<>();

        for (String dateStr : dates)
        {
            for (String course : courses)
            {
                String key = buildKey(dateStr, course);
                String stored = configManager.getConfiguration(DailyAgilityConfig.GROUP, key);
                if (stored != null && !stored.isEmpty())
                {
                    entries.add(parse(dateStr, course, stored));
                }
            }
        }

        return entries;
    }

    public List<String> getKnownDates()
    {
        String stored = configManager.getConfiguration(DailyAgilityConfig.GROUP, KEY_KNOWN_DATES);
        if (stored == null || stored.isEmpty()) return new ArrayList<>();
        return Arrays.stream(stored.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                // ISO date strings sort lexicographically, newest first
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of every course that has ever been logged.
     */
    public List<String> getKnownCourses()
    {
        String stored = configManager.getConfiguration(DailyAgilityConfig.GROUP, KEY_KNOWN_COURSES);
        if (stored == null || stored.isEmpty()) return new ArrayList<>();
        return Arrays.stream(stored.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // endregion

    // region Internals

    private LogEntry getEntry(String date, String course)
    {
        String key = buildKey(date, course);
        String stored = configManager.getConfiguration(DailyAgilityConfig.GROUP, key);
        if (stored == null || stored.isEmpty()) return new LogEntry(date, course, 0, 0);
        return parse(date, course, stored);
    }

    private void saveEntry(String date, String course, int laps, int marks)
    {
        String key = buildKey(date, course);
        configManager.setConfiguration(DailyAgilityConfig.GROUP, key, laps + "," + marks);
    }

    private void registerCourse(String course)
    {
        List<String> courses = getKnownCourses();
        if (courses.contains(course)) return;
        courses.add(course);
        configManager.setConfiguration(
                DailyAgilityConfig.GROUP,
                KEY_KNOWN_COURSES,
                String.join(",", courses)
        );
    }

    private void registerDate(String date)
    {
        List<String> dates = getKnownDates();
        if (dates.contains(date)) return;
        dates.add(date);
        configManager.setConfiguration(
                DailyAgilityConfig.GROUP,
                KEY_KNOWN_DATES,
                String.join(",", dates)
        );
    }

    private LogEntry parse(String date, String course, String stored)
    {
        try
        {
            String[] parts = stored.split(",");
            int laps = Integer.parseInt(parts[0]);
            int marks = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return new LogEntry(date, course, laps, marks);
        }
        catch (Exception e)
        {
            log.error("Failed to parse log entry for {} on {}: {}", course, date, stored);
            return new LogEntry(date, course, 0, 0);
        }
    }

    private String buildKey(String date, String course)
    {
        // e.g. "log_2026-05-05_seers_village"
        return "log_" + date + "_" + course.toLowerCase().replace(" ", "_");
    }

    // endregion
}