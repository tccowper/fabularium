#include <tads.h>

main(args)
{
    for (local i in 1..args.length())
        "args[<<i>>] = <<showstr(args[i])>>\n";
}

showstr(s)
{
    tadsSay(s);
    local pat = new RexPattern('[^\x20-\x7f]');
    if (rexSearch(pat, s))
    {
        s = s.split();
        for (local c in s)
        {
            if (rexMatch(pat, c))
                tadsSay(sprintf('\u%04x', c.toUnicode(1)));
            else
                tadsSay(c);
        }
    }
}

