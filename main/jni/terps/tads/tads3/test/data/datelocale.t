#charset "latin-1"

#include <tads.h>
#include <date.h>
#include <bignum.h>

tzcache: object
    tz = static new TimeZone('America/Los_Angeles')
    errcnt = 0
;

main()
{
    Date.setLocaleInfo(
        DateMonthNames, 'Januar,Februar,MÄRZ,April,Mai,Juni,Juli,August,'
        + 'September,Oktober,November=STRASSTOBER,Dezember=Weißtöber',

        DateMonthAbbrs, 'Jan.=Jän.=Jan=Jän,Feb.=Feb,MÄRZ=MÄR=MÄR.,'
        + 'Apr.=Apr,Mai,Juni=Jun.=Jun,Juli=Jul.=Jul,Aug.=Aug,'
        + 'Sep.=Sept.=Sep=Sept,Okt.=Okt,Nov.=Nov,Dez.=Dez',

        DateWeekdayNames, 'Sonntag,Montag,Dienstag,Mittwoch,Donnerstag,'
        + 'Freitag,Samstag',

        DateWeekdayAbbrs, 'So.=So,Mo.=Mo,Di.=Di,Mi.=Mi,Do.=Do,Fr.=Fr,Sa.=Sa',

        DateAMPM, 'AM=A.M.,PM=P.M.',

        DateEra, 'AD=A.D.=CE,BC=BCE=B.C.',

        DateParseFilter, 'eu',

        DateOrdSuffixes, '.,.,.,.',

        DateFmtDate, '%#d.%#m.%y',

        DateFmtTimestamp, '%a %#d. %b %H:%M:%S %Y');
        
    local d0 = new Date('7/2/2012 18:47:15.815 America/Los_Angeles');
    test(d0, '%X', '18:47:15');
    test(d0, '%c', 'Di. 7. Feb. 18:47:15 2012');
    test(d0, '%x', '7.2.12');

    local d1 = new Date('14. Dez 2012 America/Los_Angeles');
    test(d1, '%t %B (%b) %Y', '14. Dezember (Dez.) 2012');
    test(d1, '%A (%a)', 'Freitag (Fr.)');

    local d2 = new Date('13. JÄN 1899 America/Los_Angeles');
    test(d2, '%t %B (%b) %Y', '13. Januar (Jan.) 1899');

    test(new Date('15 WEISSTÖBER 1950'), '%#d %B %Y', '15 Dezember 1950');
    test(new Date('15 weisstöber 1950'), '%#d %B %Y', '15 Dezember 1950');
    test(new Date('15 weißtöber 1950'), '%#d %B %Y', '15 Dezember 1950');
    test(new Date('15 märz 1950'), '%#d %B %Y', '15 MÄRZ 1950');
    test(new Date('19 Straßtober 1961'), '%#d %B %Y', '19 November 1961');
}

test(date, fmt, ref)
{
    local tz = tzcache.tz;
    local result = date.formatDate(fmt, tz);
    "<<date>> + <<fmt>> -&gt; <<result>>\n";

    if (result != ref)
    {
        "\t*** Mismatch ***\n";
        tzcache.errcnt += 1;
    }
}
