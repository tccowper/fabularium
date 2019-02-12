! Object- and cursor-based menus system.
! khelwood@hotmail.com
! Feel free to credit me (Khelwood) if you use this in a game.
! DMenus Version 7
!
! version 5: glulx compatible
! version 6: added 'before' interceptions and MENU_TOPLINE
! version 7: added alternative pgup, pgdn, home and end keys,
!            including for ZCode.
!            Added FullEmblaze feature (see below).
!            Added 'after' execution.

! To open a menu, use the routine ShowMenu(mnu).

! 1) concealed options are not shown
! 2) locked options are skipped by the cursor
! 3) LineGap can be placed in menus
! 4) menus respect child-order
!    (so you can add things to menus in specific positions).
! SwitchOptions are easy to make, but there's
!  a class provided, as long as you declare a constant
!  INCLUDE_SWITCHOPTION before you include this lib.
! If you define INCLUDE_HINTOPTION
!  it will provide HintOption: see (far) below.

! And yes, amazingly, this is Glulx compatible,
!  if you have the infglk lib and bi-platform library.
!
! Use of attributes:
!  any option or submenu that is 'concealed'
!   will not appear;
!  any option or submenu that is 'locked'
!   cannot be selected, and the cursor will skip it;
!  menus and options are flagged as 'open'
!   for the period that their names are being
!   printed in the header - so short_name can
!   check this and amend their name.
!
! Classes:
!   Menu provides no properties or routines,
!    but only instances of this class will be
!    opened as submenus.
!   LineGaps are provided to be blank (unselectable)
!    lines in a menu, just for formatting.
!   The options in a menu do not have a class:
!    they can just be Objects.
!   SwitchOption and HintOption are, well, optional.
!
! The arrow keys (left, right, up, down) are
!  understood alternatives to the standard Inform
!  menu keys (Q, RETURN, P and N respectively).
! The menus also understand SPACE (and Page Down in Glulx)
!  as 'page down', minus (and Page Up in Glulx) as 'page up',
!  H (and Home in Glulx) as 'go to top', and E (and End in Glulx)
!  as 'go to bottom'.
!
! The description property for Menus themselves is not used,
!  though 'before' and 'after' may be (see below).
!  Description routines for options are entitled
!  to alter menus, remove themselves, or
!  add new items. The menu will still work
!  and won't become confused.
! If an option's description returns 0 or 1,
!  (or if it is a string) the menu will
!   print a "Press space" message
!  and wait for a key before redisplaying itself.
!  If an option's description returns 2,
!  the menu won't wait for a key.
!  If it returns 3, the menu will immediately exit itself.
!  If it returns 3+n, then the menu and the n menus
!  above it will all exit.
!  Hence even in a deeply nested menu, returning 1000 (for instance)
!  will always close all menus (and presumably return to the game).

! Menu items are displayed from eldest (child(mnu)) to youngest.
!  To add an item during the game to the BOTTOM of a menu,
!  use the routine MoveToBottom(item,mnu).
! Simply moving the item to the menu ('move item to mnu')
!  will add it to the TOP of the menu.
!
!
! MENU_TOPLINE:
!  This is printed on the two lines just above the options
!   in the menu. If you want to use it, declare it before
!   including this lib. If it is a routine, it will
!   be passed the current menu as a parameter, so if you
!   are so inclined, you could write
!   [ Menu_Topline mnu;
!     if (mnu provides topline)
!       PrintOrRun(mnu,topline);
!     else
!       print "  Select from:";
!   ];
!   so that menus could provide their own top line.
!
! FULLEMBLAZE:
!  Since these menus now have keys for pgup, pgdn, home, and end,
!  the emblazon, which normally lists the keys, can be told to
!  include these.
! Set the global variable FullEmblaze to
!  FULLEMBLAZE_ALWAYS, FULLEMBLAZE_NEVER or FULLEMBLAZE_SOMETIMES.
! If it is 'FULLEMBLAZE_SOMETIMES' then only menus longer
!  then one page will show the extra key instructions.
! To use the FullEmblaze only on specific menus, you might use
!
! Menu  "Main menu";
! Menu  -> "Fully emblazed submenu"
!  with before
!       [; FullEmblaze=FULLEMBLAZE_ALWAYS; ],
!       after
!       [; FullEmblaze=FULLEMBLAZE_NEVER; ];
!  ......
!
! 'BEFORE' and 'AFTER':
!   'before' on a menu is run at the start of ShowMenu,
!  and hence is also called for any submenu that is opened.
!  'before' on an option is run at the start of ShowOption,
!  before the option is emblazed.
!  If it returns nonzero, then the menu or option will
!  not be displayed by the routine.
!  Like 'description', a return of 2 indicates
!  'carry on immediately', a return of 3 means 'exit the menu
!  that called this', and 3+n means 'exit that menu and n
!  menus above it'.
!   'after' is run after a menu is exited (for a menu), or after
!  an option is displayed (for an option).
!  The return values for 'after' are not considered.

