/*
 * text.h - Text buffer
 * Copyright (c) 2005 Torbjörn Andersson <d91tan@Update.UU.SE>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111, USA.
 */

#ifndef _TEXT_H
#define _TEXT_H

void text_init (void);
void text_reinit (void);
void text_refresh (void);
void text_insert (gchar *fmt, ...) G_GNUC_PRINTF (1, 2);

void block_input (void);
void unblock_input (void);

#endif
