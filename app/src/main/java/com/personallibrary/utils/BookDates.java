package com.personallibrary.utils;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Dates of books (purchase date, read date) are stored as "midnight UTC" in milliseconds.
 * That is exactly what the Material date picker returns, so nothing shifts by a day.
 * 0 means "not set".
 */
public final class BookDates {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    public BookDates() {
    }

    /** Today's date (in the phone's time zone) as midnight UTC. */
    public static long todayUtc(){
        Calendar local = Calendar.getInstance();
        Calendar utc = Calendar.getInstance(UTC);
        utc.clear();
        utc.set(local.get(Calendar.YEAR),local.get(Calendar.MONTH),local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }

    /** Date in the phone's language and format, e.g. "12 Mar 2024". Formatted in UTC on purpose. */
    public static String format(long utcMillis){
        DateFormat f = DateFormat.getDateInstance(DateFormat.MEDIUM,Locale.getDefault());
        f.setTimeZone(UTC);
        return f.format(new Date(utcMillis));
    }
}
