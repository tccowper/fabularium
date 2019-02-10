/*
 * Copyright (C) 2017 Tim Cadogan-Cowper.
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
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <ctype.h>
#include <time.h>

#include "os.h"
#include "osstzprs.h"
#include <android/log.h>

// We utilize the Tads 3 Unicode character mapping facility even for
// Tads 2 (only in the interpreter).
#include "charmap.h"

// #define DEBUG_TADS

#define ispathchar(c) \
    ((c) == OSPATHCHAR || ((c) != 0 && strchr(OSPATHALT, c) != 0))
# define pathchareq(a, b) ((a) == (b))
#define fname_memcmp memcmp

/**********************
 * HELPER FUNCTIONS
 **********************/

/*
 *   Provide memicmp, since it's not a standard libc routine.
 */
int memicmp(const char *s1, const char *s2, size_t len)
{
    int i;

    for (i = 0; i < len; i++)
    {
        if (tolower(s1[i]) != tolower(s2[i]))
            return (int)tolower(s1[i]) - (int)tolower(s2[i]);
    }
    return 0;
}

/* Safe strcpy.
 * (Copied from tads2/msdos/osdos.c)
 */
static void
safe_strcpy(char *dst, size_t dstlen, const char *src)
{
    size_t copylen;

    /* do nothing if there's no output buffer */
    if (dst == 0 || dstlen == 0)
        return;

    /* do nothing if the source and destination buffers are the same */
    if (dst == src)
        return;

    /* use an empty string if given a null string */
    if (src == 0)
        src = "";

    /* 
     *   figure the copy length - use the smaller of the actual string size
     *   or the available buffer size, minus one for the null terminator 
     */
    copylen = strlen(src);
    if (copylen > dstlen - 1)
        copylen = dstlen - 1;

    /* copy the string (or as much as we can) */
    memcpy(dst, src, copylen);

    /* null-terminate it */
    dst[copylen] = '\0';
}

/* Resolve symbolic links in a path.  It's okay for 'buf' and 'path'
 * to point to the same buffer if you wish to resolve a path in place.
 */
static void
resolve_path( char *buf, size_t buflen, const char *path )
{
    // From FrobTADS

    // Starting with the full path string, try resolving the path with
    // realpath().  The tricky bit is that realpath() will fail if any
    // component of the path doesn't exist, but we need to resolve paths
    // for prospective filenames, such as files or directories we're
    // about to create.  So if realpath() fails, remove the last path
    // component and try again with the remainder.  Repeat until we
    // can resolve a real path, or run out of components to remove.
    // The point of this algorithm is that it will resolve as much of
    // the path as actually exists in the file system, ensuring that
    // we resolve any links that affect the path.  Any portion of the
    // path that doesn't exist obviously can't refer to a link, so it
    // will be taken literally.  Once we've resolved the longest prefix,
    // tack the stripped portion back on to form the fully resolved
    // path.

    // make a writable copy of the path to work with
    size_t pathl = strlen(path);
    char mypath[pathl + 1];
    memcpy(mypath, path, pathl + 1);

    // start at the very end of the path, with no stripped suffix yet
    char *suffix = mypath + pathl;
    char sl = '\0';

    // keep going until we resolve something or run out of path
    for (;;)
    {
        // resolve the current prefix, allocating the result
        char *rpath = realpath(mypath, 0);

        // un-split the path
        *suffix = sl;

        // if we resolved the prefix, return the result
        if (rpath != 0)
        {
            // success - if we separated a suffix, reattach it
            if (*suffix != '\0')
            {
                // reattach the suffix (the part after the '/')
                for ( ; *suffix == '/' ; ++suffix) ;
                os_build_full_path(buf, buflen, rpath, suffix);
            }
            else
            {
                // no suffix, so we resolved the entire path
                safe_strcpy(buf, buflen, rpath);
            }

            // done with the resolved path
            free(rpath);

            // ...and done searching
            break;
        }

        // no luck with realpath(); search for the '/' at the end of the
        // previous component in the path
        for ( ; suffix > mypath && *(suffix-1) != '/' ; --suffix) ;

        // skip any redundant slashes
        for ( ; suffix > mypath && *(suffix-1) == '/' ; --suffix) ;

        // if we're at the root element, we're out of path elements
        if (suffix == mypath)
        {
            // we can't resolve any part of the path, so just return the
            // original path unchanged
            safe_strcpy(buf, buflen, mypath);
            break;
        }

        // split the path here into prefix and suffix, and try again
        sl = *suffix;
        *suffix = '\0';
    }
}

/* Canonicalize a path: remove ".." and "." relative elements.
 * (Copied from tads2/osnoui.c)
 */
void canonicalize_path(char *path)
{
    char *orig = path;
    char *p;
    char *ele, *prvele;
    char *trimpt = 0;
    int dotcnt = 0;

    /*
     *   First, skip the root element.  This is unremovable.  For Unix, the
     *   root element is simply a leading slash (or series of slashes).  For
     *   DOS/Win, we can also have drive letters and UNC paths (as in
     *   \\MACHINE\ROOT).
     */

    /* skip leading slashes */
    for ( ; ispathchar(*path) ; ++path) ;

    /*
     *   if we didn't choose a different trim starting point, trim from after
     *   the root slashes
     */
    if (trimpt == 0)
        trimpt = path;

    /* scan elements */
    for (p = ele = path, prvele = 0 ; *ele != '\0' ; )
    {
        size_t ele_len;

        /* find the end of the current element */
        for ( ; *p != '\0' && !ispathchar(*p) ; ++p) ;
        ele_len = p - ele;

        /* skip adjacent path separators */
        for ( ; ispathchar(*p) ; ++p) ;

        /* check for special elements */
        if (ele_len == 1 && ele[0] == '.')
        {
            /* '.' -> current directory; simply remove this element */
            ++dotcnt;
            memmove(ele, p, strlen(p) + 1);

            /* revisit this element */
            p = ele;
        }
        else if (ele_len == 2 && ele[0] == '.' && ele[1] == '.')
        {
            /*
             *   '..' -> parent directory; remove this element and the
             *   previous element.  If there is no previous element, or the
             *   previous element is a DOS drive letter or root path slash,
             *   we can't remove it.  Leaving a '..' at the root level will
             *   result in an invalid path, but it would change the meaning
             *   to remove it, so leave it intact.
             *
             *   There's also a special case if the previous element is
             *   another '..'.  If so, it means this was an unremovable '..',
             *   so we can't do any combining - we need to keep this '..'
             *   AND the previous '..' to retain the meaning.
             */
            ++dotcnt;
            if (prvele == 0
                || ispathchar(*prvele)
                || (prvele[0] == '.' && prvele[1] == '.'
                    && ispathchar(prvele[2]))
                    )
                prvele = 0;

            /* remove the previous element */
            if (prvele != 0)
            {
                /* remove the element */
                memmove(prvele, p, strlen(p) + 1);

                /*
                 *   start the scan over from the beginning; this is a little
                 *   inefficient, but it's the easiest way to ensure
                 *   accuracy, since we'd otherwise have to back up to find
                 *   the previous element, which can be tricky
                 */
                p = ele = path;
                prvele = 0;
            }
            else
            {
                /*
                 *   no previous element, so keep the .. as it is and move on
                 *   to the next element
                 */
                prvele = ele;
                ele = p;
            }
        }
        else
        {
            /* ordinary element - move on to the next element */
            prvele = ele;
            ele = p;
        }
    }

    /* if we left any trailing slashes, remove them */
    for (p = trimpt + strlen(trimpt) ;
         p > trimpt && ispathchar(*(p-1)) ;
         *--p = '\0') ;

    /*
     *   If that left us with an empty string, and we had one or more "." or
     *   ".."  elements, the "." and ".." elements must have canceled out the
     *   other elements.  In this case return "." as the result.
     */
    if (orig[0] == '\0' && dotcnt != 0)
    {
        orig[0] = '.';
        orig[1] = '\0';
    }
}

/*
 *   General path builder for os_build_full_path() and os_combine_paths().
 *   The two versions do the same work, except that the former canonicalizes
 *   the result (resolving "." and ".." in the last element, for example),
 *   while the latter just builds the combined path literally.
 */
static void build_path(char *fullpathbuf, size_t fullpathbuflen,
                       const char *path, const char *filename, int canon)
{
    size_t plen, flen;
    int add_sep;

    /* presume we'll copy the entire path */
    plen = strlen(path);

    /* if the filename is an absolute path already, leave it as is */
    if (os_is_file_absolute(filename))
        plen = 0;

    /*
     *   Note whether we need to add a separator.  If the path prefix ends in
     *   a separator, don't add another; otherwise, add the standard system
     *   separator character.
     *
     *   Don't add a separator if the path is completely empty, since this
     *   simply means that we want to use the current directory.
     */
    add_sep = (plen != 0 && path[plen] == '\0' && !ispathchar(path[plen-1]));

    /* copy the path to the full path buffer, limiting to the buffer length */
    if (plen > fullpathbuflen - 1)
        plen = fullpathbuflen - 1;
    memcpy(fullpathbuf, path, plen);

    /* add the path separator if necessary (and if there's room) */
    if (add_sep && plen + 2 < fullpathbuflen)
        fullpathbuf[plen++] = OSPATHCHAR;

    /* add the filename after the path, if there's room */
    flen = strlen(filename);
    if (flen > fullpathbuflen - plen - 1)
        flen = fullpathbuflen - plen - 1;
    memcpy(fullpathbuf + plen, filename, flen);

    /* add a null terminator */
    fullpathbuf[plen + flen] = '\0';

    /* if desired, canonicalize the result */
    if (canon)
        canonicalize_path(fullpathbuf);
}

/*
 *   Get the next earlier element of a path
 *   From osnoui.c
 */
static const char *prev_path_ele(const char *start, const char *p,
                                 size_t *ele_len)
{
    int cancel = 0;
    const char *dotdot = 0;

    /* if we're at the start of the string, there are no more elements */
    if (p == start)
        return 0;

    /* keep going until we find a suitable element */
    for (;;)
    {
        const char *endp;

        /*
         *   If we've reached the start of the string, it means that we have
         *   ".."'s that canceled out every earlier element of the string.
         *   If the cancel count is non-zero, it means that we have one or
         *   more ".."'s that are significant (in that they cancel out
         *   relative elements before the start of the string).  If the
         *   cancel count is zero, it means that we've exactly canceled out
         *   all remaining elements in the string.
         */
        if (p == start)
        {
            *ele_len = (dotdot != 0 ? 2 : 0);
            return dotdot;
        }

        /*
         *   back up through any adjacent path separators before the current
         *   element, so that we're pointing to the first separator after the
         *   previous element
         */
        for ( ; p != start && ispathchar(*(p-1)) ; --p) ;

        /*
         *   If we're at the start of the string, this is an absolute path.
         *   Treat it specially by returning a zero-length initial element.
         */
        if (p == start)
        {
            *ele_len = 0;
            return p;
        }

        /* note where the current element ends */
        endp = p;

        /* now back up to the path separator before this element */
        for ( ; p != start && !ispathchar(*(p-1)) ; --p) ;

        /*
         *   if this is ".", skip it, since this simply means that this
         *   element matches the same folder as the previous element
         */
        if (endp - p == 1 && p[0] == '.')
            continue;

        /*
         *   if this is "..", it cancels out the preceding non-relative
         *   element; up the cancel count and keep searching
         */
        if (endp - p == 2 && p[0] == '.' && p[1] == '.')
        {
            /* up the cancel count */
            ++cancel;

            /* if this is the first ".." we've encountered, note it */
            if (dotdot == 0)
                dotdot = p;

            /* keep searching */
            continue;
        }

        /*
         *   This is an ordinary path element, not a relative "." or ".."
         *   link.  If we have a non-zero cancel count, we're still working
         *   on canceling out elements from ".."'s we found later in the
         *   string.
         */
        if (cancel != 0)
        {
            /* this absorbs one level of cancellation */
            --cancel;

            /*
             *   if that's the last cancellation, we've absorbed all ".."
             *   effects, so the last ".." we found is no longer significant
             */
            if (cancel == 0)
                dotdot = 0;

            /* keep searching */
            continue;
        }

        /* this item isn't canceled out by a "..", so it's our winner */
        *ele_len = endp - p;
        return p;
    }
}


/*****************************************************************
 * FABULARIUM-SPECIFIC IMPLENENTATIONS OF TADS' PORTABLE FUNCTIONS
 * See osifc.h for the portable interface and function definitions
 *****************************************************************/

/*
 *   Higher-precision time.  This retrieves the same time information as
 *   os_time() (i.e., the elapsed time since the standard Unix Epoch, January
 *   1, 1970 at midnight UTC), but retrieves it with the highest precision
 *   available on the local system, up to nanosecond precision.  If less
 *   precision is available, that's fine; just return the time to the best
 *   precision available, but expressed in terms of the number of
 *   nanoseconds.  For example, if you can retrieve milliseconds, you can
 *   convert that to nanoseconds by multiplying by 1,000,000.
 *
 *   On return, fills in '*seconds' with the number of whole seconds since
 *   the Epoch, and fills in '*nanoseconds' with the fractional portion,
 *   expressed in nanosceconds.  Note that '*nanoseconds' is merely the
 *   fractional portion of the time, so 0 <= *nanoseconds < 1000000000.
 */
void os_time_ns(os_time_t *seconds, long *nanoseconds)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_time_ns");
#endif
    // From FrobTADS - assumes system has clock_gettime
    
    // Get the current time.
    static const clockid_t clockType = CLOCK_REALTIME;
    struct timespec currTime;
    clock_gettime(clockType, &currTime);

    // return the data
    *seconds = currTime.tv_sec;
    *nanoseconds = currTime.tv_nsec;
}

/*
 *   Get the local time zone name, as a location name in the IANA zoneinfo
 *   database.  For example, locations using US Pacific Time should return
 *   "America/Los_Angeles".
 *
 *   Returns true if successful, false if not.  If the local operating system
 *   doesn't have a way to obtain this information, or if it's not available
 *   in the local machine's configuration, this returns false.
 *
 *   The zoneinfo database is also known as the Olson or TZ (timezone)
 *   database; it's widely used on Unix systems as the definitive source of
 *   local time zone settings.  See http://www.iana.org/time-zones for more
 *   information.
 *
 *   On many Unix systems, the TZ environment variable contains the zoneinfo
 *   zone name when its first character is ':'.  Windows uses a proprietary
 *   list of time zone names that can be mapped to zoneinfo names via a
 *   hand-coded list (such a list is maintained in the Unicode CLDR; our
 *   Windows implementation uses the CLDR list to generate the mapping).
 *   MacOS X uses zoneinfo keys directly; /etc/localtime is a link to the
 *   zoneinfo file for the local zone as set via the system preferences.
 *
 *   os_tzset() must be invoked at some point before this routine is called.
 */
