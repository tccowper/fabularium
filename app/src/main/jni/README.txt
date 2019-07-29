--------------------------------
GLK FILES
--------------------------------
See http://eblong.com/zarf/glk/

The files in the top level create the GLK library.

glk.h and glkstart.h are the standard GLK header files.
gi_dispa.c and gi_disp.h are the Dispatch Layer - they allow terps to use the Glk API dynamically.
gi_blorb.c and gi_blorb.h are the Blorb Layer - they allow the library to load images and sounds from a portable archive.

For the Dispatch Layer and Blorb Layer files, look under the "Association" section.

Fabularium-specific files:

  - glk.c contains Fabularium-specific definitions of each glk function, to call back into the Java module.  It also a number of non-official GLK extensions (at the bottom of the file), both to take advantage of additional terp functionality for Gargoyle and TADS banners. 
  - glkblorb.c contains Fabularium-specific blorb functions, including the two required by gi_blorb.h (giblorb_set_resource_map(str), giblorb_get_resource_map())
  - glkstart.c provides entry points for the Java module to run terps.

--------------------------------
FABULARIUM C/C++ TERP PLUGIN SYSTEM
--------------------------------

All terps need to be ported to GLK first.

Then update CMakeLists.txt to ensure that your new terp is compiled, and class Terp in the Java code to correctly select your terp.

At the moment, Fabularium includes the following terps:

    ADVSYS (ANSI) + NewParser

    AGILITY 1.1.1

    ALAN 2.8.7 and ALAN 3.

    BOCFEL 1.0
    
    GIT 1.3.5

    GLULXE 0.5.4
    
    HUGO 3.1.03

    LEVEL9 5.1
    
    MAGNETIC 2.3

    SCARE 1.3.10
    
    SCOTT 1.14

    TADS 2
    
    TADS 3


Also uses these tools:

    inform compiler 6.33 and library 6.12.1

