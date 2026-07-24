/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.manager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;

/**
 * Shapes an {@link AuthorityTimetable} into the plain map/list model the {@code timetable.ftl}
 * template consumes: authority metadata plus rows grouped by month with times formatted as
 * 24-hour {@code HH:MM}. A pure presentation transform (no business logic), kept out of the
 * manager so its orchestration stays thin.
 */
final class TimetableViewModel {

    private static final String[] MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};

    private TimetableViewModel() {
    }

    static Map<String, Object> of(AuthorityTimetable table) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("authority", table.authorityName());
        model.put("authorityCode", table.authority());
        model.put("url", table.authorityUrl());
        model.put("zone", table.zoneName());
        model.put("year", table.year());

        List<Map<String, Object>> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            List<Map<String, Object>> days = new ArrayList<>();
            for (TimetableRow row : table.rows()) {
                if (row.month() == month) {
                    days.add(day(row));
                }
            }
            if (!days.isEmpty()) {
                months.add(Map.of("name", MONTHS[month - 1], "days", days));
            }
        }
        model.put("months", months);
        return model;
    }

    private static Map<String, Object> day(TimetableRow r) {
        Map<String, Object> day = new LinkedHashMap<>();
        day.put("day", r.day());
        day.put("fajr", hhmm(r.fajr()));
        day.put("sunrise", hhmm(r.sunrise()));
        day.put("dhuhr", hhmm(r.dhuhr()));
        day.put("asr", hhmm(r.asr()));
        day.put("maghrib", hhmm(r.maghrib()));
        day.put("isha", hhmm(r.isha()));
        return day;
    }

    /** Minutes from local midnight to 24-hour HH:MM. */
    static String hhmm(int minutes) {
        int m = ((minutes % 1440) + 1440) % 1440;
        return "%02d:%02d".formatted(m / 60, m % 60);
    }
}
