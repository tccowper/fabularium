/*
 *   Copyright (c) 2012 Michael J. Roberts.  All Rights Reserved.
 *   
 *   This file is part of the TADS 3 VM source code.  Please see the
 *   accompanying license file, LICENSE.TXT, for information on using and
 *   copying this software.  
 *   
 *   This is a compiler for the IANA timezone database (known as "tz" or
 *   "zoneinfo").  It reads the zoneinfo source files and generates a
 *   custom binary file containing an encoded version of the rules.  The
 *   TADS 3 VM's Date intrinsic class reads this binary file to perform
 *   mappings between local time zones and universal time (UTC).  The
 *   zoneinfo sources contain a detailed history of time zone rules by
 *   region and timeline, including daylight savings time transitions.
 *   This historical information makes it possible for the Date class to
 *   calculate the correct local time in time zones around the world on
 *   arbitrary dates in the past, present, and future.
 *   
 *   To build the binary version of the database, run this program like so:
 *   
 *.    t3run -plain tz <IANA database directory path> <output file name>
 *   
 *   The IANA timezone database files are updated regularly, out of
 *   necessity.  The rules governing time zones in a given region are set
 *   by regional authorities (usually at the national level), and these
 *   rules are surprisingly volatile.  It perhaps shouldn't be surprising
 *   that the *historical* development of time zones involved a lot of
 *   revising and rethinking, given rapid changes over the past couple of
 *   centuries in science and technology.  What is surprising is how often
 *   the rules continue to change in the present day.  The rules for
 *   daylight savings time in particular continue to change with some
 *   frequency.  In fact, some countries don't have formulaic DST settings
 *   at all, but rather elect to set their DST transition points anew each
 *   year by fiat.  Anyway, the point is that the zoneinfo database will
 *   never be "done"; it will continue to be updated regularly to keep up
 *   with contemporary changes made by regional authorities.  This means
 *   that the TADS version will have to be updated regularly.  The update
 *   process is fairly easy; we just grab a new copy of the zoneinfo
 *   sources from the IANA site, and run this compiler to generate the TADS
 *   binary.  The TADS version is in an external binary file, so users can
 *   update it at any time without updating their whole TADS installation.
 *   
 *   Note that the TADS source code distribution doesn't contain the IANA
 *   database source files.  Those files are explicitly in the public
 *   domain, but there's some controversy (as of early 2012) about the
 *   provenance of some portions of their contents; as long as that's being
 *   sorted out, it seems best to keep them at arm's length.  Besides, it's
 *   better to avoid duplicating this kind of third-party data anyway,
 *   since doing so can create confusion over which version to use.  You
 *   can find the current sources at http://www.iana.org/time-zones.
 *   
 *   In our binary format, all time values are expressed in milliseconds.
 */
#include <tads.h>
#include <file.h>
#include <date.h>
#include <bignum.h>

usage()
{
    "usage: t3run tz [options] &lt;zoneinfo-folder&gt; &lt;output-file.t3tz&gt;\n
    options:\n
    \t--winzones - process ./windowsZones.xml, generate ./windowsZones.h\n
    \t--nodb - do not generate the database file\n
    \t--report-ambig-abbrs - create ambiguous abbr report (ambigAbbrs.txt)\n";
}

main(args)
{
    local gendb = true;
    local winzones = nil;
    local reportAmbigAbbrs = nil;
    
    /* check options */
    local argn;
    for (argn in 2..args.length() ; args[argn].startsWith('-') ; )
    {
        switch (args[argn])
        {
        case '--winzones':
            winzones = true;
            break;

        case '--nodb':
            gendb = nil;
            break;

        case '--report-ambig-abbrs':
            reportAmbigAbbrs = true;
            break;

        default:
            usage();
            return;
        }
    }

    /* check arguments - we need two more arguments (db folder, .cpp out) */
    if (argn + (gendb ? 2 : 1) != args.length() + 1)
    {
        usage();
        return;
    }

    /* scan files in the source directory */
    local inDir = new FileName(args[argn++]);
    "Reading database source files\n";
    for (local file in inDir.listDir())
    {
        /* skip directories */
        if (file.getFileInfo().isDir)
            continue;

        /* skip files with extensions and any Makefile */
        local fname = file.getBaseName();
        if (fname.find('.') != nil
            || fname.match(R'<nocase>%bmakefile$') != nil)
            continue;

        /* looks good - scan this file */
        tzData.scanFile(file);
    }

    /* read the zone description file zone.tab */
    tzData.scanZoneTab(inDir + 'zone.tab');

    /* if desired, process windowsZones.xml */
    if (winzones)
    {
        /* scan the file */
        "Reading windowsZones.xml\n";
        tzData.scanWinZones('windowsZones.xml');

        /* generate the output file */
        "Generating windowsZones.h\n";
        tzData.genWinZones('windowsZones.h');
    }

    /* if desired, resolve transitions and generate the database */
    if (gendb)
    {
        /* open the output file */
        local fpout = File.openRawFile(args[argn], FileAccessWrite);

        /* process the zone table */
        tzData.processZones();

        /* dump the table (only for debugging purposes) */
        //tzData.dump();

        /* write the version header (version=1) */
        "Writing output file\n";
        fpout.packBytes('a4 S x10', 'T3TZ', 1);

        /* write the master zone index */
        ". zone table\n";
        tzData.writeZoneIndex(fpout);

        /* write the links */
        ". links\n";
        tzData.writeLinks(fpout);

        /* write the abbreviation-to-zone table */
        ". zone abbreviations\n";
        tzData.writeZoneAbbrs(fpout, reportAmbigAbbrs ? ambigAbbrs : nil);

        /* write the individual zone information */
        ". zone data\n";
        tzData.writeZones(fpout);

        /* done with the output file */
        fpout.closeFile();

        /* if desired, write the ambiguous zone report */
        if (reportAmbigAbbrs)
            ambigAbbrs.writeReport();
        
        ". done\n";
    }

    /* figure some statistics */
    local zoneHistCnt = 0, zoneTransCnt = 0;
    tzData.zoneList.forEach(function(z) {
        zoneHistCnt += z.hist.length();
        zoneTransCnt += z.transList.length();
    });

    local ruleCnt = 0, ruleItemCnt = 0, ruleTransCnt = 0;
    tzData.ruleTab.forEach(function(r) {
        ++ruleCnt;
        if (r.items != nil)
            ruleItemCnt += r.items.length();
        if (r.propType(&transList) == TypeObject)
            ruleTransCnt += r.transList.length();
    });

    /* show statistics */
    "\b";
    "Zones: <<tzData.zoneList.length()>>\n";
    "Zone history entries: <<zoneHistCnt>>\n";
    "Zone resolved transition entries: <<zoneTransCnt>>\n";
    "Rules (unique names): <<ruleCnt>>\n";
    "Rules (line items): <<ruleItemCnt>>\n";
    "Rule transitions: <<ruleTransCnt>>\n";
    "UNTIL/FROM/TO year range: <<tzData.minYear>> - <<tzData.maxYear>>\n";
    "DST offset range: <<tzData.minDSTofs>> - <<tzData.maxDSTofs>>\n";
}

/*
 *   Ambiguous abbreviation report object
 */
ambigAbbrs: object
    writeReport()
    {
        /* open the file */
        local fp = File.openTextFile('ambigAbbr.txt', FileAccessWrite);

        /* write the zones-at-same-offset list */
        foreach (local t in zonesAtOffset)
            fp.writeFile(t);

        /* write the multiple-offsets */
        fp.writeFile('\n');
        foreach (local t in multiOffset)
            fp.writeFile(t);

        /* done with the file */
        fp.closeFile();
    }

    /* 
     *   text output list for abbreviations with multiple zones at the same
     *   offset (e.g., EST has America/New_York, America/Montreal, and many
     *   others at the same offset)
     */
    zonesAtOffset = static new Vector()

    /* 
     *   text output list for abbreviations with multiple offsets (e.g.,
     *   CST refers to North American Central Standard Time at -6, Cuba
     *   Standard Time at -5, China Standard Time at +8, Australia Central
     *   Standard Time at +9:30, and Australia Central Summer Time at
     *   +10:30) 
     */
    multiOffset = static new Vector()
;


/*
 *   Database object.  This keeps our various lists.
 */
