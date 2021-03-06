#
# Copyright (C) 2017 Tim Cadogan-Cowper.
#
# This file is part of Fabularium.
#
# Fabularium is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Fabularium; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to your build.
cmake_minimum_required(VERSION 3.4.1)

function(addprefix var prefix)
   set(listVar "")
   foreach(f ${ARGN})
      list(APPEND listVar "${CMAKE_CURRENT_SOURCE_DIR}/${prefix}/${f}")
   endforeach(f)
   set(${var} "${listVar}" PARENT_SCOPE)
endfunction(addprefix)

#################################################################
# GLK
#################################################################
addprefix( GLKMAIN
           glk
           glk.c gi_blorb.c gi_dispa.c glkstart.c )

add_library( glk
             SHARED
             ${GLKMAIN} )

target_compile_options( glk
                        PRIVATE
                        -O3 -funroll-loops -fomit-frame-pointer )

target_link_libraries( glk
                       -llog )

target_include_directories ( glk
                             PRIVATE
                             terps glk )

#################################################################
# TERPS
#################################################################

#################################################################
# ADVSYS + ANSI + NewParser + parts of glkize
# http://www.ifarchive.org/indexes/if-archiveXprogrammingXadvsys.html
# Rather than muck around applying patches, we just use Gargoyle's
# version of the code.
#
# Changes:
# - none
#################################################################
addprefix( ADVSYS_SRC
           terps/advsys
           advmsg.c advtrm.c advprs.c advdbs.c advint.c advjunk.c advexe.c glkstart.c )

add_library( advsys
             SHARED
             ${ADVSYS_SRC} )

target_compile_options( advsys
                        PRIVATE
                        -O2 )

target_link_libraries( advsys
                       glk
                       -llog )

target_include_directories( advsys
                            PRIVATE
                            terps/advsys terps glk )

#################################################################
# AGILITY 1.1.1
# http://www.ifarchive.org/if-archive/programming/agt/agility/agil111src_glk.zip
#
# Changes (see agility.diff):
# - os_glk.c: as with Gargoyle version, make gagt_get_user_file
#   use GLK prompting rather than ask for a text name or do 
#   "bad voodoo". To enable this, define GARGLK.
#################################################################
set( BASE
     agtread.c gamedata.c util.c agxfile.c auxfile.c filename.c )

set( AGIL
     parser.c exec.c runverb.c metacommand.c savegame.c debugcmd.c agil.c
     token.c disassemble.c object.c )

set( INTERFACE
     interface.c os_glk.c )

addprefix( AGIL_SRC
           terps/agility
           ${BASE} ${AGIL} ${INTERFACE} )

add_library( agility
             SHARED
             ${AGIL_SRC} )

target_compile_options( agility
                        PRIVATE
                        -DGLK -DGARGLK
                        -O2 )

target_link_libraries( agility
                       glk
                       -llog )

target_include_directories( agility
                            PRIVATE
                            terps/agility terps glk utilities )

#################################################################
# ALAN 2.8.7
# http://www.alanif.se/21-downloads/download-v2
#
# Changes (see alan2.diff):
# - acode.h: use <stdint.h> and portable definitions of Aword, Aaddr,
#   Abool, Aint, CodeValue and WrdKind.
#
# - main.c: fix security vulnerability in logprint,
#   replace fprintf(logfil, str) with fprintf(logfil, "%s", str)
#
# - exe.c: as with the Gargoyle version, change the
#   save/restore code to use GLK prompting rather than
#   ask for a text name. To enable this define GARGLK.
#################################################################
set( GLKOBJS
     glkstart.c glkio.c )

set( OBJECTS
     arun.c main.c debug.c args.c exe.c inter.c parse.c rules.c
     stack.c decode.c term.c reverse.c readline.c params.c sysdep.c alan.version.c )

addprefix( ALAN2_SRC
           terps/alan/alan2
           ${GLKOBJS} ${OBJECTS} )

add_library( alan2
             SHARED
             ${ALAN2_SRC} )

target_compile_options( alan2
                        PRIVATE
                        -DREVERSED -DGLK -DGARGLK -DPROGNAME=\"alan2\"
                        -O2 -funsigned-char )

target_link_libraries( alan2
                       glk
                       -llog )

target_include_directories( alan2
                            PRIVATE
                            terps/alan/alan2 terps glk )

#################################################################
# ALAN V3.0beta5
# https://www.alanif.se/download-alan-v3/source-code
# https://bitbucket.org/alanif/alan
#
# Changes
# - none
#################################################################
set( GLKOBJS
     glkstart.c glkio.c )

