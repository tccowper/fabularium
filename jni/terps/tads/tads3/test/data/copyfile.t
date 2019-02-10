#include <tads.h>
#include <file.h>

main(args)
{
    if (args.length() != 3)
    {
        "usage: t3run copyfile &lt;src&gt; &lt;dst&gt;\n";
        return;
    }

    try
    {
        copyfile(args[2], args[3]);
        "File copied.\n";
    }
    catch (Exception exc)
    {
        "copy failed: <<exc.displayException()>>\n";
    }
}

copyfile(src, dst)
{
    local fsrc = File.openRawFile(src, FileAccessRead);
    local fdst = File.openRawFile(dst, FileAccessWrite);

    local b;
    while ((b = fsrc.unpackBytes('b1024*')[1]).length() != 0)
        fdst.packBytes('b*', b);

    fsrc.closeFile();
    fdst.closeFile();
}
