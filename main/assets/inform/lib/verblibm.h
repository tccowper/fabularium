! ==============================================================================
!   VERBLIBM:  Core of standard verbs library.
!
!   Supplied for use with Inform 6 -- Release 6.12.1 -- Serial number 160605
!
!   Copyright Graham Nelson 1993-2004 and David Griffith 2012-2016
!
!   This code is licensed under either the traditional Inform license as
!   described by the DM4 or the Artistic License version 2.0.  See the
!   file COPYING in the distribution archive or at
!   https://github.com/DavidGriffith/inform6lib/
!
!   This file is automatically Included in your game file by "VerbLib".
! ==============================================================================

System_file;

#Ifdef MODULE_MODE;
Constant DEBUG;
Constant Grammar__Version 2;
Include "linklpa";
Include "linklv";
#Endif; ! MODULE_MODE

! ------------------------------------------------------------------------------

[ Banner i;
    #Ifdef LanguageBanner;
    LanguageBanner();
    i = 0;  ! suppress warning
    #Ifnot;
    if (Story) {
        #Ifdef TARGET_ZCODE;
        #IfV5; style bold; #Endif;
        print "^", (string) Story;
        #IfV5; style roman; #Endif;
        #Ifnot; ! TARGET_GLULX;
        glk_set_style(style_Header);
        print "^", (string) Story;
        glk_set_style(style_Normal);
        #Endif; ! TARGET_
    }
    if (Headline) print (string) Headline;
    #Ifdef TARGET_ZCODE;
    print "Release ", (HDR_GAMERELEASE-->0) & $03ff, " / Serial number ";
    for (i=0 : i<6 : i++) print (char) HDR_GAMESERIAL->i;
    #Ifnot; ! TARGET_GLULX;
    print "Release ";
    @aloads ROM_GAMERELEASE 0 i;
    print i;
    print " / Serial number ";
    for (i=0 : i<6 : i++) print (char) ROM_GAMESERIAL->i;
    #Endif; ! TARGET_
    print " / Inform v"; inversion;
    print " Library v", (string) LibRelease, " ";
    #Ifdef STRICT_MODE;
    print "S";
    #Endif; ! STRICT_MODE
    #Ifdef INFIX;
    print "X";
    #Ifnot;
    #Ifdef DEBUG;
    print "D";
    #Endif; ! DEBUG
    #Endif; ! INFIX
    new_line;
    #Endif; ! LanguageBanner
];

[ VersionSub ix;
    #Ifdef LanguageVersionSub;
    LanguageVersionSub();
    ix = 0;  ! suppress warning
    #Ifnot;
    Banner();
    #Ifdef TARGET_ZCODE;
    ix = 0; ! shut up compiler warning
    if (standard_interpreter > 0) {
        print "Standard interpreter ", standard_interpreter/256, ".", standard_interpreter%256,
            " (", HDR_TERPNUMBER->0;
        #Iftrue (#version_number == 6);
        print (char) '.', HDR_TERPVERSION->0;
        #Ifnot;
        print (char) HDR_TERPVERSION->0;
        #Endif;
        print ") / ";
        }
    else {
        print "Interpreter ", HDR_TERPNUMBER->0, " Version ";
        #Iftrue (#version_number == 6);
        print HDR_TERPVERSION->0;
        #Ifnot;
        print (char) HDR_TERPVERSION->0;
        #Endif;
        print " / ";
    }

    #Ifnot; ! TARGET_GLULX;
    @gestalt 1 0 ix;
    print "Interpreter version ", ix / $10000, ".", (ix & $FF00) / $100,
      ".", ix & $FF, " / ";
    @gestalt 0 0 ix;
    print "VM ", ix / $10000, ".", (ix & $FF00) / $100, ".", ix & $FF, " / ";
    #Endif; ! TARGET_;
    print "Library serial number ", (string) LibSerial, "^";
    #Ifdef LanguageVersion;
    print (string) LanguageVersion, "^";
    #Endif; ! LanguageVersion
    #Endif; ! LanguageVersionSub
];

[ RunTimeError n p1 p2;
    #Ifdef LanguageError;
    LanguageError(n, p1, p2);
    #Ifnot;
    #Ifdef DEBUG;
    print "** Library error ", n, " (", p1, ", ", p2, ") **^** ";
    switch (n) {
      1:    print "preposition not found (this should not occur)";
      2:    print "Property value not routine or string: ~", (property) p2, "~ of ~", (name) p1,
                  "~ (", p1, ")";
      3:    print "Entry in property list not routine or string: ~", (property) p2, "~ list of ~",
                  (name) p1, "~ (", p1, ")";
      4:    print "Too many timers/daemons are active simultaneously.
                  The limit is the library constant MAX_TIMERS
                  (currently ", MAX_TIMERS, ") and should be increased";
      5:    print "Object ~", (name) p1, "~ has no ~", (property) p2, "~ property";
      7:    print "The object ~", (name) p1, "~ can only be used as a player object if it has
                  the ~number~ property";
      8:    print "Attempt to take random entry from an empty table array";
      9:    print p1, " is not a valid direction property number";
      10:   print "The player-object is outside the object tree";
      11:   print "The room ~", (name) p1, "~ has no ~", (property) p2, "~ property";
      12:   print "Tried to set a non-existent pronoun using SetPronoun";
      13:   print "A 'topic' token can only be followed by a preposition";
      14:   print "Overflowed buffer limit of ", p1, " using '@@64output_stream 3' ", (string) p2;
      15:   print "LoopWithinObject broken because the object ", (name) p1, " was moved while the loop passed through it.";
      16:   print "Attempt to use illegal narrative_voice of ", p1, ".";
      default:
            print "(unexplained)";
    }
    " **";
    #Ifnot;
    "** Library error ", n, " (", p1, ", ", p2, ") **";
    #Endif; ! DEBUG
    #Endif; ! LanguageError
];

! ----------------------------------------------------------------------------
!  The WriteListFrom routine, a flexible object-lister taking care of
!  plurals, inventory information, various formats and so on.  This is used
!  by everything in the library which ever wants to list anything.
!
!  If there were no objects to list, it prints nothing and returns false;
!  otherwise it returns true.
!
!  o is the object, and style is a bitmap, whose bits are given by:
! ----------------------------------------------------------------------------


Constant NEWLINE_BIT   $0001;       ! New-line after each entry
Constant INDENT_BIT    $0002;       ! Indent each entry by depth
Constant FULLINV_BIT   $0004;       ! Full inventory information after entry
Constant ENGLISH_BIT   $0008;       ! English sentence style, with commas and and
Constant RECURSE_BIT   $0010;       ! Recurse downwards with usual rules
Constant ALWAYS_BIT    $0020;       ! Always recurse downwards
Constant TERSE_BIT     $0040;       ! More terse English style
Constant PARTINV_BIT   $0080;       ! Only brief inventory information after entry
Constant DEFART_BIT    $0100;       ! Use the definite article in list
Constant WORKFLAG_BIT  $0200;       ! At top level (only), only list objects
                                    ! which have the "workflag" attribute
Constant ISARE_BIT     $0400;       ! Print " is" or " are" before list
Constant CONCEAL_BIT   $0800;       ! Omit objects with "concealed" or "scenery":
                                    ! if WORKFLAG_BIT also set, then does _not_
                                    ! apply at top level, but does lower down
Constant NOARTICLE_BIT $1000;       ! Print no articles, definite or not
Constant ID_BIT        $2000;       ! Print object id after each entry

[ NextEntry o odepth;
    for (::) {
        o = sibling(o);
        if (o == 0) return 0;
        if (lt_value && o.list_together ~= lt_value) continue;
        if (c_style & WORKFLAG_BIT && odepth==0 && o hasnt workflag) continue;
        if (c_style & CONCEAL_BIT && (o has concealed || o has scenery)) continue;
        return o;
    }
];

[ WillRecurs o;
    if (c_style & ALWAYS_BIT) rtrue;
    if (c_style & RECURSE_BIT == 0) rfalse;
    if ((o has transparent or supporter) || (o has container && o has open)) rtrue;
    rfalse;
];

[ ListEqual o1 o2;
    if (child(o1) && WillRecurs(o1)) rfalse;
    if (child(o2) && WillRecurs(o2)) rfalse;
    if (c_style & (FULLINV_BIT + PARTINV_BIT)) {
        if ((o1 hasnt worn && o2 has worn) || (o2 hasnt worn && o1 has worn)) rfalse;
        if ((o1 hasnt light && o2 has light) || (o2 hasnt light && o1 has light)) rfalse;
        if (o1 has container) {
            if (o2 hasnt container) rfalse;
            if ((o1 has open && o2 hasnt open) || (o2 has open && o1 hasnt open))
                rfalse;
        }
        else if (o2 has container)
            rfalse;
    }
    return Identical(o1, o2);
];

[ SortTogether obj value;
    ! print "Sorting together possessions of ", (object) obj, " by value ", value, "^";
    ! for (x=child(obj) : x : x=sibling(x))
    !     print (the) x, " no: ", x, " lt: ", x.list_together, "^";
    while (child(obj)) {
        if (child(obj).list_together ~= value) move child(obj) to out_obj;
        else                                   move child(obj) to in_obj;
    }
    while (child(in_obj))  move child(in_obj) to obj;
    while (child(out_obj)) move child(out_obj) to obj;
];

[ SortOutList obj i k l;
    !  print "^^Sorting out list from ", (name) obj, "^  ";
    !  for (i=child(location) : i : i=sibling(i))
    !      print (name) i, " --> ";
    !  new_line;

  .AP_SOL;

    for (i=obj : i : i=sibling(i)) {
        k = i.list_together;
        if (k ~= 0) {
            ! print "Scanning ", (name) i, " with lt=", k, "^";
            for (i=sibling(i) : i && i.list_together == k :) i = sibling(i);
            if (i == 0) rfalse;
            ! print "First not in block is ", (name) i, " with lt=", i.list_together, "^";
            for (l=sibling(i) : l : l=sibling(l))
                if (l.list_together == k) {
                    SortTogether(parent(obj), k);
                    ! print "^^After ST:^  ";
                    ! for (i=child(location) : i : i=sibling(i))
                    !     print (name) i, " --> ";
                    ! new_line;
                    obj = child(parent(obj));
                    jump AP_SOL;
                }
        }
    }
];

#Ifdef TARGET_ZCODE;

[ Print__Spaces n;         ! To avoid a bug occurring in Inform 6.01 to 6.10
    if (n == 0) return;
    spaces n;
];

#Ifnot; ! TARGET_GLULX;

[ Print__Spaces n;
    while (n > 0) {
        @streamchar ' ';
        n = n - 1;
    }
];

#Endif; ! TARGET_

[ WriteListFrom o style depth
    s1 s2 s3 s4 s5 s6;

    if (o == nothing) return 0;

    s1 = c_style;      s2 = lt_value;   s3 = listing_together;
    s4 = listing_size; s5 = wlf_indent; s6 = inventory_stage;

    if (o == child(parent(o))) {
        SortOutList(o);
        o = child(parent(o));
    }
    c_style = style;
    wlf_indent = 0;
    if (WriteListR(o, depth) == 0) return 0;

    c_style = s1;      lt_value = s2;   listing_together = s3;
    listing_size = s4; wlf_indent = s5; inventory_stage = s6;
    rtrue;
];

