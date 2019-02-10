#include "cgreen/cgreen.h"
#include "xml_reporter.h"
#include <stdlib.h>
#include "gopt.h"

#ifdef SMARTALLOC
#include "smartall.h"
#endif

#define ADD_UNIT_TESTS_FOR(module) \
  TestSuite *module##Tests(); \
  add_suite(suite, module##Tests());


static void add_unittests(TestSuite *suite) {
    ADD_UNIT_TESTS_FOR(add);
    ADD_UNIT_TESTS_FOR(adv);
    ADD_UNIT_TESTS_FOR(atr);
    ADD_UNIT_TESTS_FOR(cla);
    ADD_UNIT_TESTS_FOR(description);
    ADD_UNIT_TESTS_FOR(elm);
    ADD_UNIT_TESTS_FOR(emit);
    ADD_UNIT_TESTS_FOR(exp);
    ADD_UNIT_TESTS_FOR(ext);
    ADD_UNIT_TESTS_FOR(id);
    ADD_UNIT_TESTS_FOR(ifid);
    ADD_UNIT_TESTS_FOR(ins);
    ADD_UNIT_TESTS_FOR(lst);
    ADD_UNIT_TESTS_FOR(prop);
    ADD_UNIT_TESTS_FOR(res);
    ADD_UNIT_TESTS_FOR(resource);
    ADD_UNIT_TESTS_FOR(stm);
    ADD_UNIT_TESTS_FOR(stx);
    ADD_UNIT_TESTS_FOR(sym);
    ADD_UNIT_TESTS_FOR(vrb);
    ADD_UNIT_TESTS_FOR(util);
    ADD_UNIT_TESTS_FOR(whr);
    ADD_UNIT_TESTS_FOR(wrd);
}


static int compiler_unit_tests(int argc, const char **argv) {
    int return_code = 0;
    TestSuite *suite = create_named_test_suite("compiler_unit_tests");
    TestReporter *reporter;
    TextReporterOptions reporter_options;
    const char *prefix;
    const char *tmp;

    add_unittests(suite);

    void *options= gopt_sort(&argc, argv, gopt_start(
                                                     gopt_option( 'x', 
                                                                  GOPT_ARG, 
                                                                  gopt_shorts( 'x' ), 
                                                                  gopt_longs( "xml" )),
                                                     gopt_option( 'c', 
                                                                  GOPT_NOARG, 
                                                                  gopt_shorts( 'c' ), 
                                                                  gopt_longs( "color" ))));

    if (gopt_arg(options, 'x', &prefix))
        reporter = create_xml_reporter(prefix);
    else
        reporter = create_text_reporter();

    if (gopt_arg(options, 'c', &tmp))
        reporter_options.use_colours = true;
    else
        reporter_options.use_colours = false;
    
    set_reporter_options(reporter, &reporter_options);

    if (argc == 1) {
        return_code = run_test_suite(suite, reporter);
    } else if (argc == 2) {
        return_code = run_single_test(suite, argv[1], reporter);
    } else {
        printf("Usage: %s [--xml <fileprefix>] [<test case name>]\n", argv[0]);
    }
    return return_code;
}

int main(int argc, const char **argv) {
    return compiler_unit_tests(argc, argv);
}
