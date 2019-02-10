/*
 * Copyright (C) 2018 Tim Cadogan-Cowper.
 *
 * This file is part of Fabularium.
 *
 * Fabularium is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Fabularium; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.luxlunae.glk.model;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import java.util.Calendar;
import java.util.TimeZone;

@Keep
public class GLKDate {
    private int year;      // full (four-digit) year
    private int month;     // 1-12, 1 is January
    private int day;       // 1-31
    private int weekday;   // 0-6, 0 is Sunday
    private int hour;      // 0-23
    private int minute;    // 0-59
    private int second;    // 0-59, maybe 60 during a leap second
    private int microsec;  // 0-999999

    /**
     * Initialise the date structure, using the milliseconds since 1970 and
     * the specified timezone.
     *
     * @param ms - milliseconds since the epoch
     * @param tz - timezone
     */
    public void setTime(long ms, @NonNull TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(ms);

        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH) + 1;
        day = cal.get(Calendar.DAY_OF_MONTH);

        switch (cal.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY:
                weekday = 0; break;
            case Calendar.MONDAY:
                weekday = 1; break;
            case Calendar.TUESDAY:
                weekday = 2; break;
            case Calendar.WEDNESDAY:
                weekday = 3; break;
            case Calendar.THURSDAY:
                weekday = 4; break;
            case Calendar.FRIDAY:
                weekday = 5; break;
            case Calendar.SATURDAY:
                weekday = 6; break;
        }

        hour = cal.get(Calendar.HOUR_OF_DAY);	// GLK hours are in 24 hour format
        minute = cal.get(Calendar.MINUTE);
        second = cal.get(Calendar.SECOND);
        microsec = cal.get(Calendar.MILLISECOND) * 1000;
    }

    public int getSimpleTime(@NonNull TimeZone tz, long factor) {
        Calendar cal = Calendar.getInstance(tz);
        cal.set(year, month - 1, day, hour, minute, second);
        return (int)Math.floor((cal.getTimeInMillis() + (microsec / 1000D)) / (1000D * factor));
    }

    public void getTime(@NonNull GLKTime time, @NonNull TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.set(year, month - 1, day, hour, minute, second);
        time.setTo(cal.getTimeInMillis());
        time.microsec = microsec;
    }

    @NonNull
    @Override
    public String toString() {
        String s = "GLKDate object (";
        s += "year: " + year + ", month: " + month + ", day: " + day + ", ";
        s += "weekday: " + weekday + ", hour: " + hour + ", min: " + minute + ", sec: " + second + ", microsec: " + microsec + ")";
        return s;
    }
}
