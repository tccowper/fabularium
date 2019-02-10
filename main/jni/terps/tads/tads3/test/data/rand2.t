#include <tads.h>

main(args)
{
    for (local i in 1..10)
        "<<rand(0xffffffff)>>\n";
}
