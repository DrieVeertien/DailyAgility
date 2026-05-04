package com.mvpief;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class DailyAgilityPanel extends PluginPanel
{
    // region Colors

    private static final Color COLOR_BG           = ColorScheme.DARK_GRAY_COLOR;
    private static final Color COLOR_CARD         = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color COLOR_ACCENT       = new Color(200, 150, 0);   // gold
    private static final Color COLOR_TODAY_BORDER = new Color(200, 150, 0);
    private static final Color COLOR_SELECTED_BG  = new Color(80, 60, 0);
    private static final Color COLOR_HAS_DATA     = new Color(60, 100, 60);
    private static final Color COLOR_TEXT         = Color.WHITE;
    private static final Color COLOR_MUTED        = ColorScheme.LIGHT_GRAY_COLOR;
    private static final Color COLOR_HEADER_TEXT  = ColorScheme.MEDIUM_GRAY_COLOR;

    // endregion

    // region State

    private final JPanel      logContainer;
    private final JPanel      calendarGrid   = new JPanel();
    private final JLabel      monthYearLabel = new JLabel();

    private YearMonth         viewMonth      = YearMonth.now();
    private LocalDate         selectedDate   = null;   // null = all dates
    private Set<LocalDate>    datesWithData  = new HashSet<>();

    private Consumer<LocalDate> onDateSelected = date -> {};

    // endregion

    // region Constructor

    public DailyAgilityPanel()
    {
        super(false);
        setLayout(new BorderLayout(0, 8));
        setBackground(COLOR_BG);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        add(buildNorth(), BorderLayout.NORTH);

        logContainer = new JPanel();
        logContainer.setLayout(new BoxLayout(logContainer, BoxLayout.Y_AXIS));
        logContainer.setBackground(COLOR_BG);

        JScrollPane scrollPane = new JScrollPane(logContainer);
        scrollPane.setBorder(null);
        scrollPane.setBackground(COLOR_BG);
        scrollPane.getViewport().setBackground(COLOR_BG);
        add(scrollPane, BorderLayout.CENTER);
    }

    // endregion

    // region Public API

    public void setOnDateSelected(Consumer<LocalDate> listener)
    {
        this.onDateSelected = listener;
    }

    /**
     * Tell the calendar which dates have log data so it can highlight them.
     * Call this once after LogStore is ready, and again if new data comes in.
     */
    public void setDatesWithData(Set<LocalDate> dates)
    {
        this.datesWithData = dates;
        SwingUtilities.invokeLater(this::rebuildCalendarGrid);
    }

    public void setLogEntries(List<LogEntry> entries)
    {
        SwingUtilities.invokeLater(() ->
        {
            logContainer.removeAll();

            if (entries.isEmpty())
            {
                JLabel empty = new JLabel("No data for this selection.");
                empty.setForeground(COLOR_MUTED);
                empty.setFont(FontManager.getRunescapeSmallFont());
                empty.setBorder(new EmptyBorder(4, 0, 0, 0));
                logContainer.add(empty);
            }
            else
            {
                for (LogEntry entry : entries)
                {
                    logContainer.add(buildRow(entry));
                    logContainer.add(Box.createVerticalStrut(4));
                }
            }

            logContainer.revalidate();
            logContainer.repaint();
        });
    }

    // endregion

    // region Header + Title

    private JPanel buildNorth()
    {
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBackground(COLOR_BG);

        // Title
        JLabel title = new JLabel("Daily Agility Log");
        title.setForeground(COLOR_TEXT);
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(title);

        north.add(Box.createVerticalStrut(8));

        // Calendar card
        JPanel calendarCard = new JPanel(new BorderLayout(0, 4));
        calendarCard.setBackground(COLOR_CARD);
        calendarCard.setBorder(new EmptyBorder(8, 8, 8, 8));
        calendarCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        calendarCard.add(buildMonthNav(), BorderLayout.NORTH);
        calendarCard.add(buildDayHeaders(), BorderLayout.CENTER);

        calendarGrid.setBackground(COLOR_CARD);
        calendarCard.add(calendarGrid, BorderLayout.SOUTH);

        north.add(calendarCard);
        north.add(Box.createVerticalStrut(4));

        // "Show all" button beneath the calendar
        JButton allButton = buildAllButton();
        allButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(allButton);

        north.add(Box.createVerticalStrut(6));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        north.add(sep);

        rebuildCalendarGrid();

        return north;
    }

    // endregion

    // region Month navigation

    private JPanel buildMonthNav()
    {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(COLOR_CARD);

        JButton prev = buildNavArrow("‹");
        prev.addActionListener(e ->
        {
            viewMonth = viewMonth.minusMonths(1);
            rebuildCalendarGrid();
        });

        JButton next = buildNavArrow("›");
        next.addActionListener(e ->
        {
            // Don't navigate into the future
            if (viewMonth.isBefore(YearMonth.now()))
            {
                viewMonth = viewMonth.plusMonths(1);
                rebuildCalendarGrid();
            }
        });

        monthYearLabel.setForeground(COLOR_TEXT);
        monthYearLabel.setFont(FontManager.getRunescapeSmallFont());
        monthYearLabel.setHorizontalAlignment(SwingConstants.CENTER);

        nav.add(prev, BorderLayout.WEST);
        nav.add(monthYearLabel, BorderLayout.CENTER);
        nav.add(next, BorderLayout.EAST);

        return nav;
    }

    private JButton buildNavArrow(String symbol)
    {
        JButton btn = new JButton(symbol);
        btn.setForeground(COLOR_MUTED);
        btn.setBackground(COLOR_CARD);
        btn.setBorder(new EmptyBorder(2, 6, 2, 6));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(btn.getFont().deriveFont(16f));
        return btn;
    }

    // endregion

    // region Calendar grid

    /**
     * Day-of-week column headers: Mo Tu We Th Fr Sa Su
     */
    private JPanel buildDayHeaders()
    {
        JPanel headers = new JPanel(new GridLayout(1, 7, 2, 0));
        headers.setBackground(COLOR_CARD);
        headers.setBorder(new EmptyBorder(4, 0, 2, 0));

        // Start week on Monday to match EU convention
        for (DayOfWeek dow : DayOfWeek.values())
        {
            JLabel lbl = new JLabel(
                    dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).substring(0, 2),
                    SwingConstants.CENTER
            );
            lbl.setForeground(COLOR_HEADER_TEXT);
            lbl.setFont(FontManager.getRunescapeSmallFont());
            headers.add(lbl);
        }

        return headers;
    }

    private void rebuildCalendarGrid()
    {
        calendarGrid.removeAll();

        // 6 rows max, 7 columns
        calendarGrid.setLayout(new GridLayout(0, 7, 2, 2));

        monthYearLabel.setText(
                viewMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                        + " " + viewMonth.getYear()
        );

        LocalDate firstOfMonth = viewMonth.atDay(1);
        LocalDate today        = LocalDate.now();

        // Monday = 1, so offset = dayOfWeek - 1
        int leadingBlanks = firstOfMonth.getDayOfWeek().getValue() - 1;

        for (int i = 0; i < leadingBlanks; i++)
        {
            calendarGrid.add(emptyCell());
        }

        for (int day = 1; day <= viewMonth.lengthOfMonth(); day++)
        {
            LocalDate date = viewMonth.atDay(day);
            boolean isToday    = date.equals(today);
            boolean isSelected = date.equals(selectedDate);
            boolean hasData    = datesWithData.contains(date);
            boolean isFuture   = date.isAfter(today);

            calendarGrid.add(buildDayCell(date, isToday, isSelected, hasData, isFuture));
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private JPanel buildDayCell(LocalDate date, boolean isToday, boolean isSelected,
                                boolean hasData, boolean isFuture)
    {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setPreferredSize(new Dimension(28, 24));

        if (isSelected)
        {
            cell.setBackground(COLOR_SELECTED_BG);
            cell.setBorder(new LineBorder(COLOR_ACCENT, 1));
        }
        else if (isToday)
        {
            cell.setBackground(COLOR_CARD);
            cell.setBorder(new LineBorder(COLOR_TODAY_BORDER, 1));
        }
        else if (hasData)
        {
            cell.setBackground(COLOR_HAS_DATA);
            cell.setBorder(new EmptyBorder(1, 1, 1, 1));
        }
        else
        {
            cell.setBackground(COLOR_CARD);
            cell.setBorder(new EmptyBorder(1, 1, 1, 1));
        }

        JLabel lbl = new JLabel(String.valueOf(date.getDayOfMonth()), SwingConstants.CENTER);
        lbl.setFont(FontManager.getRunescapeSmallFont());
        lbl.setForeground(isFuture ? COLOR_HEADER_TEXT : COLOR_TEXT);
        cell.add(lbl, BorderLayout.CENTER);

        if (!isFuture)
        {
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cell.addMouseListener(new java.awt.event.MouseAdapter()
            {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e)
                {
                    selectedDate = date;
                    rebuildCalendarGrid();
                    onDateSelected.accept(date);
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e)
                {
                    if (!date.equals(selectedDate))
                    {
                        cell.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e)
                {
                    if (!date.equals(selectedDate))
                    {
                        cell.setBackground(hasData ? COLOR_HAS_DATA : COLOR_CARD);
                    }
                }
            });
        }

        return cell;
    }

    private JPanel emptyCell()
    {
        JPanel cell = new JPanel();
        cell.setBackground(COLOR_CARD);
        cell.setPreferredSize(new Dimension(28, 24));
        return cell;
    }

    // endregion

    // region All button

    private JButton buildAllButton()
    {
        JButton btn = new JButton("Show all dates");
        btn.setFont(FontManager.getRunescapeSmallFont());
        btn.setForeground(COLOR_TEXT);
        btn.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        btn.setBorder(new EmptyBorder(4, 8, 4, 8));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        btn.addActionListener(e ->
        {
            selectedDate = null;
            rebuildCalendarGrid();
            onDateSelected.accept(null);
        });
        return btn;
    }

    // endregion

    // region Log rows

    private JPanel buildRow(LogEntry entry)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(COLOR_CARD);
        row.setBorder(new EmptyBorder(6, 8, 6, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        row.add(getLeft(entry), BorderLayout.WEST);
        row.add(getRight(entry), BorderLayout.EAST);

        return row;
    }

    @Nonnull
    private static JPanel getLeft(LogEntry entry)
    {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel dateLabel = new JLabel(entry.getDate());
        dateLabel.setForeground(Color.WHITE);
        dateLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel courseLabel = new JLabel(entry.getCourse());
        courseLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        courseLabel.setFont(FontManager.getRunescapeSmallFont());

        left.add(dateLabel);
        left.add(courseLabel);
        return left;
    }

    @Nonnull
    private static JPanel getRight(LogEntry entry)
    {
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel lapsLabel = new JLabel(entry.getLaps() + " laps");
        lapsLabel.setForeground(Color.WHITE);
        lapsLabel.setFont(FontManager.getRunescapeSmallFont());
        lapsLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel marksLabel = new JLabel(entry.getMarks() + " marks");
        marksLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        marksLabel.setFont(FontManager.getRunescapeSmallFont());
        marksLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        right.add(lapsLabel);
        right.add(marksLabel);
        return right;
    }

    // endregion
}