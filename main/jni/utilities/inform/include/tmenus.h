! Redesign of menus so that submenus can open embedded in their parent menus.
! You need the lib DMenus (version 7) to use this.
! This is Glulx compatible.
! khelwood@hotmail.com
! Feel free to credit me (Khelwood) if you use this in a game.
! TMenus Version 7

! version 5: Glulx compatible
! version 6: added 'before' checking
! version 7: added alternative pgup, pgdn, home and end keys,
!            including for ZCode.
!            Added FullEmblaze (see DMenus).
!            Added 'after' execution.
! See DMenus for details.
!
! Mark submenus as 'transparent' to have them open inside their
!  parent menu.
!
! Transparent submenus can supply 'before' and 'after'
!  which are treated the same as described in DMenus.
!  Particularly, if a submenu's 'before' property
!  returns something nonzero, then the submenu will
!  not be opened.
!
! To open a menu as a TMenu, use the routine ShowTMenu(mnu).
! TMenus should be of the class Menu.

System_file;

IfNDef dmenus_h;
Include "DMenus";
EndIf;

IfNDef tmenus_h;
Constant tmenus_h;

Constant QKEYT__TX = "         Q = back";

[ TMenuContent mnu  obj i;
   i=0;
   objectloop (obj in mnu && obj hasnt concealed)
   {
      i++;
      if (obj ofclass Menu && obj has transparent
	  && obj has open)
	 i=i+TMenuContent(obj);
   }
   return i;
];

[ TMenuNext mnu item  obj;
   if (item ofclass Menu && item has open &&
       item has transparent && item hasnt concealed)
      for (obj=child(item):obj:obj=sibling(obj))
	 if (obj hasnt concealed)
	    return obj;
   while (item && item~=mnu)
   {
      for (obj=sibling(item):obj:obj=sibling(obj))
	 if (obj hasnt concealed)
	    return obj;
      item=parent(item);
   }
   rfalse;
];

! Little recursive function to make sure
!  that branches of tmenus start
!  off closed. There's no reason
!  they should be open, but it's
!  simple to check anyway.
[ CloseBranches mnu  obj;
   objectloop (obj in mnu)
      if (obj ofclass Menu && obj has transparent
	  && obj hasnt concealed)
      {
	 CloseBranches(obj);
	 give obj ~open;
      }
];

! Number of generations between item and mnu.
! If parent(item)==mnu, returns 1.
! If parent(parent(item))==mnu, returns 2. And so on.
! If item is not ultimately inside mnu, returns 0.
[ TMenuDescent mnu item  i;
   for (i=0:item:i++)
   {
      if (item==mnu)
	 return i;
      item=parent(item);
   }
   rfalse;
];

! Gets the position (index, from 0)
!  of an item in a TMenu.
!  Returns -1 if item not found.
[ TMenuPos mnu item  pos obj;
   obj=child(mnu);
   while (obj && obj has concealed)
      obj=sibling(obj);
   if (obj==0)
      return -1;
   pos=0;
   while (obj)
   {
      if (obj==item)
	 return pos;
      pos++;
      obj=TMenuNext(mnu,obj);
   }
   return -1;
];

! Gets the item at a given position in the TMenu.
! Returns 0 if it runs out of items.
[ TMenuInPos mnu pos  obj;
   if (pos<0)
      rfalse;
   obj=child(mnu);
   while (obj && obj has concealed)
      obj=sibling(obj);
   while (obj && pos>0)
   {
      pos--;
      obj=TMenuNext(mnu,obj);
   }
   return obj;
];

