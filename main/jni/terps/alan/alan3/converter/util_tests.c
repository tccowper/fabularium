#include <cgreen/cgreen.h>
#include <cgreen/mocks.h>

#include "util.h"

#include "lmList.mock"


Describe(ConverterUtilities);
BeforeEach(ConverterUtilities) {}
AfterEach(ConverterUtilities) {}

static char string[100];

Ensure(ConverterUtilities, can_strip_multiple_spaces_from_empty_string) {
    strcpy(string, "");

    onlyOneSpace(string);
    assert_that(string, is_equal_to_string(""));
}

Ensure(ConverterUtilities, can_strip_multiple_space_from_single_space_string) {
    strcpy(string, " ");

    onlyOneSpace(string);
    assert_that(string, is_equal_to_string(" "));
}

Ensure(ConverterUtilities, can_strip_multiple_space_from_double_space_string) {
    strcpy(string, "  ");

    onlyOneSpace(string);
    assert_that(string, is_equal_to_string(" "));
}

Ensure(ConverterUtilities, can_strip_multiple_space_from_mulitple_double_space_occurencies) {
    strcpy(string, "a   b  c  d");

    onlyOneSpace(string);
    assert_that(string, is_equal_to_string("a b c d"));
}

Ensure(ConverterUtilities, can_strip_multiple_space_from_string_including_double_spaces) {
    strcpy(string, "a  b");

    onlyOneSpace(string);
    assert_that(string, is_equal_to_string("a b"));
}

Ensure(ConverterUtilities, can_strip_multiple_space_from_string_including_many_spaces) {
    strcpy(string, "a                   b");

    onlyOneSpace(string);
    assert_that(string, is_equal_to_string("a b"));
}

Ensure(ConverterUtilities, can_strip_single_tab_from_string) {
    strcpy(string, "a\tb");

    onlyOneSpace(string);
    assert_that(string, is_equal_to_string("a b"));
}

Ensure(ConverterUtilities, can_strip_multiple_tabs_from_string) {
    strcpy(string, "a\t\t\tb");

    onlyOneSpace(string);
    assert_that(string, is_equal_to_string("a b"));
}

Ensure(ConverterUtilities, can_strip_nl_and_tab_from_string) {
    strcpy(string, "abc def\n\tgeh ijk");

    onlyOneSpace(string);
    assert_that(string, is_equal_to_string("abc def geh ijk"));
}