int os_get_zoneinfo_key(char *buf, size_t buflen )
{
    /* First, try the TZ environment variable.  This is used on nearly
     * all Unix-alikes for a per-process timezone setting, although
     * it will only contain a zoneinfo key in newer versions.  There
     * are several possible formats for specifying a zoneinfo key:
     *
     *  TZ=/usr/share/zoneinfo/America/Los_Angeles
     *    - A full absolute path name to a tzinfo file.  We'll sense
     *      this by looking for "/zoneinfo/" in the string, and if we
     *      find it, we'll return the portion after /zoneinfo/.
     *
     *  TZ=America/Los_Angeles
     *    - Just the zoneinfo key, without a path.  If we find a string
     *      that contains all alphabetics, undersores, and slashes, and
     *      has at least one internal slash but doesn't start with a
     *      slash, we probably have a zoneinfo key.  We'll see if we
     *      can find a matching file in the usual zoneinfo database
     *      locations: /etc/zoneinfo, /usr/share/zoneinfo; if we can,
     *      we'll return the key name, otherwise we'll assume this
     *      isn't actually a zoneinfo key but just happens to look like
     *      one in terms of format.
     *
     *  TZ=:America/Los_Angeles
     *  TZ=:/etc/zoneinfo/America/Los_Angeles
     *    - POSIX systems generally use the ":" prefix to signify that
     *      this is a zoneinfo path rather than the old-style "EST5EDT"
     *      type of self-contained zone description.  If we see a colon
     *      prefix with a relative path (properly formed in terms of
     *      its character content), we'll simply assume this is a
     *      zoneinfo key without even checking for an existing file,
     *      since there's not much else it could be.  If we see an
     *      absolute path, we'll search it for /zoneinfo/ and return
     *      the portion after this, again without checking for an
     *      existing file.
     */
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_zoneinfo_key");
#endif
    // From ostzposix.c
    const char *tz = getenv("TZ");
    if (tz != 0 && tz[0] != '\0')
    {
        /* check that the string is formatted like a zoneinfo key */
#define tzcharok(c) (isalpha(c) != 0 || (c) == '/' || (c) == '_')
        int fmt_ok = TRUE;
        const char *p;
        fmt_ok &= (tz[0] == ':' || tzcharok(tz[0]));
        for (p = tz + 1 ; *p != '\0' ; ++p)
            fmt_ok &= tzcharok(*p);

        /* proceed only if it has the right format */
        if (fmt_ok)
        {
            /* check for a leading ':', per POSIX */
            if (tz[0] == ':')
            {
                /* yes, we have a leading ':', so it's almost certainly
                 * a zoneinfo key; if it's an absolute path, find the
                 * part after /zoneinfo/
                 */
                if (tz[1] == '/')
                {
                    /* absolute form - look for /zoneinfo/ */
                    const char *z = strstr(tz, "/zoneinfo/");
                    if (z != 0)
                    {
                        /* found it - return the part after /zoneinfo/ */
                        safe_strcpy(buf, buflen, z + 10);
                        return TRUE;
                    }
                }
                else
                {
                    /* relative path - return as-is minus the colon */
                    safe_strcpy(buf, buflen, tz + 1);
                    return TRUE;
                }
            }
            else
            {
                /* There's no colon, so it *might* be a zoneinfo key.
                 * If it's an absolute path containing /zoneinfo/, it's
                 * a solid bet.  If it's a relative path, look to see
                 * if we can find a file in one of the usual zoneinfo
                 * database locations.
                 */
                if (tz[0] == '/')
                {
                    /* absolute path - check for /zoneinfo/ */
                    const char *z = strstr(tz, "/zoneinfo/");
                    if (z != 0)
                    {
                        /* found it - return the part after /zoneinfo/ */
                        safe_strcpy(buf, buflen, z + 10);
                        return TRUE;
                    }
                }
                else
                {
                    /* relative path - look for a tzinfo file in the
                     * usual locations
                     */
                    static const char *dirs[] = {
                            "/etc/zoneinfo",
                            "/usr/share/zoneinfo",
                            0
                    };
                    const char **dir;
                    for (dir = dirs ; *dir != 0 ; ++dir)
                    {
                        /* build this full path */
                        char fbuf[OSFNMAX];
                        os_build_full_path(fbuf, sizeof(fbuf), *dir, tz);

                        /* check for a file at this location */
                        if (!osfacc(fbuf))
                        {
                            /* got it - looks like a good zoneinfo key */
                            safe_strcpy(buf, buflen, tz);
                            return TRUE;
                        }
                    }
                }
            }
        }
    }

    /* No luck with TZ, so try the system-wide settings next.
     *
     * If a file called /etc/timezone exists, it's usually a one-line
     * text file containing the zoneinfo key.  Read and return its
     * contents.
     */
    FILE *fp;
    if ((fp = fopen("/etc/timezone", "r")) != 0)
    {
        /* read the one-liner */
        char lbuf[256];
        int ok = FALSE;
        if (fgets(lbuf, sizeof(lbuf), fp) != 0)
        {
            /* strip any trailing newline */
            size_t l = strlen(lbuf);
            if (l != 0 && lbuf[l-1] == '\n')
                lbuf[l-1] = '\0';

            /* if it's in absolute format, return the part after
             * /zoneinfo/; otherwise just return the string
             */
            if (lbuf[0] == '/')
            {
                /* absoltue path - find /zoneinfo/ */
                const char *z = strstr(lbuf, "/zoneinfo/");
                if (z != 0)
                {
                    safe_strcpy(buf, buflen, z + 10);
                    ok = TRUE;
                }
            }
            else
            {
                /* relative notation - return it as-is */
                safe_strcpy(buf, buflen, lbuf);
                ok = TRUE;
            }
        }

        /* we're done with the file */
        fclose(fp);

        /* if we got our result, return success */
        if (ok)
            return TRUE;
    }

    /* If /etc/sysconfig/clock exists, read it and look for a line
     * starting with ZONE=.  This contains the zoneinfo key.
     */
    if ((fp = fopen("/etc/sysconfig/clock", "r")) != 0)
    {
        /* scan the file for ZONE=... */
        int ok = FALSE;
        for (;;)
        {
            /* read the next line */
            char lbuf[256];
            if (fgets(lbuf, sizeof(lbuf), fp) == 0)
                break;

            /* skip leading spaces */
            const char *p;
            for (p = lbuf ; isspace(*p) ; ++p) ;

            /* check for ZONE */
            if (memicmp(p, "zone", 4) != 0)
                continue;

            /* skip spaces after ZONE */
            for (p += 4 ; isspace(*p) ; ++p) ;

            /* check for '=' */
            if (*p != '=')
                continue;

            /* skip spaces after the '=' */
            for (++p ; isspace(*p) ; ++p) ;

            /* if it's in absolute form, look for /zoneinfo/ */
            if (*p == '/')
            {
                const char *z = strstr(p, "/zoneinfo/");
                if (z != 0)
                {
                    safe_strcpy(buf, buflen, z + 10);
                    ok = TRUE;
                }
            }
            else
            {
                /* relative notation - it's the zoneinfo key */
                safe_strcpy(buf, buflen, p);
                ok = TRUE;
            }

            /* that's our ZONE line, so we're done scanning the file */
            break;
        }

        /* done with the file */
        fclose(fp);

        /* if we got our result, return success */
        if (ok)
            return TRUE;
    }

    /* If /etc/localtime is a symbolic link, the linked file is the
     * actual zoneinfo file.  Resolve the link and return the portion
     * of the path after "/zoneinfo/".
     */
    static const char *elt = "/etc/localtime";
    unsigned long mode;
    char linkbuf[OSFNMAX];
    const char *zi;
    if (osfmode(elt, FALSE, &mode, NULL)
        && (mode & OSFMODE_LINK) != 0
        && os_resolve_symlink(elt, linkbuf, sizeof(linkbuf))
        && (zi = strstr(linkbuf, "/zoneinfo/")) != 0)
    {
        /* it's a link containing /zoneinfo/, so return the portion
         * after /zoneinfo/
         */
        safe_strcpy(buf, buflen, zi + 10);
        return TRUE;
    }

    /* well, we're out of ideas - return failure */
    return FALSE;
}

/*
 *   Get a description of the local time zone.  Fills in '*info' with the
 *   available information.  Returns true on success, false on failure.
 *
 *   See osstzprs.h/.c for a portable implementation of a parser for
 *   POSIX-style TZ strings.  That can serve as a full implementation of this
 *   function for systems that use the POSIX TZ environment variable syntax
 *   to specify the timezone.  (That routine simply parses a string from any
 *   source, so it can be used to parse the TZ syntax even on systems where
 *   the string comes from somewhere other than the TZ environment variable.)
 *
 *   os_tzset() must be invoked at some point before this routine is called.
 *
 *   The following two structures are used for the return information:
 *
 *   os_tzrule_t - Timezone Rule structure.  This describes a rule for an
 *   annual transition between daylight savings time and standard time in a
 *   time zone.  Most timezones that have recurring standard/daylight changes
 *   require two of these rules, one for switching to daylight time in the
 *   spring and one for switching to standard time in the fall.
 *
 *   os_tzinfo_t - Timezone Information structure.  This describes a
 *   timezone's clock settings, name(s), and rules for recurring annual
 *   changes between standard time and daylight time, if applicable.
 */
int os_get_timezone_info( struct os_tzinfo_t *info )
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_timezone_info");
#endif
    // From ostzposix.c

    /* try parsing environment variable TZ as a POSIX timezone string */
    const char *tz = getenv("TZ");
    if (tz != 0 && oss_parse_posix_tz(info, tz, strlen(tz), TRUE))
        return TRUE;

    /* fall back on localtime() - that'll at least give us the current
     * timezone name and GMT offset in most cases
     */
    time_t timer = time(0);
    const struct tm *tm = localtime(&timer);
    if (tm != 0)
    {
        memset(info, 0, sizeof(*info));
        info->is_dst = tm->tm_isdst;
        if (tm->tm_isdst)
        {
            info->dst_ofs = tm->tm_gmtoff;
            safe_strcpy(info->dst_abbr, sizeof(info->dst_abbr), tm->tm_zone);
        }
        else
        {
            info->std_ofs = tm->tm_gmtoff;
            safe_strcpy(info->std_abbr, sizeof(info->std_abbr), tm->tm_zone);
        }
        return TRUE;
    }

    /* no information is available */
    return FALSE;
}

/*
 *   Get the current system high-precision timer.  This function returns a
 *   value giving the wall-clock ("real") time in milliseconds, relative to
 *   any arbitrary zero point.  It doesn't matter what this value is relative
 *   to -- the only important thing is that the values returned by two
 *   different calls should differ by the number of actual milliseconds that
 *   have elapsed between the two calls.  This might be the number of
 *   milliseconds since the computer was booted, since the current user
 *   logged in, since midnight of the previous night, since the program
 *   started running, since 1-1-1970, etc - it doesn't matter what the epoch
 *   is, so the implementation can use whatever's convenient on the local
 *   system.
 *
 *   True millisecond precision isn't required.  Each implementation should
 *   simply use the best precision available on the system.  If your system
 *   doesn't have any kind of high-precision clock, you can simply use the
 *   time() function and multiply the result by 1000 (but see the note below
 *   about exceeding 32-bit precision).
 *
 *   However, it *is* required that the return value be in *units* of
 *   milliseconds, even if your system clock doesn't have that much
 *   precision; so on a system that uses its own internal clock units, this
 *   routine must multiply the clock units by the appropriate factor to yield
 *   milliseconds for the return value.
 *
 *   It is also required that the values returned by this function be
 *   monotonically increasing.  In other words, each subsequent call must
 *   return a value that is equal to or greater than the value returned from
 *   the last call.  On some systems, you must be careful of two special
 *   situations.
 *
 *   First, the system clock may "roll over" to zero at some point; for
 *   example, on some systems, the internal clock is reset to zero at
 *   midnight every night.  If this happens, you should make sure that you
 *   apply a bias after a roll-over to make sure that the value returned from
 *   this return continues to increase despite the reset of the system clock.
 *
 *   Second, a 32-bit signed number can only hold about twenty-three days
 *   worth of milliseconds.  While it seems unlikely that a TADS game would
 *   run for 23 days without a break, it's certainly reasonable to expect
 *   that the computer itself may run this long without being rebooted.  So,
 *   if your system uses some large type (a 64-bit number, for example) for
 *   its high-precision timer, you may want to store a zero point the very
 *   first time this function is called, and then always subtract this zero
 *   point from the large value returned by the system clock.  If you're
 *   using time(0)*1000, you should use this technique, since the result of
 *   time(0)*1000 will almost certainly not fit in 32 bits in most cases.
 */
long os_get_sys_clock_ms(void) {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_sys_clock_ms");
#endif
    // From FrobTADS - assumes system has clock_gettime

    // We need to remember the exact time this function has been
    // called for the first time, and use that time as our
    // zero-point.  On each call, we simply return the difference
    // in milliseconds between the current time and our zero point.
    static struct timespec zeroPoint;

    // Did we get the zero point yet?
    static bool initialized = false;

    // Not all systems provide a monotonic clock; check if it's
    // available before falling back to the global system-clock.  A
    // monotonic clock is guaranteed not to change while the system
    // is running, so we prefer it over the global clock.
    static const clockid_t clockType =
#ifdef HAVE_CLOCK_MONOTONIC
            CLOCK_MONOTONIC;
#else
            CLOCK_REALTIME;
#endif
    // We must get the current time in each call.
    struct timespec currTime;

    // Initialize our zero-point, if not already done so.
    if (not initialized) {
        clock_gettime(clockType, &zeroPoint);
        initialized = true;
    }

    // Get the current time.
    clock_gettime(clockType, &currTime);

    // Note that tv_nsec contains *nano*seconds, not milliseconds,
    // so we need to convert it; a millisec is 1.000.000 nanosecs.
    return (currTime.tv_sec - zeroPoint.tv_sec) * 1000
           + (currTime.tv_nsec - zeroPoint.tv_nsec) / 1000000;
}

/*
 *   Open text file for reading and writing, keeping the file's existing
 *   contents if the file already exists or creating a new file if no such
 *   file exists.  Returns NULL on error.
 */
osfildef *osfoprwt(const char *fname, os_filetype_t typ)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "osfoprwt");
#endif
    FILE *fp;
    fp = fopen(fname, "r+");
    if (!fp)
        fp = fopen(fname, "w+");
    return fp;
}

