/* 
 *   Copyright (c) 1987, 2002 by Michael J. Roberts.  All Rights Reserved.
 *   
 *   Please see the accompanying license file, LICENSE.TXT, for information
 *   on using and copying this software.  
 */
/*
Name
  ostzdjgpp.c - osifc timezone routines for djgpp (Gnu C for MS-DOS)
Function

Notes

Modified
  08/05/12 MJRoberts  - Creation
*/


#include <string.h>
#include <memory.h>
#include <fcntl.h>
#include <dpmi.h>
#include <sys/stat.h>
#include <time.h>
#include "osstzprs.h"


/*
 *   Get the time to the best available precision
 */
void os_time_ns(os_time_t *seconds, long *nanoseconds)
{
    struct timeval tv;
    struct timezone tz;

    /* 
     *   Get the time of day information.  This returns the seconds and
     *   microseconds since the Unix Epoch, which translates directly to what
     *   we want, with the small adjustment that we need to convert
     *   microseconds to nanoseconds. 
     */
    gettimeofday(&tv, &tz);
    *seconds = tv.tv_sec;
    *nanoseconds = tv.tv_usec * 1000;
}

/*
 *   Get the local time zone.  djgpp doesn't use zoneinfo keys, so we'll
 *   define our own environment variable, TZNAME, that users can fill in if
 *   desired.  We'll also accept the TZ=:zone format that the Gnu C library
 *   accepts; this won't work with the djgpp time functions, but when callers
 *   use zoneinfo (as TADS 3 does), the regular C time zone functions
 *   shouldn't be needed anyway.
 */
int os_get_zoneinfo_key(char *name, size_t namelen)
{
    char *tz, *p;

    /* check for TZ=:zone */
    tz = getenv("TZ");
    if (tz != 0 && tz[0] == ':')
    {
        safe_strcpy(name, namelen, tz + 1);
        return TRUE;
    }

    /* newer djgpp versions use TZ=c:/djgpp/zoneinfo/... */
    if (tz != 0 && (p = strstr(tz, "/zoneinfo/")) != 0)
    {
        safe_strcpy(name, namelen, p + 10);
        return TRUE;
    }

    /* if we have a TZNAME setting, return that */
    tz = getenv("TZNAME");
    if (tz != 0)
    {
        safe_strcpy(name, namelen, tz);
        return TRUE;
    }

    /* the zoneinfo key isn't available */
    return FALSE;
}

/*
 *   Get the current settings for the local time zone.  If the TZ string is
 *   in one of the POSIX formats, we'll use that; otherwise we'll fall back
 *   on the library settings from djgpp.
 */
int os_get_timezone_info(struct os_tzinfo_t *info)
{
    struct timeval tv;
    struct timezone tz;
    const char *tzvar = getenv("TZ");

    /* clear the structure */
    memset(info, 0, sizeof(*info));

    /* 
     *   try parsing the TZ variable, which gives us more information than we
     *   can coax out of the djgpp library 
     */
    if (oss_parse_posix_tz(info, tzvar, strlen(tzvar), TRUE))
        return TRUE;

    /* get the local time information */
    if (!gettimeofday(&tv, &tz))
    {
        /* 
         *   We want the delta from GMT to local time in seconds;
         *   gettimeofday() gives us minutes west of GMT, so multiply by 60
         *   to get seconds and negate to get the east-of-GMT offset.  Note
         *   that we can't determine the difference between standard and
         *   daylight time from what gettimeofday() tells us, so we simply
         *   use the current offset for both of them.  Since we also can't
         *   determine the DST start/end rules, the caller will only use the
         *   one that's currently in effect, so it doesn't matter that the
         *   other one is probably wrong (wrong in that it should be adjusted
         *   by the DST time change value, which we don't know).
         */
        info->std_ofs = info->dst_ofs = -tz.tz_minuteswest * 60;

        /* copy the DST flag */
        info->is_dst = tz.tz_dsttime;

        /* copy the abbreviations */
        safe_strcpy(info->std_abbr, sizeof(info->std_abbr), tzname[0]);
        safe_strcpy(info->dst_abbr, sizeof(info->dst_abbr), tzname[1]);

        /* success */
        return TRUE;
    }

    /* we couldn't find any information - return failure */
    return FALSE;
}

