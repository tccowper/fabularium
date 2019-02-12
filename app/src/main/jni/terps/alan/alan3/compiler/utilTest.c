#include <cgreen/cgreen.h>

#include "util.c"

Describe(Utilities);
BeforeEach(Utilities) {}
AfterEach(Utilities) {}

Ensure(Utilities, spaces_returns_requested_number_of_spaces) {
    assert_that(strlen(spaces(3)), is_equal_to(3));
}

Ensure(Utilities, strmov_can_move_empty_string) {
    char *from = "";
    char to[50];

    assert_that(strlen(from), is_less_than(sizeof(to)));
    strmov(to, from);
    assert_that(to, is_equal_to_string(""));
}

Ensure(Utilities, strmov_can_move_string_of_one) {
    char *from = "1";
    char to[50];

    assert_that(strlen(from), is_less_than(sizeof(to)));
    strmov(to, from);
    assert_that(to, is_equal_to_string("1"));
}

Ensure(Utilities, strmov_can_move_string_of_many) {
    char *from = "this is many characters";
    char to[50];

    assert_that(strlen(from), is_less_than(sizeof(to)));
    strmov(to, from);
    assert_that(to, is_equal_to_string("this is many characters"));
}

Ensure(Utilities, can_find_third_filename) {
    List *fnm;
    fnm = concat(NULL, "file1", STRING_LIST);
    fnm = concat(fnm, "file2", STRING_LIST);
    fnm = concat(fnm, "file3", STRING_LIST);
    fnm = concat(fnm, "file4", STRING_LIST);

    fileNames = fnm;

    assert_that(fileName(3), is_equal_to_string("file3"));
}

Ensure(Utilities, can_create_version_string_with_buildnumber) {
    const char *the_version_string = version_string(666);
    char *end_of_version = strstr(the_version_string, alan.version.string)
        +strlen(alan.version.string);
    assert_that(end_of_version, begins_with_string("-666"));
}

Ensure(Utilities, can_create_version_string_without_buildnumber) {
    const char *the_version_string = version_string(0);
    char *end_of_version = strstr(the_version_string, alan.version.string)
        +strlen(alan.version.string);
    assert_that(end_of_version, does_not_begin_with_string("-"));
}
