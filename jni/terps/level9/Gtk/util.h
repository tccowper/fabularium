/*
 * util.h - Utility functions
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

#ifndef _UTIL_H
#define _UTIL_H

#include "config.h"

gchar *file_selector (gboolean save, gchar *name, const gchar *filters[],
		      const gchar *title_fmt, ...) G_GNUC_PRINTF (4, 5);

#ifdef MAKE_FILENAMES_ABSOLUTE
gchar *make_filename_absolute (gchar *filename);
#endif

#endif
