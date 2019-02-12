#include <tads.h>

main(args)
{
    "This is a test of text in PRE mode, for HTML TADS.
    (Lots         of             spaces           here       for
    non-PRE          mode,   just      to    be         sure.)
    This initial part is normal text, not in PRE mode.
    \nLet's make sure the line break consolidation works outside of
    PRE mode...\n\n\n\n
    That should be just one line.
    Then we'll switch to PRE mode...
    <pre>\n
This is the first line of PRE mode.\n
XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n
X     Widely     Spaced     Text     On     This     Line     X\n
XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n
                                   And this is right-justified!\n
</pre>
And back to regular mode.
    <p>
    Now let's try again with some blank lines in the PRE section.
    <pre>\n
     This is the PRE text.\n
\n
     That was a blank line.\n
\n
</pre>
And back to regular HTML mode again.

    <p>
    Next test is PRE all on one line.
    <pre>This is the PRE text</pre>
    And done.

    <p>
    And now a completely inline PRE. <pre>Here it is.</pre> And done once again.";

    "\bAnd a final check that PRE mode didn't stick in the internal
        parser...      Some         widely         spaced        words...
        And a bunch of line breaks:\n\n\n\n:done.\n";
}