set( OBJECTS
     act.c actor.c alan.version.c AltInfo.c args.c arun.c 
     attribute.c chartest.c checkentry.c class.c compatibility.c
     Container.c current.c debug.c decode.c dictionary.c event.c
     exe.c fnmatch.c glkio.c glkstart.c instance.c
     inter.c lists.c literal.c Location.c main.c memory.c msg.c
     options.c output.c ParameterPosition.c params.c parse.c readline.c
     reverse.c rules.c save.c scan.c score.c set.c stack.c state.c
     StateStack.c syntax.c sysdep.c syserr.c term.c types.c utils.c word.c )

addprefix( ALAN3_SRC
           terps/alan/alan3/interpreter
           ${GLKOBJS} ${OBJECTS} )

add_library( alan3
             SHARED
             ${ALAN3_SRC} )

target_compile_options( alan3
                        PRIVATE
                        -DGLK -DHAVE_GARGLK -DBUILD=0 -DPROGNAME=\"alan3\"
                        -O2 -funsigned-char )

target_link_libraries( alan3
                       glk
                       -llog )

target_include_directories( alan3
                            PRIVATE
                            terps/alan/alan3/interpreter terps glk )

#################################################################
# BOCFEL 1.0
# https://cspiegel.github.io/bocfel/downloads.html
#
# Changes:
# - none
#################################################################
addprefix( BOCFELMAIN
           terps/bocfel
           blorb.c branch.c dict.c glkstart.c iff.c io.c math.c meta.c memory.c
           objects.c osdep.c patches.c process.c random.c screen.c sound.c stack.c
           unicode.c util.c zoom.c zterp.c )

add_library( bocfel
             SHARED
             ${BOCFELMAIN} )

target_compile_options( bocfel
                        PRIVATE
                        -DZTERP_NO_WATCHPOINTS -DZTERP_NO_SAFETY_CHECKS -DZTERP_GLK -DGARGLK
                        -O3 -std=c99 )

target_link_libraries( bocfel
                       glk
                       -llog )

target_include_directories( bocfel
                            PRIVATE
                            terps/bocfel terps glk )

#################################################################
# GIT 1.3.5
# https://github.com/DavidKinder/Git
# N.B.: Don't compile with -DDIRECT_THREADING as this seems
# to cause git to crash on some devices.
#
# Changes (see git.diff):
# - glkop.c: the original source assumes that GLK objects are
#   referenced by (opaque) C pointers. But our GLK implementation
#   is in Java. To work around this, pass GLK object IDs
#   rather than pointers and remove C code relating to class
#   registration / unregistration as this is done on the Java side.
#################################################################
addprefix( GITMAIN
           terps/git
           git.c memory.c compiler.c opcodes.c operands.c
           peephole.c terp.c glkop.c search.c git_unix.c
           savefile.c saveundo.c gestalt.c heap.c accel.c )

add_library( git
             SHARED
             ${GITMAIN} )

target_compile_options( git
                        PRIVATE
                        -DUSE_MMAP -DUSE_INLINE
                        -O3 -Wall )

target_link_libraries( git
                       glk
                       -llog )

target_include_directories( git
                            PRIVATE
                            terps/git terps glk )

#################################################################
# GLULXE 0.5.4
# http://eblong.com/zarf/glulx/index.html
#
# Changes (see glulxe.diff):
# - glkop.c: same as GIT, see above.
#################################################################
addprefix( GLULXEMAIN
           terps/glulxe
           main.c files.c vm.c exec.c funcs.c operand.c string.c glkop.c
           heap.c serial.c search.c accel.c float.c gestalt.c osdepend.c
           profile.c debugger.c unixstrt.c )

add_library( glulxe
             SHARED
             ${GLULXEMAIN} )

target_compile_options( glulxe
                        PRIVATE
                        -DOS_UNIX
                        -O2 -Wall -Wmissing-prototypes -Wstrict-prototypes -Wno-unused )

target_link_libraries( glulxe
                       glk
                       -llog )

target_include_directories( glulxe
                            PRIVATE
                            terps/glulxe terps glk )

#################################################################
# HUGO 3.1.03
# http://www.generalcoffee.com/hugo/gethugo.html
# http://www.ifarchive.org/if-archive/programming/hugo/source/
#
# Changes (see hugo.diff):
# - hemisc.c: fix two security vulnerabilities by
#   replacing printf(line) with printf("%s", line).
#
# - heglk.c: include the gargoyle port's hemedia.c.
#################################################################
addprefix( HUGOMAIN
           terps/hugo/source
           he.c heexpr.c hemisc.c heobject.c heparse.c heres.c herun.c heset.c
           stringfn.c )

