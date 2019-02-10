#include <tads.h>
#include <date.h>
#include <bignum.h>

ref: object
    tz = static new TimeZone('America/New_York')
    date = static new Date('2012-01-01')
;

main()
{
    test('1995-Nov-15', nil);
    test('Nov 15, 99 AD 1:25 pm ', nil);
    test('Nov 15, 95 4:30 pm PST', nil);
    test('May 15, 95 4:30 pm America/Los_Angeles', nil);
    test('May 15, 95 4:30 pm PST', nil);
    test('May 15, 95 4:30 pm PDT', nil);
    test('Dec 15, 95 4:30 pm America/Los_Angeles', nil);
    test('Dec 15, 95 4:30 pm PST', nil);
    test('Dec 15, 95 4:30 pm PDT', nil);

    local fmt = ['d ! y ! month', 'month ! y ! d'];
    test('15!1995!Nov', fmt);
    test('Nov!1995!15', fmt);
}

test(str, fmt)
{
    local d = Date.parseDate(str, fmt, ref.date, ref.tz);
    "<<str>> + <<showItem(fmt)>> -&gt;\n\t<<showItem(d)>>\b";
}

showItem(v)
{
    if (dataType(v) == TypeObject && v.ofKind(BigNumber))
        "<<%.4f v>>";
    else if (dataType(v) == TypeObject && v.ofKind(Date))
        "<<v.formatDate('%Y-%m-%d %H:%M:%S', ref.tz)>>";
    else if (dataType(v) == TypeSString)
        "'<<v>>'";
    else if (dataType(v) == TypeList
             || (dataType(v) == TypeObject && v.ofKind(Collection)))
        "[<<showList(v)>>]";
    else if (dataType(v) == TypeNil)
        "nil";
    else if (dataType(v) == TypeTrue)
        "true";
    else
        "<<v>>";
}

showList(l)
{
    for (local i in 1..l.length())
    {
        local e = l[i];
        "<<if i > 1>>, <<end>><<showItem(e)>>";
    }
}

