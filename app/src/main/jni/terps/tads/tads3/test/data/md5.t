#include <tads.h>
#include <file.h>

main(args)
{
    if (args.length() < 2)
    {
        "usage: t3run md5 &lt;file&gt; ...\n";
        return;
    }

    for (local i in 2..args.length())
    {
        local fname = args[i];
        local fp = File.openRawFile(fname, FileAccessRead);
        "md5(<<fname>>) = <<fp.digestMD5().toLower()>>\n";
    }
}