/*
 *   Open binary file for random-access reading/writing.  If the file already
 *   exists, keep the existing contents; if the file doesn't already exist,
 *   create a new empty file.
 *
 *   The caller is allowed to perform any mixture of read and write
 *   operations on the returned file handle, and can seek around in the file
 *   to read and write at random locations.
 *
 *   If the local file system supports file sharing or locking controls, this
 *   should generally open the file in something equivalent to "exclusive
 *   write, shared read" mode ("deny write" in DENY terms), so that other
 *   processes can't modify the file at the same time we're modifying it (but
 *   it doesn't bother us to have other processes reading from the file while
 *   we're working on it, as long as they don't mind that we could change
 *   things on the fly).  It's not absolutely necessary to assert these
 *   locking semantics, but if there's an option to do so this is preferred.
 *   Stricter semantics (such as "exclusive" or "deny all" mode) are better
 *   than less strict semantics.  Less strict semantics are dicey, because in
 *   that case the caller has no way of knowing that another process could be
 *   modifying the file at the same time, and no way (through osifc) of
 *   coordinating that activity.  If less strict semantics are implemented,
 *   the caller will basically be relying on luck to avoid corruptions due to
 *   writing by other processes.
 *
 *   Return null on error.
 */
osfildef *osfoprwb(const char *fname, os_filetype_t typ)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "osfoprwb");
#endif
    FILE *fp;
    fp = fopen(fname, "r+b");
    if (!fp)
        fp = fopen(fname, "w+b");
    return fp;
}

/*
 *   Duplicate a file handle.  Returns a new osfildef* handle that accesses
 *   the same open file as an existing osfildef* handle.  The new handle is
 *   independent of the original handle, with its own seek position,
 *   buffering, etc.  The new handle and the original handle must each be
 *   closed separately when the caller is done with them (closing one doesn't
 *   close the other).  The effect should be roughly the same as the Unix
 *   dup() function.
 *
 *   On success, returns a new, non-null osfildef* handle duplicating the
 *   original handle.  Returns null on failure.
 *
 *   'mode' is a simplified stdio fopen() mode string.  The first
 *   character(s) indicate the access type: "r" for read access, "w" for
 *   write access, or "r+" for read/write access.  Note that "w+" mode is
 *   specifically not defined, since the fopen() handling of "w+" is to
 *   truncate any existing file contents, which is not desirable when
 *   duplicating a handle.  The access type can optionally be followed by "t"
 *   for text mode, "s" for source file mode, or "b" for binary mode, with
 *   the same meanings as for the various osfop*() functions.  The default is
 *   't' for text mode if none of these are specified.
 *
 *   If the osfop*() functions are implemented in terms of stdio FILE*
 *   objects, this can be implemented as fdopen(dup(fileno(orig)), mode), or
 *   using equivalents if the local stdio library uses different names for
 *   these functions.  Note that "s" (source file format) isn't a stdio mode,
 *   so implementations must translate it to the appropriate "t" or "b" mode.
 *   (For that matter, "t" and "b" modes aren't universally supported either,
 *   so some implementations may have to translate these, or more likely
 *   simply remove them, as most platforms don't distinguish text and binary
 *   modes anyway.)
 */
osfildef *osfdup(osfildef *orig, const char *mode) {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "osfdup");
#endif
    /* From FrobTADS */
    char realmode[5];
    char *p = realmode;
    const char *m;

    /* verify that there aren't any unrecognized mode flags */
    for (m = mode ; *m != '\0' ; ++m)
    {
        if (strchr("rw+bst", *m) == 0)
            return 0;
    }

    /* figure the read/write mode - translate r+ and w+ to r+ */
    if ((mode[0] == 'r' || mode[0] == 'w') && mode[1] == '+')
        *p++ = 'r', *p++ = '+';
    else if (mode[0] == 'r')
        *p++ = 'r';
    else if (mode[0] == 'w')
        *p++ = 'w';
    else
        return 0;

    /* end the mode string */
    *p = '\0';

    /* duplicate the handle in the given mode */
    return fdopen(dup(fileno(orig)), mode);
}

/*
 *   Set a file's type information.  This is primarily for implementations on
 *   Mac OS 9 and earlier, where the file system keeps file-type metadata
 *   separate from the filename.  On such systems, this can be used to set
 *   the type metadata after a file is created.  The system should map the
 *   os_filetype_t values to the actual metadata values on the local system.
 *   On most systems, there's no such thing as file-type metadata, in which
 *   case this function should simply be stubbed out with an empty function.
 */
void os_settype(const char *f, os_filetype_t typ)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_settype");
#endif
    /* Android / Linux doesn't store metadata separately, so we don't
     * do anything in this function */
}

/*
 *   Write to a text file.  os_fprintz() takes a null-terminated string,
 *   while os_fprint() takes an explicit separate length argument that might
 *   not end with a null terminator.
 */
void os_fprintz(osfildef *fp, const char *str)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_fprintz");
#endif
    fwrite(str, 1, strlen(str), fp);
}

void os_fprint(osfildef *fp, const char *str, size_t len)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_fprint");
#endif
    fwrite(str, 1, len, fp);
}

/*
 *   Get a file's mode and attribute flags.  This retrieves information on
 *   the given file equivalent to the st_mode member of the 'struct stat'
 *   data returned by the Unix stat() family of functions, as well as some
 *   extra system-specific attributes.  On success, fills in *mode (if mode
 *   is non-null) with the mode information as a bitwise combination of
 *   OSFMODE_xxx values, fills in *attr (if attr is non-null) with a
 *   combination of OSFATTR_xxx attribute flags, and returns true; on
 *   failure, simply returns false.  Failure can occur if the file doesn't
 *   exist, can't be accessed due to permissions, etc.
 *
 *   Note that 'mode' and/or 'attr' can be null if the caller doesn't need
 *   that information.  Implementations must check these parameters for null
 *   pointers and skip returning the corresponding information if null.
 *
 *   If the file in 'fname' is a symbolic link, the behavior depends upon
 *   'follow_links'.  If 'follow_links' is true, the function should resolve
 *   the link reference (and if that points to another link, the function
 *   resolves that link as well, and so on) and return information on the
 *   object the link points to.  Otherwise, the function returns information
 *   on the link itself.  This only applies for symbolic links (not for hard
 *   links), and only if the underlying OS and file system support this
 *   distinction; if the OS transparently resolves links and doesn't allow
 *   retrieving information about the link itself, 'follow_links' can be
 *   ignored.  Likewise, hard links (on systems that support them) are
 *   generally indistinguishable from regular files, so this function isn't
 *   expected to do anything special with them.
 *
 *   The '*mode' value returned is a bitwise combination of OSFMODE_xxx flag.
 *   Many of the flags are mutually exclusive; for example, "file" and
 *   "directory" should never be combined.  It's also possible for '*mode' to
 *   be zero for a valid file; this means that the file is of some special
 *   type on the local system that doesn't fit any of the OSFMODE_xxx types.
 *   (If any ports do encounter such cases, we can add OSFMODE_xxx types to
 *   accommodate new types.  The list below isn't meant to be final; it's
 *   just what we've encountered so far on the platforms where TADS has
 *   already been ported.)
 *
 *   The OSFMODE_xxx values are left for the OS to define so that they can be
 *   mapped directly to the OS API's equivalent constants, if desired.  This
 *   makes the routine easy to write, since you can simply set *mode directly
 *   to the mode information the OS returns from its stat() or equivalent.
 *   However, note that these MUST be defined as bit flags - that is, each
 *   value must be exactly a power of 2.  Windows and Unix-like systems
 *   follow this practice, as do most "stat()" functions in C run-time
 *   libraries, so this usually works automatically if you map these
 *   constants to OS or C library values.  However, if a port defines its own
 *   values for these, take care that they're all powers of 2.
 *
 *   Obviously, a given OS might not have all of the file types listed here.
 *   If any OSFMODE_xxx values aren't applicable on the local OS, you can
 *   simply define them as zero since they'll never be returned.
 *
 *   Notes on attribute flags:
 *
 *   OSFATTR_HIDDEN means that the file is conventionally hidden by default
 *   in user interface views or listings, but is still fully accessible to
 *   the user.  Hidden files are also usually excluded by default from
 *   wildcard patterns in commands ("rm *.*").  On Unix, a hidden file is one
 *   whose name starts with "."; on Windows, it's a file with the HIDDEN bit
 *   in its file attributes.  On systems where this concept exists, the user
 *   can still manipulate these files as normal by naming them explicitly,
 *   and can typically make them appear in UI views or directory listings via
 *   a preference setting or command flag (e.g., "ls -a" on Unix).  The
 *   "hidden" flag is explicitly NOT a security or permissions mechanism, and
 *   it doesn't protect the file against intentional access by a user; it's
 *   merely a convenience designed to reduce clutter by excluding files
 *   maintained by the OS or by an application (such as preference files,
 *   indices, caches, etc) from casual folder browsing, where a user is
 *   typically only concerned with her own document files.  On systems where
 *   there's no such naming convention or attribute metadata, this flag will
 *   never appear.
 *
 *   OSFATTR_SYSTEM is similar to 'hidden', but means that the file is
 *   specially marked as an operating system file.  This is mostly a
 *   DOS/Windows concept, where it corresponds to the SYSTEM bit in the file
 *   attributes; this flag will probably never appear on other systems.  The
 *   distinction between 'system' and 'hidden' is somewhat murky even on
 *   Windows; most 'system' file are also marked as 'hidden', and in
 *   practical terms in the user interface, 'system' files are treated the
 *   same as 'hidden'.
 *
 *   OSFATTR_READ means that the file is readable by this process.
 *
 *   OSFATTR_WRITE means that the file is writable by this process.
 */
int osfmode(const char *fname, int follow_links,
            unsigned long *mode, unsigned long *attr) {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "osfmode");
#endif
    /* From FrobTADS */
    os_file_stat_t s;
    int ok;
    if ((ok = os_file_stat(fname, follow_links, &s)) != false) {
        if (mode != NULL)
            *mode = s.mode;
        if (attr != NULL)
            *attr = s.attrs;
    }
    return ok;
}

/*
 *   Get stat() information.  This fills in the portable os_file_stat
 *   structure with the requested file information.  Returns true on success,
 *   false on failure (file not found, permissions error, etc).
 *
 *   'follow_links' has the same meaning as for osfmode().
 */
int os_file_stat(const char *fname, int follow_links, os_file_stat_t *s)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_file_stat");
#endif
    /* From FrobTADS */
    struct stat buf;
    if ((follow_links ? stat(fname, &buf) : lstat(fname, &buf)) != 0)
        return false;

    s->sizelo = (uint32_t)(buf.st_size & 0xFFFFFFFF);
    s->sizehi = sizeof(buf.st_size) > 4
                ? (uint32_t)((buf.st_size >> 32) & 0xFFFFFFFF)
                : 0;
    s->cre_time = buf.st_ctime;
    s->mod_time = buf.st_mtime;
    s->acc_time = buf.st_atime;
    s->mode = buf.st_mode;
    s->attrs = 0;

    if (os_get_root_name(fname)[0] == '.') {
        s->attrs |= OSFATTR_HIDDEN;
    }

    // If we're the owner, check if we have read/write access.
    if (geteuid() == buf.st_uid) {
        if (buf.st_mode & S_IRUSR)
            s->attrs |= OSFATTR_READ;
        if (buf.st_mode & S_IWUSR)
            s->attrs |= OSFATTR_WRITE;
        return true;
    }

    // Check if one of our groups matches the file's group and if so, check
    // for read/write access.

    // Also reserve a spot for the effective group ID, which might
    // not be included in the list in our next call.
    int grpSize = getgroups(0, NULL) + 1;
    // Paranoia.
    if (grpSize > NGROUPS_MAX or grpSize < 0)
        return false;
    gid_t* groups = new gid_t[grpSize];
    if (getgroups(grpSize - 1, groups + 1) < 0) {
        delete[] groups;
        return false;
    }
    groups[0] = getegid();
    int i;
    for (i = 0; i < grpSize and buf.st_gid != groups[i]; ++i)
        ;
    delete[] groups;
    if (i < grpSize) {
        if (buf.st_mode & S_IRGRP)
            s->attrs |= OSFATTR_READ;
        if (buf.st_mode & S_IWGRP)
            s->attrs |= OSFATTR_WRITE;
        return true;
    }

    // We're neither the owner of the file nor do we belong to its
    // group.  Check whether the file is world readable/writable.
    if (buf.st_mode & S_IROTH)
        s->attrs |= OSFATTR_READ;
    if (buf.st_mode & S_IWOTH)
        s->attrs |= OSFATTR_WRITE;
    return true;
}

/*
 *   Manually resolve a symbolic link.  If the local OS and file system
 *   support symbolic links, and the given filename is a symbolic link (in
 *   which case osfmode(fname, FALSE, &m, &a) will set OSFMODE_LINK in the
 *   mode bits), this fills in 'target' with the name of the link target
 *   (i.e., the object that the link in 'fname' points to).  This should
 *   return a fully qualified file system path.  Returns true on success,
 *   false on failure.
 *
 *   This should only resolve a single level of indirection.  If the link
 *   target of 'fname' is itself a link to a second target, this should only
 *   resolve the single reference from 'fname' to its direct direct.  Callers
 *   that wish to resolve the final target of a chain of link references must
 *   iterate until the returned path doesn't refer to a link.
 */
int os_resolve_symlink(const char *fname, char *target, size_t target_size)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_resolve_symlink");
#endif
    /* From FrobTADS */

    // get the stat() information for the *undereferenced* link; if
    // it's not actually a link, there's nothing to resolve
    struct stat buf;
    if (lstat(fname, &buf) != 0 || (buf.st_mode & S_IFLNK) == 0)
        return false;

    // read the link contents (maxing out at the buffer size)
    size_t copylen = (size_t)buf.st_size;
    if (copylen > target_size - 1)
        copylen = target_size - 1;
    if (readlink(fname, target, copylen) < 0)
        return false;

    // null-terminate the result and return success
    target[copylen] = '\0';
    return true;
}

/* ------------------------------------------------------------------------ */
/*
 *   Get a list of root directories.  If 'buf' is non-null, fills in 'buf'
 *   with a list of strings giving the root directories for the local,
 *   file-oriented devices on the system.  The strings are each null
 *   terminated and are arranged consecutively in the buffer, with an extra
 *   null terminator after the last string to mark the end of the list.
 *
 *   The return value is the length of the buffer required to hold the
 *   results.  If the caller's buffer is null or is too short, the routine
 *   should return the full length required, and leaves the contents of the
 *   buffer undefined; the caller shouldn't expect any contents to be filled
 *   in if the return value is greater than buflen.  Both 'buflen' and the
 *   return value include the null terminators, including the extra null
 *   terminator at the end of the list.  If an error occurs, or the system
 *   has no concept of a root directory, returns zero.
 *
 *   Each result string should be expressed using the syntax for the root
 *   directory on a device.  For example, on Windows, "C:\" represents the
 *   root directory on the C: drive.
 *
 *   "Local" means a device is mounted locally, as opposed to being merely
 *   visible on the network via some remote node syntax; e.g., on Windows
 *   this wouldn't include any UNC-style \\SERVER\SHARE names, and on VMS it
 *   excludes any SERVER:: nodes.  It's up to each system how to treat
 *   virtual local devices, i.e., those that look synctactically like local
 *   devices but are actually mounted network devices, such as Windows mapped
 *   network drives; we recommend including them if it would take extra work
 *   to filter them out, and excluding them if it would take extra work to
 *   include them.  "File-oriented" means that the returned devices are
 *   accessed via file systems, not as character devices or raw block
 *   devices; so this would exclude /dev/xxx devices on Unix and things like
 *   CON: and LPT1: on Windows.
 *
 *   Examples ("." represents a null byte):
 *
 *   Windows: C:\.D:\.E:\..
 *
 *   Unix example: /..
 */
