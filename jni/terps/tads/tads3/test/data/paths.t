#include <tads.h>
#include <file.h>

main()
{
    local paths = ['c:\\mydir', '\\\\server\\root\\path',
                   '\\Test\\dir', 'local\\dir', 'tmp\\', 'tmp\\sub\\',
                   '..', '..\\Asdf', '..\\..\\mno\\..\\pqr\\..\..\\'];

    for (local p in paths)
        "isAbsolutePath(<<p>>) = <<File.isAbsolutePath(p) ? 'yes' : 'no'>>\n";

    for (local f in ['test.txt', '\\test.txt', '.', '..'])
    {
        "\b";
        for (local p in paths)
            "combinePath(<<p>>, <<f>>) = <<File.combinePath(p, f)>>\n";
    }

    "\b";
    for (local p in paths)
        "getAbsolutePath(<<p>>) = <<File.getAbsolutePath(p)>>\n";

    "\b";
    for (local p in paths)
        "parentPath(<<p>>) = <<File.getPathName(p)>>\n";

    "\b";
    for (local p in paths)
        "localToUniversal(<<p>>) = <<File.localToUniversal(p)>>\n";

    "\b";
    local urls = ['test', 'test/foo.bar', '../test/foo.bar'];
    for (local u in urls)
        "universalToLocal(<<u>>) <<File.universalToLocal(u)>>\n";
}