[ WriteListR o depth stack_pointer  classes_p sizes_p i j k k2 l m n q senc mr;
    if (depth > 0 && o == child(parent(o))) {
        SortOutList(o);
        o = child(parent(o));
    }
    for (::) {
        if (o == 0) rfalse;
        if (c_style & WORKFLAG_BIT && depth==0 && o hasnt workflag) {
            o = sibling(o);
            continue;
        }
        if (c_style & CONCEAL_BIT && (o has concealed || o has scenery)) {
            o = sibling(o);
            continue;
        }
        break;
    }
    classes_p = match_classes + stack_pointer;
    sizes_p   = match_list + stack_pointer;

    for (i=o,j=0 : i && (j+stack_pointer)<128 : i=NextEntry(i,depth),j++) {
        classes_p->j = 0;
        if (i.plural) k++;
    }

    if (c_style & ISARE_BIT) {
        if (j == 1 && o hasnt pluralname) Tense(IS__TX, WAS__TX);
        else                              Tense(ARE__TX, WERE__TX);
        if (c_style & NEWLINE_BIT)   print ":^";
        else                              print (char) ' ';
        c_style = c_style - ISARE_BIT;
    }

    stack_pointer = stack_pointer+j+1;

    if (k < 2) jump EconomyVersion;   ! It takes two to plural
    n = 1;
    for (i=o,k=0 : k<j : i=NextEntry(i,depth),k++)
        if (classes_p->k == 0) {
            classes_p->k = n; sizes_p->n = 1;
            for (l=NextEntry(i,depth),m=k+1 : l && m<j : l=NextEntry(l,depth),m++)
                if (classes_p->m == 0 && i.plural && l.plural ~= 0) {
                    if (ListEqual(i, l) == 1) {
                        sizes_p->n = sizes_p->n + 1;
                        classes_p->m = n;
                    }
                }
            n++;
        }
    n--;

    for (i=1,j=o,k=0 : i<=n : i++,senc++) {
        while (((classes_p->k) ~= i) && ((classes_p->k) ~= -i)) {
            k++; j=NextEntry(j, depth);
        }
        m = sizes_p->i;
        if (j == 0) mr = 0;
        else {
            if (j.list_together ~= 0 or lt_value && metaclass(j.list_together) == Routine or String &&
                j.list_together == mr) senc--;
            mr = j.list_together;
        }
    }
    senc--;

    for (i=1,j=o,k=0,mr=0 : senc>=0 : i++,senc--) {
        while (((classes_p->k) ~= i) && ((classes_p->k) ~= -i)) {
            k++; j=NextEntry(j, depth);
        }
        if (j.list_together ~= 0 or lt_value) {
            if (j.list_together == mr) {
                senc++;
                jump Omit_FL2;
            }
            k2 = NextEntry(j, depth);
            if (k2 == 0 || k2.list_together ~= j.list_together) jump Omit_WL2;
            k2 = metaclass(j.list_together);
            if (k2 == Routine or String) {
                q = j; listing_size = 1; l = k; m = i;
                while (m < n && q.list_together == j.list_together) {
                    m++;
                    while (((classes_p->l) ~= m) && ((classes_p->l) ~= -m)) {
                        l++; q = NextEntry(q, depth);
                     }
                    if (q.list_together == j.list_together) listing_size++;
                }
                ! print " [", listing_size, "] ";
                if (listing_size == 1) jump Omit_WL2;
                if (c_style & INDENT_BIT) Print__Spaces(2*(depth+wlf_indent));
                if (k2 == String) {
                    q = 0;
                    for (l=0 : l<listing_size : l++) q = q+sizes_p->(l+i);
                    EnglishNumber(q); print " ";
                    print (string) j.list_together;
                    if (c_style & ENGLISH_BIT) print " (";
                    if (c_style & INDENT_BIT)  print ":^";
                }
                q = c_style;
                if (k2 ~= String) {
                    inventory_stage = 1;
                    parser_one = j; parser_two = depth+wlf_indent;
                    if (RunRoutines(j, list_together) == 1) jump Omit__Sublist2;
                }

                #Ifdef TARGET_ZCODE;
                @push lt_value;    @push listing_together;    @push listing_size;
                #Ifnot; ! TARGET_GLULX;
                @copy lt_value sp; @copy listing_together sp; @copy listing_size sp;
                #Endif; ! TARGET_;

                lt_value = j.list_together; listing_together = j; wlf_indent++;
                WriteListR(j, depth, stack_pointer); wlf_indent--;

                #Ifdef TARGET_ZCODE;
                @pull listing_size; @pull listing_together; @pull lt_value;
                #Ifnot; ! TARGET_GLULX;
                @copy sp listing_size;
                @copy sp listing_together;
                @copy sp lt_value;
                #Endif; ! TARGET_;

                if (k2 == String) {
                    if (q & ENGLISH_BIT) print ")";
                }
                else {
                    inventory_stage = 2;
                    parser_one = j; parser_two = depth+wlf_indent;
                    RunRoutines(j, list_together);
                }

              .Omit__Sublist2;

                if (q & NEWLINE_BIT && c_style & NEWLINE_BIT == 0) new_line;
                c_style = q;
                mr = j.list_together;
                jump Omit_EL2;
            }
        }

      .Omit_WL2;

        if (WriteBeforeEntry(j, depth, 0, senc) == 1) jump Omit_FL2;
        if (sizes_p->i == 1) {
            if (c_style & NOARTICLE_BIT)  print (name) j;
            else {
                if (c_style & DEFART_BIT) print (the) j;
                else                      print (a) j;
            }
            if (c_style & ID_BIT)         print " (", j, ")";
        }
        else {
            if (c_style & DEFART_BIT) PrefaceByArticle(j, 1, sizes_p->i);
            print (number) sizes_p->i, " ";
            PrintOrRun(j, plural, 1);
        }
        if (sizes_p->i > 1 && j hasnt pluralname) {
            give j pluralname;
            WriteAfterEntry(j, depth, stack_pointer);
            give j ~pluralname;
        }
        else {
            WriteAfterEntry(j,depth,stack_pointer);
        }
      .Omit_EL2;

        if (c_style & ENGLISH_BIT) {
            if (senc == 1) print (SerialComma) i+senc, (string) AND__TX;
            if (senc > 1)  print (string) COMMA__TX;
        }
     .Omit_FL2;
    }
    rtrue;

  .EconomyVersion;

    n = j;
    for (i=1,j=o : i<=n : j=NextEntry(j,depth),i++,senc++) {
        if (j.list_together ~= 0 or lt_value && metaclass(j.list_together) == Routine or String &&
            j.list_together==mr) senc--;
        mr = j.list_together;
    }

    for (i=1,j=o,mr=0 : i<=senc : j=NextEntry(j,depth),i++) {
        if (j.list_together ~= 0 or lt_value) {
            if (j.list_together == mr) {
                i--;
                jump Omit_FL;
            }
            k = NextEntry(j, depth);
            if (k == 0 || k.list_together ~= j.list_together) jump Omit_WL;
            k = metaclass(j.list_together);
            if (k == Routine or String) {
                if (c_style & INDENT_BIT) Print__Spaces(2*(depth+wlf_indent));
                if (k == String) {
                    q = j; l = 0;
                    do {
                        q = NextEntry(q, depth); l++;
                    } until (q == 0 || q.list_together ~= j.list_together);
                    EnglishNumber(l); print " ";
                    print (string) j.list_together;
                    if (c_style & ENGLISH_BIT) print " (";
                    if (c_style & INDENT_BIT) print ":^";
                }
                q = c_style;
                if (k ~= String) {
                    inventory_stage = 1;
                    parser_one = j; parser_two = depth+wlf_indent;
                    if (RunRoutines(j, list_together) == 1) jump Omit__Sublist;
                }

                #Ifdef TARGET_ZCODE;
                @push lt_value; @push listing_together; @push listing_size;
                #Ifnot; ! TARGET_GLULX;
                @copy lt_value sp; @copy listing_together sp; @copy listing_size sp;
                #Endif; ! TARGET_;

                lt_value = j.list_together; listing_together = j; wlf_indent++;
                WriteListR(j, depth, stack_pointer); wlf_indent--;

                #Ifdef TARGET_ZCODE;
                @pull listing_size; @pull listing_together; @pull lt_value;
                #Ifnot; ! TARGET_GLULX;
                @copy sp listing_size; @copy sp listing_together; @copy sp lt_value;
                #Endif; ! TARGET_;

                if (k == String) {
                    if (q & ENGLISH_BIT) print ")";
                }
                else {
                    inventory_stage = 2;
                    parser_one = j; parser_two = depth+wlf_indent;
                    RunRoutines(j, list_together);
                }

              .Omit__Sublist;

                if (q & NEWLINE_BIT && c_style & NEWLINE_BIT == 0) new_line;
                c_style = q;
                mr = j.list_together;
                jump Omit_EL;
            }
        }

      .Omit_WL;

        if (WriteBeforeEntry(j, depth, i, senc) == 1) jump Omit_FL;
        if (c_style & NOARTICLE_BIT)  print (name) j;
        else {
            if (c_style & DEFART_BIT) print (the) j;
            else                      print (a) j;
        }
        if (c_style & ID_BIT)         print " (", j, ")";
        WriteAfterEntry(j, depth, stack_pointer);

      .Omit_EL;

        if (c_style & ENGLISH_BIT) {
            if (i == senc-1) print (SerialComma) senc, (string) AND__TX;
            if (i < senc-1) print (string) COMMA__TX;
        }

  .Omit_FL;

    }
]; ! end of WriteListR

[ WriteBeforeEntry o depth ipos sentencepos
    flag;

    inventory_stage = 1;
    if (c_style & INDENT_BIT) Print__Spaces(2*(depth+wlf_indent));
    if (o.invent && (c_style & (PARTINV_BIT|FULLINV_BIT))) {
        flag = PrintOrRun(o, invent, 1);
        if (flag) {
            if (c_style & ENGLISH_BIT) {
                if (ipos == sentencepos-1)
                    print (SerialComma) sentencepos, (string) AND__TX;
                if (ipos < sentencepos-1)
                    print (string) COMMA__TX;
            }
            if (c_style & NEWLINE_BIT) new_line;
        }
    }
    return flag;
];