size_t os_get_root_dirs(char *buf, size_t buflen)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_root_dirs");
#endif
    // From FrobTADS
    static const char ret[] = { '/', 0, 0 };

    // if there's room, copy the root string "/" and an extra null
    // terminator for the overall list
    if (buflen >= sizeof(ret))
        memcpy(buf, ret, sizeof(ret));

    // return the required size
    return sizeof(ret);
}

/* ------------------------------------------------------------------------ */
/*
 *   Open a directory.  This begins an enumeration of a directory's contents.
 *   'dirname' is a relative or absolute path to a directory.  On success,
 *   returns true, and 'handle' is set to a port-defined handle value that's
 *   used in subsequent calls to os_read_dir() and os_close_dir().  Returns
 *   false on failure.
 *
 *   If the routine succeeds, the caller must eventually call os_close_dir()
 *   to release the resources associated with the handle.
 */
int os_open_dir(const char *dirname, /*OUT*/osdirhdl_t *handle)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_open_dir");
#endif
    /* From FrobTADS */
    return (*handle = opendir(dirname)) != NULL;
}

/*
 *   Read the next file in a directory.  'handle' is a handle value obtained
 *   from a call to os_open_dir().  On success, returns true and fills in
 *   'fname' with the next filename; the handle is also internally updated so
 *   that the next call to this function will retrieve the next file, and so
 *   on until all files have been retrieved.  If an error occurs, or there
 *   are no more files in the directory, returns false.
 *
 *   The filename returned is the root filename only, without the path.  The
 *   caller can build the full path by calling os_build_full_path() or
 *   os_combine_paths() with the original directory name and the returned
 *   filename as parameters.
 *
 *   This routine lists all objects in the directory that are visible to the
 *   corresponding native API, and is non-recursive.  The listing should thus
 *   include subdirectory objects, but not the contents of subdirectories.
 *   Implementations are encouraged to simply return all objects returned
 *   from the corresponding native directory scan API; there's no need to do
 *   any filtering, except perhaps in cases where it's difficult or
 *   impossible to represent an object in terms of the osifc APIs (e.g., it
 *   might be reasonable to exclude files without names).  System relative
 *   links, such as the Unix/DOS "." and "..", specifically should be
 *   included in the listing.  For unusual objects that don't fit into the
 *   os_file_stat() taxonomy or that otherwise might create confusion for a
 *   caller, err on the side of full disclosure (i.e., just return everything
 *   unfiltered); if necessary, we can extend the os_file_stat() taxonomy or
 *   add new osifc APIs to create a portable abstraction to handle whatever
 *   is unusual or potentially confusing about the native object.  For
 *   example, Unix implementations should feel free to return symbolic link
 *   objects, including dangling links, since we have the portable
 *   os_resolve_symlink() that lets the caller examine the meaning of the
 *   link object.
 */
int os_read_dir(osdirhdl_t handle, char *fname, size_t fname_size)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_read_dir");
#endif
    /* From FrobTADS */

    // Read the next directory entry - if we've exhausted the search,
    // return failure.
    struct dirent *d = readdir(handle);
    if (d == 0)
        return false;

    // return this entry
    safe_strcpy(fname, fname_size, d->d_name);
    return true;
}

/*
 *   Close a directory handle.  This releases the resources associated with a
 *   directory search started with os_open_dir().  Every successful call to
 *   os_open_dir() must have a matching call to os_close_dir().  As usual for
 *   open/close protocols, the handle is invalid after calling this function,
 *   so no more calls to os_read_dir() may be made with the handle.
 */
void os_close_dir(osdirhdl_t handle)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_close_dir");
#endif
    /* From FrobTADS */
    closedir(handle);
}

/*   The os_find_XXXX functions have been DEPRECATED as of TADS 2.5.16/3.1.1.  Callers should
 *   use os_open_dir(), os_read_dir(), os_close_dir() instead.
 */

void *os_find_first_file(const char *dir,
                         char *outbuf, size_t outbufsiz, int *isdir,
                         char *outpathbuf, size_t outpathbufsiz)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_find_first_file");
#endif
    return 0;
}


void *os_find_next_file(void *ctx, char *outbuf, size_t outbufsiz,
                        int *isdir, char *outpathbuf, size_t outpathbufsiz)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_find_next_file");
#endif
    return 0;
}

void os_find_close(void *ctx)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_find_close");
#endif
}

/*
 *   Determine if the given filename refers to a special file.  Returns the
 *   appropriate enum value if so, or OS_SPECFILE_NONE if not.  The given
 *   filename must be a root name - it must not contain a path prefix.  The
 *   purpose here is to classify the results from os_find_first_file() and
 *   os_find_next_file() to identify the special relative links, so callers
 *   can avoid infinite recursion when traversing a directory tree.
 */
enum os_specfile_t os_is_special_file(const char *fname)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_is_special_file");
#endif
    // Based on FrobTADS
    // We also check for "./" and "../" instead of just "." and
    // "..".  (We use OSPATHCHAR instead of '/' though.)
    const char selfWithSep[3] = {'.', OSPATHCHAR, '\0'};
    const char parentWithSep[4] = {'.', '.', OSPATHCHAR, '\0'};
    if ((strcmp(fname, ".") == 0) || (strcmp(fname, selfWithSep) == 0)) return OS_SPECFILE_SELF;
    if ((strcmp(fname, "..") == 0) || (strcmp(fname, parentWithSep) == 0)) return OS_SPECFILE_PARENT;
    return OS_SPECFILE_NONE;
}

/* Convert a string to all-lowercase */
char *os_strlwr(char *s)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_strlwr");
#endif
    char *sptr;

    sptr = s;
    while (*sptr != 0)
    {
        *sptr = tolower((unsigned char)*sptr);
        sptr++;
    }
    return s;
}

/* Get the full filename (including directory path) to the executable
 * file, given the argv[0] parameter passed into the main program.
 *
 * On Unix, you can't tell what the executable's name is by just looking
 * at argv, so we always indicate failure.  No big deal though; this
 * information is only used when the interpreter's executable is bundled
 * with a game, and we don't support this at all.
 */
int os_get_exe_filename(char *buf, size_t buflen, const char *argv0)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_exe_filename");
#endif
    /* We always indicate failure */
    return false;
}

/*
 *   Get a special directory path.  Returns the selected path, in a format
 *   suitable for use with os_build_full_path().  The main program's argv[0]
 *   parameter is provided so that the system code can choose to make the
 *   special paths relative to the program install directory, but this is
 *   entirely up to the system implementation, so the argv[0] parameter can
 *   be ignored if it is not needed.
 *
 *   The 'id' parameter selects which special path is requested; this is one
 *   of the constants defined below.  If the id is not understood, there is
 *   no way of signalling an error to the caller; this routine can fail with
 *   an assert() in such cases, because it indicates that the OS layer code
 *   is out of date with respect to the calling code.
 *
 *   This routine can be implemented using one of the strategies below, or a
 *   combination of these.  These are merely suggestions, though, and systems
 *   are free to ignore these and implement this routine using whatever
 *   scheme is the best fit to local conventions.
 *
 *   - Relative to argv[0].  Some systems use this approach because it keeps
 *   all of the TADS files together in a single install directory tree, and
 *   doesn't require any extra configuration information to find the install
 *   directory.  Since we base the path name on the executable that's
 *   actually running, we don't need any environment variables or parameter
 *   files or registry entries to know where to look for related files.
 *
 *   - Environment variables or local equivalent.  On some systems, it is
 *   conventional to set some form of global system parameter (environment
 *   variables on Unix, for example) for this sort of install configuration
 *   data.  In these cases, this routine can look up the appropriate
 *   configuration variables in the system environment.
 *
 *   - Hard-coded paths.  Some systems have universal conventions for the
 *   installation configuration of compiler-like tools, so the paths to our
 *   component files can be hard-coded based on these conventions.
 *
 *   - Hard-coded default paths with environment variable overrides.  Let the
 *   user set environment variables if they want, but use the standard system
 *   paths as hard-coded defaults if the variables aren't set.  This is often
 *   the best choice; users who expect the standard system conventions won't
 *   have to fuss with any manual settings or even be aware of them, while
 *   users who need custom settings aren't stuck with the defaults.
 */
void os_get_special_path(char *buf, size_t buflen,
                         const char *argv0, int id)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_special_path");
#endif
    // Based on FrobTADS
    const char* res;

    switch (id) {
        case OS_GSP_T3_RES:
            res = getenv("T3_RESDIR");
           // if (res == 0 or res[0] == '\0') {
           //     res = T3_RES_DIR;
           // }
            break;

        case OS_GSP_T3_INC:
            res = getenv("T3_INCDIR");
           // if (res == 0 or res[0] == '\0') {
           //     res = T3_INC_DIR;
           // }
            break;

        case OS_GSP_T3_LIB:
            res = getenv("T3_LIBDIR");
           // if (res == 0 or res[0] == '\0') {
           //     res = T3_LIB_DIR;
           // }
            break;

        case OS_GSP_T3_USER_LIBS:
            // There's no compile-time default for user libs.
            res = getenv("T3_USERLIBDIR");
            break;

        case OS_GSP_T3_SYSCONFIG:
            res = getenv("T3_CONFIG");
            //if (res == 0 and argv0 != 0) {
            //    os_get_path_name(buf, buflen, argv0);
            //    return;
           // }
            break;

        case OS_GSP_LOGFILE:
            res = getenv("T3_LOGDIR");
            //if (res == 0 or res[0] == '\0') {
            //    res = T3_LOG_FILE;
           // }
            break;

        default:
            // TODO: We could print a warning here to inform the
            // user that we're outdated.
            res = 0;
    }

    if (res != 0) {
        // Only use the detected path if it exists and is a
        // directory.
        struct stat inf;
        int statRet = stat(res, &inf);
        if (statRet == 0 and (inf.st_mode & S_IFMT) == S_IFDIR) {
            strncpy(buf, res, buflen - 1);
            return;
        }
    }
    // Indicate failure.
    buf[0] = '\0';
}

/*
 *   Seek to the resource file embedded in the current executable file,
 *   given the main program's argv[0].
 *
 *   On platforms where the executable file format allows additional
 *   information to be attached to an executable, this function can be used
 *   to find the extra information within the executable.
 *
 *   The 'typ' argument gives a resource type to find.  This is an arbitrary
 *   string that the caller uses to identify what type of object to find.
 *   The "TGAM" type, for example, is used by convention to indicate a TADS
 *   compiled GAM file.
 */
osfildef *os_exeseek(const char *argv0, const char *typ)
{
    /* As with QTads and FrobTADS, we don't support this and probably never will */
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_exeseek");
#endif
    return NULL;
}

/* os_get_str_rsc is defined in osrestad.c */

/* ------------------------------------------------------------------------ */
/*
 *   Look for a file in the "standard locations": current directory, program
 *   directory, PATH-like environment variables, etc.  The actual standard
 *   locations are specific to each platform; the implementation is free to
 *   use whatever conventions are appropriate to the local system.  On
 *   systems that have something like Unix environment variables, it might be
 *   desirable to define a TADS-specific variable (TADSPATH, for example)
 *   that provides a list of directories to search for TADS-related files.
 *
 *   On return, fill in 'buf' with the full filename of the located copy of
 *   the file (if a copy was indeed found), in a format suitable for use with
 *   the osfopxxx() functions; in other words, after this function returns,
 *   the caller should be able to pass the contents of 'buf' to an osfopxxx()
 *   function to open the located file.
 *
 *   Returns true (non-zero) if a copy of the file was located, false (zero)
 *   if the file could not be found in any of the standard locations.
 */
int os_locate(const char *fname, int flen, const char *arg0,
              char *buf, size_t bufsiz)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_locate");
#endif
    if (!osfacc(fname))
    {
        memcpy(buf, fname, (size_t)flen);
        buf[flen] = 0;
        return true;
    }
    return false;
}

/* ------------------------------------------------------------------------ */
/*
 *   Create and open a temporary file.  The file must be opened to allow
 *   both reading and writing, and must be in "binary" mode rather than
 *   "text" mode, if the system makes such a distinction.  Returns null on
 *   failure.
 *
 *   If 'fname' is non-null, then this routine should create and open a file
 *   with the given name.  When 'fname' is non-null, this routine does NOT
 *   need to store anything in 'buf'.  Note that the routine shouldn't try
 *   to put the file in a special directory or anything like that; just open
 *   the file with the name exactly as given.
 *
 *   If 'fname' is null, this routine must choose a file name and fill in
 *   'buf' with the chosen name; if possible, the file should be in the
 *   conventional location for temporary files on this system, and should be
 *   unique (i.e., it shouldn't be the same as any existing file).  The
 *   filename stored in 'buf' is opaque to the caller, and cannot be used by
 *   the caller except to pass to osfdel_temp().  On some systems, it may
 *   not be possible to determine the actual filename of a temporary file;
 *   in such cases, the implementation may simply store an empty string in
 *   the buffer.  (The only way the filename would be unavailable is if the
 *   implementation uses a system API that creates a temporary file, and
 *   that API doesn't return the name of the created temporary file.  In
 *   such cases, we don't need the name; the only reason we need the name is
 *   so we can pass it to osfdel_temp() later, but since the system is going
 *   to delete the file automatically, osfdel_temp() doesn't need to do
 *   anything and thus doesn't need the name.)
 *
 *   After the caller is done with the file, it should close the file (using
 *   osfcls() as normal), then the caller MUST call osfdel_temp() to delete
 *   the temporary file.
 *
 *   This interface is intended to take advantage of systems that have
 *   automatic support for temporary files, while allowing implementation on
 *   systems that don't have any special temp file support.  On systems that
 *   do have automatic delete-on-close support, this routine should use that
 *   system-level support, because it helps ensure that temp files will be
 *   deleted even if the caller fails to call osfdel_temp() due to a
 *   programming error or due to a process or system crash.  On systems that
 *   don't have any automatic delete-on-close support, this routine can
 *   simply use the same underlying system API that osfoprwbt() normally
 *   uses (although this routine must also generate a name for the temp file
 *   when the caller doesn't supply one).
 *
 *   This routine can be implemented using ANSI library functions as
 *   follows: if 'fname' is non-null, return fopen(fname,"w+b"); otherwise,
 *   set buf[0] to '\0' and return tmpfile().
 */
osfildef *os_create_tempfile(const char *fname, char *buf)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_create_tempfile");
#endif
    // From FrobTADS
    if (fname != 0 and fname[0] != '\0') {
        // A filename has been specified; use it.
        return fopen(fname, "w+b");
    }

    //ASSERT(buf != 0);

    // No filename needed; create a nameless tempfile.
    buf[0] = '\0';
    return tmpfile();
}

