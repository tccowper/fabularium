#include <tads.h>

main(args)
{
    local seed;
    if (args.length() == 2)
    {
        seed = toInteger(args[2]);
        if (seed == 0)
            seed = args[2];
    }
    else
    {
        "usage: rand-mer &lt;seed-integer&gt;\n";
        return;
    }

    /* initialize Mersenne */
    randomize(RNG_MT19937, seed);

    /* generate 100 values */
    for (local i in 1..100)
        "<<rand()>> ";
    "\n";
}