System_file;

IfNDef dmenus_h;
Constant dmenus_h;

Ifndef PKEY__TX;
Constant LIB_PRE_63;
!   Then we are using library 6/1 or 6/2, which won't have defined these:
Constant NKEY__TX     = "  N = next option";
Constant PKEY__TX     = "P = previous";
Constant QKEY1__TX    = "  Q = resume game";
Constant QKEY2__TX    = "Q = previous menu";
Constant RKEY__TX     = "RETURN = select option";

Constant NKEY1__KY    = 'N';
Constant NKEY2__KY    = 'n';
Constant PKEY1__KY    = 'P';
Constant PKEY2__KY    = 'p';
Constant QKEY1__KY    = 'Q';
Constant QKEY2__KY    = 'q';
EndIf; ! PKEY__TX

Constant FULLEMBLAZE_NEVER     = 0;
Constant FULLEMBLAZE_SOMETIMES = 1;
Constant FULLEMBLAZE_ALWAYS    = 2;
Global FullEmblaze = FULLEMBLAZE_SOMETIMES;

! A few more, for FullEmblaze:
Constant SPACE__KY    = ' ';
Constant PGDN__TX     = "SPACE = page down";
Constant MINUS__KY    = '-';
Constant PGUP__TX     = "- = page up";
Constant HOME__TX     = "H = top";
Constant HKEY1__KY    = 'H';
Constant HKEY2__KY    = 'h';
Constant END__TX      = "E = end";
Constant EKEY1__KY    = 'E';
Constant EKEY2__KY    = 'e';

#IfDef TARGET_GLULX;

 #IfNDef infglk_h;
 Include "infglk";
 #EndIf;

! The keys for menu operation:
Constant RESIZE__KY     = $80000000;
Constant END__KY        = $fffffff3;
Constant HOME__KY       = $fffffff4;
Constant PAGEDOWN__KY   = $fffffff5;
Constant PAGEUP__KY     = $fffffff6;
Constant ESC__KY        = $fffffff8;
Constant RET1__KY       = $fffffffa;
Constant RET2__KY       = $fffffffa;
Constant UPARROW__KY    = $fffffffc;
Constant DOWNARROW__KY  = $fffffffb;
Constant RIGHTARROW__KY = $fffffffd;
Constant LEFTARROW__KY  = $fffffffe;
#IfNot; ! TARGET_ZCODE
Constant RET1__KY       = $000d;
Constant RET2__KY       = $000a; ! Probably unnecessary
Constant ESC__KY        = $001b;
Constant UPARROW__KY    = $0081;
Constant DOWNARROW__KY  = $0082;
Constant LEFTARROW__KY  = $0083;
Constant RIGHTARROW__KY = $0084;
#Endif; ! TARGET_

