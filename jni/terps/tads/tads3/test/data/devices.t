/*
 *   Enumerate DOS devices 
 */

#include <tads.h>
#include <file.h>

main()
{
    for (local d in ['con', 'prn', 'aux', 'nul', 'com#', 'lpt#', 'devices.t'])
    {
        if (d.endsWith('#'))
        {
            for (local i in 1..9)
                testDev(d.findReplace('#', toString(i)));
        }
        else
            testDev(d);
    }
}

testDev(d)
{
    "<<d>> -> <<File.getFileType(d)>>\n";
}
