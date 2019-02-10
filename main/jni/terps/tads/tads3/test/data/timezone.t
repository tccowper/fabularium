#include <tads.h>
#include <date.h>
#include <bignum.h>

cache: object
    tz = static new TimeZone('America/Los_Angeles')
;

main()
{
    /* try formatting some dates with a custom zone */
    local tz = new TimeZone('TadsST-8TadsDT,M3.2.0,M11.1.0');
    "TZ=<<tz>>:\n";
    testDate('2012-3-7 1:00pm', tz);
    testDate('2012-3-11 1:59am', tz);
    testDate('2012-3-11 2:00am', tz);
    testDate('2012-3-11 2:00:00.001am', tz);
    testDate('2012-3-11 2:01am', tz);
    testDate('2012-3-11 1:00pm', tz);
    testDate('2012-3-12 1:00pm', tz);
    testDate('2011-11-5 1:00pm', tz);
    testDate('2011-11-6 0:59am', tz);
    testDate('2011-11-6 1:59am', tz);
    testDate('2011-11-6 2:01am', tz);
    testDate('2011-11-6 1:00pm', tz);
    testDate('2011-11-7 1:00pm', tz);
    "\b";

    tz = new TimeZone('America/New_York');
    testDate('1776-7-4', tz);
    testDate('1943-4-5', tz);
    testDate('1943-12-20', tz);
    testDate('1945-9-1', tz);
    testDate('2050-1-4', tz);
    testDate('2050-8-3', tz);
    "\b";

    test();
    test('America/Los_Angeles');
    test('America/New_York');
    test('EST-5EDT,M4.1.0,M10.5.0');
    test('EST-5EDT,J72,300');
    test('EST10EST');
    test('EST10');
    test('CST9:30');
    test('CST8CDT');
    test('UTC-0700');
    test(-8*60*60);
}

test([args])
{
    local tz = new TimeZone(args...);
    "new TimeZone(<<showList(args, tz)>>) -&gt;\n";
    "\tLocation: <<showList(tz.getLocation())>>\n";
    "\tName(s): <<showList(tz.getNames())>>\n";
    "\tHistory: <<showList(tz.getHistory(), tz)>>\n";
    "\tRules: <<showList(tz.getRules())>>\n";
    "\b";
}

testDate(d, tz)
{
    local dd = new Date(d, tz);
    local fmt = '%Y-%m-%d %I:%M %p %z';
    "<<d>> -&gt;\n\t<<
      dd.formatDate(fmt, tz)>> / <<dd.formatDate(fmt, cache.tz)>>\n
    \tHistory = <<showList(tz.getHistory(dd))>>\n";
}

showList(l, tz?)
{
    for (local i in 1..l.length())
    {
        local e = l[i];
        "<<if i > 1>>, <<end>>";
        if (dataType(e) == TypeObject && e.ofKind(BigNumber))
            "<<%.4f e>>";
        else if (dataType(e) == TypeSString)
            "'<<e>>'";
        else if (dataType(e) == TypeObject && e.ofKind(Date))
            "<<e.formatDate('%Y-%m-%d %H:%M:%S %z', tz)>>";
        else if (dataType(e) == TypeList
                 || (dataType(e) == TypeObject && e.ofKind(Collection)))
            "[<<showList(e, tz)>>]";
        else
            "<<l[i]>>";
    }
}