tzData: object
    /*
     *   Scan a database source file 
     */
    scanFile(file)
    {
        ". <<file>>\n";

        /* open the file */
        local fp = File.openTextFile(file, FileAccessRead);
        fname = file;

        /* we're not processing a zone list yet */
        local zone = nil;

        /* read it line by line */
        for (local l = nil, linenum = 1 ;
             (l = fp.readFile()) != nil ;
             ++linenum)
        {
            /* remove any trailing newline */
            if (l.endsWith('\n'))
                l = l.substr(1, -1);

            /* remove any comments and trailing spaces */
            if (rexMatch('(.*?)<space>*#', l) != nil)
                l = rexGroup(1)[3];
            
            /* ignore blank lines */
            if (rexMatch('<space>*$', l) != nil)
                continue;

            /* parse the keyword portion */
            if (rexMatch('(<alpha>*)[\t ]+(.*)$', l) == nil)
            {
                errReport('invalid syntax');
                continue;
            }

            /* pull out the keyword and the rest */
            local kw = rexGroup(1)[3];
            local args = rexGroup(2)[3];
            
            /* check for a zone continuation */
            if (kw == '' && zone != nil)
            {
                /* add the zone, and we're done with the line */
                zone.addHist(args);
                continue;
            }
            else
            {
                /* not a zone continuation, so flush an open zone */
                zone = flushZone(zone);
            }

            /* match the keyword */
            switch (kw)
            {
            case 'Zone':
                /* start a new zone */
                args = args.split(delimPat, 2);
                zone = new ZoneDef(args[1]);
                zone.addHist(args[2]);
                break;

            case 'Rule':
                addRule(args.split(delimPat));
                break;

            case 'Link':
                addLink(args.split(delimPat));
                break;

            case 'Leap':
                addLeap(args.split(delimPat));
                break;

            default:
                /* unknown field */
                errReport('unknown keyword \'<<kw>>\'');
                break;
            }
        }

        /* flush any open zone rule */
        flushZone(zone);

        /* done with the file */
        fp.closeFile();
    }

    delimPat = static new RexPattern('<space>+')

    /*
     *   Scan zone.tab 
     */
    scanZoneTab(fname)
    {
        /* add the links to the zone table */
        linkList.forEach({l: zoneTab[l[2]] = zoneTab[l[1]]});

        /* scan the file */
        "Scanning <<fname>>\n";
        local fp = File.openTextFile(fname, FileAccessRead);
        self.fname = fname;
        for (local l = nil, linenum = 1 ; (l = fp.readFile()) != nil ;
             ++linenum)
        {
            /* remove any trailing newline */
            if (l.endsWith('\n'))
                l = l.substr(1, -1);

            /* remove any comments and trailing spaces */
            if (rexMatch('(.*?)<space>*#', l) != nil)
                l = rexGroup(1)[3];
            
            /* ignore blank lines */
            if (rexMatch('<space>*$', l) != nil)
                continue;

            /* split the line at tabs */
            l = l.split('\t');
            local country = l[1];
            local coords = l[2];
            local zoneName = l[3];
            local desc = l.length() >= 4 ? l[4] : '';

            /* look up the zone */
            local zone = zoneTab[zoneName];
            if (zone == nil)
            {
                errReport('undefined zone "<<zoneName>>" in zone.tab');
                continue;
            }

            /* add the location information to the zone object */
            zone.country = country;
            zone.coords = coords;
            zone.desc = desc;

            /* 
             *   set its zone.tab rank to its relative order in the file
             *   (the line number will do, as all we're after here is a
             *   sorting order that matches the order of appearance in the
             *   file) 
             */
            zone.zoneTabRank = linenum;
        }

        /* done with the file */
        fp.closeFile();
    }

    /*
     *   Scan windowsZones.xml 
     */
    scanWinZones(fname)
    {
        /* scan the file */
        local fp = File.openTextFile(fname, FileAccessRead);
        self.fname = fname;
        for (local l = nil, linenum = 1 ; (l = fp.readFile()) != nil ;
             ++linenum)
        {
            /* 
             *   if it's a <mapZone other="xxx" type="yyy"> line, parse it;
             *   ignore everything else in the file 
             */
            if (rexSearch(
                '[<]mapZone%s+other="(.+)"%s+type="(.+)"%s*/[>]', l) != nil)
            {
                /* "other" is the Windows name; "type" is the TZ name */
                local winName = rexGroup(1)[3];
                local tzName = rexGroup(2)[3];

                /* add this windows name to the zone */
                local zone = zoneTab[tzName];
                if (zone != nil)
                    zone.winZoneName = winName;
                else
                    errReport('missing Windows zone <<winName>> -> <<tzName>>');
            }
        }

        /* done with the file */
        fp.closeFile();
    }

    /*
     *   Generate windowsZones.h 
     */
    genWinZones(fname)
    {
        /* open the file */
        local fp = File.openTextFile(fname, FileAccessWrite);

        fp.writeFile('/*\n *   GENERATED FILE - DO NOT EDIT\n *\n'
                     + ' *   This file was generated by the TADS 3 zoneinfo\n'
                     + ' *   database process (tads3/tz/tz.t) from the Unicode\n'
                     + ' *   CLDR source file windowsZones.xml.  To regenerate,\n'
                     + ' *   run tz.t with the latest windowsZones.xml.\n *\n'
                     + ' *   See http://cldr.unicode.org for CLDR updates.\n *\n'
                     + ' *   (This file is designed to be #included within an\n'
                     + ' *   appropriate C struct initializer.)\n'
                     + ' */\n\n');

        /* get a list of Windows zones, sorted by Windows zone name */
        local wlst = zoneList.subset({z: z.winZoneName != nil})
            .sort(SortAsc, {a, b: a.winZoneName.compareTo(b.winZoneName)});

        /* write each zone with a Windows mapping */
        for (local zone in wlst, local i = 1 ; ; ++i)
        {
            /* figure the abbreviations for the last history entry */
            local abbrs = zone.getAbbrs() ?? ['', ''];
            
            /* write the zone information */
            fp.writeFile('<<if i>1>>,<<end>>\n'
                         + '    { L"<<zone.winZoneName>>", "<<zone.name>>", '
                         + '"<<abbrs[1]>>", "<<abbrs[2]>>" }');
        }
        fp.writeFile('\n\n');

        /* done with the file */
        fp.closeFile();
    }

    /*
     *   Process the zone table 
     */
    processZones()
    {
        /* sort the zones */
        "Sorting zone table\n";
        zoneList = zoneList.sort(
            SortAsc, { a, b: a.name.compareIgnoreCase(b.name) });
        
        /* resolve history items */
        "Resolving UNTIL dates...\n";
        local zcnt = zoneList.length(), i = 0, pct = 0;
        zoneList.forEach(function(z) {
            z.resolve();
            if (i++ * 100/zcnt > pct + 5)
            {
                "<<pct += 5>>%... ";
                flushOutput();
            }
        });
        "done!\n";
    }

    /*
     *   Write the master zone index to the output file
     */
    writeZoneIndex(fp)
    {
        /* write the placeholder table length */
        local fixCtx = prepLengthPrefix(fp, 'L');

        /* write the number of zone entries */
        fp.packBytes('L', zoneList.length());

        /* write the zones */
        for (local z in zoneList, local idx = 0 ; ; ++idx)
        {
            /* set the table index in the zone object */
            z.zoneTabIndex = idx;
            
            /* 
             *   write this zone name, a placeholder for its zone pointer,
             *   and its country code 
             */
            fp.packBytes('C/ax x4 a2', z.name, z.country);
 
            /* remember the location of this zone's master table pointer */
            z.masterTablePointerOfs = fp.getPos() - 6;
        }

        /* go back and fix up the table length pointer */
        setLengthPrefix(fixCtx);
    }

    /*
     *   Write the individual zone information 
     */
    writeZones(fp)
    {
        /* write each zone */
        for (local z in zoneList)
            z.writeZone(fp);
    }

    /*
     *   Write the links
     */
    writeLinks(fp)
    {
        /* write the placeholder table length */
        local fixCtx = prepLengthPrefix(fp, 'L');

        /* write the number of entries */
        fp.packBytes('L', linkList.length());

        /* write the links */
        for (local l in linkList)
        {
            /* look up the TO zone, resolving links */
            local to = zoneTab[resolveLink(l[1])];
            if (to == nil)
            {
                errReport('link <<l[2]>> <<l[1]>> - target not found');
                continue;
            }
            
            /* write the link - FROM:stringz, TO:uint32 */
            fp.packBytes('C/ax L', l[2], to.zoneTabIndex);
        }

        /* go back and fix up the table length pointer */
        setLengthPrefix(fixCtx);
    }

    /* 
     *   Note a DST offset.  We keep track of the largest DST offsets we
     *   find among all of the rules and zone history entries.
     *   
     *   This is useful because we make a simplifying assumption in the
     *   rule resolver that we can treat local standard time and daylight
     *   times as equivalent for the purposes of finding which rule applies
     *   to a given UNTIL date.  It's possible to construct an UNTIL date
     *   where this would pick the wrong rule: if the UNTIL date is stated
     *   as a wall clock time, and there's a transition from DST to
     *   standard time within the DST offset of the UNTIL date, we'd miss
     *   the transition.  Our sanity checker looks for cases where the
     *   difference between the UNTIL date and a rule transition time is
     *   within the maximum DST offset, and flags these cases for manual
     *   inspection.
     */
    noteDSTOffset(ofs)
    {
        local secs = parseTime(ofs);
        if (secs > maxDSTsecs)
        {
            maxDSTofs = ofs;
            maxDSTsecs = secs;
        }
        if (secs < minDSTsecs)
        {
            minDSTofs = ofs;
            minDSTsecs = secs;
        }
    }

    /* highest DST offset we've found (file representation, seconds) */
    minDSTofs = '0'
    maxDSTofs = '0'
    minDSTsecs = 0
    maxDSTsecs = 0

    /*
     *   Note an UNTIL, FROM, or TO year.  We keep track of the minimum and
     *   maximum years found in all zones, for statistics and to set the
     *   bounds for 'min' years in rule expansions. 
     */
    noteYear(yy)
    {
        if (rexMatch('%s*(%d{4})', yy))
        {
            yy = toInteger(rexGroup(1)[3]);
            if (yy < minYear)
                minYear = yy;
            if (yy > maxYear)
                maxYear = yy;
        }
    }

    /* minimum and maximum of all UNTIL, FROM, and TO years */
    maxYear = 0
    minYear = 1000000

    /*
     *   Add a zone abbreviation to our master table.  Each zone calls this
     *   for its current or ongoing abbreviations (but not old
     *   abbreviations that have been superseded).  This table lets
     *   run-time users map common zone abbreviations in parsed date/time
     *   strings to zoneinfo zones.
     *   
     *   'typ' is the standard/daylight type flag: 'S' for standard, 'D'
     *   for daylight, 'B' for both.  'B' applies to abbreviations that are
     *   used for both standard and daylight time for the same zone, such
     *   as Australian EST.
     */
    addZoneAbbr(zone, abbr, gmtofsSecs, typ)
    {
        /* 
         *   Ignore zone 'zzz': this is a placeholder used in the zoneinfo
         *   sources for certain special cases, not an actual zone format.
         *   Also ignore LMT; this is Local Mean Time, which is simply the
         *   local observed solar time used before time zones.  Every
         *   location has its own unique LMT, so this abbreviation is
         *   hopelessly ambiguous.
         */
        if (abbr is in ('zzz', 'LMT'))
            return;

        /* 
         *   if there's no country for the zone, it's one of the
         *   compatibility zones, so ignore it 
         */
        if (zone.country == nil)
            return;

        /* look up the abbreviation in our table */
        local alst = zoneAbbrTab[abbr];

        /* create a new list for the abbreviation if necessary */
        if (alst == nil)
            zoneAbbrTab[abbr] = alst = new Vector();

        /* add the new entry to the abbreviation's list */
        alst.append([zone, gmtofsSecs, typ]);
    }

    /*
     *   Write the zone abbreviation table.  'rpt' is the ambigAbbrs report
     *   object if we're supposed to generate an ambiguity report.
     */
    writeZoneAbbrs(fp, rpt)
    {
        /* get a list of the zone abbreviations, sorted by name */
        local alst = zoneAbbrTab.keysToList().sort();

        /* write a placeholder for the table length */
        local fixCtx = prepLengthPrefix(fp, 'L');

        /* write the number of entries */
        fp.packBytes('L', alst.length());

        /* set the tweak flags on zones that have them */
        foreach (local tw in tweakZones)
        {
            local z;
            if ((z = zoneTab[tw]) == nil)
                "\nMissing zone!!! <<tw>>\n";
            else
                z.abbrTweakOrder = 1;
        }

        /* write each entry */
        for (local abbr in alst)
        {
            /* get the zone list; skip it if it's empty */
            local tlst = zoneAbbrTab[abbr];

            /* sort the list  */
            tlst.sort(SortAsc, function(a, b)
            {
                /* sort first by GMT offset */
                local d;
                if ((d = a[2] - b[2]) != 0)
                    return d;

                /* if the zones are different, sort by zone */
                if (a[1] != b[1])
                {
                    /* sort by tweak order first */
                    if ((d = a[1].abbrTweakOrder - b[1].abbrTweakOrder) != 0)
                        return d;
                    
                    /* 
                     *   if either has a zone.tab rank, sort by index,
                     *   putting unindexed items at the end 
                     */
                    local ai = a[1].zoneTabRank;
                    local bi = b[1].zoneTabRank;
                    if (ai != nil || bi != nil)
                        return (ai ?? 1000000) - (bi ?? 1000000);

                    /* if neither has an index, sort by name */
                    return a[1].name.compareTo(b[1].name);
                }

                /* within a zone, put the 'B' entries first */
                return a[3].compareTo(b[3]);
            });

            /* 
             *   If desired, report ambiguous abbreviations.  This
             *   abbreviation is ambiguous only if it's associated with
             *   more than one ZoneDef. 
             */
            if (rpt != nil
                && tlst.length() > 1
                && tlst.indexWhich({x: x[1] != tlst[1][1]}) != nil)
            {
                /* 
                 *   There are two levels of ambiguity to report.  If the
                 *   same abbreviation is used for different zones at
                 *   different UTC offsets, we have a simple case of naming
                 *   collisions, where different countries or regions
                 *   picked names that worked out to the same abbreviation,
                 *   such as "Central Standard Time" in the US colliding
                 *   with "China Standard Time" and "Cuba Standard Time".
                 *   If we have multiple zones at the *same* offset, we
                 *   have a situation where there are several zoneinfo
                 *   entries for the same official time zone; this happens
                 *   when nearby locales have divergent histories but ended
                 *   up in the same zone today (e.g., Detroit vs New York,
                 *   which have different histories in that Detroit was
                 *   once in Central Time but moved to Eastern Time in
                 *   1915), and when different regions within a zone have
                 *   different rules for observing daylight time (e.g.,
                 *   Arizona is part of the US Mountain Time zone, but has
                 *   a separate zoneinfo entry because it doesn't observe
                 *   daylight savings time, which everyone else in MT
                 *   does).
                 */

                /* create a table of the zones at each offset value */
                local uz = new LookupTable();
                tlst.forEach(function(x) {
                    local key = x[2];
                    local v = uz[key];
                    if (v == nil)
                        uz[key] = v = new Vector();
                    v.append(x[1]);
                });

                /* check if we have zones at more than one offset value */
                local offsets = uz.keysToList();
                if (offsets.length() > 1)
                {
                    /* 
                     *   we do - write the abbreviation and a list of the
                     *   offsets it's tied to, with the zones at each
                     *   offset 
                     */
                    rpt.multiOffset.append(
                        '<<abbr>> offsets\n'
                        + offsets.mapAll(
                            {o: '  ' + unparseTime(o) + ' ' + uz[o].mapAll(
                                {z: z.name}).join(' ') + '\n'}).join()
                        + '\n');
                }

                /* check each offset value to see if it has multiple zones */
                uz.forEachAssoc(function(key, val)
                {
                    /* do we have more than one distinct zone at the offset? */
                    if (val.length() > 1
                        && val.indexWhich({x: x != val[1]}) != nil)
                    {
                        /* yes - report it */
                        rpt.zonesAtOffset.append(
                            '<<abbr>> zones <<unparseTime(key)>> '
                            + val.mapAll(
                                {z: z.name + (z.hasAbbrTweak ? '*' : '')}
                                ).join(' ')
                            + '\n');
                    }
                });
            }

            /* 
             *   write the zone data: the abbreviation string (null
             *   terminated); the number of zone entries; and then the
             *   entries, each with its zone index, gmt offset (in
             *   milliseconds), and dst flag.
             */
            fp.packBytes('C/ax C/[[L l a]]', abbr,
                         tlst.mapAll(
                             {x: [x[1].zoneTabIndex, x[2]*1000, x[3]]}));
        }

        /* finalize the length prefix */
        setLengthPrefix(fixCtx);
    }

    /* table mapping zone abbreviations to zones */
    zoneAbbrTab = static new LookupTable()

    /*
     *   Abbreviation tweak list.  We need some way at run-time to decide
     *   which zoneinfo key a user means if they enter a time with only a
     *   zone abbreviation.  Some abbreviations have numerous zoneinfo keys
     *   for what's effectively the same time zone, usually because there
     *   are slight differences in the history of nearby cities or regions
     *   within the zone.  In these cases, we want to pick the zoneinfo key
     *   that's the most representative of the zone, since that's what most
     *   users would expect - e.g., for EST in the US, they'd expect
     *   America/New_York, not America/Kentucky/Louisville (not to cast
     *   aspersions on Louisville).  There's no such designation in the
     *   zoneinfo file (which is to some extent a matter of ideology, I
     *   think: the zoneinfo designers feel that the abbreviations should
     *   be deprecated, so a tweak like this to support them is somewhat
     *   counterproductive to that view).
     *   
     *   Running this program with the --report-ambig-abbrs option will
     *   generate a report showing which abbreviations are ambiguous in
     *   terms of having multiple zoneinfo keys that represent what most
     *   people would think of as a single zone - i.e., they're all in the
     *   same country or region and all at the same offset.  (It'll also
     *   show which abbreviations refer to actually distinct zones in
     *   different parts of the world, at different GMT offsets; we handle
     *   those cases a little differently, by trying to find a zone at
     *   run-time within the same country as the host system's local time
     *   zone, or at least within a reasonable GMT offset difference.)
     *   
     *   The zones listed here are the ones that we elect as primary for
     *   their regions.
     */
    tweakZones = [
        'Atlantic/Bermuda',      // AST-4 ADT-3 Atlantic Time
        'America/Anchorage',     // AKST-9 AKDT-8 Alaska Time
        'America/Cuiaba',        // AMT-4 AMST-3 Amazon Time
        'Asia/Aqtobe',           // AQTT+5 Aqtobe Time
        'America/Argentina/Buenos_Aires',  // ART-3 Argentina Time
        'Asia/Riyadh',           // AST+3 Arabia Standard Time
        'America/Sao_Paulo',     // BRT-3 BRST-2 Brasilia Time
        'Africa/Harare',         // CAT+2 Central Africa Time
        'America/Chicago',       // CST-6 CDT-5 US Central Time
        'Europe/Berlin',         // CET+1 CEST+2 Central Europe Time
        'Pacific/Guam',          // ChST+10 Chamorro Standard Time
        'America/Santiago',      // CLT-4 CLST-3 Chile Time
        'Asia/Shanghai',         // CST+8 China Standard Time
        'Australia/Adelaide',    // CST+9:30 CST+10:30 AU Central Time
        'Africa/Khartoum',       // EAT+3 East Africa Time
        'America/New_York',      // EST-5 EDT-4 US Eastern Time
        'Europe/Bucharest',      // EET+2 EEST+3 Eastern Europe Time
        'Australia/Sydney',      // EST+10 EST+11 AU Eastern Time
        'Europe/Minsk',          // FET+3
        'Europe/London',         // GMT+0
        'Asia/Dubai',            // GST+4 Gulf Standard Time
        'Asia/Bangkok',          // ICT+7 Indochina Time
        'Asia/Kolkata',          // IST+5:30 India Standard Time
        'Asia/Seoul',            // KST+9 Korea Standard Time
        'America/Denver',        // MST-7 MDT-6 US Mountain Time
        'Pacific/Majuro',        // MHT+12 Marshall Islands Time
        'Asia/Kuala_Lumpur',     // MYT+8 Malaysia Time
        'Asia/Novosibirsk',      // NOVT+7 Novosibirsk Time
        'Pacific/Auckland',      // NZST+12 NZDT+13 New Zealand Time
        'America/Los_Angeles',   // PST-8 PST-7 US Pacific Time
        'Africa/Johannesburg',   // SAST+2 South Africa Standard Time
        'Pacific/Pago_Pago',     // SST-11 Somoa Standard Time
        'Asia/Tashkent',         // UZT+5 Uzbekistan Time
        'Africa/Windhoek',       // WAT+1 WAST+2 West Africa Time
        'Atlantic/Canary',       // WET+0 WEST+1 Western Europe Time
        'Asia/Jakarta',          // WIT+7 Eastern Indonesia Time
        'Australia/Perth'        // WST+8 AU Western Time
    ]

    /* current file and line number for error reporting */
    linenum = 1
    fname = ''

    /* report an error */
    errReport(msg)
    {
        "<<fname>>(<<linenum>>): <<msg>>\n";
    }

    /* flush a zone list */
    flushZone(zone)
    {
        /* skip empty zones */
        if (zone == nil)
            return nil;

        /* 
         *   the last entry in the history list (and only the last entry)
         *   should have an empty "until" field 
         */
        if (zone.hist.indexWhich({z: z.until == nil}) != zone.hist.length())
        {
            errReport('warning: zone <<zone.name>> has an extra or
                missing UNTIL field (the last must be empty, the
                others must be populated)');
        }

        /* add it to the list and zone table */
        zoneList.append(zone);
        zoneTab[zone.name] = zone;

        /* we're done with the zone, so return nil to clear the variable */
        return nil;
    }

    /* add a rule */
    addRule(args)
    {
        /* the first argument is the rule name, the rest are rule elements */
        local name = args[1];
        local item = new RuleItem(args.sublist(2)...);
        
        /* 
         *   look for an existing entry under this rule name; if there
         *   isn't one, create a new vector for its list of rule
         *   definitions
         */
        local rule = ruleTab[name];
        if (rule == nil)
            ruleTab[name] = rule = new RuleDef(name);

        /* add this rule entry to the list */
        rule.addItem(item);
    }

    /* 
     *   look up a rule by name; returns the RuleDef item for the rule
     *   name, or nil if no such rule exists 
     */
    getRule(name) { return ruleTab[name]; }

    /* day names */
    days = static ['Sun'->1, 'Mon'->2, 'Tue'->3, 'Wed'->4,
                   'Thu'->5, 'Fri'->6, 'Sat'->7]

    /* add a link */
    addLink(link)
    {
        linkList.append(link);
        linkTab[link[2]] = link[1];
    }

    /* resolve a link */
    resolveLink(name)
    {
        /* chase the link pointers */
        local l;
        for (l = linkTab[l] ; l != nil ; name = l, l = linkTab[l]) ;

        /* return the final result */
        return name;
    }

    /* add a leap second */
    addLeap(leap)
    {
        leapList.append(leap);
    }

    /* 
     *   List of Zone entries.  A Zone can have one or more timeline
     *   entries, so each zoneList entry is a list of timeline entries. 
     */
    zoneList = static new Vector(1024)

    /* lookup table of zone entries */
    zoneTab = static new LookupTable(256, 1024)

    /* 
     *   Table of Rule entries.  Each rule has a name that can be
     *   referenced from a Zone definition, so maintain a table of rules
     *   indexed by name.  Each rule can have one or more entries, so keep
     *   a list of entries for each rule name. 
     */
    ruleTab = static new LookupTable(512, 1024)

    /* list of Link entries, and table of FROM-TO mappings */
    linkList = static new Vector(1024)
    linkTab = static new LookupTable(512, 1024)

    /* list of Leap entries */
    leapList = static new Vector(64)

    /* dump the table (for debugging purposes) */
    dump()
    {
        "*** Zones ***\n";
        "Number of zones: <<zoneList.length()>>\n";
        local cnt = 0;
        zoneList.forEach({z: cnt += z.hist.length()});
        "Total zone entries: <<cnt>>\b";
        
        for (local z in zoneList)
            z.dump();

        "\b*** Rules ***\n";
        cnt = 0;
        ruleTab.forEach({r: cnt += r.length()});
        "Total rule entries: <<cnt>>\b";
        
        ruleTab.forEachAssoc(function(name, lst) {
            "<<name>>:\n";
            for (local rule in lst)
                "\t<<rule.dump()>>\n";
        });
    }
;

/* 
 *   parse an IN-ON-AT date for a given year 
 */
parseInOnAt(yy, in, on, at, errh)
{
    /* start the date string with the year and month */
    local dstr = '<<%04d yy>>-<<%02d in>>';
    
    /* add the day, checking for the various formats */
    local dd;
    if (rexMatch('last(Mon|Tue|Wed|Thu|Fri|Sat|Sun)', on) != nil)
    {
        /* 
         *   'lastSun' last <weekday> of the month.  Find the first day of
         *   the next month, then find the first occurrence of the given
         *   day before that date. 
         */
        local wday = tzData.days[rexGroup(1)[3]];
        dd = new Date(dstr).addInterval([0, 1, -1])
            .findWeekday(wday, -1).getDate()[3];
    }
    else if (rexMatch('(Mon|Tue|Wed|Thu|Fri|Sat|Sun)([<>])=(%d+)', on) != nil)
    {
        /* 
         *   'Sun>=8' - first <weekday> on or after <day> of the month;
         *   or on or before <day> if the <= form is used.
         */
        local wday = tzData.days[rexGroup(1)[3]];
        local which = (rexGroup(2)[3] == '<' ? -1 : 1);
        dd = toInteger(rexGroup(3)[3]);
        local d = new Date('<<dstr>>-<<%02d dd>>');
        d = d.findWeekday(wday, which);
        dd = d.getDate()[3];
    }
    else if (rexMatch('%d+', on) != nil)
    {
        /* '15' - simple numeric day of month format */
        dd = toInteger(on);
    }
    else
    {
        errh.errReport('''invalid ON format '<<on>>'''');
        return nil;
    }
    
    /* return the full date */
    return '<<dstr>>-<<%02d dd>> <<at>>';
}

/*
 *   Zone definition 
 */
ZoneDef: object
    construct(name)
    {
        self.name = name;
        self.hist = new Vector();
    }

    /*
     *   Write the zone information to the output file
     */
    writeZone(fp)
    {
        /* set my master zone table pointer */
        packBytesAt(fp, masterTablePointerOfs, 'L', fp.getPos());

        /* 
         *   Create a time type for times before my initial entry.  This is
         *   the legacy, pre-standardization entry that covers times before
         *   a time zone was actually established here; it's usually coded
         *   with a format of LMT (for Local Mean Time) and a GMT offset
         *   giving the local mean solar time at the primary historical
         *   population center of the location.
         *   
         *   The legacy time type is always at index 0, so we don't
         *   actually need to write the index anywhere in the file.  We
         *   ensure it's at index 0 by creating it first. 
         *   
         *   It's a good idea to fix up the legacy format string, since
         *   some entries in the database use '%s' sequences for these even
         *   though that makes no sense for the period before time zones
         *   were established.
         *   
         *   Legacy entries are never daylight time, since there wasn't
         *   such a thing as systematic daylight time before the
         *   establishment of time zones.  So hard-code a zero daylight
         *   offset for these.  (The zoneinfo sources seem to agree; they
         *   never use a time offset in this field for the first entry.
         *   They sometimes use a Rule reference, but as far as I can tell
         *   only for special compatibility entries for old Unix timezone
         *   names, not for actual location-based timezones.)
         */
        hist[1].format = fixPercentS(hist[1].format, nil, hist[1]);
        timeTypeIndex(hist[1].gmtofs, '0', hist[1].format);

        /* assign time types to our transitions */
        for (local t in transList)
            t.typeIndex = timeTypeIndex(t.gmtofs, t.save, t.format);

        /* write a byte length prefix for the zone data */
        local lpctx = prepLengthPrefix(fp, 'S');
        
        /* 
         *   write the zone table header:
         *   
         *.    #transitions : uint16
         *.    #time types : uint16
         *.    #from now ons : uint8
         *.    bytes of abbreviations : uint8
         */
        fp.packBytes('SSCC', transList.length(), typeVec.length(),
                     fromNowOn.length(), abbrIdx);

        /* write the zone transition list */
        for (local t in transList)
            t.writeTrans(fp);

        /* sort the from-now-on list by month */
        fromNowOn.sort(SortAsc, { a, b: a.rule.in - b.rule.in });

        /* 
         *   Make sure that each from-now-on rule is in a separate month;
         *   we won't necessarily sort them properly if this isn't the
         *   case, since we sort by month.  In practice it appears that all
         *   of the ongoing rules are simple twice-a-year DST on/off
         *   transitions, one in the fall and one in the spring, so our
         *   simple month sorting should always work.  This test is to help
         *   catch any exceptions that are added in the future.  It would
         *   be tricky, maybe impossible, to do finer-grained sorting than
         *   by month - expressions like "Sun >= 1" and "Sat >= 3" are not
         *   comparable because the results vary by year.  So if such a
         *   case comes up, we'll have the change the setup so that
         *   consumers of our binary know they have to sort the rules at
         *   run time on a year-by-year basis.  For now it appears safe to
         *   assume that we can sort the rules statically at compile time.
         */
        for (local i in 1..fromNowOn.length() - 1)
        {
            if (fromNowOn[i].in == fromNowOn[i+1].in)
                fromNowOn.errReport('from-Now-On rules can\'t be sorted
                    in date order, because there are two or more rules
                    that apply in the same month');
        }

        /* 
         *   Write the from-now-on list.  The rules take effect after the
         *   last enumerated transition, so the baseline GMT offset is the
         *   standard time offset from our last transition. 
         */
        if (fromNowOn.length() != 0)
        {
            local lastOfs = transList[transList.length()-1].gmtofs;
            for (local t in fromNowOn)
                t.rule.writeFromNowOn(fp, abbrIndex(t.format), lastOfs);
        }

        /* write the type list */
        for (local t in typeVec)
            t.writeTimeType(fp);

        /* write the abbreviation table */
        for (local abbr in abbrVec)
            fp.packBytes('a*x', abbr);

        /* write my descriptive information, if present */
        if (country != nil)
        {
            /* write the country, coordinates, and description */
            fp.packBytes('a2 a16 C/ax', country, coords, desc);
        }
        else
        {
            /* no descriptive information - just write a null byte */
            fp.packBytes('x');
        }

        /* finalize the length prefix */
        setLengthPrefix(lpctx);
    }

    /* add a zone history list entry */
    addHist(h)
    {
        /* parse the history item */
        if (rexMatch('<space>*'
                     + '(<^space>+)<space>+'
                     + '(<^space>+)<space>+'
                     + '("[^\"]*"|<^space|">+)'
                     + '<space>*(.*?)$', h) == nil)
        {
            tzData.errReport('invalid ZoneDef for <<name>>: <<h>>');
            return;
        }

        /* 
         *   pull out the parsed arguments, use them to create a zone
         *   history item, and add the item to our history list
         */
        hist.append(new ZoneHistoryItem(
            self, List.generate({i: rexGroup(i)[3]}, 4)));
    }

    /* 
     *   figure the abbreviations ([standard, daylight]) for the last zone
     *   history entry 
     */
    getAbbrs()
    {
        /* 
         *   work through our history list, starting with the most recent,
         *   until we get the abbreviations 
         */
        local std = nil, dst = nil;
        for (local i in hist.length()..1 step -1 ; std == nil || dst == nil ; )
        {
            local abbrs = hist[i].getAbbrs();
            if (abbrs != nil)
            {
                if (std == nil && abbrs[1] != nil)
                    std = abbrs[1];
                if (dst == nil && abbrs[2] != nil)
                    dst = abbrs[2];
            }
        }

        /* return what we found */
        return [std, dst];
    }

    /* look up or assign a time type index */
    timeTypeIndex(gmtofs, save, format)
    {
        /* use 0 for the various ways of saying '0' for SAVE */
        if (save == nil || save == '' || save == '-' || parseTime(save) == 0)
            save = '0';

        /* convert the GMTOFS and SAVE values to seconds */
        gmtofs = parseTime(gmtofs);
        save = parseTime(save);

        /* search for a matching table entry */
        local idx = typeVec.indexWhich({x: x.match(gmtofs, save, format)});

        /* if we didn't find it, add it */
        if (idx == nil)
        {
            typeVec.append(new TimeType(
                gmtofs, save, format, abbrIndex(format)));
            idx = typeVec.length();
        }

        /* return the index */
        return idx - 1;
    }
    typeVec = perInstance(new Vector())

    /* look up or assign an abbreviation index */
    abbrIndex(abbr)
    {
        local idx = abbrTab[abbr];
        if (idx == nil)
        {
            abbrTab[abbr] = idx = abbrIdx;
            abbrVec.append(abbr);
            abbrIdx += abbr.length() + 1;
        }
        return idx;
    }

    /* table of abbreviation strings for this zone */
    abbrTab = perInstance(new LookupTable())
    abbrVec = perInstance(new Vector())
    abbrIdx = 0

    /*
     *   My rank in the zone.tab file.  The zone.tab list is ordered by
     *   country, then geography, then by population.  For regions where
     *   there are several cities that share the same modern time zone,
     *   that ordering provides a way of breaking ties when we're trying to
     *   map a common time zone abbreviation ("CST") to a zoneinfo key.  As
     *   a default, use a very high number; this will make any zone that
     *   doesn't appear in zone.tab sort after all the zones that do.
     */
    zoneTabRank = 1000000

    /* windowsZones.xml zone name */
    winZoneName = nil

    /* 
     *   My index in the zone table - this is assigned when we write the
     *   zone list. 
     */
    zoneTabIndex = nil

    /* now plus five years */
    nowPlus5 = static new Date().addInterval([5])

    /* 
     *   Resolve the items in the history list - this applies the rule
     *   references to calculate the GMT version of the UNTIL time for each
     *   history item, and populates our transition list with the UTC time
     *   of each change to daylight time, UTC offset, time zone
     *   abbreviation, etc throughout the history of the time zone and up
     *   through the last year for which any rules are defined.
     */
    resolve()
    {
        /* resolve the history items to create our transitions list */
        for (local prv = nil, local h in hist ; ; prv = h)
            h.resolve(prv);

        /*
         *   Try to fix any %s parameters remaining in our transition list.
         *   These are sometimes left in the list due to (a) zone history
         *   items that occur between rule changes, and (b) history items
         *   that use %s without any rule association.  (b) seems like a
         *   bug to me, and it's not clear from the zic spec if there's an
         *   official way of handling these, but such entries do exist so
         *   we have to deal with them somehow.
         */
        for (local t in transList)
            t.format = fixPercentS(t.format, t, t.hist);

        /* 
         *   Now optimize the list.  Scan from the end for transitions
         *   caused by open-ended rules.  Remove these transitions and note
         *   the rules in a separate list of rules applicable into the
         *   future, which we'll call the "from now on" list.
         *   
         *   Transitions covered by open-ended rules can be inferred at
         *   run-time, so they don't have to be listed individually.  Okay,
         *   okay, technically, *all* transitions can be inferred at
         *   run-time, since what we're doing right now is inferring them
         *   at a sort of run-time.  But the older transitions that occur
         *   due to rules that eventually change, or due to time zone
         *   redefinitions, take a lot of work to resolve.  So we feel that
         *   it's worth essentially caching all of that resolution work
         *   we're doing now.  And we could in principle apply this
         *   reasoning across the board and resolve transitions for the
         *   whole scope of time that we want to cover, and leave the whole
         *   thing as a table search exercise at run-time.  That's more or
         *   less what zic does, although v2 of its output format (tzif)
         *   does at least make a half-hearted attempt at including the
         *   ongoing rules.  The problem with enumerating all transitions
         *   into the future is that we can't predict what the required
         *   scope will be, since users could want dates far into the
         *   future; we just can't build a big enough table to be sure
         *   we've covered every possible request.  This means that the
         *   transition list will simply never be sufficient on its own; we
         *   have to supplement it with a formula once we reach the point
         *   where the only rules remaining will be applied into the
         *   indefinite future.  So given that we need to do that anyway,
         *   we can compress the transition list somewhat by finding the
         *   point in the list where the formula that we have to work out
         *   anyway will produce the remaining list entries.  We can then
         *   elide those list entries.
         *   
         *   What's more, we have to go through this exercise even if we
         *   didn't use it to compress the table, because a second output
         *   of this scan is that precise list of open-ended rules.  We can
         *   identify those rules by looking for transition items that we
         *   can remove; a removable transition item is one that's caused
         *   by an open-ended rule that's only followed by other
         *   transitions caused by open-ended rules.  The set of open-ended
         *   rules that caused these transitions comprises our "from now
         *   on" list.
         *   
         *   As a run-time speed optimization, though, keep resolved
         *   transitions up through 5 years of the current date.  We can
         *   probably assume that current (at run-time) and nearby dates
         *   will be by far the most frequently used in practice, so it
         *   seems worth trading some memory for fast translations of those
         *   dates between UTC and local.  The 5-year deadline is
         *   arbitrary; it's meant to take into account the longevity of
         *   the generated database file (probably a couple of years is
         *   sufficient, as TADS tends to get updated on the order of a
         *   couple of times a year, and the official source database is
         *   updated a few times a year) and the locus of dates used in
         *   programs (mostly these will be dates in the present and recent
         *   past, but some programs might use near future dates as well).
         *   
         *   One further special constraint: keep at least one fixed
         *   transition in the list for each open-ended rule.  This
         *   simplifies things for the run-time system when it's trying to
         *   work out a future date, because it guarantees that the period
         *   just before a rule's trigger date is defined by the prior
         *   open-ended rule (in month order, treating the list as
         *   circular).  This is guaranteed because we know that every
         *   after the last transition is controlled by a recurrence of a
         *   open-ended rule.  It's important to know the controlling rule
         *   for the prior period, because rules are often stated in terms
         *   of wall-clock time, which is defined by the prior period's
         *   rules.  This makes working backwards from a given point in
         *   time almost impossible in general, but it becomes easy once
         *   we're unambiguously in that period of cycling through the
         *   recurring rules ad infinitum.
         *   
         *   Start by finding the last transition NOT caused by an
         *   open-ended item.
         */
        local lastFixed = transList.lastIndexWhich(
            {t: t.rule == nil || t.rule.to != 'max'});

        /* 
         *   if there are more items after the last fixed item, we have
         *   open-ended rules - make a list of them 
         */
        if (lastFixed != nil && lastFixed < transList.length())
        {
            /* everything after lastFixed is an open-ended rule */
            local firstOpen = lastFixed + 1;

            /* make a list of the unique open-ended rules */
            for (local i in firstOpen..transList.length())
            {
                /* get this item */
                local t = transList[i];

                /* check to see if it's already in the from-now-on list */
                local isRepeat = fromNowOn.indexWhich(
                    {r: r.rule == t.rule}) != nil;

                /* 
                 *   If it's new to the from-now-on list, add it.
                 *   Otherwise, if it's beyond the optimization cut-off, we
                 *   can remove it from the transition list. 
                 */
                if (!isRepeat)
                {
                    /* it's new to the from-now-on list - add it */
                    fromNowOn.append(t);
                }
                else if (t.date > nowPlus5)
                {
                    /* 
                     *   It's a repeat, and it's after our optimization
                     *   window ends, so we can remove it.  What's more, we
                     *   can remove everything that follows: since we know
                     *   we're in the recurring rule section, and since all
                     *   rules fire annually, the first recurrence of a
                     *   rule means that we've fired every other rule since
                     *   the previous occurrence of this rule.  What we
                     *   have left is just repeats of that cycle, so we can
                     *   drop this item and everything that follows.
                     */
                    transList.removeRange(i, -1);
                    break;
                }
            }
        }

        /*
         *   One final optimization.  We can now remove any redundant
         *   transitions - that is, transitions that leave everything (gmt
         *   offset, dst offset, and abbreviation) unchanged.  These
         *   transitions are sometimes created by a zone definition change
         *   that doesn't coincide with a DST transition, and we needed
         *   them in the list up until the pass above so that we wouldn't
         *   think that an open-ended rule applied indefinitely if it
         *   actually stopped applying due to a zone definition change.
         *   Now that we've figured out which rules are truly open-ended
         *   with respect to the zone history, we no longer need those
         *   mileposts, so we can save a little space by removing them.
         */
        for (local i in transList.length()..2 step -1)
        {
            /* if this item is redundant with the previous item, remove it */
            local tcur = transList[i], tprv = transList[i-1];
            if (tcur.gmtofs == tprv.gmtofs
                && tcur.save == tprv.save
                && tcur.format == tprv.format)
                transList.removeElementAt(i);
        }

        /*
         *   Add the current/ongoing abbreviation(s) for the zone to the
         *   master abbreviation mapping table.  If we have from-now-on
         *   rules, add all of the abbreviations from those rules, since
         *   the zone will indefinitely use all of those abbreviations in
         *   alternation.  If the zone doesn't have any ongoing rules, the
         *   last transition will be in effect indefinitely, so just add
         *   that one abbreviation.
         */
        if (fromNowOn.length() != 0)
        {
            /* we have ongoing rules - make list of their abbreviations */
            local abbrs = new Vector();
            for (local t in fromNowOn)
            {
                /* set up the entry */
                local e = [t.format, t.gmtofsSecs, t.isDST ? 'D' : 'S'];

                /* 
                 *   check to see if this entry is already in the list
                 *   under a different offset or D/S status - if so, we'll
                 *   need to flag it as having multiple meanings by adding
                 *   a 'B' entry
                 */
                if (abbrs.indexWhich({x: x[1] == t.format && x != e}) != nil)
                {
                    /* 
                     *   It has multiple meanings, so we need a 'B' entry
                     *   if we don't already have one.  Group it with the
                     *   lowest UTC offset, so that it stays together with
                     *   my other entries when sorted.
                     */
                    if (abbrs.indexWhich(
                        {x: x[1] == t.format && x[3] == 'B'}) == nil)
                    {
                        local o = fromNowOn.minVal({t: t.gmtofsSecs});
                        abbrs.append([t.format, o, 'B']);
                    }
                }

                /* 
                 *   Add it to the list.  Note that we keep all ongoing
                 *   entries, even if they have redundant abbreviations, so
                 *   that we can find them in explicit Abbr+Offset+Type
                 *   searches.  Those searches are useful for decoding
                 *   TZ-style specs: e.g., "EST10EST" refers to Australian
                 *   Eastern Time, which uses EST as both standard and
                 *   daylight time, but has distinct clock settings for the
                 *   two periods.  For matching "TZ=EST10EST", we need the
                 *   separate D and S entries so that we can match their
                 *   individual offsets; but for matching "1:00 pm EST" in
                 *   a parsed date, we need to know that EST is ambiguous,
                 *   so we also need the separate 'B' entry.
                 */
                abbrs.append(e);
            }

            /* add the abbreviations we came up with */
            for (local a in abbrs)
                tzData.addZoneAbbr(self, a[1], a[2], a[3]);
        }
        else if (transList.length() != 0)
        {
            /* no from-now-on rules, so just add the final abbreviation */
            local t = transList[transList.length()];
            tzData.addZoneAbbr(self, t.format, t.gmtofsSecs,
                               t.isDST ? 'D' : 'S');
        }
    }

    /*
     *   Fix an orphaned '%s' in a format abbreviation (e.g., 'P%sT' for
     *   Pacific Standard/Daylight Time).  Zone history items use '%s' as a
     *   substitution parameter when a Rule is associated with the history
     *   item; the %s is replaced at each transition with the LETTER/S
     *   field of the matching rule.  However, some zone history items that
     *   aren't associated with any rules also use %s - this seems like a
     *   bug in the zoneinfo data to me, since the only documented source
     *   of %s substitutions is the LETTER/S field from a rule, but that's
     *   what's in the data so we have to deal.  Empirically, it seems to
     *   work to find the next transition to standard time in the resolved
     *   transition list for the zone, and use the same format that's in
     *   effect after the transition.  So that's what this routine does:
     *   given a format string, if the format string contains '%s', we'll
     *   search for a standard time transition after the given starting
     *   point and return its format.
     */
    fixPercentS(format, trans, histItem)
    {
        /* if there's no format or no %s, return the format unchanged */
        if (format == nil || format.find('%s') == nil)
            return format;

        /* if there's a starting point, start the search there */
        local tlst = transList;
        local tidx = (trans != nil ? tlst.indexOf(trans) + 1 : 1);

        /* get a pattern to match to select a winner */
        local pat = new RexPattern(
            '^<<format.findReplace('%s', '<alpha>*')>>$');

        /* look ahead in the list for a matching standard-time item */
        for (local i in tidx+1 .. tlst.length())
        {
            /* 
             *   if this is a standard time transition, and it has a format
             *   string, use its format string 
             */
            if (parseTime(tlst[i].save) == 0 
                && tlst[i].format != nil
                && tlst[i].format.find('%s') == nil
                && rexMatch(pat, tlst[i].format) != nil)
                return tlst[i].format;
        }

        /* didn't find it, so try looking backwards in the list */
        for (local i in tidx-1 .. 1 step -1)
        {
            if (parseTime(tlst[i].save) == 0 
                && tlst[i].format != nil
                && tlst[i].format.find('%s') == nil
                && rexMatch(pat, tlst[i].format) != nil)
                return tlst[i].format;
        }

        /* no luck; report an error and stick with the %s string */
        histItem.errReport('unresolved %s in Zone history item');
        return format;
    }

    /* dump for debugging */
    dump()
    {
        "<<name>>:\n";
        for (local z in hist)
            "\t<<z.dump()>>\n";
        "\b";
    }

    /* zone name */
    name = nil

    /* history list */
    hist = nil

    /* zone.tab data - country code, coordinates, description/comment */
    country = nil
    coords = nil
    desc = nil

    /* 
     *   List of "from now on" rules.  These are the rules that are in
     *   effect in the time zone indefinitely after the last resolved
     *   transition in the transition list.  These are stored as
     *   ZoneTransition items, so that we can capture both the rule and the
     *   associated format string.
     */
    fromNowOn = perInstance(new Vector())

    /* add a transition item */
    addTrans(t)
    {
        /* add the new transition to our list */
        transList.append(t);
    }

    /* list of resolved historical transition items (ZoneTransitions) */
    transList = perInstance(new Vector())

    /* file seek offset of the master zone table pointer to my record */
    masterTablePointerOfs = nil

    /* 
     *   Abbreviation sorting tweak order - see tzData.tweakZones.  If the
     *   zone is in the tweak list, this is set to a low integer so that
     *   this zone will sort ahead of non-tweaked zones. 
     */
    abbrTweakOrder = 10000
    hasAbbrTweak = (abbrTweakOrder != 10000)
;

/*
 *   Time Type entry.  When we write a zone transition list, we compress
 *   the transition information by storing each transition's GMT offset,
 *   daylight offset, and format string as an index in a separate table.
 *   These objects represent entries in that separate table.  This approach
 *   compresses the type entries considerably because the transition list
 *   tends to use a few types repeatedly - in particular, it tends to
 *   involve switches back and forth between standard and daylight times,
 *   with each standard time being the same as all the other standard times
 *   in the zone, and likewise for daylight.  So a list of 50 transitions
 *   often only has two distinct type entries.
 */
class TimeType: object
    construct(gmtofs, save, format, formatIdx)
    {
        /* save the parameters */
        self.gmtofs = gmtofs;
        self.save = save;
        self.format = format;
        self.formatIdx = formatIdx;
    }

    /* match to another type object */
    match(gmtofs, save, format)
    {
        return self.gmtofs == gmtofs
            && self.save == save
            && self.format == format;
    }

    /* write to the output file */
    writeTimeType(fp)
    {
        /*
         *   Write as:
         *   
         *.   standard time offset (milliseconds) : int32
         *.   daylight offset from standard time (milliseconds) : int32
         *.   format index : int8
         */
        fp.packBytes('llC', gmtofs*1000, save*1000, formatIdx);
    }

    gmtofs = nil
    save = nil
    format = nil
    formatIdx = nil
;

/*
 *   Zone history item 
 */
class ZoneHistoryItem: object
    construct(zone, args)
    {
        /* remember the zone */
        self.zone = zone;

        /* remember the arguments from the source file */
        gmtofs = args[1];
        rule = args[2];
        format = args[3];
        until = args[4];

        /* set blank UNTIL entries to nil */
        if (until is in ('', '-'))
            until = nil;

        /* if the standard time offset doesn't start with + or -, add + */
        if (rexMatch('[-+]', gmtofs) == nil)
            gmtofs = '+<<gmtofs>>';

        /* assume not DST offset (we'll fill this in at resolve time) */
        dstofs = '0';

        /* remember my source file location */
        linenum = tzData.linenum;
        fname = tzData.fname;
        
        /* the RULE can be a simple fixed DST offset; if so, note it */
        if (rexMatch('%d+(:%d+)', rule) != nil)
            tzData.noteDSTOffset(rule);

        /* note the UNTIL in the global statistics */
        if (until != nil)
            tzData.noteYear(until);
    }

    /* figure the abbreviation for the zone in daylight/standard time */
    getAbbrs()
    {
        /* if we have a '/' for our format, it's STD/DST */
        local idx;
        if ((idx = format.find('/')) != nil)
            return [format.substr(1, idx-1), format.substr(idx+1)];

        /* 
         *   if we have a fixed offset as our rule, our format simply
         *   applies to the current time mode
         */
        if (rule is in (nil, '', '-') || rexMatch('%d+', rule) != nil)
        {
            /* 
             *   this is a fixed offset rule; if the offset is non-zero,
             *   it's a daylight saving rule, otherwise it's standard time 
             */
            return (parseTime(rule) is in (0, nil)
                    ? [format, nil] : [nil, format]);
        }

        /* look up our rule */
        local rr = tzData.getRule(rule);
        if (rr != nil)
        {
            /* 
             *   find the last daylight and last standard time entry in the
             *   rule 
             */
            local std = nil, dst = nil;
            for (local i in rr.items.length()..1 step -1 ;
                 std == nil || dst == nil ; )
            {
                local item = rr.items[i];
                if (parseTime(item.save) == 0)
                    std = item.letter;
                else
                    dst = item.letter;
            }

            return [std == nil ? nil : format.findReplace('%s', std),
                    dst == nil ? nil : format.findReplace('%s', dst)];
        }

        /* no luck */
        return nil;
    }

    /* add a ZoneTransition to our zone's resolved list */
    addTrans(t) { zone.addTrans(t); }

    /* the zone we're part of (ZoneDef) */
    zone = nil

    /* arguments from the source file */
    gmtofs = nil
    rule = nil
    format = nil
    until = nil

    /* the DST offset at our UNTIL date */
    dstofs = nil

    /* parsed Date version of our UNTIL value (figured at resolve() time) */
    untilDate = nil

    /* source file location */
    linenum = nil
    fname = nil

    /* show an error relating to this item */
    errReport(msg)
    {
        "<<fname>>(<<linenum>>): <<msg>>\n";
    }

    /* dump for debugging */
    dump()
    {
        "<<gmtofs>>, <<rule>>, <<format>>, <<untilDate>>";
    }

    /* 
     *   Resolve the history item.  This figures the GMT version of the
     *   UNTIL time for the item, based on its rule reference, and resolves
     *   transitions for rules through its UNTIL time.
     */
    resolve(prv)
    {
        /*
         *   If we're coming in from a previous history item, the start of
         *   the new history item counts as a transition.  Create an entry
         *   for it in our transition list.
         */
        if (prv != nil)
        {
            /* 
             *   the initial transition into the new rule is to standard
             *   time, unless the RULE is a daylight savings offset that
             *   covers the whole period
             */
            local save = '0', letter = nil;
            if (rexMatch('[-+]?%d+(:%d%d(:%d%d)?)?', rule) != nil)
            {
                /* the RULE is a fixed offset */
                save = rule;
            }
            else if (rule is in (nil, '', '-') || rexMatch('%s+$', rule))
            {
                /* the rule is empty */
                save = '0';
            }
            else if (format.find('%s') != nil)
            {
                /* get the rule reference */
                local rr = tzData.getRule(rule);
                if (rr != nil)
                {
                    /* 
                     *   look for a standard time letter, in case we have '%s'
                     *   in our format string
                     */
                    local letters = rr.items
                        .subset({x: parseTime(x.save) == 0})
                        .mapAll({x: x.letter}).getUnique();

                    if (letters.length() == 1)
                        letter = letters[1];
#if 0
                    // probably no need to report these, as we'll catch any
                    // remaining unresolved %s's when we write the data
                    else if (letters.length() == 0)
                        errReport('missing standard time letter
                            for \'%s\' in history entry');
                    else if (letters.length() > 1)
                        errReport('ambiguous standard time letter
                            for \'%s\' in history entry');
#endif
                }
            }

            /* 
             *   Create the transition.  The transition occurs at the
             *   resolved UNTIL date of the previous item, and switches to
             *   the new GMT offset and daylight savings time of 'self'.
             */
            addTrans(new ZoneTransition(
                prv.untilDate, gmtofs, save, format, letter, zone, nil, self));
        }

        /* get local scratchpad versions of our file arguments */
        local until = self.until;
        
        /*
         *   If the UNTIL is empty, it means that this history item is
         *   open-ended: it runs until the end of time, or at least until
         *   the database is updated for the next legislative act that
         *   changes the timezone's DST rules again.
         */
        if (until is in (nil, '', '-'))
        {
            /* 
             *   If there's no RULE, or the rule is a fixed DST offset,
             *   there are no more transitions: the zone is in its final
             *   state and will never change (at least as far as the
             *   current database knows, i.e., modulo future legislative
             *   acts).  In this case we're done.
             */
            if (rule == nil
                || rule is in ('', '-')
                || rexMatch('[-+]?%d+(:%d%d(:%d%d)?)?', rule) != nil)
            {
                /* we have a fixed DST offset, so there are no more transitions */
                return;
            }

            /*
             *   Okay, we have a rule, and this ZONE item instructs us to
             *   apply this rule ad infinitum.  Now, it's obviously not
             *   practical to actually generate transitions forever.  The
             *   official zoneinfo compiler (zic) deals with this by
             *   defining "the end of time" as 2037.  At one time that made
             *   a strange kind of sense, because of the infamous Unix Year
             *   2037 Bug.  But that's been largely fixed at this point (by
             *   changing time_t to 64 bits), which is certainly for the
             *   best, but it does mean there's no longer any practical
             *   justification for choosing an imminent "end of time"
             *   value.  The TADS Date type can in principle represent
             *   years through about +5.88 million, which is obviously out
             *   of the question in terms of generating transitions that
             *   far out.  The underlying Gregorian calendar will probably
             *   need work by Y3K or so (since its leap year system isn't
             *   quite precise enough - the seasons will eventually drift
             *   out of sync with the calendar, which is what led to the
             *   Gregorian replacing the Julian), but generating another
             *   1000 years of transitions for a few hundred time zones
             *   isn't really practical either.
             *   
             *   So our approach is as follows.  We'll generate transitions
             *   until we're left only with rules that are themselves
             *   open-ended - that is, with TO years of 'max' - or until
             *   there are no more rules in effect.  At that point, it will
             *   be possible to infer all future transitions from the
             *   remaining open-ended rules.
             *   
             *   Signal this by setting an UNTIL date of ''.
             */
            until = '';
        }
            
        /* 
         *   if the UNTIL date contains a lastDay or Day>=n field, convert
         *   that to a calendar date 
         */
        if (rexSearch('last(Sun|Mon|Tue|Wed|Thu|Fri|Sat)|'
                      + '(Sun|Mon|Tue|Wed|Thu|Fri|Sat)[<>]=%d+', until) != nil)
        {
            until = until.split(new RexPattern('<space>+'));
            until = parseInOnAt(
                until[1], until[2], until[3],
                until.length() >= 4 ? until[4] : nil,
                self);
        }
        
        /* 
         *   If the UNTIL value ends with a zone code, note it.  The zone
         *   code can be 'w' for "local wall clock time" (this is the
         *   default - it means the wall clock time in the local time zone
         *   in effect on this date at this time, which might be daylight
         *   time rather than standard time), 's' for "local standard time"
         *   (which might differ from the local wall clock time in that DST
         *   might be in effect on this date), or 'u' (synonyms: g, z) for
         *   "universal time" (i.e., UTC). 
         */
        local untilZone = 'w';
        if (rexMatch('(.*%d+)([wsugz])$', until))
        {
            until = rexGroup(1)[3];
            untilZone = rexGroup(2)[3];
        }

        /*
         *   Parse our RULE field.
         *   
         *   If our RULE field is a simple hours:minutes value (e.g.,
         *   '1:00'), it means that daylight savings time is always in
         *   effect for the duration of this history entry, so we simply
         *   add this delta to the standard time offset to get the wall
         *   clock offset.
         *   
         *   If the RULE is empty or '-', it means that standard time is
         *   always in effect, so the wall clock time offset is the same as
         *   the standard time offset.
         *   
         *   Otherwise, the RULE is the name of a rule set.  In this case,
         *   we have to parse the rule set entries to figure the transition
         *   history between the starting date for this zone history entry
         *   and the UNTIL date.
         */
        if (rule is in (nil, '', '-') || rexMatch('%s+$', rule) != nil)
        {
            /* no adjustment - wall clock time is standard time */
            dstofs = '0';
        }
        else if (rexMatch('[-+]?%d+(:%d%d(:%d%d)?)?', rule) != nil)
        {
            /* hour:minute adjustment - do the time arithmetic */
            dstofs = rule;
        }
        else
        {
            /* 
             *   Get the time zone offset based on the rule.  For the
             *   purposes of determining which rule history item to use,
             *   parse the date with the local standard time offset.  This
             *   might be off by an hour if DST is in effect, but (a) it
             *   shouldn't matter in practice, because the rules don't seem
             *   to cut over at the precise moments of standard
             *   time/daylight time switches (thank goodness), and (b) even
             *   if it did matter, there's not much we could do about it,
             *   since we have to start somewhere to look up the rule to
             *   determine whether we're in daylight or standard time.
             */
            local rr = tzData.getRule(rule);
            if (rr == nil)
            {
                errReport('''undefined rule '<<rule>>''');
                dstofs = '0';
            }
            else
            {
                /* get the DST adjustment from the rule */
                dstofs = rr.resolveTransitions(prv, self, until, untilZone);
            }
        }

        /* if there was no 'until' date, we're done */
        if (until == '')
            return;

        /* set the time zone in the string, according to the zone code */
        switch (untilZone)
        {
        case 'u':
        case 'g':
        case 'z':
            /* universal time - add GMT+00 as the zone */
            until += ' GMT+00';
            break;

        case 'w':
            /* 
             *   local wall clock time - this is the standard time offset
             *   plus the DST offset 
             */
            until += ' GMT<<timeAdjust(gmtofs, dstofs)>>';
            break;
            
        case 's':
            /* local standard time - add the local standard time offset */
            until += ' GMT<<gmtofs>>';
            break;
        }

        /* parse the result as a date */
        try
        {
            untilDate = new Date(until);
        }
        catch (Exception e)
        {
            errReport('invalid date field \'<<until>>\'');
        }
    }
;

/*
 *   Zone transition item.  This is a fully resolved historical transition,
 *   encapsulating the GMT time of the transition and the new state after
 *   the transition.  A transition can occur due to a Rule item or due to a
 *   switch (at an UNTIL date) from one zone history item to the next.
 */
class ZoneTransition: object
    construct(date, gmtofs, save, format, letter, zone, rule, hist)
    {
        /* save the date, GMT offset, and daylight savings offset */
        self.date = date;
        self.gmtofs = gmtofs;
        self.save = save;
        self.rule = rule;
        self.hist = hist;
        self.zone = zone;

        /* figure the computed time offset */
        local ps = parseTime(save);
        gmtofsSecs = parseTime(gmtofs) + ps;

        /* note if it's DST */
        isDST = (ps != 0);

        /* 
         *   Resolve the format string.  If it contains a '%s' sequence,
         *   substitute the daylight/standard letter string.  If it
         *   contains a slash '/', pick the left substring if we're on
         *   standard time (i.e., 'save' is zero), or the right substring
         *   if we're on daylight time ('save' is non-zero). 
         */
        if (format.find('%s') != nil && letter != nil)
            self.format = format.findReplace('%s', letter);
        else if (rexMatch('(.*)/(.*)$', format) != nil)
        {
            local g = List.generate({i: rexGroup(i)[3]}, 2);
            self.format = g[parseTime(save) == 0 ? 1 : 2];
        }
        else
            self.format = format;
    }

    /* write to the output file */
    writeTrans(fp)
    {
        /* figure our delta from the TADS Epoch */
        local dayno = -1, daytime = -1;
        if (date != nil)
        {
            local delta = date - epochDate;
            dayno = delta.getWhole();
            daytime = toInteger(delta.getFraction() * 24*60*60*1000);
        }

        /* 
         *   write our file data:
         *   
         *.    dayno (days since 1/1/0000 UTC) : int32
         *.    daytime (milliseconds after midnight) : int32
         *.    type index : uint8
         */
        fp.packBytes('l l C', dayno, daytime, typeIndex);
    }

    /* the Date class Epoch is 3/1/0000 at midnight UTC */
    epochDate = static new Date('0000-03-01 GMT+0')

    /* the date of the transition, as a Date object */
    date = nil

    /* 
     *   the GMT offset of local standard time after this transition, as a
     *   '+-hh[:mm[:ss]]' string 
     */
    gmtofs = nil

    /* 
     *   the daylight savings offset in effect after this transition, as an
     *   'hh[:mm[:ss]]' string; if standard time is in effect, this is
     *   simply '0' or '0:00' 
     */
    save = nil

    /* parsed GMT + daylight offset in seconds */
    gmtofsSecs = nil

    /* is this a DST zone?  true if 'save' parses to non-zero */
    isDST = nil

    /* the format name, with all variant portions fully resolved */
    format = nil

    /* 
     *   The rule that caused the transition.  This is nil if the
     *   transition is due to a change in the time zone history itself. 
     */
    rule = nil

    /* 
     *   The zone history item that caused the transition.  If this
     *   transition is due to a change in the zone history rather than a
     *   rule trigger, this is filled in with the history item. 
     */
    hist = nil

    /* our index into the zone's TimeType list */
    typeIndex = nil
;

/*
 *   Rule - this is a collection of Rule line items under the same name. 
 */
class RuleDef: object
    construct(name)
    {
        self.name = name;
    }

    addItem(item)
    {
        item.rule = self;
        items.append(item);
    }

    /*
     *   Build the historical transition list for the given range of dates
     *   in the given zone.  This generates transitions from the UNTIL date
     *   of the previous item to the UNTIL date of the current item.  For
     *   each transition, we create a ZoneTransition object and add it to
     *   the zone's list.
     *   
     *   Returns the final DST offset at the outgoing UNTIL point.  This
     *   allows the caller to resolve the UNTIL date to the correct
     *   universal time if it's stated in wall clock time.
     *   
     *   'prvHist' is the previous zone history item, and 'hist' is the
     *   current item.  We'll generate transitions between the two UNTIL
     *   dates.  'until' is the string version of hist's UNTIL date, in
     *   calendar date notation (don't pass relative notation such as
     *   'lastSun' - we assume those have already been resolved).
     *   'untilZone' is the zone code attached to the UNTIL - 'w' for wall
     *   clock time, 's' for local standard time, 'u', 'g', or 'z' for
     *   universal time (GMT).
     */
    resolveTransitions(prvHist, hist, until, untilZone)
    {
        /* 
         *   get the previous item's date, or use a date that's effectively
         *   negative infinity if there's no previous item 
         */
        local prvUntilDate = (prvHist != nil ? prvHist.untilDate :
                              new Date('0000-00-00'));

        /*
         *   If there's no ending date, we're processing the last item in a
         *   zone history, which is always open-ended - that is, it's in
         *   effect until the end of time, at least as far as this version
         *   of the database is concerned.  In this case we need to
         *   generate transitions only until the rules become fixed ad
         *   infinitum: that is, until we reach a date where the only rules
         *   in effect are themselves open-ended.  We know that there are
         *   no rules in effect after the maximum year we found in the
         *   file, so go until that year plus 1.  (Go into the next year
         *   after the last rule year to be sure we catch any transitions
         *   that happen late in the last year of a rule.  For example, if
         *   we have a rule with a TO date of 2020 and an In-On date of
         *   "Dec lastSun", we have to process until the very end of 2020
         *   to apply this rule.  The "jan-03" part is to add a little
         *   extra padding to make sure we're not tricked by a GMT offset
         *   that pushes Dec 31 local into Jan 1 UTC or vice versa.)
         */
        if (until == '')
            until = '<<tzData.maxYear+1>>-01-03';
        
        /* 
         *   Convert the UNTIL date to an actual date, if it's in a fixed
         *   time zone (i.e., standard time or universal time).  If it's in
         *   wall clock time, leave that for the loop, since we'll need to
         *   parse it each time in the then-current DST context.
         */
        local untilDate = nil;
        switch (untilZone)
        {
        case 'u':
        case 'g':
        case 'z':
            /* fixed in GMT */
            untilDate = new Date('<<until>> GMT+0');
            break;

        case 's':
            /* fixed in standard time for the zone */
            untilDate = new Date('<<until>> GMT<<hist.gmtofs>>');
            break;
        }

        /* start with the DST offset from the previous item */
        local dstofs = '0';

        /* run through the transition list looking for the UNTIL date */
        for (local t in transList, local started = nil)
        {
            /* get the item's date */
            local itemDate = t.date;

            /* get the transition date's zone code */
            local itemZone = 'w';
            if (rexMatch('(.*%d+)([wsugz])', itemDate) != nil)
            {
                itemDate = rexGroup(1)[3];
                itemZone = rexGroup(2)[3];
            }

            /* get the time zone for the zone code */
            switch (itemZone)
            {
            case 'w':
                itemZone = timeAdjust(hist.gmtofs, dstofs);
                break;

            case 's':
                itemZone = hist.gmtofs;
                break;

            case 'u':
            case 'g':
            case 'z':
                itemZone = '+0';
                break;
            }

            /* convert the transition date string to a local date/time */
            itemDate = new Date('<<itemDate>> GMT<<itemZone>>');

            /* 
             *   if we haven't reached the starting point yet, check to see
             *   if this item finally gets there 
             */
            if (!started)
            {
                /* 
                 *   if this is the first transition on or after the item's
                 *   start date (which is the previous item's UNTIL date), note
                 *   it 
                 */
                if (itemDate >= prvUntilDate)
                {
                    /* note that we've entered the valid range */
                    started = true;
                }
                else
                {
                    /* ignore items before the starting point */
                    continue;
                }
            }

            /*
             *   If the UNTIL date is a wall clock time, parse it anew in
             *   the latest DST time.
             */
            if (untilZone == 'w')
                untilDate = new Date('<<until>> GMT<<
                      timeAdjust(hist.gmtofs, dstofs)>>');
            
            /* 
             *   If the transition is on or after the UNTIL date, we're
             *   done - the new transition doesn't apply because the rule
             *   set stops applying before the transition can take effect.
             *   It appears (empirically) that a transition that happens
             *   simultaneously with the UNTIL date does not apply to the
             *   history item; the moment of the UNTIL date is actually the
             *   starting moment of the NEXT history item, so the current
             *   history item ends the instant before the UNTIL date.
             */
            if (itemDate >= untilDate)
                break;

            /* apply this transition: switch to its new DST offset */
            dstofs = t.item.save;

            /* add this transition to the zone's transition list */
            hist.addTrans(new ZoneTransition(
                itemDate, hist.gmtofs, t.item.save,
                hist.format, t.item.letter, hist.zone, t.item, nil));
        }

        /* return the final DST offset */
        return dstofs;
    }

    /* get the full transition list for this rule set */
    transList()
    {
        /* start with an empty vector */
        local lst = new Vector();

        /* expand each rule item */
        for (local i in items)
        {
            /* start at the item's FROM year, or the minimum UNTIL year */
            local from = i.from;
            if (from == 'min')
                from = tzData.minYear;

            /* 
             *   Go to the TO year, or the maximum UNTIL/TO year throughout
             *   the database, plus a year.  This will ensure that we take
             *   into account any transitions after the last zone
             *   definition change.
             */
            local to = i.to;
            if (to == 'max')
                to = tzData.maxYear + 1;

            /* run through the year range */
            for (local yy in from..to)
            {
                /* build the calendar date, resolving stuff like 'lastSun' */
                local date = parseInOnAt(yy, i.in, i.on, i.at, i);

                /* add the transition item */
                lst.append(new RuleTransition(date, i));
            }
        }

        /* 
         *   Sort the list by date.  The dates are stored as strings, not
         *   Date objects - but they're in yyyy-mm-dd hh:mm format, which
         *   happens to sort properly when compared in string collation
         *   order.  (That's not just dumb luck, of course; this format
         *   puts the date components in order from most significant to
         *   least significant, in a fixed byte-length format, so the two
         *   values can be compared byte-to-byte starting from the left.)
         */
        lst.sort(SortAsc, {a, b: a.date.compareTo(b.date)});
        
        /* 
         *   we don't need to expanded this again - replace this method
         *   with a cached copy of the rule list 
         */
        transList = lst;

        /* return the list */
        return lst;
    }

    /* name of the rule */
    name = nil

    /* list of RuleItem objects */
    items = perInstance(new Vector())
;

/* 
 *   rule transition - this represents a concrete transition from a Rule,
 *   in a given year and a given local time zone 
 */
class RuleTransition: object
    construct(date, item)
    {
        self.date = date;
        self.item = item;
    }

    /* the date of the transition, in yyyy-mm-dd hh:mm format */
    date = nil

    /* the RuleItem of the transition */
    item = nil
;

/*
 *   Rule Item - this is a single line item in a rule list
 */
class RuleItem: object
    construct(from, to, typ, in, on, at, save, letter)
    {
        /* remember my source location */
        linenum = tzData.linenum;
        fname = tzData.fname;

        /* note the year range in the global statistics */
        tzData.noteYear(from);
        tzData.noteYear(to);

        /* if the AT time uses 'hh' format, adjust to 'hh:mi' */
        if (rexMatch('%d+$', at) != nil)
            at += ':00';

        /* remember the parameters */
        self.from = from = yearArg(from);
        self.to = (to == 'only' ? from : yearArg(to));
        self.typ = (typ != '-' ? typ : nil);
        self.in = mon[in];
        self.on = on;
        self.at = at;
        self.save = save;
        self.letter = (letter != '-' ? letter : '');

        /* 
         *   Find and pull out the zone suffix from the At field, if any.
         *   This can be 'w' for local wall clock time (this is the default
         *   if there's no suffix), 'u' (or 'g' or 'z') for GMT, or 's' for
         *   local standard time.  "Local wall clock time" is the local
         *   time in effect up to the transition point, which could be
         *   either local standard time or local daylight time.
         */
        atZone = 'w';
        if (rexMatch('(.*)([wsugz])$', at) != nil)
        {
            self.at = rexGroup(1)[3];
            atZone = rexGroup(2)[3];
        }

        /* note the highest DST offset we've seen */
        if (save not in ('', '-'))
            tzData.noteDSTOffset(save);

        /* make sure the type is nil */
        if (self.typ != nil)
            errReport('non-empty TYPE field found (tz ignores this field)');

        /* make sure the LETTER field fits in four bytes */
        if (self.letter.length() > 4)
            errReport('LETTER field longer than 4 characters');
    }

    /*
     *   Write this rule to the output file as a From Now On entry in a
     *   zone record.  'stdofs' is the standard time GMT offset from the
     *   last enumerated transition in the zone.
     */
    writeFromNowOn(fp, abbrIdx, stdofs)
    {
        /* 
         *   figure our type:
         *   
         *.    0 -> day of the month
         *.    1 -> last <weekday> of the month, as in 'lastSun'
         *.    2 -> <weekday> after <dd>, as in 'Sun>=7'
         *.    3 -> <weekday> before <dd>, as in 'Sun<=28'
         */
        local onType = 0, onDD = 0, onWeekday = 0;
        if (rexMatch('%d+', on))
        {
            onType = 0;
            onDD = toInteger(on);
        }
        else if (rexMatch('last(Sun|Mon|Tue|Wed|Thu|Fri|Sat)', on))
        {
            onType = 1;
            onWeekday = tzData.days[rexGroup(1)[3]];
        }
        else if (rexMatch('(Sun|Mon|Tue|Wed|Thu|Fri|Sat)([<>])=(%d+)', on))
        {
            onType = rexGroup(2)[3] == '>' ? 2 : 3;
            onDD = toInteger(rexGroup(3)[3]);
            onWeekday = tzData.days[rexGroup(1)[3]];
        }
        else
        {
            errReport('invalid ON format in rule');
        }

        /*
         *   Figure the 'at' time.  This is the time of day in seconds, ORd
         *   with 0x80000000 if it's expressed in UTC time, or 0x40000000
         *   if it's in standard time.  The default (with no extra flag
         *   bits) is wall clock time.
         */
        local at = self.at, atType = 'w';
        if (rexMatch('.*[wsugz]$', at))
        {
            atType = at.substr(-1, 1);
            at = at.substr(1, -1);
        }

        /* parse the time to milliseconds after midnight */
        at = parseTime(at)*1000;

        /* apply the time type */
        if (atType is in ('u', 'g', 'z'))
        {
            errReport('Note - found a \'u\' AT type in a from-now-on rule');
            at |= 0x80000000;
        }
        else if (atType == 's')
        {
            errReport('Note - found a \'s\' AT type in a from-now-on rule');
            at |= 0x40000000;
        }

        /* 
         *   write the rule:
         *   
         *.    abbr : uint8 (abbreviation index)
         *.    in : uint8   (month number, 1=January)
         *.    type : uint8 (see above)
         *.    dd : uint8   (day of the month, 1-31)
         *.    ww : uint8   (weekday, 1-7 for Sunday to Saturday)
         *.    at : int32   (time of day in seconds | time type - see above)
         *.    gmtofs : int32  (standard time GMT offset, in milliseconds)
         *.    save : int32  (daylight time offset from standard time, ms)
         */
        fp.packBytes('CCCCCLlL', abbrIdx, in, onType, onDD, onWeekday,
                     at, (parseTime(stdofs) ?? 0)*1000,
                     (parseTime(save) ?? 0)*1000);
    }

    /*
     *   Resolve the date and time for this rule's transition in the given
     *   year in the given time zone.  'yy' is the year in which to resolve
     *   the date, 'gmtofs' is the GMT offset of the local standard time
     *   for the resolved date and time, and 'dstofs' is the DST offset
     *   from the previous transition.
     */
    resolveDate(yy, gmtofs, dstofs)
    {
        /* parse the In-On-At date */
        local d = parseInOnAt(yy, in, on, at, self);

        /* figure the time zone based on the w/s/u zone suffix */
        switch (atZone)
        {
        case 'w':
        default:
            /* wall clock time - local time plus DST offset */
            return new Date('<<d>> GMT<<timeAdjust(gmtofs, dstofs)>>');
            
        case 's':
            /* local standard time */
            return new Date('<<d>> GMT<<gmtofs>>');
            
        case 'u':
        case 'g':
        case 'z':
            /* universal time (UTC) */
            return new Date('<<d>> GMT+0');
        }
    }

    /* 
     *   Convert a FROM/TO year argument from the file to our internal
     *   storage format.  If it's a specific year value, we'll return the
     *   year as an integer; if it's 'min' or 'max', we'll leave it as a
     *   string with that exact text.
     */
    yearArg(arg)
    {
        arg = arg.toLower();
        return (arg is in ('min', 'max') ? arg : toInteger(arg));
    }

    mon = static ['Jan'->1, 'Feb'->2, 'Mar'->3, 'Apr'->4, 'May'->5,
                  'Jun'->6, 'Jul'->7, 'Aug'->8, 'Sep'->9, 'Oct'->10,
                  'Nov'->11, 'Dec'->12]

    errReport(msg)
    {
        "<<fname>>(<<linenum>>): <<msg>>\n";
    }

    dump()
    {
        "<<from>>-<<to>> <<in>> <<on>> <<at>> '<<save>>'";
    }

    /* the rule parameters from the file */
    from = nil
    to = nil
    typ = nil
    in = nil
    on = nil
    at = nil
    atZone = nil
    save = nil
    letter = nil

    /* the RuleDef I'm part of */
    rule = nil

    /* the source location where this rule was defined */
    linenum = nil
    fname = nil
;


trim(s)
{
    if (s == nil)
        return '';

    rexMatch('<space>*(.*?)<space>*$', s);
    return rexGroup(1)[3];
}

/*
 *   Adjust a time in hh:mi:ss format by adding another time in +-hh:mi:ss
 *   format, returning a combined hh:mi:ss value
 */
timeAdjust(t, delta)
{
    return unparseTime(parseTime(t) + parseTime(delta));
}

/* parse an hh:mi:ss value, returning the number of seconds it represents */
parseTime(t)
{
    /* parse the components */
    if (t != nil
        && rexMatch('([-+]?)(%d+)(?::([0-5]%d)(?::([0-5]%d))?)?$', t) != nil)
    {
        /* pull out the fields */
        local sg = ((rexGroup(1) ?? [nil,nil,''])[3] == '-' ? -1 : 1);
        local hh = toInteger(rexGroup(2)[3]);
        local mi = toInteger((rexGroup(3) ?? [nil,nil,''])[3]);
        local ss = toInteger((rexGroup(4) ?? [nil,nil,''])[3]);
        
        /* calculate the combined value in seconds */
        return sg*(hh*60*60 + mi*60 + ss);
    }
    else
    {
        /* not a time value */
        return nil;
    }
}

/* unparse a number of seconds into an hh:mi:ss representation */
unparseTime(secs)
{
    /* note the sign, and convert to positive for the arithmetic */
    local sg = '+';
    if (secs < 0)
        sg = '-', secs = -secs;

    /* convert to hours, minutes, and seconds */
    local hh = secs/(60*60);
    local mi = (secs/60) % 60;
    local ss = secs % 60;

    /* return an hh:mi:ss representation */
    mi = (mi != 0 || ss != 0 ? ':<<%02d mi>>' : '');
    ss = (ss != 0 ? ':<<%02d ss>>' : '');
    return '<<sg>><<hh>><<mi>><<ss>>';
}

showList(l)
{
    for (local i = 1, local ele in l ; ; ++i)
    {
        if (i > 1)
            ", ";
        
        if (dataType(ele) == TypeList
            || (dataType(ele) == TypeObject && ele.ofKind(Collection)))
            "[<<showList(ele)>>]";
        else
            "<<ele>>";
    }
}

/* ------------------------------------------------------------------------ */
/*
 *   Prepare a length prefix.  This writes a placeholder for a length
 *   prefix to the file, and returns a context that can be passed to
 *   setLengthPrefix() when done writing the intervening table.
 */
prepLengthPrefix(fp, fmt)
{
    /* remember the starting position */
    local startofs = fp.getPos();

    /* write a placeholder for the length prefix */
    local prefixlen = fp.packBytes(fmt, 0);

    /* 
     *   return a context list - this is passed to setLengthPrefix() to
     *   finish the job after the table has been finished 
     */
    return [startofs, prefixlen, fmt, fp];
}

/* 
 *   Set the final value for a length prefix.  This goes back and writes
 *   the actual length of the table started with prepLengthPrefix(),
 *   filling in the placeholder length we wrote with the actual number of
 *   bytes between the palceholder and the current file position.
 */
setLengthPrefix(ctx)
{
    /* decode the context */
    local startofs = ctx[1], prefixlen = ctx[2], fmt = ctx[3], fp = ctx[4];

    /* write the actual length at the prefix position */
    packBytesAt(fp, startofs, fmt, fp.getPos() - (startofs + prefixlen));
}

/*
 *   Pack bytes to a file at the given position, leaving the seek pointer
 *   where it started before this call. 
 */
packBytesAt(fp, ofs, fmt, [vals])
{
    /* remember the ending position of the table */
    local cur = fp.getPos();
    
    /* seek back to the length prefix position */
    fp.setPos(ofs);
    
    /* write the bytes */
    fp.packBytes(fmt, vals...);
    
    /* seek back to where we started */
    fp.setPos(cur);
}
