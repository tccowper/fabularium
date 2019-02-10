File last updated:  21 Nov 2016

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

Licences:

   - babel: public domain, Aladdin-specific licence for md5.c and Creative Commons Attribution2.5 License for the other files
   - GLK and blorb:  Andrew Plotkin's custom licence
   - others: (c) Tim Cadogan-Cowper 2016, GPL v2.

--------------------------------
FABULARIUM C/C++ TERP PLUGIN SYSTEM
--------------------------------

All terps need to be ported to GLK first.

Then:

1) Dump the C or C++ source files for the new terp into a new subfolder (e.g. jni/scott or jni/scare).

2) Update the Android.mk file in the parent folder to ensure that your new terp is compiled.  Include the glkterp.c with the rest of the source files.

3) Update class Terp in the Java code to correctly select your terp.

At the moment, Fabularium includes the following terps:

    ADVSYS (ANSI) + NewParser
    Licence: BSD licence.
    (http://www.ifarchive.org/indexes/if-archiveXprogrammingXadvsys.html)

    AGILITY 1.1.1
    Licence: GPL v2.
    (http://www.ifarchive.org/if-archive/programming/agt/agility/agil111src_glk.zip)

    ALAN 2.8.7 and ALAN 3.
    Licence: Artistic Licence 2.0
    N.B. linked website below states that:
        "The licensing for Alan v3 also applies for Alan v2. You can find the license in the
         Alan v3 manual."
    (ALAN 2.8.7:  http://www.alanif.se/21-downloads/download-v2)

    BOCFEL 1.0
    Licence: GPL v3.
    (https://cspiegel.github.io/bocfel/downloads.html)
    
    GIT 1.3.5
    Licence: MIT.
    (https://github.com/DavidKinder/Git)

    GLULXE 0.5.4
    Licence: MIT.
    (http://eblong.com/zarf/glulx/index.html)
    
    HUGO 3.1.03
    Licence: Custom. May need to get permission before can release.
    (http://www.generalcoffee.com/hugo/gethugo.html)

    LEVEL9 5.1
    Licence: GPL v2.
    (http://www.ifarchive.org/indexes/if-archiveXlevel9XinterpretersXlevel9.html)
    
    MAGNETIC 2.3
    Licence: GPL v2.
    (http://www.ifarchive.org/indexes/if-archiveXmagnetic-scrollsXinterpretersXmagnetic.html)

    SCARE 1.3.10
    Licence: GPL v2.
    (http://www.ifarchive.org/indexes/if-archiveXprogrammingXadrift.html)
    
    SCOTT 1.14
    Licence: GPL v2.
    (http://www.ifarchive.org/indexes/if-archiveXscott-adamsXinterpretersXscottfree.html)

    TADS 2
    Licence: Custom.
    (http://www.tads.org/t2_patch.htm)
    
    TADS 3
    Licence: Same as TADS2 - Custom.
    (http://www.tads.org/t3dl/t3_src.zip)


Also uses these tools:

    inform compiler 6.33 and library 6.12.1
    Licence: Artistic Licence
    (http://www.ifarchive.org/indexes/if-archiveXinfocomXcompilersXinform6Xsource.html)

