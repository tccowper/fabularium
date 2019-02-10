#include <tads.h>

main()
{
    local str = '{ }}} {{ }}} {{{ }}} {{{ }} {{{ }';
    "Normal: <<str>>\n";
    "Escaped: <<escape(str)>>\n";
}

escape(str) {
    return rexReplace(
        ['<lbrace>','<rbrace>'], str,
        ['&#x7b;', '&#x7d;']);
}
