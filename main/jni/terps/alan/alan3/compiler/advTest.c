/*======================================================================*\
 *
 * advTest.c
 *
 * Unit tests for ADV node in the Alan compiler
 *
 *======================================================================*/

#include "adv.c"

#include <cgreen/cgreen.h>

#include "unitList.h"

#include "wht_x.h"
#include "exp_x.h"
#include "id_x.h"

static Srcp srcp = {2,3,4};
static Id *unknownId;
static Id *instanceId;
static Id *locationInstanceId;
static Id *classId;

Describe(Adventure);
BeforeEach(Adventure) {
    unknownId = newId(srcp, "UnknownId");
    instanceId = newId(srcp, "InstanceId");
    locationInstanceId = newId(srcp, "LocationInstanceId");
    classId = newId(srcp, "ClassId");
    initAdventure();
    (void) newClass(&srcp, classId, NULL, NULL);
    (void) newInstance(&srcp, instanceId, NULL, NULL);
    (void) newInstance(&srcp, locationInstanceId, newId(srcp, "location"), NULL);
}
AfterEach(Adventure) {}

Ensure(Adventure, can_analyze_start_at_here) {
    adv.whr = newWhere(&srcp, FALSE, WHERE_HERE, NULL);
    symbolizeAdventure();
    analyzeStartAt();		/* Can not Start At Here */
    assert_that(readSev(), is_equal_to(sevERR));
    assert_that(readEcode(), is_equal_to(211));
}

Ensure(Adventure, can_analyze_start_at_unknown_id) {
    adv.whr = newWhere(&srcp, FALSE, WHERE_AT,
                       newWhatExpression(srcp, newWhatId(srcp, unknownId)));
    symbolizeAdventure();
    assert_that(readSev(), is_equal_to(sevERR));
    assert_that(readEcode(), is_equal_to(310));
}

Ensure(Adventure, can_analyze_start_at_class) {
    adv.whr = newWhere(&srcp, FALSE, WHERE_AT,
                       newWhatExpression(srcp, newWhatId(srcp, classId)));
    symbolizeAdventure();
    analyzeStartAt();		/* Can not Start At Id not an instance */
    assert_that(readSev(), is_equal_to(sevERR));
    assert_that(readEcode(), is_equal_to(351));
}

Ensure(Adventure, can_analyze_start_at_not_location){
    adv.whr = newWhere(&srcp, FALSE, WHERE_AT,
                       newWhatExpression(srcp, newWhatId(srcp, instanceId)));
    symbolizeAdventure();
    analyzeStartAt();		/* Can not Start At Id not inheriting from location */
    assert_that(readSev(), is_equal_to(sevERR));
    assert_that(readEcode(), is_equal_to(351));
}

Ensure(Adventure, can_analyze_start_at_location) {
    adv.whr = newWhere(&srcp, FALSE, WHERE_AT,
                       newWhatExpression(srcp, newWhatId(srcp, locationInstanceId)));
    symbolizeAdventure();
    analyzeStartAt();		/* Can Start At Id that's an instance */
    assert_that(readSev(), is_equal_to(sevNONE));
    assert_that(readEcode(), is_equal_to(0));
}
