This directory contains test cases for the compiler that does not
involve interpreting the output (those are in ../../regression) and
supplements the unit tests in "unit".

The commands are:

    regr     - run all or specified regression tests
    ok	     - save the current output for a case as its expected output
    f	     - lists all cases that did not succeed last run
    d	     - diff the output against the expected for a specified case

You need to have ../../bin in your path since the command scripts are
stored there.