addprefix( HUGOGLK
           terps/hugo/heglk
           heglk.c heglkunix.c )

add_library( hugo
             SHARED
             ${HUGOMAIN} ${HUGOGLK} )

target_compile_options( hugo
                        PRIVATE
                        -DGLK -DGARGLK -DCOMPILE_V25 -DNO_KEYPRESS_CURSOR -DHUGO_INLINE=static\ inline
                        -O3 )

target_link_libraries( hugo
                       glk
                       -llog )

target_include_directories( hugo
                            PRIVATE
                            terps/hugo/heglk terps/hugo/source terps glk )

#################################################################
# LEVEL9 5.1
# http://www.ifarchive.org/indexes/if-archiveXlevel9XinterpretersXlevel9.html
#
# Changes (see level9.diff):
# - level9.h: improve portability by including <stdint.h> and using
#   the typedefs from there, rather than assuming width of types 
#   like unsigned int, etc.
#################################################################
addprefix( LEVEL9_SRC
           terps/level9
           level9.c bitmap.c Glk/glk.c )

add_library( level9
             SHARED
             ${LEVEL9_SRC} )

target_compile_options( level9
                        PRIVATE
                        -DBITMAP_DECODER
                        -DNEED_STRICMP_PROTOTYPE -Dstricmp=gln_strcasecmp -Dstrnicmp=gln_strncasecmp
                        -O2 -Wl,--wrap,toupper,--wrap,tolower )

target_link_libraries( level9
                       glk
                       -llog )

target_include_directories( level9
                            PRIVATE
                            terps/level9 terps/level9/Glk terps glk )

#################################################################
# MAGNETIC 2.3
# http://www.ifarchive.org/indexes/if-archiveXmagnetic-scrollsXinterpretersXmagnetic.html
#
# Changes (see magnetic.diff):
# - glk.c: always use GLK save / restore prompts, even if
#   game has already asked user to enter filename.
#
# - defs.h: improve portability by including <stdint.h> and using
#   the typedefs from there, rather than assuming width of types
#   like unsigned int, etc.
#################################################################
addprefix( MAGNETIC_SRC
           terps/magnetic
           Generic/emu.c Glk/glk.c )

add_library( magnetic
             SHARED
             ${MAGNETIC_SRC} )

target_compile_options( magnetic
                        PRIVATE
                        -DFAB
                        -O2 -Wl,--wrap,toupper,--wrap,tolower )

target_link_libraries( magnetic
                       glk
                       -llog )

target_include_directories( magnetic
                            PRIVATE
                            terps/magnetic/Generic terps/magnetic/Glk terps glk )

#################################################################
# SCARE 1.3.10
# http://www.ifarchive.org/indexes/if-archiveXprogrammingXadrift.html
#
# Changes (see scare.diff):
# - os_glk.c: handle images and sound using same strategy as Hugo port
#################################################################
addprefix( SCAREMAIN
           terps/scare/source
           sctafpar.c sctaffil.c scprops.c scvars.c scexpr.c scprintf.c
           scinterf.c scparser.c sclibrar.c scrunner.c scevents.c scnpcs.c
           scobjcts.c sctasks.c screstrs.c scgamest.c scserial.c
           scresour.c scutils.c scdebug.c sclocale.c scmemos.c os_glk.c )

add_library( scare
             SHARED
             ${SCAREMAIN} )

target_compile_options( scare
                        PRIVATE
                        -DFAB
                        -O2 )

target_link_libraries( scare
                       glk
                       -lz -llog )

target_include_directories( scare
                            PRIVATE
                            terps/scare/source terps glk )

#################################################################
# SCOTT 1.14
# https://github.com/cspiegel/scottfree-glk
#
# Changes:
# - none
#################################################################
addprefix( SCOTTMAIN
           terps/scott
           scott.c bsd.c )

add_library( scott
             SHARED
             ${SCOTTMAIN} )

target_compile_options( scott
                        PRIVATE
                        -O2 )

target_link_libraries( scott
                       glk
                       -llog )

target_include_directories( scott
                            PRIVATE
                            terps/scott terps glk )

#################################################################
# TADS 2 AND 3
# http://www.tads.org/t2_patch.htm
# http://www.tads.org/t3dl/t3_src.zip
#
# Changes (see tads.diff):
# - oemfab.c, osfab.cc, osfab.h, osglk.c, vmglk23.cpp: 
#   new source files to implement Fabularium port.
#   These files draw heavily from frobtads and qtads.
#
# - os.h: include a reference to our Fabularium port.
#
# - resfind.h: we need to do some fiddling with this to ensure
#   it is included in the "right" way depending on whether linking
#   to C++ or just C files.
#
# - vmtz.cpp: explicitly cast to (void *) for pointer comparison
#   to fix compiler error.
#################################################################