/*
 *   Delete a temporary file - this is used to delete a file created with
 *   os_create_tempfile().  For most platforms, this can simply be defined
 *   the same way as osfdel().  For platforms where the operating system or
 *   file manager will automatically delete a file opened as a temporary
 *   file, this routine should do nothing at all, since the system will take
 *   care of deleting the temp file.
 *
 *   Callers are REQUIRED to call this routine after closing a file opened
 *   with os_create_tempfile().  When os_create_tempfile() is called with a
 *   non-null 'fname' argument, the same value should be passed as 'fname' to
 *   this function.  When os_create_tempfile() is called with a null 'fname'
 *   argument, then the buffer passed in the 'buf' argument to
 *   os_create_tempfile() must be passed as the 'fname' argument here.  In
 *   other words, if the caller explicitly names the temporary file to be
 *   opened in os_create_tempfile(), then that same filename must be passed
 *   here to delete the named file; if the caller lets os_create_tempfile()
 *   generate a filename, then the generated filename must be passed to this
 *   routine.
 *
 *   If os_create_tempfile() is implemented using ANSI library functions as
 *   described above, then this routine can also be implemented with ANSI
 *   library calls as follows: if 'fname' is non-null and fname[0] != '\0',
 *   then call remove(fname); otherwise do nothing.
 */
int osfdel_temp(const char *fname)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "osfdel_temp");
#endif
    // From FrobTADS
    //ASSERT(fname != 0);

    if (fname[0] == '\0' or remove(fname) == 0) {
        // Either it was a nameless temp-file and has been
        // already deleted by the system, or deleting it
        // succeeded.  In either case, the operation was
        // successful.
        return 0;
    }
    // It was not a nameless tempfile and remove() failed.
    return 1;
}

/*
 *   Get the temporary file path.  This should fill in the buffer with a
 *   path prefix (suitable for strcat'ing a filename onto) for a good
 *   directory for a temporary file, such as the swap file.
 */
void os_get_tmp_path(char *buf)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_tmp_path");
#endif
    // From FrobTADS
    // Try the usual env. variable first.
    const char* tmpDir = getenv("TMPDIR");

    // If no such variable exists, try P_tmpdir (if defined in
    // <stdio.h>).
#ifdef P_tmpdir
    if (tmpDir == 0 or tmpDir[0] == '\0') {
        tmpDir = P_tmpdir;
    }
#endif

    // If we still lack a path, use "/tmp".
    if (tmpDir == 0 or tmpDir[0] == '\0') {
        tmpDir = "/tmp";
    }

    // If the directory doesn't exist or is not a directory, don't
    // provide anything at all (which means that the current
    // directory will be used).
    struct stat inf;
    int statRet = stat(tmpDir, &inf);
    if (statRet != 0 or (inf.st_mode & S_IFMT) != S_IFDIR) {
        buf[0] = '\0';
        return;
    }

    strcpy(buf, tmpDir);

    // Append a slash if necessary.
    size_t len = strlen(buf);
    if (buf[len - 1] != '/') {
        buf[len] = '/';
        buf[len + 1] = '\0';
    }
}

/*
 *   Generate a name for a temporary file.  This constructs a random file
 *   path in the system temp directory that isn't already used by an existing
 *   file.
 *
 *   On systems with long filenames, this can be implemented by selecting a
 *   GUID-strength random name (such as 32 random hex digits) with a decent
 *   random number generator.  That's long enough that the odds of a
 *   collision are essentially zero.  On systems that only support short
 *   filenames, the odds of a collision are non-zero, so the routine should
 *   actually check that the chosen filename doesn't exist.
 *
 *   Optionally, before returning, this routine *may* create (and close) an
 *   empty placeholder file to "reserve" the chosen filename.  This isn't
 *   required, and on systems with long filenames it's usually not necessary
 *   because of the negligible chance of a collision.  On systems with short
 *   filenames, a placeholder can be useful to prevent a subsequent call to
 *   this routine, or a separate process, from using the same filename before
 *   the caller has had a chance to use the returned name to create the
 *   actual temp file.
 *
 *   Returns true on success, false on failure.  This can fail if there's no
 *   system temporary directory defined, or the temp directory is so full of
 *   other files that we can't find an unused filename.
 */
int os_gen_temp_filename(char *buf, size_t buflen)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_gen_temp_filename");
#endif
    // From FrobTADS
    // Get the temporary directory: use the environment variable TMPDIR
    // if it's defined, otherwise use P_tmpdir; if even that's not
    // defined, return failure.
    const char* tmp = getenv("TMPDIR");
    if (tmp == 0)
        tmp = P_tmpdir;
    if (tmp == 0)
        return false;

    // Build a template filename for mkstemp.
    snprintf(buf, buflen, "%s/tads-XXXXXX", tmp);

    // Generate a unique name and open the file.
    int fd = mkstemp(buf);
    if (fd >= 0) {
        // Got it - close the placeholder file and return success.
        close(fd);
        return true;
    }
    // Failed.
    return false;
}

/* ------------------------------------------------------------------------ */
/*
 *   Basic directory/folder management routines
 */

/*
 *   Create a directory.  This creates a new directory/folder with the given
 *   name, which may be given as a relative or absolute path.  Returns true
 *   on success, false on failure.
 *
 *   If 'create_parents' is true, and the directory has mulitiple path
 *   elements, this routine should create each enclosing parent that doesn't
 *   already exist.  For example, if the path is specified as "a/b/c", and
 *   there exists a folder "a" in the working directory, but "a" is empty,
 *   this should first create "b" and then create "c".  If an error occurs
 *   creating any parent, the routine should simply stop and return failure.
 *   (Optionally, the routine may attempt to behave atomically by undoing any
 *   parent folder creations it accomplished before failing on a nested
 *   folder, but this isn't required.  To reduce the chances of a failure
 *   midway through the operation, the routine might want to scan the
 *   filename before starting to ensure that it contains only valid
 *   characters, since an invalid character is the most likely reason for a
 *   failure part of the way through.)
 *
 *   We recommend making the routine flexible in terms of the notation it
 *   accepts; e.g., on Unix, "/dir/sub/folder" and "/dir/sub/folder/" should
 *   be considered equivalent.
 */
int os_mkdir(const char *dir, int create_parents)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_mkdir");
#endif
    // From FrobTADS

    //assert(dir != 0);

    if (dir[0] == '\0')
        return true;

    // Copy the directory name to a new string so we can strip any trailing
    // path seperators.
    size_t len = strlen(dir);
    char tmp[len + 1];
    strncpy(tmp, dir, len);
    while (tmp[len - 1] == OSPATHCHAR)
        --len;
    tmp[len] = '\0';

    // If we're creating intermediate diretories, and the path contains
    // multiple elements, recursively create the parent directories first.
    if (create_parents && strchr(tmp, OSPATHCHAR) != 0) {
        char par[OSFNMAX];

        // Extract the parent path.
        os_get_path_name(par, sizeof(par), tmp);

        // If the parent doesn't already exist, create it recursively.
        if (osfacc(par) != 0 && !os_mkdir(par, true)) {
            return false;
        }
    }

    // Create the directory.
    int ret = mkdir(tmp, S_IRWXU | S_IRWXG | S_IRWXO);
    return ret == 0;
}

/*
 *   Remove a directory.  Returns true on success, false on failure.
 *
 *   If the directory isn't already empty, this routine fails.  That is, the
 *   routine does NOT recursively delete the contents of a non-empty
 *   directory.  It's up to the caller to delete any contents before removing
 *   the directory, if that's the caller's intention.  (Note to implementors:
 *   most native OS APIs to remove directories fail by default if the
 *   directory isn't empty, so it's usually safe to implement this simply by
 *   calling the native API.  However, if your system's version of this API
 *   can remove a non-empty directory, you MUST add an extra test before
 *   removing the directory to ensure it's empty, and return failure if it's
 *   not.  For the purposes of this test, "empty" should of course ignore any
 *   special objects that are automatically or implicitly present in all
 *   directories, such as the Unix "." and ".." relative links.)
 */
int os_rmdir(const char *dir)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_rmdir");
#endif
    /* From FrobTADS */
    return rmdir(dir) == 0;
}


/* ------------------------------------------------------------------------ */
/*
 *   Filename manipulation routines
 */

/* apply a default extension to a filename, if it doesn't already have one */
void os_defext(char *fn, const char *ext)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_defext");
#endif
    // from osnoui.c
    char *p;

    /*
     *   Scan backwards from the end of the string, looking for the last
     *   dot ('.') in the filename.  Stop if we encounter a path separator
     *   character of some kind, because that means we reached the start
     *   of the filename without encountering a period.
     */
    p = fn + strlen(fn);
    while (p > fn)
    {
        /* on to the previous character */
        p--;

        /*
         *   if it's a period, return without doing anything - this
         *   filename already has an extension, so don't apply a default
         */
        if (*p == '.')
            return;

        /*
         *   if this is a path separator character, we're no longer in the
         *   filename, so stop looking for a period
         */
        if (ispathchar(*p))
            break;
    }

    /* we didn't find an extension - add the dot and the extension */
    strcat(fn, ".");
    strcat(fn, ext);
}

/* unconditionally add an extention to a filename */
void os_addext(char *fn, const char *ext)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_addext");
#endif
    // from osnoui.c
    if (strlen(fn) + 1 + strlen(ext) + 1 < OSFNMAX)
    {
        strcat(fn, ".");
        strcat(fn, ext);
    }
}

/* remove the extension from a filename */
void os_remext(char *fn)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_remext");
#endif
    // from osnoui.c
    char *p;

    /* scan backwards from the end of the string, looking for a dot */
    p = fn + strlen(fn);
    while ( p>fn )
    {
        /* move to the previous character */
        p--;

        /* if it's a period, we've found the extension */
        if ( *p=='.' )
        {
            /* terminate the string here to remove the extension */
            *p = '\0';

            /* we're done */
            return;
        }

        /*
         *   if this is a path separator, there's no extension, so we can
         *   stop looking
         */
        if (ispathchar(*p))
            return;
    }
}

/*
 *   Compare two file names/paths for syntactic equivalence.  Returns true if
 *   the names are equivalent names according to the local file system's
 *   syntax conventions, false if not.  This does a syntax-only comparison of
 *   the paths, without looking anything up in the file system.  This means
 *   that a false return doesn't guarantee that the paths don't point to the
 *   same file.
 *
 *   This routine DOES make the following equivalences:
 *
 *   - if the local file system is insensitive to case, the names are
 *   compared ignoring case
 *
 *   - meaningless path separator difference are ignored: on Unix, "a/b" ==
 *   "a//b" == "a/b/"; on Windows, "a/b" == "a\\b"
 *
 *   - relative links that are strictly structural or syntactic are applied;
 *   for example, on Unix or Windows, "a/./b" == "a/b" = "a/b/c/..".  This
 *   only applies for special relative links that can be resolved without
 *   looking anything up in the file system.
 *
 *   This DOES NOT do the following:
 *
 *   - it doesn't apply working directories/volums to relative paths
 *
 *   - it doesn't follow symbolic links in the file system
 */
int os_file_names_equal(const char *a, const char *b)
{
    /* start at the end of each name and work backwards */
    const char *pa = a + strlen(a), *pb = b + strlen(b);

    /* keep going until we reach the start of one or the other path */
    for (;;)
    {
        size_t lena, lenb;

        /* get the next earlier element of each path */
        pa = prev_path_ele(a, pa, &lena);
        pb = prev_path_ele(b, pb, &lenb);

        /* if one or the other ran out, we're done */
        if (pa == 0 || pb == 0)
        {
            /* the paths match if they ran out at the same time */
            return pa == pb;
        }

        /* if the two elements don't match, return unequal */
        if (lena != lenb || fname_memcmp(pa, pb, lena) != 0)
            return FALSE;
    }
}

/*
 *   Get a pointer to the root name portion of a filename.  This is the part
 *   of the filename after any path or directory prefix.  For example, on
 *   Unix, given the string "/home/mjr/deep.gam", this function should return
 *   a pointer to the 'd' in "deep.gam".  If the filename doesn't appear to
 *   have a path prefix, it should simply return the argument unchanged.
 *
 *   IMPORTANT: the returned pointer MUST point into the original 'buf'
 *   string, and the contents of that buffer must NOT be modified.  The
 *   return value must point into the same buffer because there are no
 *   allowances for the alternatives.  In particular, (a) you can't return a
 *   pointer to newly allocated memory, because callers won't free it, so
 *   doing so would cause a memory leak; and (b) you can't return a pointer
 *   to an internal static buffer, because callers might call this function
 *   more than once and still rely on a value returned on an older call,
 *   which would be invalid if a static buffer could be overwritten on each
 *   call.  For these reasons, it's required that the return value point to a
 *   position within the original string passed in 'buf'.
 */
char *os_get_root_name(const char *buf)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_root_name");
#endif
    // From osnoui.c
    const char *rootname;

    /* scan the name for path separators */
    for (rootname = buf ; *buf != '\0' ; ++buf)
    {
        /* if this is a path separator, remember it */
        if (ispathchar(*buf))
        {
            /*
             *   It's a path separators - for now, assume the root will
             *   start at the next character after this separator.  If we
             *   find another separator later, we'll forget about this one
             *   and use the later one instead.
             */
            rootname = buf + 1;
        }
    }

    /*
     *   Return the last root name candidate that we found.  (Cast it to
     *   non-const for the caller's convenience: *we're* not allowed to
     *   modify this buffer, but the caller is certainly free to pass in a
     *   writable buffer, and they're free to write to it after we return.)
     */
    return (char *)rootname;
}

/*
 *   Determine whether a filename specifies an absolute or relative path.
 *   This is used to analyze filenames provided by the user (for example,
 *   in a #include directive, or on a command line) to determine if the
 *   filename can be considered relative or absolute.  This can be used,
 *   for example, to determine whether to search a directory path for a
 *   file; if a given filename is absolute, a path search makes no sense.
 *   A filename that doesn't specify an absolute path can be combined with
 *   a path using os_build_full_path().
 *
 *   Returns true if the filename specifies an absolute path, false if
 *   not.
 */
int os_is_file_absolute(const char *fname)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_is_file_absolute");
#endif
    // from osnoui.c

    /* if the name starts with a path separator, it's absolute */
    if (ispathchar(fname[0]))
        return TRUE;

    /* the path is relative */
    return FALSE;
}