! To get the routine MoveToBottom,
!  define the constant Write_MoveToBottom
!  before including this file.
IfDef Write_MoveToBottom;
 IfNDef temp_pile;
  Object temp_pile;
 EndIf;
[ MoveToBottom o p
    x;
   while ( (x=child(p))~=0 )
      move x to temp_pile;
   move o to p;
   while ( (x=child(temp_pile))~=0 )
      move x to p;
];
EndIf;

IfNDef MENU_TOPLINE;
Constant MENU_TOPLINE="  Select from:";
EndIf;


Global screen_width;
Global screen_height;

#IfDef TARGET_GLULX;
Array ForUseByOptions -> 32;
#IfNot; ! TARGET_ZCODE
Array ForUseByOptions string 128;
#EndIf;

Class	Menu;

Class	LineGap
 has 	locked;

[ MenuCursor x y;
#IfDef TARGET_GLULX;
   glk_window_move_cursor(gg_statuswin,x,y);
#IfNot; ! TARGET_ZCODE
   x++; y++;
   @set_cursor y x;
#EndIf; ! TARGET_
];

[ PrintAtPos str x y;
#IfDef TARGET_GLULX;
   glk_window_move_cursor(gg_statuswin,x,y);
#IfNot; ! TARGET_ZCODE
   x++; y++;
   @set_cursor y x;
#EndIf; ! TARGET_
   if (str ofclass String)
      print (string) str;
   if (str ofclass Routine)
      str.call();
   if (str ofclass Object)
      print (name) str;
];

[ MenuPagePages mnu page pages  tmp;
   tmp=(mnu has open);
   give mnu open;
   ! Mark the menu open while we print its header.
   print (name) mnu;
   if (~~tmp)
      give mnu ~open;
   if (pages>1)
      print " [",page,"/",pages,"]";
];

[ EmblazeOption opt bar_height page pages  tmp;

   ! Get screen_width, clear the screen, and split it. Set font style.
#IfDef TARGET_GLULX;
   glk_window_get_size(gg_statuswin, gg_arguments, gg_arguments+4);
   screen_width = gg_arguments-->0;
   glk_window_clear(gg_statuswin);
   glk_window_clear(gg_mainwin);
   StatusLineHeight(bar_height);
   glk_set_window(gg_statuswin);
   glk_set_style(style_Subheader);
#IfNot; ! TARGET_ZCODE
   screen_width = 0->33;
   @erase_window $ffff;
   @split_window bar_height;
   @set_window 1;
   font off;
   style reverse;
   ! For ZCode, reverse the top line.
   @set_cursor 1 1;
   spaces screen_width;
#EndIf; ! TARGET_

   ! Find the length of the option title.
#IfDef TARGET_GLULX;
   tmp=PrintAnyToArray(ForUseByOptions,32,MenuPagePages,opt,page,pages);
   if (tmp>=screen_width) tmp=0;
   else
      tmp=(screen_width - tmp)/2;
#IfNot; ! TARGET_ZCODE
   if (standard_interpreter)
   {
      @storew ForUseByOptions 0 128;
      @output_stream 3 ForUseByOptions;
      MenuPagePages(opt,page,pages);
      @output_stream -3;
      if (ForUseByOptions-->0>=screen_width)
	 tmp=0;
      else
      	 tmp = (screen_width - ForUseByOptions-->0)/2;
   }
   else tmp=0;
#EndIf; ! TARGET_
   MenuCursor(tmp,0);
   MenuPagePages(opt,page,pages);

];

Global top_menu = 0;