####### SOURCE FILES COMMON TO TERP AND COMPILER  ###############

addprefix( T2COMMON
           terps/tads/tads2
           osifc.c osstzprs.c osrestad.c )

addprefix( T3COMMON
           terps/tads/tads3
           charmap.cpp md5.cpp resldexe.cpp
           resload.cpp sha2.cpp std_dbg.cpp
           std.cpp tccmdutl.cpp tcerr.cpp
           tcerrmsg.cpp tcgen.cpp tcglob.cpp
           tchostsi.cpp tclibprs.cpp tcmain.cpp
           tcprs.cpp tcprsstm.cpp tcsrc.cpp tct3.cpp
           tct3stm.cpp tct3unas.cpp tctok.cpp utf8.cpp
           vmanonfn.cpp vmbif.cpp vmbifreg.cpp vmbift3.cpp
           vmbiftad.cpp vmbiftio.cpp vmbignum.cpp vmbignumlib.cpp
           vmbt3_nd.cpp vmbytarr.cpp vmcfgmem.cpp vmcoll.cpp
           vmconhmp.cpp vmconsol.cpp vmcrc.cpp vmcset.cpp
           vmdate.cpp vmdict.cpp vmdynfunc.cpp vmerr.cpp
           vmerrmsg.cpp vmfile.cpp vmfilnam.cpp vmfilobj.cpp
           vmfref.cpp vmfunc.cpp vmglob.cpp vmgram.cpp
           vmhash.cpp vmhostsi.cpp vmhosttx.cpp vmimage.cpp
           vmimg_nd.cpp vmini_nd.cpp vminit.cpp vminitim.cpp
           vmintcls.cpp vmisaac.cpp vmiter.cpp vmlog.cpp
           vmlookup.cpp vmlst.cpp vmmcreg.cpp vmmeta.cpp
           vmnetfillcl.cpp vmobj.cpp vmop.cpp vmpack.cpp
           vmpat.cpp vmpool.cpp vmpoolim.cpp vmregex.cpp
           vmrun.cpp vmrunsym.cpp vmsave.cpp vmsort.cpp
           vmsortv.cpp vmsrcf.cpp vmstack.cpp vmstr.cpp
           vmstrbuf.cpp vmstrcmp.cpp vmtmpfil.cpp vmtobj.cpp
           vmtype.cpp vmtypedh.cpp vmtz.cpp vmtzobj.cpp
           vmundo.cpp vmvec.cpp )

addprefix( TCOMMON
           terps/tads
           osfab.cc
           tads3/derived/vmuni_cs.cpp
           tads3/indlg_tx3.cpp )

add_library( tadsc
             STATIC
             ${T2COMMON} ${T3COMMON} ${TCOMMON})

target_compile_options( tadsc
                        PRIVATE
                        -DFAB -DVMGLOB_VARS -DTC_TARGET_T3 -DOS_PTHREAD_TLS -DRUNFAST
                        -O2 -fno-strict-aliasing -pthread )

target_include_directories( tadsc
                            PRIVATE
                            terps terps/tads terps/tads/tads2 terps/tads/tads3 terps/tads/tads3/unix
                            terps/tads/tads3/test )

##################### TADS TERP #################################

addprefix( T2TERP
           terps/tads/tads2
           argize.c askf_os.c bif.c bifgdum.c
           cmap.c cmd.c dat.c dbgtr.c errmsg.c
           execmd.c fio.c fioxor.c getstr.c
           indlg_tx.c ler.c linfdum.c lst.c
           mch.c mcm.c mcs.c obj.c os0.c
           oserr.c out.c output.c ply.c qas.c
           regex.c run.c runstat.c suprun.c
           trd.c voc.c vocab.c )

addprefix( T3TERP
           terps/tads/tads3
           gameinfl.cpp gameinfo.cpp resfind.cpp tcprs_rt.cpp
           tcprsnf.cpp tcprsnl.cpp tct3_d.cpp tct3nl.cpp
           vmbifl.cpp vmconhtm.cpp vmmain.cpp vmsa.cpp )

addprefix( TERP
           terps/tads
           oemfab.c osglk.c
           tads3/askf_os3.cpp vmglk23.cpp )

add_library( tads
             SHARED
             ${T2TERP} ${T3TERP} ${TERP})

