#include <tads.h>
#include <file.h>

cache: object
    defaultsFile = static new FileName(LibraryDefaultsFile)
    fakeDefaultsFile = static new FileName(defaultsFile.getName())
    prefsFile = static new FileName(WebUIPrefsFile)
    saveFile = static new FileName('filenames.t3v')
    dir1 = nil
;

main(args)
{
    if (args.length() >= 2 && args[2] == 'restore')
    {
        "Restoring saved state...\n";
        restoreGame(cache.saveFile);
    }
    else
    {
        "Creating dirtest...\n";
        local dt = new FileName('dirtest');
        dt.createDirectory();

        "Creating some files in dirtest...\n";
        createFile(dt + 'one.txt');
        createFile(dt + 'two.txt');
        createFile(dt + 'three.txt');
        
        cache.dir1 = dt.listDir().sort(
            SortAsc, {a, b: a.getName().compareIgnoreCase(b.getName())});
        saveGame(cache.saveFile);

        "Removing directory...\n";
        dt.removeDirectory(true);
    }

    try
    {
        local rootf = FileName.fromUniversal('/filenameSave.t3v');
        "Trying to save to save /filenameSave.t3v\n";
        saveGame(rootf);
        "... ok!\n";
    }
    catch(Exception e)
    {
        "error: <<e.displayException()>>\n";
    }
    "\b";

    dump(cache.defaultsFile, 'defaults file as special');
    dump(cache.fakeDefaultsFile, 'defaults file as ordinary path');
    test(cache.saveFile);
    foreach (local f in cache.dir1)
        test(f);
}

test(f)
{
    "<<f.toUniversal()>>\n";
}

dump(f, desc)
{
    try
    {
        "--- <<desc>> ---\n";
        local fp = File.openTextFile(f, FileAccessRead);
        "Ok!!!\n";
        fp.closeFile();
    }
    catch (Exception e)
    {
        "error: <<e.displayException()>>\n";
    }
    "\b";
}

createFile(f)
{
    local fp = File.openTextFile(f, FileAccessWrite);
    fp.writeFile(toString(f));
    fp.closeFile();
}
