/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.app.scrape;

import java.util.HashMap;
import java.util.Map;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;

/**
 * Compares a freshly scraped zone table against the last committed table for the same zone - the
 * S1 safety net (ADR-0017). It reports, per zone, how many days matched, how many committed days
 * are missing from the scrape, and the largest per-minute drift on any field (the six published
 * times plus the enriched solar-noon, so the enrichment is regression-checked against the committed
 * values too). The report is advisory: the operator reviews it before committing. Zero drift on a
 * same-year re-scrape means the parser is faithful; a few minutes across years is normal
 * astronomical drift; a large jump or missing days flags a broken feed, wrong zone, or parser
 * regression.
 */
final class GoldenDiff {

    private GoldenDiff() {
    }

    /** The outcome of comparing one zone's scrape against its committed base table. */
    record ZoneDiff(String zoneId, int baseDays, int comparedDays, int missingDays,
                    int maxDriftMinutes, String worstField, int worstMonth, int worstDay) {

        boolean clean() {
            return missingDays == 0 && maxDriftMinutes == 0;
        }

        String describe() {
            String base = "zone " + zoneId + ": " + comparedDays + "/" + baseDays + " days compared";
            if (missingDays > 0) {
                base += ", " + missingDays + " missing";
            }
            if (maxDriftMinutes == 0) {
                return base + ", no drift";
            }
            return base + ", max drift " + maxDriftMinutes + " min ("
                    + worstField + " on " + worstMonth + "-" + worstDay + ")";
        }
    }

    static ZoneDiff compare(String zoneId, AuthorityTimetable base, AuthorityTimetable scraped) {
        Map<Integer, TimetableRow> scrapedByDate = new HashMap<>();
        for (TimetableRow r : scraped.rows()) {
            scrapedByDate.put(key(r.month(), r.day()), r);
        }

        int compared = 0;
        int missing = 0;
        int maxDrift = 0;
        String worstField = "";
        int worstMonth = 0;
        int worstDay = 0;

        for (TimetableRow b : base.rows()) {
            TimetableRow s = scrapedByDate.get(key(b.month(), b.day()));
            if (s == null) {
                missing++;
                continue;
            }
            compared++;
            int[] drifts = {
                    Math.abs(b.fajr() - s.fajr()),
                    Math.abs(b.sunrise() - s.sunrise()),
                    Math.abs(b.dhuhr() - s.dhuhr()),
                    Math.abs(b.asr() - s.asr()),
                    Math.abs(b.maghrib() - s.maghrib()),
                    Math.abs(b.isha() - s.isha()),
                    Math.abs(b.solarNoon() - s.solarNoon()),
            };
            String[] fields = {"fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha", "solar-noon"};
            for (int i = 0; i < drifts.length; i++) {
                if (drifts[i] > maxDrift) {
                    maxDrift = drifts[i];
                    worstField = fields[i];
                    worstMonth = b.month();
                    worstDay = b.day();
                }
            }
        }
        return new ZoneDiff(zoneId, base.rows().size(), compared, missing,
                maxDrift, worstField, worstMonth, worstDay);
    }

    private static int key(int month, int day) {
        return month * 100 + day;
    }
}
