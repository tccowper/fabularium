#include <filename.h>

main()
{
    for (local r in FileName.getRootDirs())
        "<<r.getName().htmlify()>>\n";
}