/*
 *   Extract the path from a filename.  Fills in pathbuf with the path
 *   portion of the filename.  If the filename has no path, the pathbuf
 *   should be set appropriately for the current directory (on Unix or DOS,
 *   for example, it can be set to an empty string).
 *
 *   The result can end with a path separator character or not, depending on
 *   local OS conventions.  Paths extracted with this function can only be
 *   used with os_build_full_path(), so the conventions should match that
 *   function's.
 *
 *   Unix examples:
 *
 *.   /home/mjr/deep.gam -> /home/mjr
 *.   games/deep.gam -> games
 *.   deep.gam -> [empty string]
 *
 *   Mac examples:
 *
 *    :home:mjr:deep.gam -> :home:mjr
 *.   Hard Disk:games:deep.gam -> Hard Disk:games
 *.   Hard Disk:deep.gam -> Hard Disk:
 *.   deep.gam -> [empty string]
 *
 *   VMS examples:
 *
 *.   SYS$DISK:[mjr.games]deep.gam -> SYS$DISK:[mjr.games]
 *.   SYS$DISK:[mjr.games] -> SYS$DISK:[mjr]
 *.   deep.gam -> [empty string]
 *
 *   Note in the last example that we've retained the trailing colon in the
 *   path, whereas we didn't in the others; although the others could also
 *   retain the trailing colon, it's required only for the last case.  The
 *   last case requires the colon because it would otherwise be impossible to
 *   determine whether "Hard Disk" was a local subdirectory or a volume name.
 *
 */
void os_get_path_name(char *pathbuf, size_t pathbuflen, const char *fname)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_path_name");
#endif
    // from osnoui.c
    const char *lastsep;
    const char *p;
    size_t len;
    int root_path;

    /* find the last separator in the filename */
    for (p = fname, lastsep = fname ; *p != '\0' ; ++p)
    {
        /*
         *   If it's a path separator character, remember it as the last one
         *   we've found so far.  However, don't count it if it's the last
         *   separator - i.e., if only more path separators follow.
         */
        if (ispathchar(*p))
        {
            const char *q;

            /* skip any immediately adjacent path separators */
            for (q = p + 1 ; *q != '\0' && ispathchar(*q) ; ++q) ;

            /* if we found more following, *p is the last separator */
            if (*q != '\0')
                lastsep = p;
        }
    }

    /* get the length of the prefix, not including the separator */
    len = lastsep - fname;

    /*
     *   Normally, we don't include the last path separator in the path; for
     *   example, on Unix, the path of "/a/b/c" is "/a/b", not "/a/b/".
     *   However, on Unix/DOS-like file systems, a root path *does* require
     *   the last path separator: the path of "/a" is "/", not an empty
     *   string.  So, we need to check to see if the file is in a root path,
     *   and if so, include the final path separator character in the path.
     */
    for (p = fname, root_path = TRUE ; p != lastsep ; ++p)
    {
        /*
         *   if this is NOT a path separator character, we don't have all
         *   path separator characters before the filename, so we don't have
         *   a root path
         */
        if (!ispathchar(*p))
        {
            /* note that we don't have a root path */
            root_path = FALSE;

            /* no need to look any further */
            break;
        }
    }

    /* if we have a root path, keep the final path separator in the path */
    if (root_path && ispathchar(fname[len]))
        ++len;

    /* make sure it fits in our buffer (with a null terminator) */
    if (len > pathbuflen - 1)
        len = pathbuflen - 1;

    /* copy it and null-terminate it */
    memcpy(pathbuf, fname, len);
    pathbuf[len] = '\0';
}

/*
 *   Build a full path name, given a path and a filename.  The path may have
 *   been specified by the user, or may have been extracted from another file
 *   via os_get_path_name().  This routine must take care to add path
 *   separators as needed, but also must take care not to add too many path
 *   separators.
 *
 *   This routine should reformat the path into canonical format to the
 *   extent possible purely through syntactic analysis.  For example, special
 *   relative links, such as Unix "." and "..", should be resolved; for
 *   example, combining "a/./b/c" with ".." on Unix should yield "a/b".
 *   However, symbolic links that require looking up names in the file system
 *   should NOT be resolved.  We don't want to perform any actual file system
 *   lookups because might want to construct hypothetical paths that don't
 *   necessarily relate to files on the local system.
 *
 *   Note that relative path names may require special care on some
 *   platforms.  In particular, if the source path is relative, the result
 *   should also be relative.  For example, on the Macintosh, a path of
 *   "games" and a filename "deep.gam" should yield ":games:deep.gam" - note
 *   the addition of the leading colon to make the result path relative.
 *
 *   Note also that the 'filename' argument is not only allowed to be an
 *   ordinary file, possibly qualified with a relative path, but is also
 *   allowed to be a subdirectory.  The result in this case should be a path
 *   that can be used as the 'path' argument to a subsequent call to
 *   os_build_full_path; this allows a path to be built in multiple steps by
 *   descending into subdirectories one at a time.
 *
 *   Unix examples:
 *
 *.   /home/mjr + deep.gam -> /home/mjr/deep.gam"
 *.   /home/mjr + .. -> /home
 *.   /home/mjr + ../deep.gam -> /home/deep.gam
 *.   /home/mjr/ + deep.gam -> /home/mjr/deep.gam"
 *.   games + deep.gam -> games/deep.gam"
 *.   games/ + deep.gam -> games/deep.gam"
 *.   /home/mjr + games/deep.gam -> /home/mjr/games/deep.gam"
 *.   games + scifi/deep.gam -> games/scifi/deep.gam"
 *.   /home/mjr + games -> /home/mjr/games"
 *
 *   Mac examples:
 *
 *.   Hard Disk: + deep.gam -> Hard Disk:deep.gam
 *.   :games: + deep.gam -> :games:deep.gam
 *.   :games:deep + ::test.gam -> :games:test.gam
 *.   games + deep.gam -> :games:deep.gam
 *.   Hard Disk: + :games:deep.gam -> Hard Disk:games:deep.gam
 *.   games + :scifi:deep.gam -> :games:scifi:deep.gam
 *.   Hard Disk: + games -> Hard Disk:games
 *.   Hard Disk:games + scifi -> Hard Disk:games:scifi
 *.   Hard Disk:games:scifi + deep.gam -> Hard Disk:games:scifi:deep.gam
 *.   Hard Disk:games + :scifi:deep.gam -> Hard Disk:games:scifi:deep.gam
 *
 *   VMS examples:
 *
 *.   [home.mjr] + deep.gam -> [home.mjr]deep.gam
 *.   [home.mjr] + [-]deep.gam -> [home]deep.gam
 *.   mjr.dir + deep.gam -> [.mjr]deep.gam
 *.   [home]mjr.dir + deep.gam -> [home.mjr]deep.gam
 *.   [home] + [.mjr]deep.gam -> [home.mjr]deep.gam
 */
void os_build_full_path(char *fullpathbuf, size_t fullpathbuflen,
                        const char *path, const char *filename)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_build_full_path");
#endif
    // From osnoui.c

    /* build the combined path, and canonicalize the result */
    build_path(fullpathbuf, fullpathbuflen, path, filename, TRUE);
}

/*
 *   Combine a path and a filename to form a full path to the file.  This is
 *   *almost* the same as os_build_full_path(), but if the 'filename' element
 *   is a special relative link, such as Unix '.' or '..', this preserves
 *   that special link in the final name.
 *
 *   Unix examples:
 *
 *.    /home/mjr + deep.gam -> /home/mjr/deep.gam
 *.    /home/mjr + . -> /home/mjr/.
 *.    /home/mjr + .. -> /home/mjr/..
 *
 *   Mac examples:
 *
 *.    Hard Disk:games + deep.gam -> HardDisk:games:deep.gam
 *.    Hard Disk:games + :: -> HardDisk:games::
 *
 *   VMS exmaples:
 *
 *.    [home.mjr] + deep.gam -> [home.mjr]deep.gam
 *.    [home.mjr] + [-] -> [home.mjr.-]
 */
void os_combine_paths(char *fullpathbuf, size_t fullpathbuflen,
                      const char *path, const char *filename)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_combine_paths");
#endif
    // From osnoui.c

    /* build the path, without any canonicalization */
    build_path(fullpathbuf, fullpathbuflen, path, filename, FALSE);
}

/*
 *   Get the absolute, fully qualified filename for a file.  This fills in
 *   'result_buf' with the absolute path to the given file, taking into
 *   account the current working directory and any other implied environment
 *   information that affects the way the file system would resolve the given
 *   file name to a specific file on disk if we opened the file now using
 *   this name.
 *
 *   The returned path should be in absolute path form, meaning that it's
 *   independent of the current working directory or any other environment
 *   settings.  That is, this path should still refer to the same file even
 *   if the working directory changes.
 *
 *   Note that it's valid to get the absolute path for a file that doesn't
 *   exist, or for a path with directory components that don't exist.  For
 *   example, a caller might generate the absolute path for a file that it's
 *   about to create, or a hypothetical filename for path comparison
 *   purposes.  The function should succeed even if the file or any path
 *   components don't exist.  If the file is in relative format, and any path
 *   elements don't exist but are syntactically well-formed, the result
 *   should be the path obtained from syntactically combining the working
 *   directory with the relative path.
 *
 *   On many systems, a given file might be reachable through more than one
 *   absolute path.  For example, on Unix it might be possible to reach a
 *   file through symbolic links to the file itself or to parent directories,
 *   or hard links to the file.  It's up to the implementation to determine
 *   which path to use in such cases.
 *
 *   On success, returns true.  If it's not possible to resolve the file name
 *   to an absolute path, the routine copies the original filename to the
 *   result buffer exactly as given, and returns false.
 */
int os_get_abs_filename(char* buf, size_t buflen, const char* filename)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_abs_filename");
#endif
    // From FrobTADS

    // If the filename is already absolute, copy it; otherwise combine
    // it with the working directory.
    if (os_is_file_absolute(filename))
    {
        // absolute - copy it as-is
        safe_strcpy(buf, buflen, filename);
    }
    else
    {
        // combine it with the working directory to get the path
        char pwd[OSFNMAX];
        if (getcwd(pwd, sizeof(pwd)) != 0)
            os_build_full_path(buf, buflen, pwd, filename);
        else
            safe_strcpy(buf, buflen, filename);
    }

    // canonicalize the result
    canonicalize_path(buf);

    // Try getting the canonical path from the OS (allocating the
    // result buffer).
    char* newpath = realpath(filename, 0);
    if (newpath != 0) {
        // Copy the output (truncating if it's too long).
        safe_strcpy(buf, buflen, newpath);
        free(newpath);
        return true;
    }

    // realpath() failed, but that's okay - realpath() only works if the
    // path refers to an existing file, but it's valid for callers to
    // pass non-existent filenames, such as names of files they're about
    // to create, or hypothetical paths being used for comparison
    // purposes or for future use.  Simply return the canonical path
    // name we generated above.
    return true;
}

int os_get_rel_path(char *result, size_t result_len,
                    const char *basepath, const char *filename)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_rel_path");
#endif
    // From osnoui.c
    const char *fp;
    const char *bp;
    const char *fsep;
    const char *bsep;
    size_t rem;
    char *rp;

    /*
     *   Find the common path prefix.  We can lop off any leading elements
     *   that the two paths have in common.
     */
    for (fp = filename, bp = basepath, fsep = 0, bsep = 0 ;
         pathchareq(*fp, *bp) || (ispathchar(*fp) && ispathchar(*bp)) ;
         ++fp, ++bp)
    {
        /*
         *   if this is a separator character, note it - we want to keep
         *   track of the last separator in the common portion, since this is
         *   the end of the common path prefix
         */
        if (ispathchar(*fp) || *fp == '\0')
        {
            fsep = fp;
            bsep = bp;
        }

        /* stop at the end of the strings */
        if (*fp == '\0' || *bp == '\0')
            break;
    }

    /* if we didn't find any separators, we can't relativize the paths */
    if (bsep == 0 || fsep == 0)
    {
        /* nothing in common - return the filename unchanged */
        strncpy(result, filename, result_len - 1);
        result[result_len - 1] = '\0';
        return FALSE;
    }

    /*
     *   If we reached the end of the base path string, and we're at a path
     *   separator in the filename string, then the entire base path prefix
     *   is in common to both names.
     */
    if (*bp == '\0' && (ispathchar(*fp) || *fp == '\0'))
    {
        fsep = fp;
        bsep = bp;
    }

    /* if we're at a path separator in the base path, skip it */
    if (*bsep != '\0')
        ++bsep;

    /* if we're at a path separator in the filename, skip it */
    if (*fsep != '\0')
        ++fsep;

    /*
     *   Everything up to fsep and bsep can be dropped, because it's a common
     *   directory path prefix.  We must now add the relative adjustment
     *   portion: add a ".." directory for each remaining directory in the
     *   base path, since we must move from the base path up to the common
     *   ancestor; then add the rest of the filename path.
     */

    /*
     *   first, set up to copy into the result buffer - leave space for null
     *   termination
     */
    rp = result;
    rem = result_len - 1;

    /* add a ".." for each remaining directory in the base path string */
    while (*bsep != '\0')
    {
        /* add ".." and a path separator */
        if (rem > 3)
        {
            *rp++ = '.';
            *rp++ = '.';
            *rp++ = OSPATHCHAR;
            rem -= 3;
        }
        else
        {
            /* no more room - give up */
            rem = 0;
            break;
        }

        /* scan to the next path separator */
        for ( ; *bsep != '\0' && !ispathchar(*bsep) ; ++bsep) ;

        /* if this is a path separator, skip it */
        if (*bsep != '\0')
            ++bsep;
    }

    /*
     *   Copy the remainder of the filename, or as much as will fit, and
     *   ensure that the result is properly null-terminated
     */
    strncpy(rp, fsep, rem);
    rp[rem] = '\0';

    /* if the result is empty, return "." to represent the current dir */
    if (result[0] == '\0')
        strcpy(rp, ".");

    /* success */
    return TRUE;
}

/*
 *   Determine if the given file is in the given directory.  Returns true if
 *   so, false if not.  'filename' is a relative or absolute file name;
 *   'path' is a relative or absolute directory path, such as one returned
 *   from os_get_path_name().
 *
 *   If 'include_subdirs' is true, the function returns true if the file is
 *   either directly in the directory 'path', OR it's in any subdirectory of
 *   'path'.  If 'include_subdirs' is false, the function returns true only
 *   if the file is directly in the given directory.
 *
 *   If 'match_self' is true, the function returns true if 'filename' and
 *   'path' are the same directory; otherwise it returns false in this case.
 *
 *   This routine is allowed to return "false negatives" - that is, it can
 *   claim that the file isn't in the given directory even when it actually
 *   is.  The reason is that it's not always possible to determine for sure
 *   that there's not some way for a given file path to end up in the given
 *   directory.  In contrast, a positive return must be reliable.
 *
 *   If possible, this routine should fully resolve the names through the
 *   file system to determine the path relationship, rather than merely
 *   analyzing the text superficially.  This can be important because many
 *   systems have multiple ways to reach a given file, such as via symbolic
 *   links on Unix; analyzing the syntax alone wouldn't reveal these multiple
 *   pathways.
 *
 *   SECURITY NOTE: If possible, implementations should fully resolve all
 *   symbolic links, relative paths (e.g., Unix ".."), etc, before rendering
 *   judgment.  One important application for this routine is to determine if
 *   a file is in a sandbox directory, to enforce security restrictions that
 *   prevent a program from accessing files outside of a designated folder.
 *   If the implementation fails to resolve symbolic links or relative paths,
 *   a malicious program or user could bypass the security restriction by,
 *   for example, creating a symbolic link within the sandbox directory that
 *   points to the root folder.  Implementations can avoid this loophole by
 *   converting the file and directory names to absolute paths and resolving
 *   all symbolic links and relative notation before comparing the paths.
 */
