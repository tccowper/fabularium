#include <tads.h>
#include <date.h>
#include <bignum.h>

tzcache: object
    tz = static new TimeZone('America/Los_Angeles')
    errcnt = 0
;

main()
{
    local tz = tzcache.tz;
        
    // basic format tests
    local d0 = new Date('2/7/2012 15:46:27.123 America/Los_Angeles');
    test(d0, '%#m/%#d/%Y', '2/7/2012');
    test(d0, '%#m/%#d/%y', '2/7/12');
    test(d0, '%#m/%\ d/%y', '2/\ 7/12');
    test(d0, '%m/%d/%Y', '02/07/2012');
    test(d0, '%D', '02/07/12');
    test(d0, '%F', '2012-02-07');
    test(d0, '%H:%M percent%%', '15:46 percent%');
    test(d0, '%I:%M', '03:46');
    test(d0, '%#I:%M', '3:46');
    test(d0, '%#I:%M %P', '3:46 PM');
    test(d0, '%#I:%M %p', '3:46 pm');
    test(d0, '%#H:%M:%S.%N', '15:46:27.123');
    test(d0, '%T', '15:46:27');
    test(d0, '%r', '3:46:27 PM');
    test(d0, '%R', '15:46');
    test(d0, '%j', '038');
    test(d0, '%X', '15:46:27');
    test(d0, '%c', 'Tue Feb 7 15:46:27 2012');
    test(d0, '%x', '02/07/2012');
    test(new Date('12/31/2012 America/Los_Angeles'), '%j', '366');
    test(new Date('12/31/2011 America/Los_Angeles'), '%j', '365');
    test(d0, '%B %#d, %e', 'February 7, 2012 AD');
    test(d0, '%B %#d, %-e', 'February 7, AD 2012');
    test(d0, '%B %#d, %&E', 'February 7, AD MMXII');
    test(d0, '%B %#d, %-&E', 'February 7, MMXII AD');
    local d0a = new Date('-0043-06-10 America/Los_Angeles');
    test(d0a, '%B %#d, %e', 'June 10, 44 BC');
    test(d0a, '%B %#d, %-e', 'June 10, BC 44');
    test(d0a, '%B %#d, %E', 'June 10, 44 BC');
    test(d0a, '%B %#d, %-E', 'June 10, BC 44');
    test('1999 Jan 18 America/Los_Angeles',
         '%B %&d, %&Y', 'January XVIII, MCMXCIX');
    test('1888 Jun 29 America/Los_Angeles',
         '%B %&d, %&Y', 'June XXIX, MDCCCLXXXVIII');
    "\b";

    test('1/1/1970 1:00 GMT+0', '%s', '3600');
    test('9/10/1979 8:40 America/New_York', '%s', '305815200');
    test('1/19/2038 3:14:08 GMT+0', '%s', '2147483648');
    "\b";

    test('2011-1-1 America/Los_Angeles', '%a %U %W', 'Sat 00 00');
    test('2011-1-2 America/Los_Angeles', '%a %U %W', 'Sun 01 00');
    test('2011-1-3 America/Los_Angeles', '%a %U %W', 'Mon 01 01');
    test('2011-12-24 America/Los_Angeles', '%a %U %W', 'Sat 51 51');
    test('2011-12-25 America/Los_Angeles', '%a %U %W', 'Sun 52 51');
    test('2011-12-26 America/Los_Angeles', '%a %U %W', 'Mon 52 52');
    test('2011-12-31 America/Los_Angeles', '%a %U %W', 'Sat 52 52');
    "\b";

    // weekdays
    test(d0, '%m/%d/%Y weekday=%w, ISO weekday=%u',
         '02/07/2012 weekday=2, ISO weekday=2');
    d0a = new Date('2/5/2012 America/Los_Angeles');
    test(d0a, '%m/%d/%Y weekday=%w, ISO weekday=%u',
         '02/05/2012 weekday=0, ISO weekday=7');

    // day names
    test(d0, '%m/%d/%Y %a %A', '02/07/2012 Tue Tuesday');
    d0 += 4;
    test(d0, '%m/%d/%Y %a %A', '02/11/2012 Sat Saturday');
    d0 -= 6;
    test(d0, '%m/%d/%Y %a %A', '02/05/2012 Sun Sunday');
    "\b";

    // month names, day ordinals
    test(d0, '%b %B %t, %Y', 'Feb February 5th, 2012');
    d0 = d0.addInterval([0, -1]);
    test(d0, '%a %A, %b %B %t, %Y', 'Thu Thursday, Jan January 5th, 2012');
    d0 = d0.addInterval([0, -1]);
    test(d0, '%a %A, %b %B %t, %Y', 'Mon Monday, Dec December 5th, 2011');
    d0 = d0.addInterval([0, -36]);
    test(d0, '%a %A, %b %B %t, %Y', 'Fri Friday, Dec December 5th, 2008');
    d0 = d0.addInterval([0, 51]);
    test(d0, '%a %A, %b %B %t, %Y', 'Tue Tuesday, Mar March 5th, 2013');
    d0 = d0.addInterval([0, 0, 0, -24]);
    test(d0, '%a %A, %b %B %t, %Y', 'Mon Monday, Mar March 4th, 2013');
    d0 = d0.addInterval([0, 0, 0, -24]);
    test(d0, '%a %A, %b %B %t, %Y', 'Sun Sunday, Mar March 3rd, 2013');
    d0 = d0.addInterval([0, 0, 0, -24]);
    test(d0, '%a %A, %b %B %t, %Y', 'Sat Saturday, Mar March 2nd, 2013');
    d0 = d0.addInterval([0, 0, 0, -24]);
    test(d0, '%a %A, %b %B %t, %Y', 'Fri Friday, Mar March 1st, 2013');
    d0 = d0.addInterval([0, 0, 0, -24]);
    test(d0, '%a %A, %b %B %t, %Y', 'Thu Thursday, Feb February 28th, 2013');
    d0 = d0.addInterval([0, 0, -7]);
    test(d0, '%a %A, %b %B %t, %Y', 'Thu Thursday, Feb February 21st, 2013');
    d0 = d0.addInterval([0, 0, 1]);
    test(d0, '%a %A, %b %B %t, %Y', 'Fri Friday, Feb February 22nd, 2013');
    d0 = d0.addInterval([0, 0, 1]);
    test(d0, '%a %A, %b %B %t, %Y', 'Sat Saturday, Feb February 23rd, 2013');
    d0 = d0.addInterval([0, 0, 1]);
    test(d0, '%a %A, %b %B %t, %Y', 'Sun Sunday, Feb February 24th, 2013');
    d0 = d0.addInterval([0, 0, -4]);
    test(d0, '%a %A, %b %B %t, %Y', 'Wed Wednesday, Feb February 20th, 2013');
    "\b";

    // interval of more than 24 hours
    d0 = d0.addInterval([0, 0, 0, 99]);
    test(d0, '%m/%d/%Y %H:%M', '02/24/2013 18:46');
    d0 = d0.addInterval([0, 0, 0, -121]);
    test(d0, '%m/%d/%Y %H:%M', '02/19/2013 17:46');
    "\b";

    // some time zone tests in standard and daylight time
    local d1 = new Date('1/1/2012 America/Los_Angeles');
    local d2 = new Date('7/1/2012 America/Los_Angeles');
    test(d1, '%m/%d/%Y %H:%M:%S %z', '01/01/2012 00:00:00 PST');
    test(d1, '%m/%d/%Y %H:%M:%S %Z', '01/01/2012 00:00:00 -0800');
    test(d2, '%m/%d/%Y %H:%M:%S %z', '07/01/2012 00:00:00 PDT');
    test(d2, '%m/%d/%Y %H:%M:%S %Z', '07/01/2012 00:00:00 -0700');
    "\b";

    // PST to PDT time change - clock is set ahead, so there's no ambiguity
    // (local wall clock times from 2:00-2:59 do not exist, since the wall
    // clock time jumps straight from 1:59:59.999... to 3:00 AM
    local d3 = new Date('3/11/2012 1:59 America/Los_Angeles');
    local d4 = new Date('3/11/2012 3:01 America/Los_Angeles');
    test(d3, '%m/%d/%Y %H:%M:%S %z', '03/11/2012 01:59:00 PST');
    test(d4, '%m/%d/%Y %H:%M:%S %z', '03/11/2012 03:01:00 PDT');

    // PDT to PST time change - clock is set back, so there's ambiguity;
    // 1:00 through 1:59:59.999 happens first in PDT time, then suddenly
    // the clock jumps back to 1:00, now in PST, to repeat the nominal
    // 1 AM hour.  So wall clock times from 1:00-1:59:59 happen twice.
    // We happen to interpret times in the overlapping interval as being
    // in the post-change setting (PST), because we conceptually work
    // backwards and thus choose the later of the two possibilities
    // (1:xx PST is later than 1:xx PDT).
    local d5 = new Date('11/4/2012 0:59:59 America/Los_Angeles');
    local d6 = new Date('11/4/2012 1:59:59 America/Los_Angeles');
    local d7 = new Date('11/4/2012 2:00:01 America/Los_Angeles');
    test(d5, '%m/%d/%Y %H:%M:%S %z', '11/04/2012 00:59:59 PDT');
    test(d6, '%m/%d/%Y %H:%M:%S %z', '11/04/2012 01:59:59 PST');
    test(d7, '%m/%d/%Y %H:%M:%S %z', '11/04/2012 02:00:01 PST');
    
    // ... but we can also explicitly say we're in the pre-change zone
    local d8 = new Date('11/4/2012 1:59:59 PDT');
    test(d8, '%m/%d/%Y %H:%M:%S %z', '11/04/2012 01:59:59 PDT');

    // ... we can also pretend we didn't set our clocks back at exactly 2 AM,
    // but when displayed we'll still get the officially correct setting
    local d9 = new Date('11/4/2012 2:00:15 PDT');
    test(d9, '%m/%d/%Y %H:%M:%S %z', '11/04/2012 01:00:15 PST');
    "\b";

    // ISO week numbers
    local d10 = new Date('31 December 2006 PST');
    test(d10, '%G-W%V-%u', '2006-W52-7');
    test('2005-01-01 PST', '%G-W%V-%u', '2004-W53-6');
    test('2005-01-02 PST', '%G-W%V-%u', '2004-W53-7');
    test('2005-01-03 PST', '%G-W%V-%u', '2005-W01-1');
    test('2005-12-31 PST', '%G-W%V-%u', '2005-W52-6');
    test('2006-01-01 PST', '%G-W%V-%u', '2005-W52-7');
    test('2006-01-02 PST', '%G-W%V-%u', '2006-W01-1');
    test('2006-12-31 PST', '%G-W%V-%u', '2006-W52-7');
    test('2007-01-01 PST', '%G-W%V-%u', '2007-W01-1');
    test('2007-12-31 PST', '%G-W%V-%u', '2008-W01-1');
    test('2008-01-01 PST', '%G-W%V-%u', '2008-W01-2');
    test('2008-12-29 PST', '%G-W%V-%u', '2009-W01-1');
    test('2008-12-31 PST', '%G-W%V-%u', '2009-W01-3');
    test('2009-01-01 PST', '%G-W%V-%u', '2009-W01-4');
    test('2009-12-31 PST', '%G-W%V-%u', '2009-W53-4');
    test('2010-01-01 PST', '%G-W%V-%u', '2009-W53-5');
    test('2010-01-02 PST', '%G-W%V-%u', '2009-W53-6');
    test('2010-01-03 PST', '%G-W%V-%u', '2009-W53-7');
    test('2008-12-28 PST', '%G-W%V-%u', '2008-W52-7');
    test('2008-12-29 PST', '%G-W%V-%u', '2009-W01-1');
    test('2008-12-30 PST', '%G-W%V-%u', '2009-W01-2');
    test('2008-12-31 PST', '%G-W%V-%u', '2009-W01-3');
    test('2009-01-01 PST', '%G-W%V-%u', '2009-W01-4');
    "\b";

    // julian dates
    d0 = new Date('2/7/2012 15:46:27.123 America/Los_Angeles');
    jtest(d0, '%m/%d/%y', '01/25/12');
    jtest(d0, '%m/%d/%Y', '01/25/2012');
    jtest(d0, '%A, %B %#d, %Y', 'Tuesday, January 25, 2012');

    local j = d0.getJulianDay(), jd = d0.getJulianDate();
    "<<d0>> = %J format <<d0.formatDate('%J', tz)>>,
    %#J <<d0.formatDate('%#J', tz)>>,
    julian day <<j>>, julian date <<showList(jd)>>\b";

    julToGreg('October 4, 1582');
    julToGreg('2/16/2012');
    julToGreg('3/3/0275');
    julToGreg('Jan 1, AD 1');
    julToGreg('11/1/5000');
    julToGreg('11/1 4000 BC');

    if (tzcache.errcnt != 0)
        "\b*** <<tzcache.errcnt>> mismatch<<if tzcache.errcnt > 1>>es<<end>>
        found ***\n";
}

