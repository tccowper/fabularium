# Declaration of sources required for various types of builds
#
# Unittests are done using the excellent
# CGreen unit test, stub and mocking framework by
# Marcus Baker et al. (http://sourceforge.net/projects/cgreen)

# Either using its runner which discovers test automatically...
# With everything mocked so they run in complete isolation...
MODULES_WITH_ISOLATED_UNITTESTS = \
	compatibility \
	exe \
	instance \
	lists \
	memory \
	rules \
	stack \

# ... or with a common runner (much less clean... but better than ...)
MODULES_WITH_UNITTESTS_USING_RUNNER = \
	args.c \
	AltInfo.c \
	act.c \
	debug.c \
	exe.c \
	instance.c \
	inter.c \
	main.c \
	output.c \
	ParameterPosition.c \
	params.c \
	parse.c \
	reverse.c \
	save.c \
	set.c \
	stack.c \
	state.c \
	StateStack.c \
	sysdep.c \
	utils.c \
	word.c \

# ... using a main program which requires remembering adding every test
# manually in a collector function
MODULES_WITH_UNITTESTS_USING_MAIN = \

MODULES_WITHOUT_UNITTESTS = \
	Container.c \
	Location.c \
	actor.c \
	attribute.c \
	checkentry.c \
	class.c \
	current.c \
	compatibility.c \
	decode.c \
	dictionary.c \
	event.c \
	lists.c \
	literal.c \
	memory.c \
	msg.c \
	options.c \
	readline.c \
	rules.c \
	scan.c \
	score.c \
	syntax.c \
	syserr.c \
	term.c \
	types.c \
	fnmatch.c

# All sources common for the main build
MAINSRCS = \
	$(MODULES_WITH_UNITTESTS_USING_RUNNER) \
	$(MODULES_WITH_UNITTESTS_USING_MAIN) \
	$(MODULES_WITHOUT_UNITTESTS)

MODULES_WITH_UNITTESTS_USING_RUNNER_TESTSRCS = ${MODULES_WITH_UNITTESTS_USING_RUNNER:.c=Tests.c}
MODULES_WITH_UNITTESTS_USING_MAIN_TESTSRCS = ${MODULES_WITH_UNITTESTS_USING_MAIN:.c=Tests.c}
UNITTESTSMAIN = unittests.c xml_reporter.c gopt.c

UNITTESTS_USING_MAIN_SRCS = $(UNITTESTSMAIN) $(MODULES_WITH_UNITTESTS_USING_RUNNER_TESTSRCS) $(MODULES_WITH_UNITTESTS_USING_MAIN_TESTSRCS) $(MODULES_WITHOUT_UNITTESTS)
UNITTESTS_USING_RUNNER_SRCS = $(MODULES_WITH_UNITTESTS_USING_RUNNER_TESTSRCS) $(MODULES_WITH_UNITTESTS_USING_MAIN) $(MODULES_WITHOUT_UNITTESTS)

UNITTESTS_ALL_SRCS = $(MAINSRCS) $(MODULES_WITH_UNITTESTS_USING_MAIN_TESTSRCS) $(MODULES_WITH_UNITTESTS_USING_RUNNER_TESTSRCS) $(UNITTESTSMAIN)

ARUNSRCS = arun.c $(MAINSRCS)
GLKSRCS = glkstart.c glkio.c
WINARUNSRCS = ${GLKSRCS} ${ARUNSRCS} winglk.c
GLKARUNSRCS = ${GLKSRCS} ${ARUNSRCS}
GARARUNSRCS = ${GLKSRCS} ${ARUNSRCS}

# Sources triggering new version marking
VERSIONSRCS = $(MAINSRCS) arun.c glkstart.c glkio.c winglk.c