[ EmblazeMenu mnu bar_height page pages infull
    tmp;
   if (infull) infull=2;
   EmblazeOption(mnu,bar_height,page,pages);
   ! Set printing style
#IfDef TARGET_GLULX;
   glk_set_style(style_Subheader);
#IfNot; ! TARGET_ZCODE
   font off;
   style reverse;
   tmp=2+infull;
   @set_cursor tmp 1;
   spaces screen_width;
   tmp++;
   @set_cursor tmp 1;
   spaces screen_width;
#EndIf; ! TARGET_
   PrintAtPos(NKEY__TX,1,1+infull);
   PrintAtPos(PKEY__TX,screen_width-13,1+infull);
   PrintAtPos(RKEY__TX,1,2+infull);
   if (top_menu==mnu)
      tmp=QKEY1__TX;
   else
      tmp=QKEY2__TX;
   PrintAtPos(tmp,screen_width-18,2+infull);
   ! In ZCode, mark the bottom of the menu.
#IfNDef TARGET_GLULX;
   style roman; font off;
   @set_cursor bar_height 1;
   for (tmp=0:tmp<screen_width:tmp++)
      print (char) '-';
#EndIf; ! TARGET_
   ! Mark the top of the menu:
   MenuCursor(0,3+infull);
   for (tmp=0:tmp<screen_width:tmp++)
      print (char) '-';

   if (MENU_TOPLINE)
   {
#IfDef TARGET_GLULX;
      glk_set_style(style_Normal);
#EndIf;
      MenuCursor(0,4+infull);
      if (MENU_TOPLINE ofclass Routine)
	 MENU_TOPLINE.call(mnu);
      if (MENU_TOPLINE ofclass String)
	 print (string) MENU_TOPLINE;
   }

   if (~~infull)
      return;

#IfDef TARGET_GLULX;
   glk_set_style(style_Subheader);
#IfNot; ! TARGET_ZCODE
   style reverse;
   @set_cursor 2 1;
   spaces screen_width;
   @set_cursor 3 1;
   spaces screen_width;
#EndIf;
   PrintAtPos(PGUP__TX,1,1);
   PrintAtPos(PGDN__TX,1,2);
   PrintAtPos(HOME__TX,screen_width-8,1);
   PrintAtPos(END__TX,screen_width-8,2);
];
! Call with ShowMenu(mnu,true) to skip the 'before' checking.
[ ShowMenu mnu tmp   lines page pages pos old_pos obj pkey
    page_lines cur_item top_obj infull;
   if (tmp==0)
   {  tmp=RunRoutines(mnu,before);
      if (tmp) return tmp;
   }
   if (top_menu==0)
      top_menu=mnu;
   cur_item=0;
   .TotalRedisplay; ! This is redisplay from scratch,
             ! so options are recounted
             ! and the menu height is recalculated
   ! Jump to .TotalRedisplay if the window is resized (Glulx)
   !  and after displaying any option, in case
   !  the menu is altered by disappearing or
   !  appearing options.

   ! count options.
   lines=0;
   top_obj=0;
   for (obj=child(mnu):obj:obj=sibling(obj))
      if (obj hasnt concealed)
      {
      	 lines++;
      	 if (top_obj==0) top_obj=obj;
      	 if (cur_item==0 && obj hasnt locked)
	    cur_item=obj;
      }
   tmp=2;
   if (cur_item==0)
      jump ExitMenu;
   ! Find the available height, to work out the number of pages.

#IfDef TARGET_GLULX;
   StatusLineHeight(1); ! set the status line to 1
   ! so we can measure the screen:
   glk_window_get_size(gg_mainwin, gg_arguments, gg_arguments+4);
   screen_height = gg_arguments-->1;
   ! screen_height has been set to the sum of the heights
   !  of the two display windows.
   ! This'll actually be one less than the actual screen height.
   ! So the mainwin will have a little room at the bottom.

#IfNot; ! TARGET_ZCODE
   screen_height=0->32;
#EndIf; ! TARGET_
   ! Some interpreters won't tell you their height.
   ! So here, check it's something vaguely sensible.
#IfDef TARGET_GLULX;
   if (screen_height<=0 || screen_height>=250)
   {
      ! There's at least one Glulx 'terp that
      !  will tell you the height of the status window
      !  but not the height of the main window.
      ! So try and expand the status window and
      !  hopefully we can see how big it actually gets.
      StatusLineHeight(250);
      glk_window_get_size(gg_statuswin, gg_arguments, gg_arguments+4);
      screen_height = gg_arguments-->1;
   }
#EndIf; ! TARGET_
   ! If that didn't work, or if we're in ZCode,
   !  we can do little but make a conservative estimate.
   ! 18 lines is enough space to list 11 items,
   !  or 8 if fullemblaze is active.
   if (screen_height<=0 || screen_height>=250)
      screen_height=18;

   switch (FullEmblaze)
   {
     FULLEMBLAZE_NEVER:
      infull=0;
     FULLEMBLAZE_ALWAYS:
      infull=2;
     FULLEMBLAZE_SOMETIMES:
      if (lines+7>screen_height)
	 infull=2;
      else infull=0;
   }
   ! A little check here: in the highly unlikely
   !  cirumstance that your screen is 8 or 9
   !  lines tall, that FullEmblaze information will
   !  completely kill your menu.
   !  So turn off the fullemblaze.
   if (screen_height<10) infull=0;
   if (screen_height<8) screen_height=8;
   ! 8 it the minimum height with the menu in this layout,
   !  and that's one option. One option per page is usable.

   if (lines+7+infull>screen_height)
   {
      ! Set pages to however many we need,
      !  and use as much space as possible per page.
      page_lines=screen_height-7-infull;
      pages=1+(lines-1)/page_lines;
   }
   else
   {
      pages=1;
      ! Set the status window to the right
      !  size to fit everything in.
      screen_height=lines+7+infull;
      page_lines=lines;
   }
   page=1;

   .ReDisplay;

   ! Find out what page we're on
   pos=0;
   for (obj=child(mnu):obj:obj=sibling(obj))
      if (obj hasnt concealed)
      {
	 if (obj==cur_item) break;
	 pos++;
      }
   if (obj==0)
   {
      tmp=2; jump ExitMenu;
   }
   page=1+pos/page_lines;
   if (page>1)
      pos=pos%page_lines;
   EmblazeMenu(mnu,page_lines+7+infull,page,pages,infull);

   ! Set the font style to something appropriate
#IfDef TARGET_GLULX;
   glk_set_style(style_Normal);
#IfNot; ! TARGET_ZCODE
   style roman; font off;
#EndIf; ! TARGET_

   ! If we have multiple pages, we have to make sure
   !  we have top_obj correct.
   if (pages>1)
   {
      old_pos=(page-1)*page_lines;
      tmp=0;
      top_obj=0;
      for (obj=child(mnu):obj:obj=sibling(obj))
	 if (obj hasnt concealed)
	 {
	    if (tmp==old_pos)
	    {
	       top_obj=obj;
	       break;
	    }
	    tmp++;
	 }
   }

   if (top_obj==0)
   { tmp=2; jump ExitMenu; }

   ! Display the options
   tmp=0;
   for (obj=top_obj:obj && tmp<page_lines:obj=sibling(obj))
      if (obj hasnt concealed)
      {
	 if (~~obj ofclass LineGap)
	 {
	    PrintAtPos(obj,5,tmp+6+infull);
	 }
	 tmp++;
      }

   ! The moving-the-pointer-up-and-down loop
   old_pos=-1;
   for (::)
   {
      if (old_pos~=pos)
      {
	 ! erase the old pointer
	 if (old_pos>=0)
	 {
	    MenuCursor(3,old_pos+6+infull);
	    print (char)' ';
	 }
	 old_pos=pos;
	 ! draw the new pointer
	 MenuCursor(3,pos+6+infull);
	 print (char)'>';
      }
      ! Button press:
#IfDef TARGET_GLULX;
      pkey=KeyCharPrimitive(gg_statuswin, true);
#IfNot; ! TARGET_ZCODE
      @read_char 1 -> pkey;
#EndIf; ! TARGET_

      ! Glulx-only keys: pageup, pagedown, home, end, and resizing
#IfDef TARGET_GLULX;
      ! On resize, go back and recalculate everything
      if (pkey==RESIZE__KY)
	 jump TotalRedisplay;
      if (pkey==PAGEUP__KY)
	 pkey=MINUS__KY;
      if (pkey==PAGEDOWN__KY)
	 pkey=SPACE__KY;
      if (pkey==HOME__KY)
	 pkey=HKEY1__KY;
      if (pkey==END__KY)
	 pkey=EKEY1__KY;
#EndIf; ! TARGET_

      ! Pageup on page 1 means "go to top"
      if (pkey==MINUS__KY && page==1)
	 pkey=HKEY1__KY;
      ! Pagedown on the last page means "go to bottom"
      if (pkey==SPACE__KY &&
 	  (page==pages || pos+page*page_lines>lines))
	 pkey=EKEY1__KY;
      ! page up
      if (pkey==MINUS__KY)
      {
	 ! Desired position is page_lines higher
	 !  than current position.
	 pos=pos+(page-2)*page_lines;
	 tmp=0;
	 cur_item=0;
	 ! count down to it from the top
	 for (obj=child(mnu):obj:obj=sibling(obj))
	    if (obj hasnt concealed)
	    {
	       if (obj hasnt locked)
		  cur_item=obj;
	       if (tmp>=pos)
		  break;
	       tmp++;
	    }
	 ! cur_item is the last selectable option
	 !  before (or at) the desired position.
	 if (cur_item==0)
	 {
	    ! If we didn't find one, keep counting
	    !  down the options to the first
	    !  selectable thing we find.
	    while (obj && (obj has concealed
			   || obj has locked))
	       obj=sibling(obj);
	    ! If we don't find one, we'll have to exit
	    if (obj==0) { tmp=2; break; }
	    cur_item=obj;
	 }
	 jump ReDisplay;
      }

      ! page down
      if (pkey==SPACE__KY)
      {
	 tmp=0;
	 obj=cur_item;
	 ! try to go down page_lines number of lines.
	 while (tmp<page_lines && cur_item)
	 {
	    if (cur_item hasnt concealed)
            {
	       tmp++;
	       if (cur_item hasnt locked)
		  obj=cur_item;
	    }
	    cur_item=sibling(cur_item);
	 }
	 ! If cur_item is selectable, then that's the one.
	 if (cur_item && cur_item hasnt locked
	     && cur_item hasnt concealed)
	    jump Redisplay;
	 ! cur_item is unselectable, but there may
	 !  be more selectable options beneath it.
	 while (cur_item && (cur_item has locked
			     || cur_item has concealed))
	    cur_item=sibling(cur_item);
	 ! If there aren't, we'll have to use the last
	 !  selectable object we passed, which is obj.
	 if (cur_item==0)
	    cur_item=obj;
	 jump Redisplay;
      }
      ! Go to the end: the last SELECTABLE option in the menu.
      if (pkey==EKEY1__KY or EKEY2__KY)
      {
	 pos=lines-1;
	 cur_item=youngest(mnu);
	 while (cur_item && (cur_item has concealed
			     || cur_item has locked))
	 {
	    if (cur_item hasnt concealed)
	       pos--;
	    cur_item=elder(cur_item);
	 }
	 if (cur_item==0)
	 { tmp=2; break; }
	 if (page~=1+pos/page_lines)
	    jump Redisplay;
	 pos=pos%page_lines;
	 continue;
      }

      ! Go to the start: the first selectable option in the menu.
      if (pkey==HKEY1__KY or HKEY2__KY)
      {
	 pos=0;
	 cur_item=child(mnu);
	 while (cur_item && (cur_item has concealed
			     || cur_item has locked))
	 {
	    if (cur_item hasnt concealed)
	       pos++;
	    cur_item=sibling(cur_item);
	 }
	 if (cur_item==0)
	 { tmp=2; break; }
	 if (page~=1+pos/page_lines)
	    jump Redisplay;
	 pos=pos%page_lines;
	 continue;
      }

      ! Moving the pointer down
      if (pkey==NKEY1__KY or NKEY2__KY or DOWNARROW__KY)
      {
	 ! increment position
	 pos++;
	 cur_item=sibling(cur_item);
	 while (cur_item && (cur_item has concealed
			     || cur_item has locked))
	 {
	    if (cur_item hasnt concealed)
	       pos++;
   	    cur_item=sibling(cur_item);
	 }
	 ! If we've gone off the end...
	 if (cur_item==0)
	 {
	    cur_item=child(mnu);
	    pos=0;
	    while (cur_item && (cur_item has concealed
				|| cur_item has locked))
	    {
	       if (cur_item hasnt concealed)
		  pos++;
	       cur_item=sibling(cur_item);
	    }
	    if (cur_item==0) ! something gone wrong...
	    {
	       tmp=2; break;
	    }
	    if (page~=1+pos/page_lines)
	       jump ReDisplay;
	    pos=pos%page_lines;
	 }
	 if (pos<0 || pos>=page_lines)
	    jump ReDisplay;
	 continue;
      }

      ! Moving the pointer up
      if (pkey==PKEY1__KY or PKEY2__KY or UPARROW__KY)
      {
	 pos--;
	 cur_item=elder(cur_item);
	 while (cur_item && (cur_item has concealed
			     || cur_item has locked))
	 {
	    if (cur_item hasnt concealed)
	       pos--;
	    cur_item=elder(cur_item);
	 }


	 ! If we've gone off the top
	 if (cur_item==0)
	 {
	    cur_item=youngest(mnu);
	    pos=lines-1;
 	    while (cur_item && (cur_item has concealed
				|| cur_item has locked))
	    {
	       if (cur_item hasnt concealed)
		  pos--;
	       cur_item=elder(cur_item);
	    }
	    if (cur_item==0) ! something gone wrong
	    { tmp=2; break; }
	    if (page~=1+pos/page_lines)
	       jump ReDisplay;
	    pos=pos%page_lines;
	 }
	 if (pos<0 || pos>=page_lines)
	    jump ReDisplay;
	 continue;
      }

      ! Quitting
      if (pkey==QKEY1__KY or QKEY2__KY or ESC__KY or LEFTARROW__KY)
      {
	 tmp=2;
	 break;
      }

      ! Selecting
      if (pkey==RET1__KY or RET2__KY or RIGHTARROW__KY)
      {
	 ! Selecting a menu
	 if (cur_item ofclass Menu)
	    tmp=ShowMenu(cur_item);
	 else
	    ! selecting an option
	    tmp=ShowOption(cur_item);
	 if (tmp>2)
	 {
	    tmp--;
	    break;
	 }
	 if (tmp<2)
	 {
#ifdef LIB_PRE_63;
	    print "[Please press SPACE to continue.]^";
#ifnot;
	    L__M(##Miscellany, 53);
#endif;
#IfDef TARGET_GLULX;
	    pkey=KeyCharPrimitive(gg_mainwin,true);
#IfNot; ! TARGET_ZCODE
	    @read_char 1 -> pkey;
#EndIf; ! TARGET_
	 }

	 ! It's possible that an option might change
	 !  when it's selected - moving it, concealing
	 !  or locking it. If that happens, we have to
	 !  find a selectable option to leave the cursor at.

	 if (cur_item notin mnu)
	    cur_item=0;
	 while (cur_item && (cur_item has concealed
			     || cur_item has locked))
	    cur_item=sibling(cur_item);
	 ! We can leave cur_item at 0 if necessary,
	 !  and it will be set to the first selectable
	 !  option in the menu.

	 jump TotalRedisplay;
      }

   } ! end of the pointer-moving loop

   .ExitMenu;
   RunRoutines(mnu,after);
   if (top_menu~=mnu)
      return tmp;
   top_menu=0;
#IfDef TARGET_GLULX;
   glk_set_window(gg_mainwin);
   glk_window_clear(gg_mainwin);
   StatusLineHeight(1);
#IfNot; ! TARGET_ZCODE
   font on; @set_cursor 1 1;
   @erase_window $ffff;
   @set_window 0;
#EndIf; ! TARGET_
   new_line; new_line; new_line;
   ! Look, as long as we're alive and somewhere
   if (parent(player) && location && ~~deadflag)
   { LookSub(1); rtrue; }
   return tmp;
];
! Call with ShowOption(option,true)
!  to skip before-checking.
[ ShowOption opt fl;
   if (fl==0)
   {
      fl=RunRoutines(opt,before);
      if (fl) return fl;
   }
   EmblazeOption(opt,1,1,1);
#IfDef TARGET_GLULX;
   glk_set_style(style_Normal);
   glk_set_window(gg_mainwin);
#IfNot; ! TARGET_ZCODE
   @set_window 0;
   font on; style roman;
#EndIf; ! TARGET_

   new_line; new_line; new_line;
   if (opt.description)
   {
      fl=PrintOrRun(opt,description);
   }
   else
   {
      fl=1;
      "[No text written for this option.]";
   }
   RunRoutines(opt,after);
   return fl;
];

IfDef INCLUDE_HINTOPTION;

 #IfNDef HINT_MESSAGE;
Constant HINT_MESSAGE
  "[Press ENTER to finish, or 'h' for another hint.]^";
 #EndIf;
! Use as follows:
! Menu "Some menu";
! HintOption -> MyHints
!  with getHints
!       [ n;
!          switch (n)
!          {
!            0: return 3; ! number of hints
!            1: "first hint";
!            2: "second hint";
!            3: "third hint";
!          }
!       ];
! MyHints.number is by default 1, so the
!  first hint is immediately shown.
!  If you start it off with number 0,
!  no hints will be shown at the start.

Class  	HintOption
 with	hintMessage HINT_MESSAGE,
	number 1,
	getHints 0,
	description
	[ n i;
	   if (self.getHints)
	      n=self.getHints();
	   else
	      n=0;
	   if (n==0)
	      "No hints available for this option.";
	  if (self.number<n) PrintOrRun(self,hintMessage);
	  for (i=1: i<=self.number: i++)
	  {
	     new_line;
	    ! font off;
	     if (i<10 && n>=10) print (char) ' ';
	     print (char) 40,i,(char) '/',n,(char) 41,(char) ' ';
	    ! font on;
	     self.getHints(i);
	  }

	  while (self.number<n)
	  {
#IfDef TARGET_GLULX;
	     i=KeyCharPrimitive(gg_mainwin,true);
#IfNot; ! TARGET_ZCODE
	     @read_char 1 -> i;
#EndIf; ! TARGET_
	     if (i ~= 'H' or 'h'
 		 or DOWNARROW__KY or RIGHTARROW__KY)
 		return 2;
	     ! WE NEED THIS NEW_LINE EXACTLY HERE
	     !  otherwise (in WinFrotz apparently)
	     !  strange things happen. Tut.
	     new_line;
	     (self.number)++;
	     !font off;
	     if (n>=10 && self.number<10)
	      	print (char) ' ';
	     print (char) 40,(self.number),(char) '/',n,
		 (char) 41, (char) ' ';
	    ! font on;
	     self.getHints(self.number);
	  }
	  rtrue;
	];
EndIf; ! hintoption

IfDef INCLUDE_SWITCHOPTION;
Class 	SwitchOption
 with 	short_name
       	[; print (object) self;
           if (self has on) print " (on)";
 	   else  print " (off)";
           rtrue;
       	],
       	before
       	[; if (self has on) give self ~on;
 	   else  give self on;
           return 2;
       	];

EndIf; ! switchoption

EndIf; ! dmenus_h