[ WriteAfterEntry o depth stack_p
    p recurse_flag parenth_flag eldest_child child_count combo i j;

    inventory_stage = 2;
    if (c_style & PARTINV_BIT) {
        if (o.invent && RunRoutines(o, invent))
            if (c_style & NEWLINE_BIT) ""; else rtrue;

        combo = 0;
        if (o has light && location hasnt light) combo=combo+1;
        if (o has container && o hasnt open)     combo=combo+2;
        if ((o has container && (o has open || o has transparent))) {
            objectloop(i in o) {
                if (i hasnt concealed && i hasnt scenery) {
                    j = true; break;
                }
            }
            if (~~j) combo=combo+4;
        }
        if (combo) L__M(##ListMiscellany, combo, o);
    }   ! end of PARTINV_BIT processing

    if (c_style & FULLINV_BIT) {
        if (o.invent && RunRoutines(o, invent))
            if (c_style & NEWLINE_BIT) ""; else rtrue;

        if (o has light && o has worn) { L__M(##ListMiscellany, 8, o);  parenth_flag = true; }
        else {
            if (o has light)           { L__M(##ListMiscellany, 9, o);  parenth_flag = true; }
            if (o has worn)            { L__M(##ListMiscellany, 10, o); parenth_flag = true; }
        }

        if (o has container)
            if (o has openable) {
                if (parenth_flag) print (string) AND__TX;
                else              L__M(##ListMiscellany, 11, o);
                if (o has open)
                    if (child(o)) L__M(##ListMiscellany, 12, o);
                    else          L__M(##ListMiscellany, 13, o);
                else
                    if (o has lockable && o has locked) L__M(##ListMiscellany, 15, o);
                    else                                L__M(##ListMiscellany, 14, o);
                parenth_flag = true;
            }
            else
                if (child(o)==0 && o has transparent)
                    if (parenth_flag) L__M(##ListMiscellany, 16, o);
                    else              L__M(##ListMiscellany, 17, o);

        if (parenth_flag) print ")";
    }   ! end of FULLINV_BIT processing

    if (c_style & CONCEAL_BIT) {
        child_count = 0;
        objectloop (p in o)
            if (p hasnt concealed && p hasnt scenery) { child_count++; eldest_child = p; }
    }
    else { child_count = children(o); eldest_child = child(o); }

    if (child_count && (c_style & ALWAYS_BIT)) {
        if (c_style & ENGLISH_BIT) L__M(##ListMiscellany, 18, o);
        recurse_flag = true;
    }

    if (child_count && (c_style & RECURSE_BIT)) {
        if (o has supporter) {
            if (c_style & ENGLISH_BIT) {
                if (c_style & TERSE_BIT) L__M(##ListMiscellany, 19, o);
                else                     L__M(##ListMiscellany, 20, o);
                if (o has animate)       print (string) WHOM__TX;
                else                     print (string) WHICH__TX;
            }
            recurse_flag = true;
        }
        if (o has container && (o has open || o has transparent)) {
            if (c_style & ENGLISH_BIT) {
                if (c_style & TERSE_BIT) L__M(##ListMiscellany, 21, o);
                else                     L__M(##ListMiscellany, 22, o);
                if (o has animate)       print (string) WHOM__TX;
                else                     print (string) WHICH__TX;
                }
            recurse_flag = true;
        }
    }

    if (recurse_flag && (c_style & ENGLISH_BIT))
        if (child_count > 1 || eldest_child has pluralname) Tense(ARE2__TX, WERE2__TX);
        else                                                Tense(IS2__TX, WAS2__TX);

    if (c_style & NEWLINE_BIT) new_line;

    if (recurse_flag) {
        o = child(o);
        #Ifdef TARGET_ZCODE;
        @push lt_value; @push listing_together; @push listing_size;
        #Ifnot; ! TARGET_GLULX;
        @copy lt_value sp; @copy listing_together sp; @copy listing_size sp;
        #Endif;
        lt_value = 0;   listing_together = 0;   listing_size = 0;
        WriteListR(o, depth+1, stack_p);
        #Ifdef TARGET_ZCODE;
        @pull listing_size; @pull listing_together; @pull lt_value;
        #Ifnot; ! TARGET_GLULX;
        @copy sp listing_size; @copy sp listing_together; @copy sp lt_value;
        #Endif;
        if (c_style & TERSE_BIT) print ")";
    }
];

! ----------------------------------------------------------------------------
! LoopWithinObject(rtn,obj,arg)
!
! rtn is the address of a user-supplied routine.
! obj is an optional parent object whose dependents are to be processed; the
! default is the current actor (normally the player).
! arg is an optional argument passed to the rtn; this can be a single variable
! or constant, or the address of an array (which enables multiple values to be
! passed and returned).
!
! For each object o which is a child, grandchild, great-grandchild, etc, of the
! original obj, LoopWithinObject() calls rtn(o,arg).
!
! The rtn should perform any appropriate testing or processing on each object o,
! using the optional arg value if necessary. If the rtn returns true (or any
! positive value), the children of o, if any, are also tested; those children
! are skipped if rtn returns false. To terminate the loop before all objects
! have been processed, rtn should return a large negative number (eg -99).
!
! To deal with supporters and open containers, so that objects are processed
! only if they are accessible to the player, rtn might end with these
! statements:
!   if ((o has transparent or supporter) || (o has container && o has open)) rtrue;
!   rfalse;
! or alternatively with:
!   c_style = RECURSE_BIT; return WillRecurs(o);
!
! LoopWithinObject() returns the number of objects which have been processed.
! ----------------------------------------------------------------------------

[ LoopWithinObject rtn obj arg
    n o x y;
    if (obj == 0) obj = actor;
    o = child(obj);
    while (o) {
        y = parent(o); n++;
        x = rtn(o, arg);    ! user-supplied routine returning x.
                            ! if x < 0: skip up to next parent
                            ! if x = 0: jump across to next sibling
                            ! if x > 0: continue down to child objects
        if (y ~= parent(o)) { RunTimeError(15, o); rfalse; }
        if (x > 0 && child(o)) o = child(o);
        else
            while (o) {
                if (++x > 0 && sibling(o)) { o = sibling(o); break; }
                o = parent(o);
                if (o == obj) return n;
            }
    }
];


! ----------------------------------------------------------------------------
!  Much better menus can be created using one of the optional library
!  extensions.  These are provided for compatibility with previous practice:
! ----------------------------------------------------------------------------

[ LowKey_Menu menu_choices EntryR ChoiceR lines main_title i j;
    menu_nesting++;

  .LKRD;

    menu_item = 0;
    lines = EntryR();
    main_title = item_name;

    print "--- "; print (string) main_title; print " ---^^";

    if (menu_choices ofclass Routine) menu_choices();
    else                              print (string) menu_choices;

    for (::) {
        L__M(##Miscellany, 52, lines);
        print "> ";

        #Ifdef TARGET_ZCODE;
        #IfV3;
        read buffer parse;
        #Ifnot;
        read buffer parse DrawStatusLine;
        #Endif; ! V3
        j = parse->1; ! number of words
        #Ifnot; ! TARGET_GLULX;
        KeyboardPrimitive(buffer, parse);
        j = parse-->0; ! number of words
        #Endif; ! TARGET_

        i = parse-->1;
        if (j == 0 || (i == QUIT1__WD or QUIT2__WD)) {
            menu_nesting--; if (menu_nesting > 0) rfalse;
            if (deadflag == 0) <<Look>>;
            rfalse;
        }
        i = TryNumber(1);
        if (i == 0) jump LKRD;
        if (i < 1 || i > lines) continue;
        menu_item = i;
        j = ChoiceR();
        if (j == 2) jump LKRD;
        if (j == 3) rfalse;
    }
];

#Ifdef TARGET_ZCODE;

#IfV3;

[ DoMenu menu_choices EntryR ChoiceR; LowKey_Menu(menu_choices, EntryR, ChoiceR); ];

#Endif; ! V3

#IfV5;

[ DoMenu menu_choices EntryR ChoiceR
         lines main_title main_wid cl i j oldcl pkey ch cw y x;
    if (pretty_flag == 0) return LowKey_Menu(menu_choices, EntryR, ChoiceR);
    menu_nesting++;
    menu_item = 0;
    lines = EntryR();
    main_title = item_name; main_wid = item_width;
    cl = 7;

  .ReDisplay;

    oldcl = 0;
    @erase_window $ffff;
    #Iftrue (#version_number == 6);
    @set_cursor -1;
    ch = HDR_FONTWUNITS->0;
    #Ifnot;
    ch = 1;
    #Endif;
    i = ch * (lines+7);
    @split_window i;
    i = HDR_SCREENWCHARS->0;
    if (i == 0) i = 80;
    @set_window 1;
    @set_cursor 1 1;

    #Iftrue (#version_number == 6);
    @set_font 4 -> cw;
    cw = HDR_FONTHUNITS->0;
    #Ifnot;
    cw = 1;
    #Endif;

    style reverse;
    spaces(i); j=1+(i/2-main_wid)*cw;
    @set_cursor 1 j;
    print (string) main_title;
    y=1+ch; @set_cursor y 1; spaces(i);
    x=1+cw; @set_cursor y x; print (string) NKEY__TX;
    j=1+(i-13)*cw; @set_cursor y j; print (string) PKEY__TX;
    y=y+ch; @set_cursor y 1; spaces(i);
    @set_cursor y x; print (string) RKEY__TX;
    j=1+(i-18)*cw; @set_cursor y j;

    if (menu_nesting == 1) print (string) QKEY1__TX;
    else                   print (string) QKEY2__TX;
    style roman;
    y = y+2*ch;
    @set_cursor y x; font off;

    if (menu_choices ofclass String) print (string) menu_choices;
    else                             menu_choices();

    x = 1+3*cw;
    for (::) {
        if (cl ~= oldcl) {
            if (oldcl>0) {
                y=1+(oldcl-1)*ch; @set_cursor y x; print " ";
            }
            y=1+(cl-1)*ch; @set_cursor y x; print ">";
        }

        oldcl = cl;
        @read_char 1 -> pkey;
        if (pkey == NKEY1__KY or NKEY2__KY or 130) {
            cl++; if (cl == 7+lines) cl = 7; continue;
        }
        if (pkey == PKEY1__KY or PKEY2__KY or 129) {
            cl--; if (cl == 6) cl = 6+lines; continue;
        }
        if (pkey == QKEY1__KY or QKEY2__KY or 27 or 131) break;
        if (pkey == 10 or 13 or 132) {
            @set_window 0; font on;
            new_line; new_line; new_line;

            menu_item = cl-6;
            EntryR();

            @erase_window $ffff;
            @split_window ch;
            i = HDR_SCREENWCHARS->0; if ( i== 0) i = 80;
            @set_window 1; @set_cursor 1 1; style reverse; spaces(i);
            j=1+(i/2-item_width)*cw;
            @set_cursor 1 j;
            print (string) item_name;
            style roman; @set_window 0; new_line;

            i = ChoiceR();
            if (i == 2) jump ReDisplay;
            if (i == 3) break;

            L__M(##Miscellany, 53);
            @read_char 1 -> pkey; jump ReDisplay;
        }
    }

    menu_nesting--; if (menu_nesting > 0) rfalse;
    font on; @set_cursor 1 1;
    @erase_window $ffff; @set_window 0;
    #Iftrue (#version_number == 6);
    @set_cursor -2;
    #Endif;
    new_line; new_line; new_line;
    if (deadflag == 0) <<Look>>;
];

#Endif; ! V5

#Ifnot; ! TARGET_GLULX

[ DoMenu menu_choices EntryR ChoiceR
    winwid winhgt lines main_title main_wid cl i oldcl pkey;

    if (pretty_flag == 0 || gg_statuswin == 0) return LowKey_Menu(menu_choices, EntryR, ChoiceR);

    menu_nesting++;
    menu_item = 0;
    lines = EntryR();
    main_title = item_name;
    main_wid = item_width;

    cl = 0;

    ! If we printed "hit arrow keys" here, it would be appropriate to
    ! check for the availability of Glk input keys. But we actually
    ! print "hit N/P/Q". So it's reasonable to silently accept Glk
    ! arrow key codes as secondary options.

  .ReDisplay;

    glk_window_clear(gg_statuswin);
    glk_window_clear(gg_mainwin);
    glk_set_window(gg_statuswin);
    StatusLineHeight(lines+7);
    glk_window_get_size(gg_statuswin, gg_arguments, gg_arguments+4);
    winwid = gg_arguments-->0;
    winhgt = gg_arguments-->1;
    glk_set_style(style_Subheader);
    glk_window_move_cursor(gg_statuswin, winwid/2-main_wid, 0);
    print (string) main_title;
    glk_window_move_cursor(gg_statuswin, 1, 1);
    print (string) NKEY__TX;
    glk_window_move_cursor(gg_statuswin, winwid-13, 1);
    print (string) PKEY__TX;
    glk_window_move_cursor(gg_statuswin, 1, 2);
    print (string) RKEY__TX;
    glk_window_move_cursor(gg_statuswin, winwid-18, 2);
    if (menu_nesting == 1) print (string) QKEY1__TX;
    else                   print (string) QKEY2__TX;
    glk_set_style(style_Normal);
    glk_window_move_cursor(gg_statuswin, 1, 4);
    if (menu_choices ofclass String) print (string) menu_choices;
    else                             menu_choices();

    oldcl = -1;

    for (::) {
        if (cl ~= oldcl) {
            if (cl < 0 || cl >= lines) cl = 0;
            if (oldcl >= 0) {
                glk_window_move_cursor(gg_statuswin, 3, oldcl+6);
                print (char) ' ';
            }
            oldcl = cl;
            glk_window_move_cursor(gg_statuswin, 3, oldcl+6);
            print (char) '>';
        }
        pkey = KeyCharPrimitive(gg_statuswin, true);
        if (pkey == $80000000) jump ReDisplay;
        if (pkey == NKEY1__KY or NKEY2__KY or $fffffffb) {
            cl++;
            if (cl >= lines) cl = 0;
            continue;
        }
        if (pkey == PKEY1__KY or PKEY2__KY or $fffffffc) {
            cl--;
            if (cl < 0) cl = lines-1;
            continue;
        }
        if (pkey == QKEY1__KY or QKEY2__KY or $fffffff8 or $fffffffe) break;
        if (pkey == $fffffffa or $fffffffd) {
            glk_set_window(gg_mainwin);
            new_line; new_line; new_line;
            menu_item = cl+1;
            EntryR();
            glk_window_clear(gg_statuswin);
            glk_window_clear(gg_mainwin);
            glk_set_window(gg_statuswin);
            StatusLineHeight(1);
            glk_window_get_size(gg_statuswin, gg_arguments, gg_arguments+4);
            winwid = gg_arguments-->0;
            winhgt = gg_arguments-->1;
            glk_set_style(style_Subheader);
            glk_window_move_cursor(gg_statuswin, winwid/2-item_width, 0);
            print (string) item_name;
            glk_set_style(style_Normal);
            glk_set_window(gg_mainwin);
            new_line;
            i = ChoiceR();
            if (i == 2) jump ReDisplay;
            if (i == 3) break;
            L__M(##Miscellany, 53);
            pkey = KeyCharPrimitive(gg_mainwin, 1);
            jump ReDisplay;
        }
    }

    ! done with this menu...
    menu_nesting--;
    if (menu_nesting > 0) rfalse;
    glk_set_window(gg_mainwin);
    glk_window_clear(gg_mainwin);
    new_line; new_line; new_line;
    if (deadflag == 0) <<Look>>;
];

#Endif; ! TARGET_

! ----------------------------------------------------------------------------
!   A cunning routine (which could have been a daemon, but isn't, for the
!   sake of efficiency) to move objects which could be in many rooms about
!   so that the player never catches one not in place
! ----------------------------------------------------------------------------

[ MoveFloatingObjects i k l m address flag;
    if (location == player or nothing) return;
    objectloop (i) {
        address = i.&found_in;
        if (address && i hasnt non_floating && ~~IndirectlyContains(player, i)) {
            if (metaclass(address-->0) == Routine)
                flag = i.found_in();
            else {
                flag = false;
                k = i.#found_in/WORDSIZE;
                for (l=0 : l<k : l++) {
                    m = address-->l;
                    if ((m in Class && location ofclass m) ||
                            m == location || m in location) {
                        flag = true;
                        break;
                    }
                }
            }
            if (flag) {
                if (i notin location) move i to location;
            } else {
                if (parent(i)) remove i;
            }
        }
    }
];

! ----------------------------------------------------------------------------
!   Two little routines for moving the player safely.
! ----------------------------------------------------------------------------

[ PlayerTo newplace flag;
    NoteDeparture();
    move player to newplace;
    while (parent(newplace)) newplace = parent(newplace);
    location = real_location = newplace;
    MoveFloatingObjects(); AdjustLight(1);
    switch (flag) {
      0:    <Look>;
      1:    NoteArrival(); ScoreArrival();
      2:    LookSub(1);
    }
];

[ MovePlayer direc; <Go direc>; <Look>; ];

! ----------------------------------------------------------------------------
!   The handy YesOrNo routine, and some "meta" verbs
! ----------------------------------------------------------------------------

[ YesOrNo noStatusRedraw
    i j;
    for (::) {
        #Ifdef TARGET_ZCODE;
        if (location == nothing || parent(player) == nothing || noStatusRedraw)
            read buffer parse;
        else read buffer parse DrawStatusLine;
        j = parse->1;
        #Ifnot; ! TARGET_GLULX;
        noStatusRedraw = 0; ! suppress warning
        KeyboardPrimitive(buffer, parse);
        j = parse-->0;
        #Endif; ! TARGET_
        if (j) { ! at least one word entered
            i = parse-->1;
            if (i == YES1__WD or YES2__WD or YES3__WD) rtrue;
            if (i == NO1__WD  or NO2__WD  or NO3__WD) rfalse;
        }
        L__M(##Quit, 1); print "> ";
    }
];

#Ifdef TARGET_ZCODE;

[ QuitSub;
    L__M(##Quit, 2);
    if (YesOrNo()) quit;
];

[ RestartSub;
    L__M(##Restart, 1);
    if (YesOrNo()) { @restart; L__M(##Restart, 2); }
];

[ RestoreSub;
    restore Rmaybe;
    return L__M(##Restore, 1);
  .RMaybe;
    L__M(##Restore, 2);
];

[ SaveSub flag;
    #IfV5;
    @save -> flag;
    switch (flag) {
      0: L__M(##Save, 1);
      1: L__M(##Save, 2);
      2:
        RestoreColours();
        L__M(##Restore, 2);
    }
    #Ifnot;
    save Smaybe;
    return L__M(##Save, 1);
  .SMaybe;
    L__M(##Save, 2);
    #Endif; ! V5
];

[ VerifySub;
    @verify ?Vmaybe;
    jump Vwrong;
  .Vmaybe;
    return L__M(##Verify, 1);
  .Vwrong;
    L__M(##Verify, 2);
];

[ ScriptOnSub;
    transcript_mode = ((HDR_GAMEFLAGS-->0) & 1);
    if (transcript_mode) return L__M(##ScriptOn, 1);
    @output_stream 2;
    if (((HDR_GAMEFLAGS-->0) & 1) == 0) return L__M(##ScriptOn, 3);
    L__M(##ScriptOn, 2); VersionSub();
    transcript_mode = true;
];

[ ScriptOffSub;
    transcript_mode = ((HDR_GAMEFLAGS-->0) & 1);
    if (transcript_mode == false) return L__M(##ScriptOff, 1);
    L__M(##ScriptOff, 2);
    @output_stream -2;
    if ((HDR_GAMEFLAGS-->0) & 1) return L__M(##ScriptOff, 3);
    transcript_mode = false;
];

[ CommandsOnSub;
    @output_stream 4;
    xcommsdir = 1;
    L__M(##CommandsOn, 1);
];

[ CommandsOffSub;
    if (xcommsdir == 1) @output_stream -4;
    xcommsdir = 0;
    L__M(##CommandsOff, 1);
];

[ CommandsReadSub;
    @input_stream 1;
    xcommsdir = 2;
    L__M(##CommandsRead, 1);
];

#Ifnot; ! TARGET_GLULX;

[ QuitSub;
    L__M(##Quit, 2);
    if (YesOrNo()) quit;
];

[ RestartSub;
    L__M(##Restart,1);
    if (YesOrNo()) { @restart; L__M(##Restart, 2); }
];

[ RestoreSub res fref;
    fref = glk_fileref_create_by_prompt($01, $02, 0);
    if (fref == 0) jump RFailed;
    gg_savestr = glk_stream_open_file(fref, $02, GG_SAVESTR_ROCK);
    glk_fileref_destroy(fref);
    if (gg_savestr == 0) jump RFailed;
    @restore gg_savestr res;
    glk_stream_close(gg_savestr, 0);
    gg_savestr = 0;
  .RFailed;
    L__M(##Restore, 1);
];

[ SaveSub res fref;
    fref = glk_fileref_create_by_prompt($01, $01, 0);
    if (fref == 0) jump SFailed;
    gg_savestr = glk_stream_open_file(fref, $01, GG_SAVESTR_ROCK);
    glk_fileref_destroy(fref);
    if (gg_savestr == 0) jump SFailed;
    @save gg_savestr res;
    if (res == -1) {
        ! The player actually just typed "restore". We're going to print
        !  L__M(##Restore,2); the Z-Code Inform library does this correctly
        ! now. But first, we have to recover all the Glk objects; the values
        ! in our global variables are all wrong.
        GGRecoverObjects();
        glk_stream_close(gg_savestr, 0);
        gg_savestr = 0;
        return L__M(##Restore, 2);
    }
    glk_stream_close(gg_savestr, 0);
    gg_savestr = 0;
    if (res == 0) return L__M(##Save, 2);
  .SFailed;
    L__M(##Save, 1);
];

[ VerifySub res;
    @verify res;
    if (res == 0) return L__M(##Verify, 1);
    L__M(##Verify, 2);
];

[ ScriptOnSub;
    if (gg_scriptstr) return L__M(##ScriptOn, 1);
    if (gg_scriptfref == 0) {
        gg_scriptfref = glk_fileref_create_by_prompt($102, $05, GG_SCRIPTFREF_ROCK);
        if (gg_scriptfref == 0) jump S1Failed;
    }
    gg_scriptstr = glk_stream_open_file(gg_scriptfref, $05, GG_SCRIPTSTR_ROCK);
    if (gg_scriptstr == 0) jump S1Failed;
    glk_window_set_echo_stream(gg_mainwin, gg_scriptstr);
    L__M(##ScriptOn, 2);
    VersionSub();
    return;
  .S1Failed;
    L__M(##ScriptOn, 3);
];

[ ScriptOffSub;
    if (gg_scriptstr == 0) return L__M(##ScriptOff,1);
    L__M(##ScriptOff, 2);
    glk_stream_close(gg_scriptstr, 0);
    gg_scriptstr = 0;
];

[ CommandsOnSub fref;
    if (gg_commandstr) {
        if (gg_command_reading) return L__M(##CommandsOn, 2);
        else                    return L__M(##CommandsOn, 3);
    }
    fref = glk_fileref_create_by_prompt($103, $01, 0);
    if (fref == 0) return L__M(##CommandsOn, 4);
    gg_command_reading = false;
    gg_commandstr = glk_stream_open_file(fref, $01, GG_COMMANDWSTR_ROCK);
    glk_fileref_destroy(fref);
    if (gg_commandstr == 0) return L__M(##CommandsOn, 4);
    L__M(##CommandsOn, 1);
];

[ CommandsOffSub;
    if (gg_commandstr == 0) return L__M(##CommandsOff, 2);
    if (gg_command_reading) return L__M(##CommandsRead, 5);
    glk_stream_close(gg_commandstr, 0);
    gg_commandstr = 0;
    gg_command_reading = false;
    L__M(##CommandsOff, 1);
];

[ CommandsReadSub fref;
    if (gg_commandstr) {
        if (gg_command_reading) return L__M(##CommandsRead, 2);
        else                    return L__M(##CommandsRead, 3);
    }
    fref = glk_fileref_create_by_prompt($103, $02, 0);
    if (fref == 0) return L__M(##CommandsRead, 4);
    gg_command_reading = true;
    gg_commandstr = glk_stream_open_file(fref, $02, GG_COMMANDRSTR_ROCK);
    glk_fileref_destroy(fref);
    if (gg_commandstr == 0) return L__M(##CommandsRead, 4);
    L__M(##CommandsRead, 1);
];

#Endif; ! TARGET_;

[ NotifyOnSub;  notify_mode = true; L__M(##NotifyOn);  ];
[ NotifyOffSub; notify_mode = false; L__M(##NotifyOff); ];

[ Places1Sub i j k;
    L__M(##Places, 1);
    objectloop (i has visited) j++;
    objectloop (i has visited) {
        print (name) i; k++;
        if (k == j) return L__M(##Places, 2);
        if (k == j-1) print (SerialComma) j, (string) AND__TX;
        else          print (string) COMMA__TX;
    }
];

[ Objects1Sub i j f;
    L__M(##Objects, 1);
    objectloop (i has moved) {
       f = 1; print (the) i; j = parent(i);
        if (j) {
           if (j == player) {
               if (i has worn) L__M(##Objects, 3, j, i);
               else            L__M(##Objects, 4, j, i);
                jump Obj__Ptd;
            }
            if (j has animate)   { L__M(##Objects, 5, j, i); jump Obj__Ptd; }
            if (j has visited)   { L__M(##Objects, 6, j, i); jump Obj__Ptd; }
            if (j has container) { L__M(##Objects, 8, j, i); jump Obj__Ptd; }
            if (j has supporter) { L__M(##Objects, 9, j, i); jump Obj__Ptd; }
            if (j has enterable) { L__M(##Objects, 7, j, i); jump Obj__Ptd; }
        }
        L__M(##Objects, 10, j, i);

      .Obj__Ptd;

        new_line;
    }
    if (f == 0) L__M(##Objects, 2);
];

! ----------------------------------------------------------------------------
!   The scoring system
! ----------------------------------------------------------------------------

[ ScoreSub;
    #Ifdef NO_SCORE;
    if (deadflag == 0) L__M(##Score, 2);
    #Ifnot;
    if (deadflag) new_line;
    L__M(##Score, 1);
    if(PrintRank() == false) LibraryExtensions.RunAll(ext_printrank);
    #Endif; ! NO_SCORE
];

#Ifndef TaskScore;
[ TaskScore i;
    return task_scores->i;
];
#Endif;

[ Achieved num;
    if (task_done->num == 0) {
        task_done->num = 1;
        score = score + TaskScore(num);
    }
];

[ PANum m n;
    print "  ";
    n = m;
    if (n < 0)    { n = -m; n = n*10; }
    if (n < 10)   { print "   "; jump Panuml; }
    if (n < 100)  { print "  "; jump Panuml; }
    if (n < 1000) { print " "; }

  .Panuml;

    print m, " ";
];

[ FullScoreSub i;
    ScoreSub();
    if (score == 0 || TASKS_PROVIDED == 1) rfalse;
    new_line;
    L__M(##FullScore, 1);
    for (i=0 : i<NUMBER_TASKS : i++)
        if (task_done->i == 1) {
            PANum(TaskScore(i));
            if(PrintTaskName(i) == false)
                LibraryExtensions.RunAll(ext_printtaskname,i);
        }
    if (things_score) {
        PANum(things_score);
        L__M(##FullScore, 2);
    }
    if (places_score) {
        PANum(places_score);
        L__M(##FullScore, 3);
    }
    new_line; PANum(score); L__M(##FullScore, 4);
];

! ----------------------------------------------------------------------------
!   Real verbs start here: Inventory
! ----------------------------------------------------------------------------

[ InvWideSub;
    if (actor == player)
        inventory_style = ENGLISH_BIT+FULLINV_BIT+RECURSE_BIT;
    else
        inventory_style = ENGLISH_BIT+PARTINV_BIT;
    <Inv, actor>;
    inventory_style = 0;
];

[ InvTallSub;
    if (actor == player)
        inventory_style = NEWLINE_BIT+INDENT_BIT+FULLINV_BIT+RECURSE_BIT;
    else
        inventory_style = NEWLINE_BIT+INDENT_BIT+PARTINV_BIT;
    <Inv, actor>;
    inventory_style = 0;
];

[ InvSub x;
    if (child(actor) == 0)   return L__M(##Inv, 1);
    if (inventory_style == 0)
        if (actor == player) return InvTallSub();
        else                 return InvWideSub();
    L__M(##Inv, 2);
    if (inventory_style & NEWLINE_BIT) L__M(##Inv, 3); else print " ";

    WriteListFrom(child(actor), inventory_style, 1);
    if (inventory_style & ENGLISH_BIT) L__M(##Inv, 4);

    #Ifndef MANUAL_PRONOUNS;
    objectloop (x in player) PronounNotice(x);
    #Endif;
    x = 0; ! To prevent a "not used" error
    AfterRoutines();
];

! ----------------------------------------------------------------------------
!   The object tree and determining the possibility of moves
! ----------------------------------------------------------------------------

[ CommonAncestor o1 o2 i j;
    ! Find the nearest object indirectly containing o1 and o2,
    ! or return 0 if there is no common ancestor.
    i = o1;
    while (i) {
        j = o2;
        while (j) {
            if (j == i) return i;
            j = parent(j);
        }
        i = parent(i);
    }
    return 0;
];

[ IndirectlyContains o1 o2;
    ! Does o1 indirectly contain o2?  (Same as testing if their common ancestor is o1.)
    while (o2) {
        if (o1 == o2) rtrue;
        if (o2 ofclass Class) rfalse;
        o2 = parent(o2);
    }
    rfalse;
];

[ ObjectScopedBySomething item i j k l m;
    i = item;
    objectloop (j .& add_to_scope) {
        l = j.&add_to_scope;
        k = (j.#add_to_scope)/WORDSIZE;
        if (l-->0 ofclass Routine) continue;
        for (m=0 : m<k : m++)
            if (l-->m == i) return j;
    }
    rfalse;
];

[ ObjectIsUntouchable item flag1 flag2 ancestor i;
    ! Determine if there's any barrier preventing the actor from moving
    ! things to "item".  Return false if no barrier; otherwise print a
    ! suitable message and return true.
    ! If flag1 is set, do not print any message.
    ! If flag2 is set, also apply Take/Remove restrictions.

    ! If the item has been added to scope by something, it's first necessary
    ! for that something to be touchable.

    ancestor = CommonAncestor(actor, item);
    if (ancestor == 0) {
        ancestor = item;
        while (ancestor && (i = ObjectScopedBySomething(ancestor)) == 0)
            ancestor = parent(ancestor);
        if (i) {
            if (ObjectIsUntouchable(i, flag1, flag2)) return;
            ! An item immediately added to scope
        }
    }
    else

    ! First, a barrier between the actor and the ancestor.  The actor
    ! can only be in a sequence of enterable objects, and only closed
    ! containers form a barrier.

    if (actor ~= ancestor) {
        i = parent(actor);
        while (i ~= ancestor) {
            if (i has container && i hasnt open) {
                if (flag1) rtrue;
                return L__M(##Take, 9, i, noun);
            }
            i = parent(i);
        }
    }

    ! Second, a barrier between the item and the ancestor.  The item can
    ! be carried by someone, part of a piece of machinery, in or on top
    ! of something and so on.

    i = parent(item);
    if (item ~= ancestor && i ~= player) {
        while (i ~= ancestor) {
            if (flag2 && i hasnt container && i hasnt supporter) {
                if (i has animate) {
                    if (flag1) rtrue;
                    return L__M(##Take, 6, i, noun);
                }
                if (i has transparent) {
                    if (flag1) rtrue;
                    return L__M(##Take, 7, i, noun);
                }
                if (flag1) rtrue;
                return L__M(##Take, 8, item, noun);
            }
            if (i has container && i hasnt open) {
                if (flag1) rtrue;
                return L__M(##Take, 9, i, noun);
            }
            i = parent(i);
        }
    }
    rfalse;
];

[ AttemptToTakeObject item
    ancestor after_recipient i k;
    ! Try to transfer the given item to the actor: return false
    ! if successful, true if unsuccessful, printing a suitable message
    ! in the latter case.
    ! People cannot ordinarily be taken.
    if (item == actor) return L__M(##Take, 2, noun);
    if (item has animate) return L__M(##Take, 3, item);

    ancestor = CommonAncestor(actor, item);

    if (ancestor == 0) {
        i = ObjectScopedBySomething(item);
        if (i) ancestor = CommonAncestor(actor, i);
    }

    ! Is the actor indirectly inside the item?
    if (ancestor == item) return L__M(##Take, 4, item);

    ! Does the actor already directly contain the item?
    if (item in actor) return L__M(##Take, 5, item);

    ! Can the actor touch the item, or is there (e.g.) a closed container
    ! in the way?
    if (ObjectIsUntouchable(item, false, true)) rtrue;

    ! The item is now known to be accessible.

    ! Consult the immediate possessor of the item, if it's in a container
    ! which the actor is not in.

    i = parent(item);
    if (i && i ~= ancestor && (i has container or supporter)) {
        after_recipient = i;
        k = action; action = ##LetGo;
        if (RunRoutines(i, before)) { action = k; rtrue; }
        action = k;
    }

    if (item has scenery) return L__M(##Take, 10, item);
    if (item has static)  return L__M(##Take, 11, item);

    ! The item is now known to be available for taking.  Is the player
    ! carrying too much?  If so, possibly juggle items into the rucksack
    ! to make room.

    if (ObjectDoesNotFit(item, actor) ||
        LibraryExtensions.RunWhile(ext_objectdoesnotfit, false, item, actor)) return;
    if (AtFullCapacity(item, actor)) return L__M(##Take, 12, item);

    ! Transfer the item.

    move item to actor; give item ~worn;

    ! Send "after" message to the object letting go of the item, if any.

    if (after_recipient) {
        k = action; action = ##LetGo;
        if (RunRoutines(after_recipient, after)) { action = k; rtrue; }
        action = k;
    }
    rfalse;
];

[ AtFullCapacity n s
    obj k;
    n = n; ! suppress compiler warning
    if (s == actor) {
        objectloop (obj in s)
            if (obj hasnt worn) k++;
    } else
        k = children(s);

    if (k < RunRoutines(s, capacity) || (s == player && RoomInSack())) rfalse;
];

[ RoomInSack
    obj ks;
    if (SACK_OBJECT && SACK_OBJECT in player) {
        ks = keep_silent; keep_silent = 2;
        for (obj=youngest(player) : obj : obj=elder(obj))
            if (obj ~= SACK_OBJECT && obj hasnt worn or light) {
                <Insert obj SACK_OBJECT>;
                if (obj in SACK_OBJECT) {
                    keep_silent = ks;
                    return L__M(##Take, 13, obj, SACK_OBJECT);
                }
            }
        keep_silent = ks;
    }
    rfalse;
];

! ----------------------------------------------------------------------------
!   Support for implicit actions
! ----------------------------------------------------------------------------

[ CheckImplicitAction act o1 o2
    sav_act sav_noun sav_sec res;
    if (o1 provides before_implicit) {
        sav_act  = action; action = act;
        sav_noun = noun;   noun   = o1;
        if (o2) { sav_sec  = second; second = o2; }
        res = RunRoutines(o1, before_implicit);
        action = sav_act; noun = sav_noun;
        if (sav_sec) second = sav_sec;
    }
    else {
	if (no_implicit_actions)
            res = 2;
        else
            res = 0;
    }
    return res;
];

[ ImplicitTake obj
    res ks supcon;
    switch (metaclass(obj)) { Class, String, Routine, nothing: rfalse; }
    if (obj in actor) rfalse;
    if (action_to_be == ##Drop && ~~IndirectlyContains(actor, obj)) rfalse;
    res = CheckImplicitAction(##Take, obj);
    ! 0 = Take object, Tell the user (normal default)
    ! 1 = Take object, don't Tell
    ! 2 = don't Take object  continue       (default with no_implicit_actions)
    ! 3 = don't Take object, don't continue
    if (res >= 2) rtrue;
    if (parent(obj) && parent(obj) has container or supporter) supcon = parent(obj);
    ks = keep_silent; keep_silent = 2; AttemptToTakeObject(obj); keep_silent = ks;
    if (obj notin actor) rtrue;
    if (res == 0 && ~~keep_silent)
        if (supcon) L__M(##Miscellany, 58, obj, supcon);
        else        L__M(##Miscellany, 26, obj);
    rfalse;
];

[ ImplicitExit obj
    res ks;
    if (parent(obj) == nothing) rfalse;
    res = CheckImplicitAction(##Exit, obj);
    ! 0 = Exit object, Tell the user (normal default)
    ! 1 = Exit object, don't Tell
    ! 2 = don't Take object  continue       (default with no_implicit_actions)
    ! 3 = don't Take object, don't continue
    if (res >= 2) rtrue;
    ks = keep_silent; keep_silent = 2; <Exit obj, actor>; keep_silent = ks;
    if (parent(actor) == obj) rtrue;
    if (res == 0 && ~~keep_silent) L__M(##Exit, 5, obj);
    rfalse;
];

[ ImplicitClose obj
    res ks;
    if (obj hasnt open) rfalse;
    res = CheckImplicitAction(##Close, obj);
    ! 0 = Close object, Tell the user (normal default)
    ! 1 = Close object, don't Tell
    ! 2 = don't Take object  continue       (default with no_implicit_actions)
    ! 3 = don't Take object, don't continue
    if (res >= 2) rtrue;
    ks = keep_silent; keep_silent = 2; <Close obj, actor>; keep_silent = ks;
    if (obj has open) rtrue;
    if (res == 0 && ~~keep_silent) L__M(##Close, 4, obj);
    rfalse;
];

[ ImplicitOpen obj
    res temp;
    if (obj has open) rfalse;
    res = CheckImplicitAction(##Open, obj);
    ! 0 = Open object, Tell the user (normal default)
    ! 1 = Open object, don't Tell
    ! 2 = don't Take object  continue       (default with no_implicit_actions)
    ! 3 = don't Take object, don't continue
    if (res >= 2) rtrue;
    if (obj has locked) rtrue;
    temp = keep_silent; keep_silent = 2; <Open obj, actor>; keep_silent = temp;
    if (obj hasnt open) rtrue;
    if (res == 0 && ~~keep_silent) L__M(##Open, 6, obj);
    temp = action; action = ##Open; AfterRoutines(); action = temp;
    rfalse;
];

[ ImplicitUnlock obj;
    if (obj has locked) rtrue;
    rfalse;
];

[ ImplicitDisrobe obj
    res ks;
    if (obj hasnt worn) rfalse;
    res = CheckImplicitAction(##Disrobe, obj);
    ! 0 = Take off object, Tell the user (normal default)
    ! 1 = Take off object, don't Tell
    ! 2 = don't Take object  continue       (default with no_implicit_actions)
    ! 3 = don't Take object, don't continue
    if (res >= 2) rtrue;
    ks = keep_silent; keep_silent = 1; <Disrobe obj, actor>; keep_silent = ks;
    if (obj has worn && obj in actor) rtrue;
    if (res == 0 && ~~keep_silent) L__M(##Drop, 3, obj);
    rfalse;
];


! ----------------------------------------------------------------------------
!   Object movement verbs
! ----------------------------------------------------------------------------

[ TakeSub;
    if (onotheld_mode == 0 || noun notin actor)
        if (AttemptToTakeObject(noun)) return;
    if (AfterRoutines()) return;
    notheld_mode = onotheld_mode;
    if (notheld_mode == 1 || keep_silent) return;
    L__M(##Take, 1, noun);
];

[ RemoveSub i;
    i = parent(noun);
    if (i && i has container && i hasnt open && ImplicitOpen(i)) return L__M(##Remove, 1, i);
    if (i ~= second)   return L__M(##Remove, 2, noun);
    if (i has animate) return L__M(##Take, 6, i, noun);

    if (AttemptToTakeObject(noun)) rtrue;

    action = ##Remove; if (AfterRoutines()) return;
    action = ##Take;   if (AfterRoutines()) return;
    if (keep_silent) return;
    L__M(##Remove, 3, noun);
];

[ DropSub;
    if (noun == actor)         return L__M(##PutOn, 4, noun);
    if (noun in parent(actor)) return L__M(##Drop, 1, noun);
    if (noun notin actor && ~~ImplicitTake(noun)) return L__M(##Drop, 2, noun);
    if (noun has worn && ImplicitDisrobe(noun)) return;
    move noun to parent(actor);
    if (AfterRoutines() || keep_silent) return;
    L__M(##Drop, 4, noun);
];

[ PutOnSub ancestor;
    receive_action = ##PutOn;
    if (second == d_obj || actor in second) <<Drop noun, actor>>;
    if (parent(noun) == second) return L__M(##Drop, 1, noun);
    if (noun notin actor && ImplicitTake(noun)) return L__M(##PutOn, 1, noun);

    ancestor = CommonAncestor(noun, second);
    if (ancestor == noun) return L__M(##PutOn, 2, noun);
    if (ObjectIsUntouchable(second)) return;

    if (second ~= ancestor) {
        action = ##Receive;
        if (RunRoutines(second, before)) { action = ##PutOn; return; }
        action = ##PutOn;
    }
    if (second hasnt supporter) return L__M(##PutOn, 3, second);
    if (ancestor == actor)      return L__M(##PutOn, 4, second);
    if (noun has worn && ImplicitDisrobe(noun)) return;

    if (ObjectDoesNotFit(noun, second) ||
        LibraryExtensions.RunWhile(ext_objectdoesnotfit, false, noun, second)) return;
    if (AtFullCapacity(noun, second)) return L__M(##PutOn, 6, second);

    move noun to second;

    if (AfterRoutines()) return;

    if (second ~= ancestor) {
        action = ##Receive;
        if (RunRoutines(second, after)) { action = ##PutOn; return; }
        action = ##PutOn;
    }
    if (keep_silent) return;
    if (multiflag) return L__M(##PutOn, 7);
    L__M(##PutOn, 8, noun, second);
];

[ InsertSub ancestor;
    receive_action = ##Insert;
    if (second == d_obj || actor in second) <<Drop noun, actor>>;
    if (parent(noun) == second) return L__M(##Drop, 1, noun);
    if (noun notin actor && ImplicitTake(noun)) return L__M(##Insert, 1, noun);
    ancestor = CommonAncestor(noun, second);
    if (ancestor == noun) return L__M(##Insert, 5, noun);
    if (ObjectIsUntouchable(second)) return;
    if (second ~= ancestor) {
        action = ##Receive;
        if (RunRoutines(second,before)) { action = ##Insert; rtrue; }
        action = ##Insert;
        if (second has container && second hasnt open && ImplicitOpen(second))
            return L__M(##Insert, 3, second);
    }
    if (second hasnt container) return L__M(##Insert, 2, second);
    if (noun has worn && ImplicitDisrobe(noun)) return;

    if (ObjectDoesNotFit(noun, second) ||
        LibraryExtensions.RunWhile(ext_objectdoesnotfit, false, noun, second)) return;
    if (AtFullCapacity(noun, second)) return L__M(##Insert, 7, second);

    move noun to second;

    if (AfterRoutines()) rtrue;

    if (second ~= ancestor) {
        action = ##Receive;
        if (RunRoutines(second, after)) { action = ##Insert; rtrue; }
        action = ##Insert;
    }
    if (keep_silent) rtrue;
    if (multiflag) return L__M(##Insert, 8, noun);
    L__M(##Insert, 9, noun, second);
];

! ----------------------------------------------------------------------------
!   Empties and transfers are routed through the actions above
! ----------------------------------------------------------------------------

[ TransferSub;
    if (noun notin actor && AttemptToTakeObject(noun)) return;
    if (second has supporter) <<PutOn noun second, actor>>;
    if (second == d_obj) <<Drop noun, actor>>;
    <<Insert noun second, actor>>;
];

[ EmptySub; second = d_obj; EmptyTSub(); ];

[ EmptyTSub i j k flag;
    if (noun == second) return L__M(##EmptyT, 4, noun);
    if (ObjectIsUntouchable(noun)) return;
    if (noun hasnt container) return L__M(##EmptyT, 1, noun);
    if (noun hasnt open && ImplicitOpen(noun)) return L__M(##EmptyT, 2, noun);
    if (second ~= d_obj) {
        if (second hasnt supporter) {
            if (second hasnt container) return L__M(##EmptyT, 1, second);
            if (second hasnt open && ImplicitOpen(second))
                return L__M(##EmptyT, 2, second);
        }
    }
    i = child(noun); k = children(noun);
    if (i == 0) return L__M(##EmptyT, 3, noun);
    while (i) {
        j = sibling(i);
        flag = false;
        if (ObjectIsUntouchable(noun)) flag = true;
        if (noun hasnt container) flag = true;
        if (noun hasnt open) flag = true;
        if (second ~= d_obj) {
            if (second hasnt supporter) {
                if (second hasnt container) flag = true;
                if (second hasnt open) flag = true;
            }
        }
        if (k-- == 0) flag = 1;
        if (flag) break;
        if (keep_silent == 0) print (name) i, (string) COLON__TX;
        <Transfer i second, actor>;
        i = j;
    }
];

! ----------------------------------------------------------------------------
!   Gifts
! ----------------------------------------------------------------------------

[ GiveSub;
    if (noun notin actor && ImplicitTake(noun)) return L__M(##Give, 1, noun);
    if (second == actor) return L__M(##Give, 2, noun);
    if (noun has worn && ImplicitDisrobe(noun)) return;
    if (second == player) {
        move noun to player;
        return L__M(##Give, 4, noun);
    }

    if (RunLife(second, ##Give)) return;
    L__M(##Give, 3, second);
];

[ GiveRSub; <Give second noun, actor>; ];

[ ShowSub;
    if (noun notin actor && ImplicitTake(noun)) return L__M(##Show, 1, noun);
    if (second == player) <<Examine noun, actor>>;
    if (RunLife(second, ##Show)) return;
    L__M(##Show, 2, second);
];

[ ShowRSub; <Show second noun, actor>; ];

! ----------------------------------------------------------------------------
!   Travelling around verbs
! ----------------------------------------------------------------------------

[ EnterSub ancestor j ks;
    if (noun has door || noun in compass) <<Go noun, actor>>;
    if (actor in noun) return L__M(##Enter, 1, noun);
    if (noun hasnt enterable) return L__M(##Enter, 2, noun, verb_word);

    if (parent(actor) ~= parent(noun)) {
        ancestor = CommonAncestor(actor, noun);
        if (ancestor == actor or 0) return L__M(##Enter, 4, noun);
        while (actor notin ancestor) {
            j = parent(actor);
            ks = keep_silent;
            if (parent(j) ~= ancestor || noun ~= ancestor) {
                L__M(##Enter, 6, j);
                keep_silent = 1;
            }
            <Exit, actor>;
            keep_silent = ks;
            if (actor in j) return;
        }
        if (actor in noun) return;
        if (noun notin ancestor) {
            j = parent(noun);
            while (parent(j) ~= ancestor) j = parent(j);
            L__M(##Enter, 7, j);
            ks = keep_silent; keep_silent = 1;
            <Enter j, actor>;
            keep_silent = ks;
            if (actor notin j) return;
            <<Enter noun, actor>>;
        }
    }

    if (noun has container && noun hasnt open && ImplicitOpen(noun)) return L__M(##Enter, 3, noun);
    move actor to noun;

    if (AfterRoutines() || keep_silent) return;
    L__M(##Enter, 5, noun);
    if (actor == player) Locale(noun);
];

[ GetOffSub;
    if (parent(actor) == noun) <<Exit, actor>>;
    L__M(##GetOff, 1, noun);
];

[ ExitSub p;
    p = parent(actor);
    if (noun ~= nothing && noun ~= p) return L__M(##Exit, 4 ,noun);
    if (p == location || (location == thedark && p == real_location)) {
        if (actor provides posture && actor.posture) {
            actor.posture = 0;
            return L__M(##Exit, 6);
        }
        if ((location.out_to) || (location == thedark && real_location.out_to))
            <<Go out_obj, actor>>;
        return L__M(##Exit, 1);
    }
    if (p has container && p hasnt open && ImplicitOpen(p))
        return L__M(##Exit, 2, p);

    if (noun == nothing) {
        inp1 = p;
	if (RunRoutines(p, before)) return;
    }

    move actor to parent(p);
    if (player provides posture) player.posture = 0;

    if (AfterRoutines() || keep_silent) return;
    L__M(##Exit, 3, p);
    if (actor == player && p has container) LookSub(1);
];

[ VagueGoSub; L__M(##VagueGo); ];

[ GoInSub; <<Go in_obj, actor>>; ];

[ GoSub i j k movewith thedir next_loc;

    ! first, check if any PushDir object is touchable
    if (second && second notin Compass && ObjectIsUntouchable(second)) return;

    movewith = 0;
    i = parent(actor);
    if ((location ~= thedark && i ~= location) || (location == thedark && i ~= real_location)) {
        j = location;
        if (location == thedark) location = real_location;
        k = RunRoutines(i, before); if (k ~= 3) location = j;
        if (k == 1) {
           movewith = i; i = parent(i);
        }
        else {
            if (k) rtrue;
            if (ImplicitExit(i)) return L__M(##Go, 1, i);
            i = parent(actor);
        }
    }

    thedir = noun.door_dir;
    if (metaclass(thedir) == Routine) thedir = RunRoutines(noun, door_dir);

    next_loc = i.thedir; k = metaclass(next_loc);
    if (k == String) { print (string) next_loc; new_line; rfalse; }
    if (k == Routine) {
        next_loc = RunRoutines(i, thedir);
        if (next_loc == 1) rtrue;
    }

    if (k == nothing || next_loc == 0) {
        if (i.cant_go ~= 0 or CANTGO__TX) PrintOrRun(i, cant_go);
        else                              L__M(##Go, 2);
        rfalse;
    }
    if (next_loc has door) {
        if (next_loc has concealed) return L__M(##Go, 2);
        if (next_loc hasnt open && ImplicitOpen(next_loc)) {
            if (noun == u_obj) return L__M(##Go, 3, next_loc);
            if (noun == d_obj) return L__M(##Go, 4, next_loc);
            return L__M(##Go, 5, next_loc);
        }
        k = RunRoutines(next_loc, door_to);
        if (k == 0) return L__M(##Go, 6, next_loc);
        if (k == 1) rtrue;
        next_loc = k;
    }

    action = ##Going;
    if (RunRoutines(next_loc, before)) { action = ##Go; return; }
    action = ##Go;

    if (movewith == 0) move actor to next_loc; else move movewith to next_loc;
    if (actor ~= player) return L__M(##Go, 7);

    k = location; location = next_loc;
    MoveFloatingObjects();
    if (OffersLight(location))
        lightflag = true;
    else {
        lightflag = false;
        if (k == thedark) {
            if(DarkToDark() == false) ! From real_location To location
                LibraryExtensions.RunAll(ext_darktodark);
            if (deadflag) rtrue;
        }
        location = thedark;
    }
    NoteDeparture(); real_location = next_loc;
    action = ##Going;
    if (RunRoutines(prev_location, after)) { action = ##Go; return; }
    action = ##Go;
    if (AfterRoutines() || keep_silent) return;
    LookSub(1);
];

! ----------------------------------------------------------------------------
!   Describing the world.  SayWhatsOn(object) does just that (producing
!   no text if nothing except possibly "scenery" and "concealed" items are).
!   Locale(object) runs through the "tail end" of a Look-style room
!   description for the contents of the object, printing up suitable
!   descriptions as it goes.
! ----------------------------------------------------------------------------

[ SayWhatsOn descon j f;
    if (descon == parent(player)) rfalse;
    objectloop (j in descon)
        if (j hasnt concealed && j hasnt scenery) f = 1;
    if (f == 0) rfalse;
    L__M(##Look, 4, descon);
];

[ NotSupportingThePlayer o i;
    i = parent(player);
    while (i && i ~= visibility_ceiling) {
        if (i == o) rfalse;
        i = parent(i);
        if (i && i hasnt supporter) rtrue;
    }
    rtrue;
];
! modified with the fix for L61122
[ Locale descin text_without_ALSO text_with_ALSO
    o p num_objs must_print_ALSO;
    objectloop (o in descin) give o ~workflag;
    num_objs = 0;
    objectloop (o in descin)
        if (o hasnt concealed && NotSupportingThePlayer(o)) {
            #Ifndef MANUAL_PRONOUNS;
            PronounNotice(o);
            #Endif;
            if (o has scenery) {
                if (o has supporter && child(o)) SayWhatsOn(o);
            }
            else {
                give o workflag; num_objs++;
                p = initial;
                if ((o has door or container) && o has open && o provides when_open) {
                    p = when_open; jump Prop_Chosen;
                }
                if ((o has door or container) && o hasnt open && o provides when_closed) {
                    p = when_closed; jump Prop_Chosen;
                }
                if (o has switchable && o has on && o provides when_on) {
                    p = when_on; jump Prop_Chosen;
                }
                if (o has switchable && o hasnt on && o provides when_off) {
                    p = when_off;
                }

              .Prop_Chosen;

                if (o.&describe && RunRoutines(o, describe)) {
                    must_print_ALSO = true;
                    give o ~workflag; num_objs--;
                    continue;
                }
                if (o.p && (o hasnt moved || p ~= initial)) {
                    new_line;
                    PrintOrRun(o, p);
                    must_print_ALSO = true;
                    give o ~workflag; num_objs--;
                    if (o has supporter && child(o)) SayWhatsOn(o);
                }
            }
        }

    if (num_objs == 0) return 0;

    if (actor ~= player) give actor concealed;
    if (text_without_ALSO) {
        new_line;
        if (must_print_ALSO) print (string) text_with_ALSO, " ";
        else print (string) text_without_ALSO, " ";
        WriteListFrom(child(descin),
          ENGLISH_BIT+RECURSE_BIT+PARTINV_BIT+TERSE_BIT+CONCEAL_BIT+WORKFLAG_BIT);
    }
    else {
        if (must_print_ALSO) L__M(##Look, 5, descin);
        else L__M(##Look, 6, descin);
    }
    if (actor ~= player) give actor ~concealed;
    return num_objs;
];

! ----------------------------------------------------------------------------
!   Looking.  LookSub(1) is allowed to abbreviate long descriptions, but
!     LookSub(0) (which is what happens when the Look action is generated)
!     isn't.  (Except that these are over-ridden by the player-set lookmode.)
! ----------------------------------------------------------------------------

[ LMode1Sub; lookmode=1; print (string) Story; L__M(##LMode1); ];  ! Brief

[ LMode2Sub; lookmode=2; print (string) Story; L__M(##LMode2); ];  ! Verbose

[ LMode3Sub; lookmode=3; print (string) Story; L__M(##LMode3); ];  ! Superbrief

[ LModeNormalSub;       ! 'normal' value: the default, or as set in Initialise()
    switch (initial_lookmode) {
      1:       <<LMode1>>;
      3:       <<LMode3>>;
      default: <<LMode2>>;
    }
];

[ NoteArrival descin;
    if (location ~= lastdesc) {
        if (location.initial) PrintOrRun(location, initial);
        if (location == thedark) { lastdesc = thedark; return; }
        descin = location;
        if(NewRoom() == false) LibraryExtensions.RunAll(ext_newroom);
        lastdesc = descin;
    }
];

[ NoteDeparture;
    prev_location = real_location;
];

[ ScoreArrival;
    if (location hasnt visited) {
        give location visited;
        if (location has scored) {
            score = score + ROOM_SCORE;
            places_score = places_score + ROOM_SCORE;
        }
    }
];

[ FindVisibilityLevels visibility_levels;
    visibility_levels = 1;
    visibility_ceiling = parent(player);
    while ((parent(visibility_ceiling)) &&
                  (visibility_ceiling hasnt container || visibility_ceiling has open or transparent)) {
        visibility_ceiling = parent(visibility_ceiling);
        visibility_levels++;
    }
    return visibility_levels;
];

[ LookSub allow_abbrev  visibility_levels i j k nl_flag;
    if (parent(player) == 0) return RunTimeError(10);

  .MovedByInitial;

    if (location == thedark) { visibility_ceiling = thedark; NoteArrival(); }
    else {
        visibility_levels = FindVisibilityLevels();
        if (visibility_ceiling == location) {
            NoteArrival();
            if (visibility_ceiling ~= location) jump MovedByInitial;
        }
    }
    ! Printing the top line: e.g.
    ! Octagonal Room (on the table) (as Frodo)
    new_line;
    #Ifdef TARGET_ZCODE;
    style bold;
    #Ifnot; ! TARGET_GLULX;
    glk_set_style(style_Subheader);
    #Endif; ! TARGET_
    if (visibility_levels == 0) print (name) thedark;
    else {
        if (visibility_ceiling ~= location) print (The) visibility_ceiling;
        else print (name) visibility_ceiling;
    }
    #Ifdef TARGET_ZCODE;
    style roman;
    #Ifnot; ! TARGET_GLULX;
    glk_set_style(style_Normal);
    #Endif; ! TARGET_

    for (j=1,i=parent(player) : j<visibility_levels : j++,i=parent(i))
        if (i has supporter) L__M(##Look, 1, i);
        else                 L__M(##Look, 2, i);

    if (print_player_flag == 1) L__M(##Look, 3, player);
    new_line;

    ! The room description (if visible)

    if (lookmode < 3 && visibility_ceiling == location) {
        if ((allow_abbrev ~= 1) || (lookmode == 2) || (location hasnt visited)) {
            if (location.&describe) RunRoutines(location, describe);
            else {
                if (location.description == 0) RunTimeError(11, location, description);
                else PrintOrRun(location, description);
            }
        }
    }

    if (visibility_ceiling == location) nl_flag = 1;

    if (visibility_levels == 0) Locale(thedark);
    else {
        for (i=player,j=visibility_levels : j>0 : j--,i=parent(i)) give i workflag;

        for (j=visibility_levels : j>0 : j--) {
            for (i=player,k=0 : k<j : k++) i=parent(i);
            if (i.inside_description) {
                if (nl_flag) new_line; else nl_flag = 1;
                    PrintOrRun(i,inside_description);
                }
            if (Locale(i)) nl_flag=1;
        }
    }

    if(LookRoutine() == false) LibraryExtensions.RunAll(ext_lookroutine);
    ScoreArrival();
    action = ##Look;
    AfterRoutines();
];

[ ExamineSub i;
    if (location == thedark) return L__M(##Examine, 1, noun);
    i = noun.description;
    if (i == 0) {
        if (noun has container)
            if (noun has open or transparent) <<Search noun, actor>>;
            else return L__M(##Search, 5, noun);
        if (noun has switchable) { L__M(##Examine, 3, noun); rfalse; }
        return L__M(##Examine, 2, noun);
    }
    i = PrintOrRun(noun, description);
    if (i < 2 && noun has switchable) L__M(##Examine, 3, noun);
    AfterRoutines();
];

[ LookUnderSub;
    if (location == thedark) return L__M(##LookUnder, 1, noun);
    L__M(##LookUnder, 2);
];

[ VisibleContents o  i f;
    objectloop (i in o) if (i hasnt concealed or scenery) f++;
    return f;
];

[ SearchSub f;
    if (location == thedark) return L__M(##Search, 1, noun);
    if (ObjectIsUntouchable(noun)) return;
    f = VisibleContents(noun);
    if (noun has supporter) {
        if (f == 0) return L__M(##Search, 2, noun);
        return L__M(##Search, 3, noun);
    }
    if (noun hasnt container) return L__M(##Search, 4, noun);
    if (noun hasnt transparent or open && ImplicitOpen(noun)) return L__M(##Search, 5, noun);
    if (AfterRoutines()) return;

    if (f == 0) return L__M(##Search, 6, noun);
    L__M(##Search, 7, noun);
];

! ----------------------------------------------------------------------------
!   Verbs which change the state of objects without moving them
! ----------------------------------------------------------------------------

[ UnlockSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun hasnt lockable)     return L__M(##Unlock, 1, noun);
    if (noun hasnt locked)       return L__M(##Unlock, 2, noun);
    if (noun.with_key ~= second) return L__M(##Unlock, 3, second);

    give noun ~locked;

    if (AfterRoutines() || keep_silent) return;
    L__M(##Unlock, 4, noun);
];

[ LockSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun hasnt lockable) return L__M(##Lock, 1, noun);
    if (noun has locked)     return L__M(##Lock, 2 ,noun);
    if (noun has open && ImplicitClose(noun)) return L__M(##Lock, 3, noun);
    if (noun.with_key ~= second) return L__M(##Lock, 4, second);

    give noun locked;
    if (AfterRoutines() || keep_silent) return;
    L__M(##Lock, 5, noun);
];

[ SwitchonSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun hasnt switchable) return L__M(##SwitchOn, 1, noun);
    if (noun has on)           return L__M(##SwitchOn, 2, noun);

    give noun on;
    if (AfterRoutines() || keep_silent) return;
    L__M(##SwitchOn, 3, noun);
];

[ SwitchoffSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun hasnt switchable) return L__M(##SwitchOff, 1, noun);
    if (noun hasnt on)         return L__M(##SwitchOff, 2, noun);

    give noun ~on;
    if (AfterRoutines() || keep_silent) return;
    L__M(##SwitchOff, 3, noun);
];

[ OpenSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun hasnt openable) return L__M(##Open, 1, noun);
    if (noun has locked && ImplicitUnlock(noun)) return L__M(##Open, 2, noun);
    if (noun has open)       return L__M(##Open, 3, noun);
    give noun open;

    if (keep_silent || AfterRoutines()) return;

    if (noun hasnt container)
	return L__M(##Open, 5, noun);

    if ((noun has container && location ~= thedark && VisibleContents(noun)
         && IndirectlyContains(noun, player)) == 0) {
         if (noun hasnt transparent && noun hasnt door) return L__M(##Open, 4, noun);
    }
    L__M(##Open, 5, noun);
];

[ CloseSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun hasnt openable) return L__M(##Close, 1, noun);
    if (noun hasnt open)     return L__M(##Close, 2, noun);

    give noun ~open;
    if (AfterRoutines() || keep_silent) return;
    L__M(##Close, 3, noun);
];

[ DisrobeSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun hasnt worn) return L__M(##Disrobe, 1, noun);

    give noun ~worn;
    if (AfterRoutines() || keep_silent) return;
    L__M(##Disrobe, 2, noun);
];

[ WearSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun hasnt clothing)    return L__M(##Wear, 1, noun);
    if (noun notin actor && ImplicitTake(noun)) return L__M(##Wear, 2, noun);
    if (noun has worn)          return L__M(##Wear, 3, noun);

    give noun worn;
    if (AfterRoutines() || keep_silent) return;
    L__M(##Wear, 4, noun);
];

[ EatSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun hasnt edible) return L__M(##Eat, 1, noun);
    if (noun has worn && ImplicitDisrobe(noun)) return;

    remove noun;
    if (AfterRoutines() || keep_silent) return;
    L__M(##Eat, 2, noun);
];

! ----------------------------------------------------------------------------
!   Verbs which are really just stubs (anything which happens for these
!   actions must happen in before rules)
! ----------------------------------------------------------------------------

[ AllowPushDir i;
    if (parent(second) ~= compass) return L__M(##PushDir, 2, noun);
    if (second == u_obj or d_obj)  return L__M(##PushDir, 3, noun);
    AfterRoutines(); i = noun; move i to actor;
    <Go second, actor>;
    if (location == thedark) move i to real_location;
    else                     move i to location;
];

[ AnswerSub;
    if (second && RunLife(second,##Answer)) rfalse;
    L__M(##Answer, 1, noun);
];

[ AskSub;
    if (RunLife(noun,##Ask)) rfalse;
    L__M(##Ask, 1, noun);
];

[ AskForSub;
    if (noun == player) <<Inv, actor>>;
    L__M(##Order, 1, noun);
];

[ AskToSub; L__M(##Order, 1, noun); ];

[ AttackSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun has animate && RunLife(noun, ##Attack)) rfalse;
    L__M(##Attack, 1, noun);
];

[ BlowSub; L__M(##Blow, 1, noun); ];

[ BurnSub;
    if (noun has animate) return L__M(##Burn, 2, noun);
    L__M(##Burn, 1, noun);
];

[ BuySub; L__M(##Buy, 1, noun); ];

[ ClimbSub;
    if (noun has animate) return L__M(##Climb, 2, noun);
    L__M(##Climb, 1, noun);
];

[ ConsultSub; L__M(##Consult, 1, noun); ];

[ CutSub;
    if (noun has animate) return L__M(##Cut, 2, noun);
    L__M(##Cut, 1, noun);
];

[ DigSub; L__M(##Dig, 1, noun); ];

[ DrinkSub; L__M(##Drink, 1, noun); ];

[ FillSub;
    if (second == nothing) return L__M(##Fill, 1, noun);
    L__M(##Fill, 2, noun, second);
];

[ JumpSub; L__M(##Jump, 1, noun); ];

[ JumpInSub;
    if (noun has animate) return L__M(##JumpIn, 2, noun);
    if (noun has enterable) <<Enter noun>>;
    L__M(##JumpOn, 1, noun);
];

[ JumpOnSub;
    if (noun has animate) return L__M(##JumpOn, 2, noun);
    if (noun has enterable && noun has supporter) <<Enter noun>>;
    L__M(##JumpOn, 1, noun);
];

[ JumpOverSub;
    if (noun has animate) return L__M(##JumpOver, 2, noun);
    L__M(##JumpOver, 1, noun);
];

[ KissSub;
    if (ObjectIsUntouchable(noun)) return;
    if (RunLife(noun, ##Kiss)) return;
    if (noun == actor) return L__M(##Touch, 3, noun);
    L__M(##Kiss, 1, noun);
];

[ ListenSub; L__M(##Listen, 1, noun); ];

[ MildSub; L__M(##Mild, 1, noun); ];

[ NoSub; L__M(##No); ];

[ PraySub; L__M(##Pray, 1, noun); ];

[ PullSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun == player)   return L__M(##Pull, 1, noun);
    if (noun == actor)    return L__M(##Pull, 6, noun);
    if (noun has static)  return L__M(##Pull, 2, noun);
    if (noun has scenery) return L__M(##Pull, 3, noun);
    if (noun has animate) return L__M(##Pull, 5, noun);
    L__M(##Pull, 4, noun);
];

[ PushSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun == player)   return L__M(##Push, 1, noun);
    if (noun == actor)    return L__M(##Push, 5, noun);
    if (noun has static)  return L__M(##Push, 2, noun);
    if (noun has scenery) return L__M(##Push, 3, noun);
    if (noun has animate) return L__M(##Push, 5, noun);
    L__M(##Push, 4, noun);
];

[ PushDirSub; L__M(##PushDir, 1, noun); ];

[ RubSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun has animate) return L__M(##Rub, 2, noun);
    L__M(##Rub, 1, noun);
];

[ SetSub; L__M(##Set, 1, noun); ];

[ SetToSub; L__M(##SetTo, 1, noun); ];

[ SingSub; L__M(##Sing, 1, noun); ];

[ SleepSub; L__M(##Sleep, 1, noun); ];

[ SmellSub;
    if (noun ~= nothing && noun has animate) return L__M(##Smell, 2, noun);
    L__M(##Smell, 1, noun);
];

[ SorrySub; L__M(##Sorry, 1, noun); ];

[ SqueezeSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun has animate && noun ~= player) return L__M(##Squeeze, 1, noun);
    L__M(##Squeeze, 2, noun);
];

[ StrongSub; L__M(##Strong, 1, noun); ];

[ SwimSub; L__M(##Swim, 1, noun); ];

[ SwingSub; L__M(##Swing, 1, noun); ];

[ TasteSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun has animate) return L__M(##Taste, 2, noun);
    L__M(##Taste, 1, noun);
];

[ TellSub;
    if (noun == actor) return L__M(##Tell, 1, noun);
    if (RunLife(noun, ##Tell)) return;
    L__M(##Tell, 2, noun);
];

[ ThinkSub; L__M(##Think, 1, noun); ];

[ ThrowAtSub;
    if (ObjectIsUntouchable(noun)) return;
    if (second > 1) {
        action = ##ThrownAt;
        if (RunRoutines(second, before)) { action = ##ThrowAt; rtrue; }
        action = ##ThrowAt;
    }
    if (noun has worn && ImplicitDisrobe(noun)) return;
    if (second hasnt animate) return L__M(##ThrowAt, 1, noun);
    if (RunLife(second, ##ThrowAt)) return;
    L__M(##ThrowAt, 2, noun);
];

[ TieSub;
    if (noun has animate) return L__M(##Tie, 2, noun);
    L__M(##Tie, 1, noun);
];

[ TouchSub;
    if (noun == actor)   return L__M(##Touch, 3, noun);
    if (ObjectIsUntouchable(noun)) return;
    if (noun has animate) return L__M(##Touch, 1, noun);
    L__M(##Touch, 2,noun);
];

[ TurnSub;
    if (ObjectIsUntouchable(noun)) return;
    if (noun == player)    return L__M(##Turn, 1, noun);
    if (noun == actor)     return L__M(##Turn, 5, noun);
    if (noun has static)   return L__M(##Turn, 2, noun);
    if (noun has scenery)  return L__M(##Turn, 3, noun);
    if (noun has animate)  return L__M(##Turn, 5, noun);
    L__M(##Turn, 4, noun);
];

[ WaitSub;
    if (AfterRoutines()) rtrue;
    L__M(##Wait, 1, noun);
];

[ WakeSub; L__M(##Wake, 1, noun); ];

[ WakeOtherSub;
    if (ObjectIsUntouchable(noun)) return;
    if (RunLife(noun, ##WakeOther)) return;
    L__M(##WakeOther, 1, noun);
];

[ WaveSub;
    if (noun == player) return L__M(##Wave, 2 ,noun, second);
    if (noun == actor) return L__M(##Wave, 3, noun, second);
    if (noun notin actor && ImplicitTake(noun)) return L__M(##Wave, 1, noun);
    L__M(##Wave, 2, noun, second);
];

[ WaveHandsSub;
    if (noun) return L__M(##WaveHands, 2, noun);
    L__M(##WaveHands, 1, noun); ];

[ YesSub; L__M(##Yes); ];

! ----------------------------------------------------------------------------
!   Debugging verbs
! ----------------------------------------------------------------------------

#Ifdef DEBUG;

[ TraceOnSub; parser_trace = 1; "[Trace on.]"; ];

[ TraceLevelSub;
    parser_trace = noun;
    print "[Parser tracing set to level ", parser_trace, ".]^";
];

[ TraceOffSub; parser_trace = 0; "Trace off."; ];

[ RoutinesOnSub;
    debug_flag = debug_flag |  DEBUG_MESSAGES;
    "[Message listing on.]";
];

[ RoutinesOffSub;
    debug_flag = debug_flag & ~DEBUG_MESSAGES;
    "[Message listing off.]";
];

[ RoutinesVerboseSub;
    debug_flag = debug_flag | (DEBUG_VERBOSE|DEBUG_MESSAGES);
    "[Verbose message listing on.]";
];

[ ActionsOnSub;
    debug_flag = debug_flag |  DEBUG_ACTIONS;
    "[Action listing on.]";
];

[ ActionsOffSub;
    debug_flag = debug_flag & ~DEBUG_ACTIONS;
    "[Action listing off.]";
];

[ TimersOnSub;
    debug_flag = debug_flag |  DEBUG_TIMERS;
    "[Timers listing on.]";
];

[ TimersOffSub;
    debug_flag = debug_flag & ~DEBUG_TIMERS;
    "[Timers listing off.]";
];

#Ifdef VN_1610;

[ ChangesOnSub;   debug_flag = debug_flag |  DEBUG_CHANGES;  "[Changes listing on.]"; ];
[ ChangesOffSub;  debug_flag = debug_flag & ~DEBUG_CHANGES;  "[Changes listing off.]"; ];

#Ifnot;

[ ChangesOnSub; "[Changes listing available only from Inform 6.2 onwards.]"; ];

[ ChangesOffSub; "[Changes listing available only from Inform 6.2 onwards.]"; ];

#Endif; ! VN_1610

#Ifdef TARGET_ZCODE;

[ PredictableSub i;
    i = random(-100);
    "[Random number generator now predictable.]";
];

#Ifnot; ! TARGET_GLULX;

[ PredictableSub;
    @setrandom 100;
    "[Random number generator now predictable.]";
];

#Endif; ! TARGET_;

[ XTestMove obj dest;
    if (~~obj ofclass Object) "[Not an object.]";
    if (~~dest ofclass Object) "[Destination not an object.]";
    if ((obj <= InformLibrary) || (obj == LibraryMessages) || (obj in 1))
        "[Can't move ", (name) obj, ": it's a system object.]";
    while (dest) {
        if (dest == obj) "[Can't move ", (name) obj, ": it would contain itself.]";
        dest = parent(dest);
    }
    rfalse;
];

[ XPurloinSub;
    if (XTestMove(noun, player)) return;
    move noun to player; give noun moved ~concealed;
    "[Purloined.]";
];

[ XAbstractSub;
    if (XTestMove(noun, second)) return;
    move noun to second;
    "[Abstracted.]";
];

[ XObj obj f;
    if (parent(obj) == 0) print (name) obj; else print (a) obj;
    print " (", obj, ") ";
    if (f && parent(obj))
        print "in ~", (name) parent(obj), "~ (", parent(obj), ")";
    new_line;
    if (child(obj) == 0) rtrue;
    if (obj == Class) ! ???
        WriteListFrom(child(obj), NEWLINE_BIT+INDENT_BIT+ALWAYS_BIT+ID_BIT+NOARTICLE_BIT, 1);
    else
        WriteListFrom(child(obj), NEWLINE_BIT+INDENT_BIT+ALWAYS_BIT+ID_BIT+FULLINV_BIT, 1);
];

[ XTreeSub i;
    if (noun && ~~noun ofclass Object) "[Not an object.]";
    if (noun == 0) {
        objectloop (i)
            if (i ofclass Object && parent(i) == 0) XObj(i);
    }
    else XObj(noun, true);
];

[ GotoSub;
    if ((~~noun ofclass Object) || parent(noun)) "[Not a safe place.]";
    PlayerTo(noun);
];

[ GoNearSub x;
    if (~~noun ofclass Object) "[Not a safe place.]";
    x = noun;
    while (parent(x)) x = parent(x);
    PlayerTo(x);
];

[ Print_ScL obj; print_ret ++x_scope_count, ": ", (a) obj, " (", obj, ")"; ];

[ ScopeSub;
    if (noun && ~~noun ofclass Object) "[Not an object.]";
    x_scope_count = 0;
    LoopOverScope(Print_ScL, noun);
    if (x_scope_count == 0) "Nothing is in scope.";
];

#Ifdef TARGET_GLULX;

[ GlkListSub id val;
    id = glk_window_iterate(0, gg_arguments);
    while (id) {
        print "Window ", id, " (", gg_arguments-->0, "): ";
        val = glk_window_get_type(id);
        switch (val) {
          1: print "pair";
          2: print "blank";
          3: print "textbuffer";
          4: print "textgrid";
          5: print "graphics";
          default: print "unknown";
        }
        val = glk_window_get_parent(id);
        if (val) print ", parent is window ", val;
        else     print ", no parent (root)";
        val = glk_window_get_stream(id);
        print ", stream ", val;
        val = glk_window_get_echo_stream(id);
        if (val) print ", echo stream ", val;
        print "^";
        id = glk_window_iterate(id, gg_arguments);
    }
    id = glk_stream_iterate(0, gg_arguments);
    while (id) {
        print "Stream ", id, " (", gg_arguments-->0, ")^";
        id = glk_stream_iterate(id, gg_arguments);
    }
    id = glk_fileref_iterate(0, gg_arguments);
    while (id) {
        print "Fileref ", id, " (", gg_arguments-->0, ")^";
        id = glk_fileref_iterate(id, gg_arguments);
    }
    val = glk_gestalt(gestalt_Sound, 0);
    if (val) {
        id = glk_schannel_iterate(0, gg_arguments);
        while (id) {
            print "Soundchannel ", id, " (", gg_arguments-->0, ")^";
            id = glk_schannel_iterate(id, gg_arguments);
        }
    }
];

#Endif; ! TARGET_;

#Endif; ! DEBUG

! ----------------------------------------------------------------------------
!   Finally: the mechanism for library text (the text is in the language defn)
! ----------------------------------------------------------------------------

[ L__M act n x1 x2 s;
    if (keep_silent == 2) return;
    s = sw__var;
    sw__var = act;
    if (n == 0) n = 1;
    L___M(n, x1, x2);
    sw__var = s;
];

[ L___M n x1 x2 s;
    s = action;
    lm_n = n;
    lm_o = x1;
    lm_s = x2;
    action = sw__var;
    if (RunRoutines(LibraryMessages, before))             { action = s; rfalse; }
    if (LibraryExtensions.RunWhile(ext_messages, false )) { action = s; rfalse; }
    action = s;
    LanguageLM(n, x1, x2);
];

! ==============================================================================
