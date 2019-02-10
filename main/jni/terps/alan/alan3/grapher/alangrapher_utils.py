import subprocess
from xml.dom import minidom
from os.path import split
import sys
import os

import gtk


__author__ = 'Thomas'

def is_location(instance, classes):
    try:
        parent_name = instance.attributes['PARENT'].value
        while parent_name.lower() != 'location':
            parent_class = [c for c in classes
                            if c.attributes['NAME'].value.lower() == parent_name
                           ]
            parent_name = parent_class[0].attributes['PARENT'].value
        return True
    except Exception as e:
        return False

def get_locations(xmltree, ignore):
    classes = xmltree.getElementsByTagName("class")
    return [i for i in xmltree.getElementsByTagName("instance")
            if is_location(i, classes) and
            not (i.attributes['NAME'].value.lower() in map(str.lower, ignore))
           ]

def compile_game_to_xml(filename):
    xmlfilename = split(filename)[1]+".xml"
    if os.path.isfile(xmlfilename):
        os.remove(xmlfilename)
    try:
        p = subprocess.call(["alan", "-xml", filename])
    except Exception as e:
        message_dialog("Exception", str(type(e)) + str(e.args) + str(e))
    return minidom.parse(xmlfilename)


def get_exits(location, ignore):
    return [e for e in location.getElementsByTagName('exit')
            if not (e.attributes['TARGET'].value.lower() in map(str.lower, ignore))
            ]


def dot_for_location_header(l, start_location):
    name = l.attributes['NAME'].value.lower()
    if name == start_location:
        node_color = ", fillcolor=yellow"
    else:
        node_color = ""
    return '{0} [label="{0}"{1}];'.format(name, node_color)


def dot_for_exit(location_name, x):
    target = x.attributes['TARGET'].value.lower()
    direction = x.attributes['DIRECTION'].value.lower()
    return "{0} -> {1} [label={2}];".format(location_name.lower(), target, direction)

def message_dialog(message, text, title="Error!", type=gtk.MESSAGE_ERROR):
    dialog = gtk.MessageDialog(type=type, buttons=gtk.BUTTONS_OK,
                               message_format=message)
    dialog.set_title(title)
    dialog.format_secondary_text(text)
    dialog.run()
    dialog.destroy()