int os_is_file_in_dir(const char *filename, const char *path,
                      int include_subdirs, int match_self)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_is_file_in_dir");
#endif
    /* From FrobTADS */
    char filename_buf[OSFNMAX], path_buf[OSFNMAX];
    size_t flen, plen;

    // Absolute-ize the filename, if necessary.
    if (!os_is_file_absolute(filename)) {
        os_get_abs_filename(filename_buf, sizeof(filename_buf), filename);
        filename = filename_buf;
    }

    // Absolute-ize the path, if necessary.
    if (!os_is_file_absolute(path)) {
        os_get_abs_filename(path_buf, sizeof(path_buf), path);
        path = path_buf;
    }

    // Canonicalize the paths, to remove .. and . elements - this will make
    // it possible to directly compare the path strings.  Also resolve it
    // to the extent possible, to make sure we're not fooled by symbolic
    // links.
    safe_strcpy(filename_buf, sizeof(filename_buf), filename);
    canonicalize_path(filename_buf);
    resolve_path(filename_buf, sizeof(filename_buf), filename_buf);
    filename = filename_buf;

    safe_strcpy(path_buf, sizeof(path_buf), path);
    canonicalize_path(path_buf);
    resolve_path(path_buf, sizeof(path_buf), path_buf);
    path = path_buf;

    // Get the length of the filename and the length of the path.
    flen = strlen(filename);
    plen = strlen(path);

    // If the path ends in a separator character, ignore that.
    if (plen > 0 && path[plen-1] == '/')
        --plen;

    // if the names match, return true if and only if we're matching the
    // directory to itself
    if (plen == flen && memcmp(filename, path, flen) == 0)
        return match_self;

    // Check that the filename has 'path' as its path prefix.  First, check
    // that the leading substring of the filename matches 'path', ignoring
    // case.  Note that we need the filename to be at least two characters
    // longer than the path: it must have a path separator after the path
    // name, and at least one character for a filename past that.
    if (flen < plen + 2 || memcmp(filename, path, plen) != 0)
        return false;

    // Okay, 'path' is the leading substring of 'filename'; next make sure
    // that this prefix actually ends at a path separator character in the
    // filename.  (This is necessary so that we don't confuse "c:\a\b.txt"
    // as matching "c:\abc\d.txt" - if we only matched the "c:\a" prefix,
    // we'd miss the fact that the file is actually in directory "c:\abc",
    // not "c:\a".)
    if (filename[plen] != '/')
        return false;

    // We're good on the path prefix - we definitely have a file that's
    // within the 'path' directory or one of its subdirectories.  If we're
    // allowed to match on subdirectories, we already have our answer
    // (true).  If we're not allowed to match subdirectories, we still have
    // one more check, which is that the rest of the filename is free of
    // path separator charactres.  If it is, we have a file that's directly
    // in the 'path' directory; otherwise it's in a subdirectory of 'path'
    // and thus isn't a match.
    if (include_subdirs) {
        // Filename is in the 'path' directory or one of its
        // subdirectories, and we're allowed to match on subdirectories, so
        // we have a match.
        return true;
    }

    // We're not allowed to match subdirectories, so scan the rest of
    // the filename for path separators.  If we find any, the file is
    // in a subdirectory of 'path' rather than directly in 'path'
    // itself, so it's not a match.  If we don't find any separators,
    // we have a file directly in 'path', so it's a match.
    const char* p;
    for (p = filename; *p != '\0' && *p != '/' ; ++p)
        ;

    // If we reached the end of the string without finding a path
    // separator character, it's a match .
    return *p == '\0';
}

/* ------------------------------------------------------------------------ */
/*
 *   Convert an OS filename path to URL-style format.  This isn't a true URL
 *   conversion; rather, it simply expresses a filename in Unix-style
 *   notation, as a series of path elements separated by '/' characters.
 *   Unlike true URLs, we don't use % encoding or a scheme prefix (file://,
 *   etc).
 *
 *   The result path never ends in a trailing '/', unless the entire result
 *   path is "/".  This is for consistency; even if the source path ends with
 *   a local path separator, the result doesn't.
 *
 *   If the local file system syntax uses '/' characters as ordinary filename
 *   characters, these must be replaced with some other suitable character in
 *   the result, since otherwise they'd be taken as path separators when the
 *   URL is parsed.  If possible, the substitution should be reversible with
 *   respect to os_cvt_dir_url(), so that the same URL read back in on this
 *   same platform will produce the same original filename.  One particular
 *   suggestion is that if the local system uses '/' to delimit what would be
 *   a filename extension on other platforms, replace '/' with '.', since
 *   this will provide reversibility as well as a good mapping if the URL is
 *   read back in on another platform.
 *
 *   The local equivalents of "." and "..", if they exist, are converted to
 *   "." and ".." in the URL notation.
 *
 *   Examples:
 *
 *.   Windows: images\rooms\startroom.jpg -> images/rooms/startroom.jpg
 *.   Windows: ..\startroom.jpg -> ../startroom.jpg
 *.   Mac:     :images:rooms:startroom.jpg -> images/rooms/startroom.jpg
 *.   Mac:     ::startroom.jpg -> ../startroom.jpg
 *.   VMS:     [.images.rooms]startroom.jpg -> images/rooms/startroom.jpg
 *.   VMS:     [-.images]startroom.jpg -> ../images/startroom.jpg
 *.   Unix:    images/rooms/startroom.jpg -> images/rooms/startroom.jpg
 *.   Unix:    ../images/startroom.jpg -> ../images/startroom.jpg
 *
 *   If the local name is an absolute path in the local file system (e.g.,
 *   Unix /file, Windows C:\file), translate as follows.  If the local
 *   operating system uses a volume or device designator (Windows C:, VMS
 *   SYS$DISK:, etc), make the first element of the path the exact local
 *   syntax for the device designator: /C:/ on Windows, /SYS$DISK:/ on VMS,
 *   etc.  Include the local syntax for the device prefix.  For a system like
 *   Unix with a unified file system root ("/"), simply start with the root
 *   directory.  Examples:
 *
 *.    Windows:  C:\games\deep.gam         -> /C:/games/deep.gam
 *.    Windows:  C:games\deep.gam          -> /C:./games/deep.gam
 *.    Windows:  \\SERVER\DISK\games\deep.gam -> /\\SERVER/DISK/games/deep.gam
 *.    Mac OS 9: Hard Disk:games:deep.gam  -> /Hard Disk:/games/deep.gam
 *.    VMS:      SYS$DISK:[games]deep.gam  -> /SYS$DISK:/games/deep.gam
 *.    Unix:     /games/deep.gam           -> /games/deep.gam
 *
 *   Rationale: it's effectively impossible to create a truly portable
 *   representation of an absolute path.  Operating systems are too different
 *   in the way they represent root paths, and even if that were solvable, a
 *   root path is essentially unusable across machines anyway because it
 *   creates a dependency on the contents of a particular machine's disk.  So
 *   if we're called upon to translate an absolute path, we can forget about
 *   trying to be truly portable and instead focus on round-trip fidelity -
 *   i.e., making sure that applying os_cvt_url_dir() to our result recovers
 *   the exact original path, assuming it's done on the same operating
 *   system.  The approach outlined above should achieve round-trip fidelity
 *   when a local path is converted to a URL and back on the same machine,
 *   since the local URL-to-path converter should recognize its own special
 *   type of local absolute path prefix.  It also produces reasonable results
 *   on other platforms - see the os_cvt_url_dir() comments below for
 *   examples of the decoding results for absolute paths moved to new
 *   platforms.  The result when a device-rooted absolute path is encoded on
 *   one machine and then decoded on another will generally be a local path
 *   with a root on the default device/volume and an outermost directory with
 *   a name based on the original machine's device/volume name.  This
 *   obviously won't reproduce the exact original path, but since that's
 *   impossible anyway, this is probably as good an approximation as we can
 *   create.
 *
 *   Character sets: the input could be in local or UTF-8 character sets.
 *   The implementation shouldn't care, though - just treat bytes in the
 *   range 0-127 as plain ASCII, and everything else as opaque.  I.e., do not
 *   quote or otherwise modify characters outside the 0-127 range.
 */
void os_cvt_dir_url(char *result_buf, size_t result_buf_size,
                    const char *src_url)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_cvt_dir_url");
#endif
    // From osnoui.c
    char *dst = result_buf;
    const char *src = src_url;
    size_t rem = result_buf_size;

    /*
     *   check for an absolute path
     */

    /*
     *   Run through the source buffer, copying characters to the output
     *   buffer.  If we encounter a '/', convert it to a path separator
     *   character.
     */
    for ( ; *src != '\0' && rem > 1 ; ++src, ++dst, --rem)
    {
        /*
         *   replace slashes with path separators; expand '%' sequences; copy
         *   all other characters unchanged
         */
        if (*src == '/')
        {
            /* change '/' to the local path separator */
            *dst = OSPATHCHAR;
        }
        else if ((unsigned char)*src < 32)
        {
            *dst = '_';
        }
        else
        {
            /* copy this character unchanged */
            *dst = *src;
        }
    }

    /* add a null terminator and we're done */
    *dst = '\0';
}

/*
 *   Convert a URL-style path into a filename path expressed in the local
 *   file system's syntax.  Fills in result_buf with a file path, constructed
 *   using the local file system syntax, that corresponds to the path in
 *   src_url expressed in URL-style syntax.  Examples:
 *
 *   images/rooms/startroom.jpg ->
 *.   Windows   -> images\rooms\startroom.jpg
 *.   Mac OS 9  -> :images:rooms:startroom.jpg
 *.   VMS       -> [.images.rooms]startroom.jpg
 *
 *   The source format isn't a true URL; it's simply a series of path
 *   elements separated by '/' characters.  Unlike true URLs, our input
 *   format doesn't use % encoding and doesn't have a scheme (file://, etc).
 *   (Any % in the source is treated as an ordinary character and left as-is,
 *   even if it looks like a %XX sequence.  Anything that looks like a scheme
 *   prefix is left as-is, with any // treated as path separators.
 *
 *   images/file%20name.jpg ->
 *.   Windows   -> images\file%20name.jpg
 *
 *   file://images/file.jpg ->
 *.   Windows   -> file_\\images\file.jpg
 *
 *   Any characters in the path that are invalid in the local file system
 *   naming rules are converted to "_", unless "_" is itself invalid, in
 *   which case they're converted to "X".  One exception is that if '/' is a
 *   valid local filename character (rather than a path separator as it is on
 *   Unix and Windows), it can be used as the replacement for the character
 *   that os_cvt_dir_url uses as its replacement for '/', so that this
 *   substitution is reversible when a URL is generated and then read back in
 *   on this same platform.
 *
 *   images/file:name.jpg ->
 *.   Windows   -> images\file_name.jpg
 *.   Mac OS 9  -> :images:file_name.jpg
 *.   Unix      -> images/file:name.jpg
 *
 *   The path elements "." and ".." are specifically defined as having their
 *   Unix meanings: "." is an alias for the preceding path element, or the
 *   working directory if it's the first element, and ".." is an alias for
 *   the parent of the preceding element.  When these appear as path
 *   elements, this routine translates them to the appropriate local
 *   conventions.  "." may be translated simply by removing it from the path,
 *   since it reiterates the previous path element.  ".." may be translated
 *   by removing the previous element - HOWEVER, if ".." appears as the first
 *   element, it has to be retained and translated to the equivalent local
 *   notation, since it will have to be applied later, when the result_buf
 *   path is actually used to open a file, at which point it will combined
 *   with the working directory or another base path.
 *
 *.  /images/../file.jpg -> [Windows] file.jpg
 *.  ../images/file.jpg ->
 *.   Windows  -> ..\images\file.jpg
 *.   Mac OS 9 -> ::images:file.jpg
 *.   VMS      -> [-.images]file.jpg
 *
 *   If the URL path is absolute (starts with a '/'), the routine inspects
 *   the path to see if it was created by the same OS, according to the local
 *   rules for converting absolute paths in os_cvt_dir_url() (see).  If so,
 *   we reverse the encoding done there.  If it doesn't appear that the name
 *   was created by the same operating system - that is, if reversing the
 *   encoding doesn't produce a valid local filename - then we create a local
 *   absolute path as follows.  If the local system uses device/volume
 *   designators, we start with the current working device/volume or some
 *   other suitable default volume.  We then add the first element of the
 *   path, if any, as the root directory name, applying the usual "_" or "X"
 *   substitution for any characters that aren't allowed in local names.  The
 *   rest of the path is handled in the usual fashion.
 *
 *.  /images/file.jpg ->
 *.    Windows -> \images\file.jpg
 *.    Unix    -> /images/file.jpg
 *
 *.  /c:/images/file.jpg ->
 *.    Windows -> c:\images\file.jpg
 *.    Unix    -> /c:/images/file.jpg
 *.    VMS     -> SYS$DISK:[c__.images]file.jpg
 *
 *.  /Hard Disk:/images/file.jpg ->
 *.    Windows -> \Hard Disk_\images\file.jpg
 *.    Unix    -> SYS$DISK:[Hard_Disk_.images]file.jpg
 *
 *   Note how the device/volume prefix becomes the top-level directory when
 *   moving a path across machines.  It's simply not possible to reconstruct
 *   the exact original path in such cases, since device/volume syntax rules
 *   have little in common across systems.  But this seems like a good
 *   approximation in that (a) it produces a valid local path, and (b) it
 *   gives the user a reasonable basis for creating a set of folders to mimic
 *   the original source system, if they want to use that approach to port
 *   the data rather than just changing the paths internally in the source
 *   material.
 *
 *   Character sets: use the same rules as for os_cvt_dir_url().
 */
void os_cvt_url_dir(char *result_buf, size_t result_buf_size,
                    const char *src_url)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_cvt_url_dir");
