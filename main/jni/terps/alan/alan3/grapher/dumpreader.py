__author__ = 'thomas'

import unittest


class DumpReader():
    def __init__(self, lines):
        self.reader = iter(lines)
        self.line = ""
        self.has_next = True
        try:
            while self.line.find("ADV:") == -1:
                self.line = self.reader.next()
        except StopIteration:
            self.has_next = False

    def __iter__(self):
        return self

    def next(self):
        if not self.has_next:
            raise StopIteration
        line_to_return = self.line
        try:
            self.line = self.reader.next()
            if len(self.line) > 0 and self.line[0] != ".":
                line_to_return += self.line[8:]
                self.line = self.reader.next()
        except StopIteration:
            self.has_next = False
        return line_to_return

def dummyLineIterator(lines):
    for x in lines:
        yield x


class DumpReaderTests(unittest.TestCase):
    def test_single_adv_line_returns_that_line(self):
        reader = DumpReader(["ADV:"])
        self.assertEqual("ADV:", reader.next())

    def test_first_line_returned_is_the_adv_line(self):
        reader = DumpReader(["anything", "another", "ADV:"])
        self.assertEqual("ADV:", reader.next())

    def test_reading_past_end_throws_StopIteration(self):
        reader = DumpReader([])
        try:
            reader.next()
        except StopIteration:
            None
        else:
            self.fail("Should get StopIteration")

    def test_continued_line_is_concatenated_when_split_between_words(self):
        reader = DumpReader(["ADV:", ". . . first line", "         second line"])
        reader.next()
        self.assertEqual(". . . first line second line", reader.next())

    def test_continued_line_is_concatenated_when_split_within_word(self):
        reader = DumpReader(["ADV:", ". . . firstline", "        secondline"])
        reader.next()
        self.assertEqual(". . . firstlinesecondline", reader.next())

if __name__ == '__main__':
    unittest.main()
