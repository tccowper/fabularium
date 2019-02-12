#!/usr/bin/env python
import sys
import os
from os.path import split

import argparse

import glib
import gtk
import gtk.gdk
import pygtk

import xdot

from alangrapher_utils import compile_game_to_xml, get_locations, get_exits, dot_for_location_header, dot_for_exit
import alangrapher_utils

IGNORE_LOCATIONS_TOOLTIP = "Locations listed here won't be include in the map. " \
                           "Separate location names with a space. " \
                           "This is handy for 'nowhere' and the like."

VERSION = "v0.4"


def handle_args():
    global parser
    parser = argparse.ArgumentParser(description='Create a graph over locations from an Alan source file.')
    parser.add_argument('filename', nargs="?", help='the name of the Alan source file to create the graph from.')
    parser.add_argument('--ignore-location', dest='ignore_list', action="append", metavar="LOCATION",
                        help="ignore the location with this name (handy for 'nowhere' etc.")
    parser.add_argument('--ignore-inherited', dest='inherited', action="store_false",
                        help="ignore any inherited exits")
    parser.add_argument('--shape', default="octagon",
                        help='shape to use for locations (see http://www.graphviz.org/doc/info/shapes.html for names)')
    parser.add_argument('--version', action='version', version='%(prog)s {}'.format(VERSION))
    return parser.parse_args()


def init_output(gamename, shape):
    return """
digraph {{
    concentrate=true;
    rankdir=LR;
    node [shape={};style=filled;]
""".format(shape)


def terminate_output():
    return '}'

def choose_file():
    chooser = gtk.FileChooserDialog(title="AlanGrapher {} - Select Alan source file to create a map from".format(VERSION),
                                 action=gtk.FILE_CHOOSER_ACTION_OPEN,
                                 buttons=(gtk.STOCK_CANCEL,gtk.RESPONSE_CANCEL,gtk.STOCK_OPEN,gtk.RESPONSE_OK))
    filter = gtk.FileFilter()
    filter.set_name("Alan source files")
    filter.add_pattern("*.alan")

    chooser.add_filter(filter)

    ignore_box = gtk.HBox()

    ignore_label = gtk.Label(" Ignore locations named: ")
    ignore_label.set_tooltip_text(IGNORE_LOCATIONS_TOOLTIP)
    ignore_box.pack_start(ignore_label, expand=False, fill=False)
    ignore_label.show()

    ignore_text = gtk.Entry()
    ignore_text.set_tooltip_text(IGNORE_LOCATIONS_TOOLTIP)
    ignore_box.add(ignore_text)
    ignore_text.show()

    chooser.set_extra_widget(ignore_box)

    response = chooser.run()
    if response == gtk.RESPONSE_CANCEL:
        chooser.destroy()
        sys.exit()

    filename = chooser.get_filename()

    filename = os.path.splitext(filename)[0]
    ignore_list = ignore_text.get_text().split()

    chooser.destroy()
    return (filename, ignore_list)    


def main():
    args = handle_args()
    if getattr(sys, 'frozen', False):
        basedir = sys._MEIPASS
    else:
        basedir = os.getcwd()

    if args.filename is None:
        filename, ignore_list = choose_file()
    else:
        filename = args.filename
        ignore_list = args.ignore_list

    path, filename = split(filename)

    if ignore_list is None:
        ignore_list = []

    os.chdir(path)
    try:
        xmltree = compile_game_to_xml(filename)

    except Exception as e:
        alangrapher_utils.message_dialog("Could not compile to XML!", "Do you have an Alan compiler installed "
                                        "and in the system PATH?\n" + str(e))
        sys.exit(-1)

    location_list = get_locations(xmltree, ignore_list)
    start_location = xmltree.getElementsByTagName("start")[0].attributes['WHERE'].value
    dotcode = init_output(os.path.basename(filename), args.shape)
    for l in location_list:
        name = l.attributes['NAME'].value
        dotcode += "\n    {}    \n".format(dot_for_location_header(l, start_location))
        xs = get_exits(l, ignore_list)
        for x in xs:
            dotcode += "      {}\n".format(dot_for_exit(name, x))
    dotcode += terminate_output()

    try:
        window = xdot.DotWindow()
        window.set_dotcode(dotcode)
        window.connect('destroy', gtk.main_quit)
        gtk.main()
    except Exception as e:
        alangrapher_utils.message_dialog("Could not draw graph!",
                                         "Do you have Graphviz (http://graphviz.org/)\n"
                                         "installed and in the system PATH?")
        sys.exit(-1)


if __name__ == '__main__':
    main()