test(date, fmt, ref)
{
    if (dataType(date) == TypeSString)
        date = new Date(date);
    local tz = tzcache.tz;
    local result = date.formatDate(fmt, tz);
    "<<date>> + <<fmt>> -&gt; <<result>>\n";

    if (result != ref)
    {
        "\t*** Mismatch ***\n";
        tzcache.errcnt += 1;
    }
}

jtest(date, fmt, ref)
{
    local tz = tzcache.tz;
    local result = date.formatJulianDate(fmt, tz);
    "<<date>> + <<fmt>> Julian -&gt; <<result>>\n";

    if (result != ref)
    {
        "\t*** Mismatch ***\n";
        tzcache.errcnt += 1;
    }
}

julToGreg(date)
{
    local tz = tzcache.tz;
    local d = Date.parseJulianDate(date, nil, nil, tz);
    if (d != nil)
    {
        d = d[1];
        "<<date>> -&gt; <<d.formatJulianDate('%B %#d, %E', tz)>> Julian
        -&gt; <<d.formatDate('%B %#d, %E', tz)>> Gregorian\b";
    }
    else
        "<<date>> - invalid Julian date\n";
}

showList(l)
{
    for (local i in 1..l.length())
    {
        local e = l[i];
        "<<if i > 1>>, <<end>>";
        if (dataType(e) == TypeObject && e.ofKind(BigNumber))
            "<<%.4f e>>";
        else if (dataType(e) == TypeSString)
            "'<<e>>'";
        else
            "<<l[i]>>";
    }
}
