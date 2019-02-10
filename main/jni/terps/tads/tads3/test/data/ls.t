#include <tads.h>
#include <file.h>

main(args)
{
    if (args.length() < 2)
    {
        "usage: t3run ls &lt;dir&gt; ...\n";
        return;
    }
    
    for (local i in 2..args.length())
    {
        local d = args[i];
        try
        {
            local files = File.getFilesInDir(d);
            "<<d.htmlify()>>:\n";
            for (local f in files)
                "\t<<f.htmlify()>>\n";
        }
        catch (Exception e)
        {
            "<<d.htmlify()>>: <<e.displayException()>>\n";
        }
        "\b";
    }
}
