from __future__ import with_statement
import gdb

class AlanPrefixCommand(gdb.Command):
    "Prefix to do clever alan gdb commands"

    def __init__(self):
        super(AlanPrefixCommand, self).__init__("alan",
                                                gdb.COMMAND_SUPPORT,
                                                gdb.COMPLETE_NONE,
                                                True)

AlanPrefixCommand()

class AlanSymbolsCommand(gdb.Command):
    "Print the symbols in the Alan compiler symbol table"

    def __init__(self):
        super(AlanSymbolsCommand, self).__init__("alan symbols",
                                                 gdb.COMMAND_SUPPORT,
                                                 gdb.COMPLETE_NONE)

    def invoke(self, arg, from_tty):
        (symbol, found) = gdb.lookup_symbol("symbolTree")
        if symbol is not None:
            print("symbolTree = {} ({})".format(symbol.value(), symbol.type))
        else:
            print("Could not find symbolTree")

    def printSymbols(self, symbol):
        if symbol:
            print("{}: ")

AlanSymbolsCommand()

def select_field(self, field_name, field, fields):
    if field.name != field_name:
        return ((field.name, str(self.val[field.name])))
    else:
        kind = str(self.val['kind'])
        if kind in fields:
            return ((field_name+'.'+fields[kind], str(self.val[field.name][fields[kind]])))
        else:
            return None

def select_fields(self, field_name, fields):
    kids = []
    for field in self.val.type.fields():
        kids.append(select_field(self, field_name, field, fields))
    return kids
    
    
class SymbolPrinter(object):
    "Print an Alan Compiler Symbol Node"

    def __init__(self, val):
        self.val = val

    def to_string(self):
        return None

    def children(self):
        symbol_fields = {
            'CLASS_SYMBOL':'entity',
            'INSTANCE_SYMBOL':'entity',
            'VERB_SYMBOL':'verb',
            'PARAMETER_SYMBOL':'parameter',
            'LOCAL_SYMBOL':'local'
        }
        
        return select_fields(self, 'fields', symbol_fields)

    
class ExpressionPrinter(object):
    "Print an Alan Compiler Expression Node"

    def __init__(self, val):
        self.val = val

    def to_string(self):
        return None

    def children(self):
        expression_fields = {
            'WHERE_EXPRESSION':'whr',
            'ATTRIBUTE_EXPRESSION':'atr',
            'BINARY_EXPRESSION':'bin',
            'INTEGER_EXRESSION':'val',
            'STRING_EXRESSION':'str',
            'SET_EXRESSION':'set',
            'AGGREGATE_EXRESSION':'agr',
            'RANDOM_EXRESSION':'rnd',
            'RANDOM_IN_EXRESSION':'rin',
            'WHAT_EXPRESSION':'wht',
            'BETWEEN_EXRESSION':'btw',
            'ISA_EXRESSION':'isa'
        }
        
        return select_fields(self, 'fields', expression_fields)

    
class StatementPrinter(object):
    "Print an Alan Compiler Statement Node"

    def __init__(self, val):
        self.val = val

    def to_string(self):
        return None

    def children(self):
        statement_fields = {
            'PRINT_STATEMENT':'print',
            'STYLE_STATEMENT':'style',
            'SCORE_STATEMENT':'score',
            'VISITS_STATEMENT':'visits',
            'DESCRIBE_STATEMENT':'describe',
            'SAY_STATEMENT':'say',
            'LIST_STATEMENT':'list',
            'SHOW_STATEMENT':'show',
            'PLAY_STATEMENT':'play',
            'EMPTY_STATEMENT':'empty',
            'LOCATE_STATEMENT':'locate',
            'INCLUDE_STATEMENT':'include',
            'EXCLUDE_STATEMENT':'include',
            'MAKE_STATEMENT':'make',
            'SET_STATEMENT':'set',
            'INCREASE_STATEMENT':'incr',
            'DECREASE_STATEMENT':'incr',
            'SCHEDULE_STATEMENT':'schedule',
            'CANCEL_STATEMENT':'cancel',
            'IF_STATEMENT':'iff',
            'USE_STATEMENT':'use',
            'STOP_STATEMENT':'stop',
            'SYSTEM_STATEMENT':'system',
            'DEPEND_STATEMENT':'depend',
            'DEPENDCASE_STATEMENT':'depcase',
            'EACH_STATEMENT':'each',
            'STRIP_STATEMENT':'strip',
            'TRANSCRIPT_STATEMENT':'transcript'
        }

        return select_fields(self, 'fields', statement_fields)
        

class ListPrinter(object):
    "Print an Alan Compiler List Node"

    def __init__(self, val):
        self.val = val

    def to_string(self):
        return None

    def children(self):
        list_fields = {
            'ADD_LIST':'add',
            'ALTERNATIVE_LIST':'alt',
            'ATTRIBUTE_LIST':'atr',
            'CASE_LIST':'stm',
            'CHECK_LIST':'chk',
            'CLASS_LIST':'cla',
            'CONTAINER_LIST':'cnt',
            'ELEMENT_LIST':'elm',
            'EVENT_LIST':'evt',
            'EXIT_LIST':'ext',
            'EXPRESSION_LIST':'exp',
            'ID_LIST':'id',
            'INSTANCE_LIST':'ins',
            'LIMIT_LIST':'lim',
            'ELEMENT_ENTRIES_LIST':'eent',
            'LIST_LIST':'lst',
            'MESSAGE_LIST':'msg',
            'NAME_LIST':'',
            'REFERENCE_LIST':'word',
            'RESTRICTION_LIST':'res',
            'RESOURCE_LIST':'resource',
            'RULE_LIST':'rul',
            'SCRIPT_LIST':'script',
            'STATEMENT_LIST':'stm',
            'STEP_LIST':'stp',
            'STRING_LIST':'str',
            'SYMBOL_LIST':'sym',
            'SYNONYM_LIST':'syn',
            'SYNTAX_LIST':'stx',
            'SRCP_LIST':'srcp',
            'VERB_LIST':'vrb',
            'WORD_LIST':'word',
            'IFID_LIST':'ifid'
        }

        return select_fields(self, 'member', list_fields)
        
class SrcpPrinter(object):
    "Print an Alan Compiler Srcp struct"

    def __init__(self, val):
        self.val = val

    def to_string(self):
        return "{%d:%d:%d %d-%d}" % (self.val['file'], self.val['line'], self.val['col'], self.val['startpos'], self.val['endpos'])

    
import gdb.printing

def build_pretty_printers():
    pp = gdb.printing.RegexpCollectionPrettyPrinter("alanCommands")
    pp.add_printer('Symbol', '^Symbol$', SymbolPrinter)
    pp.add_printer('Expression', '^Expression$', ExpressionPrinter)
    pp.add_printer('Statement', '^Statement$', StatementPrinter)
    pp.add_printer('List', '^List$', ListPrinter)
    pp.add_printer('Srcp', '^Srcp$', SrcpPrinter)
    return pp

gdb.printing.register_pretty_printer(gdb.current_objfile(), build_pretty_printers(), replace=True)
