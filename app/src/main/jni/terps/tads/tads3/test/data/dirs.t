/*
 *   mkdir/rmdir test 
 */

#include <tads.h>
#include <file.h>
#include <date.h>

modify String
    trim()
    {
        rexMatch('%s*(.*?)%s*$', self);
        return rexGroup(1)[3];
    }
;

main()
{
    "Commands:\n
    &gt;mkdir|md|m [-r] &lt;dir&gt;\n
    &gt;rmdir|rd|r [-r] &lt;dir&gt;\n
    &gt;abs &lt;dir&gt; \  (show absolute path)\n
    &gt;ls [-r] [-b] &lt;dir&gt;\n
    &gt;mv &lt;oldname&gt; &lt;newname&gt;\n
    &gt;quit|q\n";
    local spaces = new RexPattern('%s+');
    for (;;)
    {
        "\b&gt;";
        local l = inputLine();
        if (l == nil)
            break;

        local toks = l.trim().split(spaces);
        if (toks.length() == 0)
            continue;

        local cmd = toks[1];
        local opts = [];
        local i;
        for (i in 2..toks.length() ; toks[i].startsWith('-') ; )
            opts += toks[i];
        local args = toks.sublist(i);
        
        try
        {
            if (cmd is in ('mkdir', 'md', 'm'))
            {
                local createParents = (opts.indexOf('-r') != nil);
                for (local arg in args)
                    new FileName(arg).createDirectory(createParents);
            }
            else if (cmd is in ('rmdir', 'rd', 'r'))
            {
                local removeContents = (opts.indexOf('-r') != nil);
                for (local arg in args)
                    new FileName(arg).removeDirectory(removeContents);
            }
            else if (cmd is in ('ls'))
            {
                local recurse = (opts.indexOf('-r') != nil);
                local bare = (opts.indexOf('-b') != nil);
                if (args.length() == 0)
                    showDirList(new FileName(), recurse, bare);
                else
                {
                    for (local arg in args)
                        showDirList(new FileName(arg), recurse, bare);
                }
            }
            else if (cmd is in ('abs'))
            {
                for (local arg in args)
                {
                    arg = new FileName(arg);
                    "<<if arg.isAbsolute()>>absolute<<else>>relative<<end>>;
                    <<arg.getAbsolutePath().htmlify()>>\n";
                }
            }
            else if (cmd is in ('mv'))
            {
                if (args.length() == 2)
                    new FileName(args[1]).renameFile(args[2]);
                else
                    "usage: mv &lt;oldname&gt; &lt;newname&gt;\n";
            }
            else if (cmd is in ('q', 'quit'))
            {
                "Bye!\n";
                break;
            }
            else
            {
                "Bad command\n";
            }
        }
        catch (Exception e)
        {
            "Error: <<e.displayException()>>\n";
        }
    }
}

showDirList(dir, recurse, bare, level = 0)
{
    if (bare)
    {
        dir.forEachFile({file: "<<file>>\n"}, recurse);
    }
    else
    {
        local sortf = {
            a, b: a.getBaseName().compareIgnoreCase(b.getBaseName()) };
        for (local file in dir.listDir().sort(SortAsc, sortf))
        {
            local info = file.getFileInfo();
            
            "<<makeString('\t', level)>>
            <<sprintf('%_\ ,12d', info.fileSize)>>
            \ \ <<formatAttrs(info.fileAttrs)>>
            \ \ <<formatTime(info.fileModifyTime)>>
            \ \ <<file.getBaseName().htmlify()>><<if info.isDir>>/<<end>>\n";
             
            if (recurse && info.isDir && !info.specialLink)
                showDirList(file, true, nil, level + 1);
        }
    }
}

formatAttrs(attrs)
{
    local map = [FileAttrHidden -> 'h',
                 FileAttrSystem -> 's',
                 FileAttrRead -> 'r',
                 FileAttrWrite -> 'w'];
    local attrBits = [FileAttrHidden,
                      FileAttrSystem,
                      FileAttrRead,
                      FileAttrWrite];
    for (local bit in attrBits)
        "<<(attrs & bit) != 0 ? map[bit] : '-'>>";
}

formatTime(t)
{
    return (t == nil ? nil : t.formatDate('%m-%d-%Y %H:%M'));
}

dumpRex()
{
    for (local i in 1..3)
    {
        "rexGroup(<<i>>) = <<if rexGroup(i) == nil>>nil<<else>><<
          rexGroup(i)[3]>><<end>>\n";
    }
}