target_compile_options( tads
                        PRIVATE
                        -DTADS_TERP -DFAB -DVMGLOB_VARS -DTC_TARGET_T3 -DOS_PTHREAD_TLS -DRUNFAST
                        -O2 -fno-strict-aliasing -pthread )

target_link_libraries( tads
                       tadsc
                       glk
                       -llog )

target_include_directories( tads
                            PRIVATE
                            glk terps terps/tads terps/tads/tads2 terps/tads/tads3 terps/tads/tads3/unix )

##################### TADS COMPILER #################################

addprefix( T3COMP
           terps/tads/tads3
           askf_tx3.cpp os_stdio.cpp osnetdum.cpp rcmain.cpp
           tcgenfil.cpp tcmake.cpp tcmakecl.cpp tcprsfil.cpp
           tcprsimg.cpp tcprsprg.cpp tct3img.cpp tct3prg.cpp
           vmbifc.cpp vmconmor.cpp vmimgrb.cpp vmpreini.cpp vmwrtimg.cpp )

addprefix( TCOMP
           terps/tads
           ost3comp.cc )

add_library( t3make
             SHARED
             ${T2COMP} ${T3COMP} ${TCOMP} )

target_compile_options( t3make
                        PRIVATE
                        -DFAB -DVMGLOB_VARS -DTC_TARGET_T3 -DOS_PTHREAD_TLS -DRUNFAST
                        -O2 -fno-strict-aliasing -pthread )

target_link_libraries( t3make
                       tadsc
                       -llog )

target_include_directories( t3make
                            PRIVATE
                            terps terps/tads terps/tads/tads2 terps/tads/tads3 terps/tads/tads3/unix
                            terps/tads/tads3/test
                            utilities )

#################################################################
# UTILITIES
#################################################################

#################################################################
# LOADER
#################################################################
add_library( utils
             SHARED
             utilities/ifutils.c )

target_compile_options( utils
                        PRIVATE
                        -O3 -funroll-loops -fomit-frame-pointer )

target_link_libraries( utils
                       -llog )

target_include_directories ( utils
                             PRIVATE
                             utilities )

#################################################################
# AGT2AGX
# (tool provided by AGILITY to convert bundle of AGT files into
# one AGX file)
#
# Changes:
# - none
#################################################################
set( BASE
     agtread.c gamedata.c util.c agxfile.c auxfile.c filename.c )

addprefix( AGT2AGX_SRC
           terps/agility
           ${BASE} agt2agx.c agilstub.c )

add_library( agt2agx
             SHARED
             ${AGT2AGX_SRC} )

target_compile_options( agt2agx
                        PRIVATE
                        -DLINUX
                        -O2
                        -static )

target_link_libraries( agt2agx
                       -llog )

target_include_directories( agt2agx
                            PRIVATE
                            terps/agility
                            utilities )

#################################################################
# BABEL
# 
# Changes:
# - none
#################################################################
set( TREATY_OBJS
     zcode.c magscrolls.c blorb.c glulx.c hugo.c agt.c level9.c executable.c advsys.c tads.c tads2.c tads3.c adrift.c alan.c )

set( BH_OBJS
     babel_handler.c register.c misc.c md5.c ${TREATY_OBJS} )

set( IFICTION_OBJS
     ifiction.c register_ifiction.c )

addprefix( BABEL_OBJS
           utilities/babel
           babel_android.c ${IFICTION_OBJS} ${BH_OBJS} )

add_library( babel
             SHARED
             ${BABEL_OBJS} )

target_compile_options( babel
                        PRIVATE
                        -O2 )

target_link_libraries( babel
                       -llog )

target_include_directories( babel
                            PRIVATE
                            utilities/babel
                            utilities )

#################################################################
# INFORM 6.33
# http://www.ifarchive.org/indexes/if-archiveXinfocomXcompilersXinform6Xsource.html
#
# Changes:
# - none
#################################################################
addprefix( OBJECTS
           utilities/inform/src
           arrays.c asm.c bpatch.c chars.c directs.c errors.c
           expressc.c expressp.c files.c inform.c lexer.c
           linker.c memory.c objects.c states.c symbols.c
           syntax.c tables.c text.c veneer.c verbs.c )

add_library( inform
             SHARED
             ${OBJECTS} )

target_compile_options( inform
                        PRIVATE
                        -DTemporary_Directory=\"/tmp\"
                        -O2 )

target_link_libraries( inform
                       -llog )

target_include_directories( inform
                            PRIVATE
                            utilities/inform/lib
                            utilities/inform/src
                            utilities )