#endif
    char *dst;
    const char *src;
    size_t rem;
    int last_was_sep;

    /*
     *   Run through the source buffer, copying characters to the output
     *   buffer.  If we encounter a '/', convert it to a path separator
     *   character.
     */
    for (last_was_sep = false, dst = result_buf, src = src_url,
         rem = result_buf_size ;
         *src != '\0' && rem > 1 ; ++dst, ++src, --rem)
    {
        /*
         *   replace slashes with path separators; expand '%' sequences; copy
         *   all other characters unchanged
         */
        if (*src == '/')
        {
            *dst = OSPATHCHAR;
            last_was_sep = true;
        }
        else if (*src == '%'
                 && isxdigit((unsigned char)*(src+1))
                 && isxdigit((unsigned char)*(src+2)))
        {
            unsigned char c;
            unsigned char acc;

            /* convert the '%xx' sequence to its character code */
            c = *++src;
            acc = (c - (c >= 'A' && c <= 'F'
                        ? 'A' - 10
                        : c >= 'a' && c <= 'f'
                          ? 'a' - 10
                          : '0')) << 4;
            c = *++src;
            acc += (c - (c >= 'A' && c <= 'F'
                         ? 'A' - 10
                         : c >= 'a' && c <= 'f'
                           ? 'a' - 10
                           : '0'));

            /* set the character */
            *dst = acc;

            /* it's not a separator */
            last_was_sep = false;
        }
        else
        {
            *dst = *src;
            last_was_sep = false;
        }
    }

    /*
     *   add an additional ending separator if desired and if the last
     *   character wasn't a separator
     */
    /*if (end_sep && rem > 1 && !last_was_sep)
        *dst++ = OSPATHCHAR;*/

    /* add a null terminator and we're done */
    *dst = '\0';
}

/* ------------------------------------------------------------------------ */
/*
 *   Get a suitable seed for a random number generator; should use the system
 *   clock or some other source of an unpredictable and changing seed value.
 */
void os_rand(long *seed)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_rand");
#endif
    time_t t;
    time( &t );
    *seed = (long)t;
}

/*
 *   Generate random bytes for use in seeding a PRNG (pseudo-random number
 *   generator).  This is an extended version of os_rand() for PRNGs that use
 *   large seed vectors containing many bytes, rather than the simple 32-bit
 *   seed that os_rand() assumes.
 *
 *   As with os_rand(), this function isn't meant to be used directly as a
 *   random number source for ongoing use - instead, this is intended mostly
 *   for seeding a PRNG, which will then be used as the primary source of
 *   random numbers.  The big problem with using this routine directly as a
 *   randomness source is that some implementations might rely heavily on
 *   environmental randomness, such as the real-time clock or OS usage
 *   statistics.  Such sources tend to provide reasonable entropy from one
 *   run to the next, but not within a single session, as the underlying data
 *   sources don't change rapidly enough.
 *
 *   Ideally, this routine should generate *truly* random bytes obtained from
 *   hardware sources.  Not all systems can provide that, though, so true
 *   randomness isn't guaranteed.  Here are the suggested implementation
 *   options, in descending order of desirability:
 *
 *   1.  Use a hardware source of true randomness, such as a /dev/rand type
 *   of device.  However, note that this call should return reasonably
 *   quickly, so always use a non-blocking source.  Some Unix /dev/rand
 *   devices, for example, can block indefinitely to allow sufficient entropy
 *   to accumulate.
 *
 *   2. Use a cryptographic random number source provided by the OS.  Some
 *   systems provide this as an API service.  If going this route, be sure
 *   that the OS generator is itself "seeded" with some kind of true
 *   randomness source, as it defeats the whole purpose if you always return
 *   a fixed pseudo-random sequence each time the program runs.
 *
 *   3. Use whatever true random sources are available locally to seed a
 *   software pseudo-random number generator, then generate bytes from your
 *   PRNG.  Some commonly available sources of true randomness are a
 *   high-resolution timer, the system clock, the current process ID,
 *   logged-in user ID, environment variables, uninitialized pages of memory,
 *   the IP address; each of these sources might give you a few bits of
 *   entropy at best, so the best bet is to use an ensemble.  You could, for
 *   example, concatenate a bunch of this type of information together and
 *   calculate an MD5 or SHA1 hash to mix the bits more thoroughly.  For the
 *   PRNG, use a cryptographic generator.  (You could use the ISAAC generator
 *   from TADS 3, as that's a crypto PRNG, but it's probably better to use a
 *   different generator here since TADS 3 is going to turn around and use
 *   this function's output to seed ISAAC - seeding one ISAAC instance with
 *   another ISAAC's output seems likely to magnify any weaknesses in the
 *   ISAAC algorithm.)  Note that this option is basically the DIY version of
 *   option 2.  Option 2 is better because the OS probably has access to
 *   better sources of true randomness than an application does.
 */
void os_gen_rand_bytes(unsigned char *buf, size_t len)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_gen_rand_bytes");
#endif
    // Copied from FrobTADS

    // Read bytes from /dev/urandom to fill the buffer (use /dev/urandom
    // rather than /dev/random, so that we don't block for long periods -
    // /dev/random can be quite slow because it's designed not to return
    // any bits until a fairly high threshold of entropy has been reached).
    int f = open("/dev/urandom", O_RDONLY);
    read(f, (void*)buf, len);
    close(f);
}

/* ------------------------------------------------------------------------ */
/*
 *   Display routines.
 *
 *  For Fabularium, we use our GLK implementation - see osglk.c
 *
 */

/* os_printz
 * os_print
 * os_dbg_printf
 * os_dbg_vprintf
 * os_status
 * os_get_status
 * os_score
 * os_strsc
 * oscls
 * os_redraw
 * os_flush
 * os_update_display
 * os_set_text_attr
 * os_set_text_color
 * os_set_screen_color
 * os_plain
 * os_set_title
 * os_more_prompt
 * os_start_html
 * os_end_html
 * os_nonstop_mode
 * os_csr_busy
 *
 * are all defined in osglk.c. */


/* ------------------------------------------------------------------------ */
/*
 *   User Input Routines
 */

/* os_askfile
 * os_gets
 * os_gets_timeout
 * os_gets_cancel
 * os_getc
 * os_getc_raw
 * os_waitc
 * os_get_event
 *
 * are all defined in osglk.c. */

/* os_input_dialog
 *
 * is defined in indlg_tx3.cpp */

/* ------------------------------------------------------------------------ */
/*
 *   OS main entrypoint
 */

/* os0main
 * os0main2
 *
 * are defined in os0.c */

/*
 *   OBSOLETE - Get filename from startup parameter, if possible; returns
 *   true and fills in the buffer with the parameter filename on success,
 *   false if no parameter file could be found.
 *
 *   (This was used until TADS 2.2.5 for the benefit of the Mac interpreter,
 *   and interpreters on systems with similar desktop shells, to allow the
 *   user to launch the terp by double-clicking on a saved game file.  The
 *   terp would read the launch parameters, discover that a saved game had
 *   been used to invoke it, and would then stash away the saved game info
 *   for later retrieval from this function.  This functionality was replaced
 *   in 2.2.5 with a command-line parameter: the terp now uses the desktop
 *   launch data to synthesize a suitable argv[] vectro to pass to os0main()
 *   or os0main2().  This function should now simply be stubbed out - it
 *   should simply return FALSE.)
 */
int os_paramfile(char *buf)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_paramfile");
#endif
    return false;
}

/* os_init
 * os_uninit
 * os_expause
 * os_term
 * os_instbrk
 * os_break
 *
 * are defined in osglk.c */

/*
 *   Sleep for a given interval.  This should simply pause for the given
 *   number of milliseconds, then return.  On multi-tasking systems, this
 *   should use a system API to suspend the current process for the desired
 *   delay; on single-tasking systems, this can simply sit in a wait loop
 *   until the desired interval has elapsed.
 */
void os_sleep_ms(long delay_in_milliseconds)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_sleep_ms");
#endif
    usleep(delay_in_milliseconds * 1000);
}

/* os_yield
 * os_set_save_ext
 * os_get_save_ext
 *
 * are defined in osglk.c */

/* ------------------------------------------------------------------------*/
/*
 *   Translate a character from the HTML 4 Unicode character set to the
 *   current character set used for display.  Takes an HTML 4 character
 *   code and returns the appropriate local character code.
 *
 *   The result buffer should be filled in with a null-terminated string
 *   that should be used to represent the character.  Multi-character
 *   results are possible, which may be useful for certain approximations
 *   (such as using "(c)" for the copyright symbol).
 *
 *   Note that we only define this prototype if this symbol isn't already
 *   defined as a macro, which may be the case on some platforms.
 *   Alternatively, if the function is already defined (for example, as an
 *   inline function), the defining code can define OS_XLAT_HTML4_DEFINED,
 *   in which case we'll also omit this prototype.
 *
 *   Important: this routine provides the *default* mapping that is used
 *   when no external character mapping file is present, and for any named
 *   entities not defined in the mapping file.  Any entities in the
 *   mapping file, if used, will override this routine.
 *
 *   A trivial implementation of this routine (that simply returns a
 *   one-character result consisting of the original input character,
 *   truncated to eight bits if necessary) can be used if you want to
 *   require an external mapping file to be used for any game that
 *   includes HTML character entities.  The DOS version implements this
 *   routine so that games will still look reasonable when played with no
 *   mapping file present, but other systems are not required to do this.
 */
void
os_xlat_html4( unsigned int html4_char, char* result, size_t result_buf_len )
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_xlat_html4");
#endif
    // HTML 4 characters are Unicode.  Tads 3 provides just the
    // right mapper: Unicode to ASCII.  We make it static in order
    // not to create a mapper on each call and save CPU cycles.
    static CCharmapToLocalASCII mapper;
    result[mapper.map_char(html4_char, result, result_buf_len)] = '\0';
}

/*
 *   Generate a filename for a character-set mapping file.  This function
 *   should determine the current native character set in use, if
 *   possible, then generate a filename, according to system-specific
 *   conventions, that we should attempt to load to get a mapping between
 *   the current native character set and the internal character set
 *   identified by 'internal_id'.
 *   
 *   The internal character set ID is a string of up to 4 characters.
 *   
 *   On DOS, the native character set is a DOS code page.  DOS code pages
 *   are identified by 3- or 4-digit identifiers; for example, code page
 *   437 is the default US ASCII DOS code page.  We generate the
 *   character-set mapping filename by appending the internal character
 *   set identifier to the DOS code page number, then appending ".TCP" to
 *   the result.  So, to map between ISO Latin-1 (internal ID = "La1") and
 *   DOS code page 437, we would generate the filename "437La1.TCP".
 *   
 *   Note that this function should do only two things.  First, determine
 *   the current native character set that's in use.  Second, generate a
 *   filename based on the current native code page and the internal ID.
 *   This function is NOT responsible for figuring out the mapping or
 *   anything like that -- it's simply where we generate the correct
 *   filename based on local convention.
 *   
 *   'filename' is a buffer of at least OSFNMAX characters.
 *   
 *   'argv0' is the executable filename from the original command line.
 *   This parameter is provided so that the system code can look for
 *   mapping files in the original TADS executables directory, if desired.
 */
void os_gen_charmap_filename(char *filename, char *internal_id, char *argv0)
{
    __android_log_write(ANDROID_LOG_ERROR, "osfab.c", "FIXME: os_gen_charmap_filename");
    filename[0] = 0;
}

/*
 *   Receive notification that a character mapping file has been loaded.
 *   The caller doesn't require this routine to do anything at all; this
 *   is purely for the system-dependent code's use so that it can take
 *   care of any initialization that it must do after the caller has
 *   loaded a charater mapping file.  'id' is the character set ID, and
 *   'ldesc' is the display name of the character set.  'sysinfo' is the
 *   extra system information string that is stored in the mapping file;
 *   the interpretation of this information is up to this routine.
 *   
 *   For reference, the Windows version uses the extra information as a
 *   code page identifier, and chooses its default font character set to
 *   match the code page.  On DOS, the run-time requires the player to
 *   activate an appropriate code page using a DOS command (MODE CON CP
 *   SELECT) prior to starting the run-time, so this routine doesn't do
 *   anything at all on DOS. 
 */
void os_advise_load_charmap(char *id, char *ldesc, char *sysinfo)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_advise_load_charmap");
#endif
}

/*
 *   Generate the name of the character set mapping table for Unicode
 *   characters to and from the given local character set.  Fills in the
 *   buffer with the implementation-dependent name of the desired
 *   character set map.  See below for the character set ID codes.
 *   
 *   For example, on Windows, the implementation would obtain the
 *   appropriate active code page (which is simply a Windows character set
 *   identifier number) from the operating system, and build the name of
 *   the Unicode mapping file for that code page, such as "CP1252".  On
 *   Macintosh, the implementation would look up the current script system
 *   and return the name of the Unicode mapping for that script system,
 *   such as "ROMAN" or "CENTEURO".
 *   
 *   If it is not possible to determine the specific character set that is
 *   in use, this function should return "asc7dflt" (ASCII 7-bit default)
 *   as the character set identifier on an ASCII system, or an appropriate
 *   base character set name on a non-ASCII system.  "asc7dflt" is the
 *   generic character set mapping for plain ASCII characters.
 *   
 *   The given buffer must be at least 32 bytes long; the implementation
 *   must limit the result it stores to 32 bytes.  (We use a fixed-size
 *   buffer in this interface for simplicity, and because there seems no
 *   need for greater flexibility in the interface; a character set name
 *   doesn't carry very much information so shouldn't need to be very
 *   long.  Note that this function doesn't generate a filename, but
 *   simply a mapping name; in practice, a map name will be used to
 *   construct a mapping file name.)
 *   
 *   Because this function obtains the Unicode mapping name, there is no
 *   need to specify the internal character set to be used: the internal
 *   character set is Unicode.  
 */
/*
 *   Implementation note: when porting this routine, the convention that
 *   you use to name your mapping files is up to you.  You should simply
 *   choose a convention for this implementation, and then use the same
 *   convention for packaging the mapping files for your OS release.  In
 *   most cases, the best convention is to use the names that the Unicode
 *   consortium uses in their published cross-mapping listings, since
 *   these listings can be used as the basis of the mapping files that you
 *   include with your release.  For example, on Windows, the convention
 *   is to use the code page number to construct the map name, as in
 *   CP1252 or CP1250.  
 */
void os_get_charmap(char *mapname, int charmap_id)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osfab.c", "os_get_charmap");
#endif
    switch (charmap_id)
    {
        case OS_CHARMAP_DISPLAY:
        case OS_CHARMAP_FILECONTENTS:
            strcpy(mapname, "utf-8");
            break;

        case OS_CHARMAP_FILENAME:
        default:
            strcpy(mapname, "asc7dflt");
            break;
    }
}

/* ------------------------------------------------------------------------ */
/*
 *   External Banner Interface.  This interface provides the ability to
 *   divide the display window into multiple sub-windows, each with its own
 *   independent contents. */

/* os_banner_create
 * os_banner_delete
 * os_banner_orphan
 * os_banner_getinfo
 * os_banner_getcharwidth
 * os_banner_getcharheight
 * os_banner_clear
 * os_banner_disp
 * os_banner_set_attr
 * os_banner_set_color
 * os_banner_set_screen_color
 * os_banner_flush
 * os_banner_set_size
 * os_banner_size_to_contents
 * os_banner_start_html
 * os_banner_end_html
 * os_banner_goto
 *
 * are defined in osglk.c */

/* os_get_sysinfo
 *
 * is defined in osglk.c */