! And here's the prize:
! Call ShowTMenu(mnu) to display mnu as
!  a menu with embedded submenus.
[ ShowTMenu mnu tmp   lines page pages pos old_pos obj pkey
    page_lines cur_item top_obj cur_menu infull;
   if (tmp==0)
   {
      tmp=RunRoutines(mnu,before);
      if (tmp) return tmp;
   }
   if (top_menu==0)
      top_menu=mnu;
   cur_item=0;
   cur_menu=mnu;
   .TotalRedisplay;
   ! cur_item==0 means either
   !    1) the routine has just started
   ! or 2) the menu's in a mess.
   ! In either case, get everything ready
   !  from scratch.
   if (cur_item && cur_item notin mnu)
   {
      cur_menu=parent(cur_item);
      if (~~IndirectlyContains(mnu,cur_menu))
      {
	 cur_item=0;
	 cur_menu=mnu;
	 CloseBranches(mnu);
      }
   }
   else if (cur_item==0 && cur_menu~=0 or mnu)
   {
      if (~~IndirectlyContains(mnu,cur_menu))
	 cur_menu=mnu;
   }
   if (cur_menu==0)
      cur_menu=mnu;
   if (cur_item && cur_item notin cur_menu)
      cur_item=0;

   top_obj=0; ! force top_obj to be recalculated
   tmp=2;
   ! count options
   lines=TMenuContent(mnu);
   if (lines==0)
      jump ExitMenu;
   ! set cur_item (if it's unset)
   for (obj=child(cur_menu):obj && ~~cur_item:obj=sibling(obj))
      if (obj hasnt concealed && obj hasnt locked)
	 cur_item=obj;
   if (cur_item==0)
      jump ExitMenu;

   ! Find the available height.
#IfDef TARGET_GLULX;
   StatusLineHeight(1); ! set the status line to 1
   ! so we can measure the screen:
   glk_window_get_size(gg_mainwin, gg_arguments, gg_arguments+4);
   screen_height = gg_arguments-->1;
   ! screen_height has been set to the sum of the heights
   !  of the two display windows.
   ! This'll actually be one less than the actual screen height.
   ! So the mainwin will be visible at the bottom.

#IfNot; ! TARGET_ZCODE
   screen_height=0->32;
#EndIf; ! TARGET_



   ! Check height is vaguely sensible.
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
   if (screen_height<10) infull=0;
   if (screen_height<8) screen_height=8;
   ! 8 it the minimum height with the menu in this layout,
   !  and that's one option. One option per page is usable.

   if (lines+7+infull>screen_height)
   {
      page_lines=screen_height-7-infull;
      pages=1+(lines-1)/page_lines;
   }
   else
   {
      pages=1; page=1;
      screen_height=lines+7+infull;
      page_lines=lines;
   }

   .ReDisplay;

   ! Find pos and page.
   pos=TMenuPos(mnu,cur_item);

   if (pages>1)
   {
      page=1+pos/page_lines;
      pos=pos%page_lines;
   }

   EmblazeMenu(mnu,page_lines+7+infull,page,pages,infull);

   if (cur_menu~=mnu)
   {
#IfDef TARGET_GLULX;
      glk_set_style(style_Subheader);
#IfNot; ! TARGET_ZCODE
      style reverse;
#EndIf; ! TARGET_
      PrintAtPos(QKEYT__TX,screen_width-18,2+infull);
   }

   ! Set the font style to something appropriate
#IfDef TARGET_GLULX;
   glk_set_style(style_Normal);
#IfNot; ! TARGET_ZCODE
   style roman; font off;
#EndIf; ! TARGET_

   ! Find top_obj for the page
   if (pages>1 || top_obj==0)
      top_obj=TMenuInPos(mnu,(page-1)*page_lines);
   if (top_obj==0)
      jump TotalRedisplay;

   ! print the options.
   tmp=0;
   for (obj=top_obj:obj && tmp<page_lines:obj=TMenuNext(mnu,obj))
   {
      if (~~obj ofclass LineGap)
      {
	 old_pos=3+2*TMenuDescent(mnu,obj);
	 pkey=(obj has open);
	 give obj ~open;
	 PrintAtPos(obj,old_pos,tmp+6+infull);
	 if (pkey)
	 {
	    give obj open;
	    ! Mark open submenus:
	    if (obj ofclass Menu && obj has transparent)
	       PrintAtPos(">",old_pos-2,tmp+6+infull);
	 }
      }
      tmp++;
   }

   old_pos=-1;
   ! The moving-the-pointer-up-and-down loop
   tmp=2*TMenuDescent(mnu,cur_item);
   for (::)
   {
      if (old_pos~=pos)
      {
	 if (old_pos>=0)
	 {
	    PrintAtPos("  ",tmp,old_pos+6+infull);
	 }
	 old_pos=pos;
	 ! draw the pointer
      	 MenuCursor(tmp,pos+6+infull);
      	 if (cur_menu~=mnu)
	    print "->";
      	 else
	    print " >";
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
#EndIf; !TARGET_


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
	 ! Try to go up page_lines number of lines
	 tmp=0;
	 while (tmp<page_lines && cur_item)
	 {
	    if (cur_item hasnt concealed)
	    {
	       tmp++;
	       if (cur_item hasnt locked)
		  obj=cur_item;
	    }
	    cur_item=elder(cur_item);
	 }
	 ! skip up past unselectable options...
	 while (cur_item && (cur_item has concealed
			     || cur_item has locked))
	    cur_item=elder(cur_item);
	 ! No good? go to the last good option we passed,
	 !  which is obj.
	 if (cur_item==0)
	    cur_item=obj;
	 jump Redisplay;

      }

      ! page down
      if (pkey==SPACE__KY)
      {
	 ! Try to go down page_lines number of lines
	 tmp=0;
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
	 ! If cur_item is unselectable, there may
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
      ! Go to the last selectable option in the current (sub?)menu
      if (pkey==EKEY1__KY or EKEY2__KY)
      {
	 cur_item=youngest(cur_menu);
	 while (cur_item && (cur_item has concealed
			     || cur_item has locked))
	    cur_item=elder(cur_item);
	 pos=TMenuPos(mnu,cur_item);
	 if (page~=1+pos/page_lines)
	    jump Redisplay;
	 pos=pos%page_lines;
	 continue;
      }

      ! Go to the first selectable option in the current (sub?)menu
      if (pkey==HKEY1__KY or HKEY2__KY)
      {
	 cur_item=child(cur_menu);
	 while (cur_item && (cur_item has concealed
			     || cur_item has locked))
	    cur_item=sibling(cur_item);
	 pos=TMenuPos(mnu,cur_item);
	 if (page~=1+pos/page_lines)
	    jump Redisplay;
	 pos=pos%page_lines;
	 continue;
      }

      ! Cursor down
      if (pkey==NKEY1__KY or NKEY2__KY or DOWNARROW__KY)
      {
	 pos++;
	 cur_item=sibling(cur_item);
	 while (cur_item && (cur_item has concealed
			     || cur_item has locked))
	 {
	    if (cur_item hasnt concealed)
	       pos++;
	    cur_item=sibling(cur_item);
	 }
	 if (cur_item==0)
	 {
	    cur_item=child(cur_menu);
	    while (cur_item && (cur_item has concealed
				|| cur_item has locked))
	       cur_item=sibling(cur_item);
	    pos=TMenuPos(mnu,cur_item);
	    if (page~=1+pos/page_lines)
	       jump Redisplay;
	    pos=pos%page_lines;
	 }
	 if (pos<0 || pos>=page_lines)
	    jump ReDisplay;
	 continue;
      }

      ! Cursor up
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
	 if (cur_item==0)
	 {
	    cur_item=youngest(cur_menu);
	    while (cur_item && (cur_item has concealed
				|| cur_item has locked))
	       cur_item=elder(cur_item);
	    pos=TMenuPos(mnu,cur_item);
	    if (page~=1+pos/page_lines)
	       jump ReDisplay;
	    pos=pos%page_lines;
	 }
	 if (pos<0 || pos>=page_lines)
	    jump Redisplay;
	 continue;
      }

      ! Quitting, and exitting sub-menus
      if (pkey==QKEY1__KY or QKEY2__KY or ESC__KY
	  or LEFTARROW__KY)
      {
	 tmp=2;
	 if (cur_menu==mnu)
	    break;
	 RunRoutines(cur_menu,after);
	 cur_item=cur_menu;
	 cur_menu=parent(cur_menu);
	 if (cur_menu==0) ! Certainly shouldn't happen
	    cur_menu=mnu;
	 give cur_item ~open;
	 jump TotalRedisplay;
      }

      if (pkey==RET1__KY or RET2__KY or RIGHTARROW__KY)
      {
	 ! Transparent submenu
	 tmp=0;
	 if (cur_item ofclass Menu && cur_item has transparent)
	 {
	    cur_menu=cur_item;
	    give cur_menu open;
	    tmp=RunRoutines(cur_menu,before);
	    if (tmp==1)
	    {
	       give cur_menu ~open;
	       cur_item=cur_menu;
	       cur_menu=parent(cur_menu);
	       ! Hold tmp's value...
	    }
	    if (tmp==0)
	    {
	       cur_item=TMenuNext(mnu,cur_menu);
	       if (cur_item && cur_item in cur_menu)
	       	  while (cur_item && (cur_item has locked
				      || cur_item has concealed))
		     cur_item=sibling(cur_item);
	       if (cur_item==0 || cur_item notin cur_menu)
	       {
	       	  cur_item=cur_menu;
	       	  cur_menu=parent(cur_item);
	       	  give cur_item ~open;
	       	  continue;
	       }
	       jump TotalRedisplay;
	    }
	 }
	 if (tmp==0) ! If tmp was set above by a 'before'
	    ! execution, then we keep it until here,
	    ! so it can be considered with these
	    ! other possibilities.
	 {
	    if (cur_item ofclass Menu)
	       tmp=ShowTMenu(cur_item);
	    else
	       tmp=ShowOption(cur_item);
	 }
	 while (cur_menu~=mnu or 0 && tmp>2)
	 {
	    tmp--;
	    cur_item=cur_menu;
	    give cur_menu ~open;
	    cur_menu=parent(cur_menu);
	 }
	 if (tmp>2)
	 { tmp--; break; }
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

	 ! Checks:

	 if (cur_item notin cur_menu)
	    cur_item=0;
	 while (cur_item && (cur_item has concealed
			     || cur_item has locked))
	    cur_item=sibling(cur_item);
	 ! If cur_item==0, it will be set in totalRedisplay
	 jump TotalRedisplay;
      }
   } ! end of pointer-loop

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

]; ! end of ShowTMenu


EndIf; ! tmenus_h
