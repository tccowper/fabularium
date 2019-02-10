/*
 * Original ADRIFT code Copyright (C) 1997 - 2018 Campbell Wild
 * This port and modifications Copyright (C) 2018 - 2019 Tim Cadogan-Cowper.
 *
 * This file is part of Fabularium.
 *
 * Fabularium is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Fabularium; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.luxlunae.bebek.model.collection;

import android.support.annotation.NonNull;

import java.util.ArrayList;

public class MStringArrayList extends ArrayList<String> {

    @Override
    @NonNull
    public MStringArrayList clone() {
        return (MStringArrayList) super.clone();
    }

    @NonNull
    public String list() {
        int num = size();
        StringBuilder ret = new StringBuilder();
        for (String current : this) {
            ret.append(current);
            num--;
            if (num > 1) {
                ret.append(", ");
            }
            if (num == 1) {
                ret.append(" and ");
            }
        }
        if (ret.length() == 0) {
            return "nothing";
        }
        return ret.toString();
    }
}
