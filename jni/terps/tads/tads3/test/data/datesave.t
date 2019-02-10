#charset "latin-1"
#include <tads.h>
#include <date.h>

cache: object
    tz = static new TimeZone('America/Los_Angeles')
    tz1 = nil
    date1 = nil
    date2 = nil
;

main(args)
{
    if (args.length() != 2 || args[2] not in ('save', 'restore'))
    {
        "usage: datesave save | restore\n";
        return;
    }

    if (args[2] == 'restore')
        restoreGame('datesave.t3v');

    "--- Initial conditions ---\n";
    "date1 = <<cache.date1>>\n";
    "date2 = <<cache.date2>>\n";
    "tz1 = <<cache.tz1>>\n";

    local d = new Date('8-23-2010', cache.tz);
    "locale formatting: <<d.formatDate('%A, %B %t, %E', cache.tz)>>\n";

    /* create some persistent dates and change locale settings */
    cache.date1 = new Date('11-11-2011', cache.tz);
    cache.date2 = new Date('4-4-1994', cache.tz);
    cache.tz1 = new TimeZone('Australia/Sydney');
    Date.setLocaleInfo(
        DateMonthNames,
        'janvier,février,mars,avril,mai,juin,juillet,août,septembre,octobre,'
        + 'novembre,décembre',
        DateWeekdayNames, 'lundi,mardi,mercedi,jeudi,vendredi,samedi,dimanche',
        DateOrdSuffixes, 're,e');
        
    "\b--- With updates ---\n";
    "date1 = <<cache.date1>>\n";
    "date2 = <<cache.date2>>\n";
    "tz1 = <<cache.tz1>>\n";
    "locale formatting: <<d.formatDate('%A, %B %t, %E', cache.tz)>>\n";

    if (args[2] == 'save')
        saveGame('datesave.t3v');
}
