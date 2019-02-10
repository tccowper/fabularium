#!/usr/bin/env python

from xml.dom import minidom
import unittest

from alangrapher_utils import is_location, get_locations, get_exits, dot_for_location_header


class AlangrapherUtilsTests(unittest.TestCase):
    def test_can_see_if_instance_is_location(self):
        document = """<adventure>
                        <classes>
                        </classes>
                        <instances>
                            <instance NAME='loc1' PARENT='location'></instance>
                        </instances>
                    </adventure>"""
        xmltree = minidom.parseString(document)
        instance = xmltree.getElementsByTagName("instance")[0]
        self.assertTrue(is_location(instance, []))

    def test_can_see_if_instance_inherits_directly_from_class_inheriting_from_location(self):
        document = """<adventure>
                        <classes>
                            <class NAME='location_class' PARENT='location'>
                            </class>
                        </classes>
                        <instances>
                            <instance NAME='loc1' PARENT='location_class'></instance>
                        </instances>
                    </adventure>"""
        xmltree = minidom.parseString(document)
        instance = xmltree.getElementsByTagName("instance")[0]
        self.assertTrue(is_location(instance, xmltree.getElementsByTagName('class')))

    def test_can_see_if_instance_inherits_directly_from_class_inheriting_from_location_uppercase(self):
        document = """<adventure>
                        <classes>
                            <class NAME='location_class' PARENT='LOCATION'>
                            </class>
                        </classes>
                        <instances>
                            <instance NAME='loc1' PARENT='location_class'></instance>
                        </instances>
                    </adventure>"""
        xmltree = minidom.parseString(document)
        instance = xmltree.getElementsByTagName("instance")[0]
        self.assertTrue(is_location(instance, xmltree.getElementsByTagName('class')))
        
    def test_can_see_if_instance_inherits_indirectly_from_class_inheriting_from_location(self):
        document = """<adventure>
                        <classes>
                            <class NAME='location_class_parent' PARENT='location'>
                            </class>
                            <class NAME='location_class' PARENT='location_class_parent'>
                            </class>
                        </classes>
                        <instances>
                            <instance NAME='loc1' PARENT='location_class'></instance>
                        </instances>
                    </adventure>"""
        xmltree = minidom.parseString(document)
        instance = xmltree.getElementsByTagName("instance")[0]
        self.assertTrue(is_location(instance, xmltree.getElementsByTagName('class')))

    def test_can_return_list_of_locations(self):
        document = """<adventure>
                        <classes>
                            <class NAME='location_class_parent' PARENT='location'>
                            </class>
                            <class NAME='location_class' PARENT='location_class_parent'>
                            </class>
                        </classes>
                        <instances>
                            <instance NAME='loc1' PARENT='location_class'></instance>
                            <instance NAME='loc2' PARENT='location'></instance>
                            <instance NAME='loc3' PARENT='location_class_parent'></instance>
                        </instances>
                    </adventure>"""
        xmltree = minidom.parseString(document)
        self.assertEqual(3, len(get_locations(xmltree, [])))

    def test_can_return_list_of_locations_ignoring_some(self):
        document = """<adventure>
                        <classes>
                            <class NAME='location_class_parent' PARENT='location'>
                            </class>
                            <class NAME='location_class' PARENT='location_class_parent'>
                            </class>
                        </classes>
                        <instances>
                            <instance NAME='loc1' PARENT='location_class'></instance>
                            <instance NAME='loc2' PARENT='location'></instance>
                            <instance NAME='loc3' PARENT='location_class_parent'></instance>
                        </instances>
                    </adventure>"""
        xmltree = minidom.parseString(document)
        self.assertEqual(2, len(get_locations(xmltree, ['loc1'])))

    def test_can_find_exits_in_location_instance(self):
        document = """<adventure>
                        <instances>
                            <instance NAME='loc1' PARENT='location'>
                                <exit DIRECTION='e' TARGET='loc2'></exit>
                                <exit DIRECTION='w' TARGET='loc3' />
                            </instance>
                        </instances>
                    </adventure>"""
        location = get_locations(minidom.parseString(document), [])[0]
        self.assertEqual(2, len(get_exits(location, [])))

    def test_can_create_header_for_location(self):
        document = """<adventure>
                        <instances>
                            <instance NAME='loc1' PARENT='location'>
                                <exit DIRECTION='e' TARGET='loc1'></exit>
                            </instance>
                        </instances>
                    </adventure>"""
        location = get_locations(minidom.parseString(document), [])[0]
        self.assertEqual('loc1 [label="loc1"];', dot_for_location_header(location, start_location))

if __name__ == 'main':
    unittest.main()
