;;; inform-mode.el --- Major mode for Inform 6 interactive fiction code

;; Author: Rupert Lane <rupert@rupert-lane.org>
;;         Gareth Rees <Gareth.Rees@cl.cam.ac.uk>
;;         Michael Fessler
;; Created: 1 Dec 1994
;; Version: 1.6.2
;; Released: 10-Oct-2013
;; Url: http://www.rupert-lane.org/inform-mode/
;; Keywords: languages

;;; Copyright:

;; Original version copyright (c) by Gareth Rees 1996
;; Portions copyright (c) by Michael Fessler 1997-1998
;; Portions copyright (c) by Rupert Lane 1999-2013

;; inform-mode is free software; you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation; either version 3, or (at your option)
;; any later version.
;;
;; inform-mode is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; General Public License for more details.

;;; Commentary:

;; Inform is a compiler for adventure games by Graham Nelson,
;; available at
;; http://www.inform-fiction.org/inform6.html
;;
;; This file implements a major mode for editing Inform 6 programs. It
;; understands most Inform syntax and is capable of indenting lines
;; and formatting quoted strings. Type `C-h m' within Inform mode for
;; more details.
;;
;; Because Inform header files use the extension ".h" just as C header
;; files do, the function `inform-maybe-mode' is provided.  It looks at
;; the contents of the current buffer; if it thinks the buffer is in
;; Inform, it selects inform-mode; otherwise it selects the mode given
;; by the variable `inform-maybe-other'.

;; Latest version of this mode can be found at
;; http://www.rupert-lane.org/inform-mode/

;; Please send any bugs or comments to rupert@rupert-lane.org

;;; History:

;; See the NEWS file in the distribution
;; or http://www.rupert-lane.org/inform-mode/news.html

;;; Code:

(require 'font-lock)
(require 'regexp-opt)
(require 'ispell)
(require 'term)
(require 'compile)
(require 'imenu)


;;;
;;; Customize support
;;;

(defgroup inform-mode nil
  "Settings for Inform source code."
  :group 'languages)

(defgroup inform-mode-indent nil
  "Customize indentation and highlighting of Inform source code."
  :group 'inform-mode)

(defgroup inform-mode-build-run nil
  "Customize build and run options for Inform code."
  :group 'inform-mode)



;;;
;;; General variables
;;;

(defconst inform-mode-version "1.6.2")

(defcustom inform-maybe-other 'c-mode
  "*`inform-maybe-mode' runs this if current file is not in Inform mode."
  :type 'function
  :group 'inform-mode)

(defcustom inform-startup-message t
  "*Non-nil means display a message when Inform mode is loaded."
  :type 'boolean
  :group 'inform-mode)

(defcustom inform-auto-newline t
  "*Non-nil means automatically newline before/after braces, after semicolons.
If you do not want a leading newline before opening braces then use:
  \(define-key inform-mode-map \"{\" 'inform-electric-semi\)"
  :type 'boolean
  :group 'inform-mode)

(defvar inform-mode-map nil
  "Keymap for Inform mode.")

(if inform-mode-map nil
  (let ((map (make-sparse-keymap "Inform")))
    (setq inform-mode-map (make-sparse-keymap))
    (define-key inform-mode-map "\C-m" 'newline-and-indent)
    (define-key inform-mode-map "\177" 'backward-delete-char-untabify)
    (define-key inform-mode-map "\C-c\C-r" 'inform-retagify)
    (define-key inform-mode-map "\C-c\C-t" 'visit-tags-table)
    (define-key inform-mode-map "\C-c\C-b" 'inform-build-project)
    (define-key inform-mode-map "\C-c\C-c" 'inform-run-project)
    (define-key inform-mode-map "\C-c\C-a" 'inform-toggle-auto-newline)
    (define-key inform-mode-map "\C-c\C-s" 'inform-spell-check-buffer)
    (define-key inform-mode-map "\M-n" 'inform-next-object)
    (define-key inform-mode-map "\M-p" 'inform-prev-object)
    (define-key inform-mode-map "{" 'inform-electric-brace)
    (define-key inform-mode-map "}" 'inform-electric-brace)
    (define-key inform-mode-map "]" 'inform-electric-brace)
    (define-key inform-mode-map ";" 'inform-electric-semi)
    (define-key inform-mode-map ":" 'inform-electric-key)
    (define-key inform-mode-map "!" 'inform-electric-key)
    (define-key inform-mode-map "," 'inform-electric-comma)
    (define-key inform-mode-map [menu-bar] (make-sparse-keymap))
    (define-key inform-mode-map [menu-bar inform] (cons "Inform" map))
    (define-key map [separator4] '("--" . nil))
    (define-key map [inform-spell-check-buffer]
      '("Spellcheck buffer" . inform-spell-check-buffer))
    (define-key map [ispell-region] '("Spellcheck region" . ispell-region))
    (define-key map [ispell-word] '("Spellcheck word" . ispell-word))
    (define-key map [separator3] '("--" . nil))
    (define-key map [load-tags] '("Load tags table" . visit-tags-table))
    (define-key map [retagify] '("Rebuild tags table" . inform-retagify))
    (define-key map [build] '("Build project" . inform-build-project))
    (define-key map [run] '("Run project" . inform-run-project))
    (define-key map [separator2] '("--" . nil))
    (define-key map [next-object] '("Next object" . inform-next-object))
    (define-key map [prev-object] '("Previous object" . inform-prev-object))
    (define-key map [separator1] '("--" . nil))
    (define-key map [comment-region] '("Comment Out Region" . comment-region))
    (put 'comment-region 'menu-enable 'mark-active)
    (define-key map [indent-region] '("Indent Region" . indent-region))
    (put 'indent-region 'menu-enable 'mark-active)
    (define-key map [indent-line] '("Indent Line" . indent-for-tab-command))))

(defvar inform-mode-abbrev-table nil
  "Abbrev table used while in Inform mode.")

(define-abbrev-table 'inform-mode-abbrev-table nil)



;;;
;;; Indentation parameters
;;;

(defcustom inform-indent-property 8
  "*Indentation of the start of a property declaration."
  :type 'integer
  :group 'inform-mode-indent)
(put 'inform-indent-property 'safe-local-variable 'integerp)

(defcustom inform-indent-has-with-class 1
  "*Indentation of has/with/class lines in object declarations."
  :type 'integer
  :group 'inform-mode-indent)
(put 'inform-indent-has-with-class 'safe-local-variable 'integerp)

(defcustom inform-indent-level 4
  "*Indentation of lines of block relative to first line of block."
  :type 'integer
  :group 'inform-mode-indent)
(put 'inform-indent-level 'safe-local-variable 'integerp)

(defcustom inform-indent-label-offset -3
  "*Indentation of label relative to where it should be."
  :type 'integer
  :group 'inform-mode-indent)
(put 'inform-indent-label-offset 'safe-local-variable 'integerp)

(defcustom inform-indent-cont-statement 4
  "*Indentation of continuation relative to start of statement."
  :type 'integer
  :group 'inform-mode-indent)
(put 'inform-indent-cont-statement 'safe-local-variable 'integerp)

(defcustom inform-indent-fixup-space t
  "*If non-NIL, fix up space in object declarations."
  :type 'boolean
  :group 'inform-mode-indent)
(put 'inform-indent-fixup-space 'safe-local-variable 'booleanp)

(defcustom inform-indent-action-column 40
  "*Column at which action names should be placed in verb declarations."
  :type 'integer
  :group 'inform-mode-indent)
(put 'inform-indent-action-column 'safe-local-variable 'integerp)

(defcustom inform-comments-line-up-p nil
  "*If non-nil, comments spread over several lines will line up with the first."
  :type 'boolean
  :group 'inform-mode-indent)
(put 'inform-comments-line-up-p 'safe-local-variable 'booleanp)

(defcustom inform-strings-line-up-p nil
  "*Variable controlling indentation of multi-line strings.
If nil (default), string will be indented according to context.
If a number, will always set the indentation to that column.
If 'char', will line up with the first character of the string.
If 'quote', or other non-nil value, will line up with open quote on
first line."
  :type '(radio (const :tag "Indent according to context" nil)
                (integer :tag "Column to indent to")
                (const :tag "Line up with first character of string" char)
                (const :tag "Line up with open quote on first line" quote))
  :group 'inform-mode-indent)
(put 'inform-strings-line-up-p 'safe-local-variable 'sexp)

(defcustom inform-indent-semicolon t
  "*If nil, a semicolon on a line of its own will not be indented."
  :type 'boolean
  :group 'inform-mode-indent)
(put 'inform-indent-semicolon 'safe-local-variable 'booleanp)



;;;
;;; Syntax variables
;;;

(defvar inform-mode-syntax-table nil
  "Syntax table to use in Inform mode buffers.")

(if inform-mode-syntax-table
    nil
  (setq inform-mode-syntax-table (make-syntax-table))
  (modify-syntax-entry ?\\ "\\" inform-mode-syntax-table)
  (modify-syntax-entry ?\n ">" inform-mode-syntax-table)
  (modify-syntax-entry ?! "<" inform-mode-syntax-table)
  (modify-syntax-entry ?# "_" inform-mode-syntax-table)
  (modify-syntax-entry ?% "." inform-mode-syntax-table)
  (modify-syntax-entry ?& "." inform-mode-syntax-table)
  (modify-syntax-entry ?\' "." inform-mode-syntax-table)
  (modify-syntax-entry ?* "." inform-mode-syntax-table)
  (modify-syntax-entry ?- "." inform-mode-syntax-table)
  (modify-syntax-entry ?/ "." inform-mode-syntax-table)
  (modify-syntax-entry ?\; "." inform-mode-syntax-table)
  (modify-syntax-entry ?< "." inform-mode-syntax-table)
  (modify-syntax-entry ?= "." inform-mode-syntax-table)
  (modify-syntax-entry ?> "." inform-mode-syntax-table)
  (modify-syntax-entry ?+ "." inform-mode-syntax-table)
  (modify-syntax-entry ?| "." inform-mode-syntax-table)
  (modify-syntax-entry ?^ "w" inform-mode-syntax-table))


;;;
;;; Build and run variables
;;;

(defcustom inform-project-file nil
  "*The top level Inform project file to which the current file belongs."
  :type '(radio (file :tag "Project file name")
                (const :tag "Disabled" nil))
  :group 'inform-mode-build-run)

(make-variable-buffer-local 'inform-project-file)

(defcustom inform-autoload-tags t
  "*Non-nil means automatically load tags table when entering Inform mode."
  :type 'boolean
  :group 'inform-mode-build-run)

(defcustom inform-etags-program "etags"
  "*The shell command with which to run the etags program."
  :type 'string
  :group 'inform-mode-build-run)

(defcustom inform-command "inform"
  "*The shell command with which to run the Inform compiler."
  :type 'string
  :group 'inform-mode-build-run)

(defcustom inform-libraries-directory nil
  "*If non-NIL, gives the directory in which libraries are found."
  :type '(radio (directory :tag "Library directory")
                (const :tag "Disabled" nil))
  :group 'inform-mode-build-run)

(defcustom inform-command-options ""
  "*Options with which to call the Inform compiler."
  :type 'string
  :group 'inform-mode-build-run)

(defcustom inform-interpreter-command "frotz"
  "*The command with which to run the ZCode interpreter.
If a string, the name of a command.  If a symbol or a function value, an
Emacs-lisp function to be called with the name of the story file."
  :type '(choice (string :tag "Command to run the ZCode interpreter")
                 (function :tag "Emacs-lisp function to run on the file"))
  :group 'inform-mode-build-run)

(defcustom inform-interpreter-options ""
  "*Additional options with which to call the ZCode interpreter.
Only used if `inform-interpreter-command' is a string."
  :type 'string
  :group 'inform-mode-build-run)

(defcustom inform-interpreter-kill-old-process t
  "*Whether to kill the old interpreter process when starting a new one."
  :type 'boolean
  :group 'inform-mode-build-run)

(defcustom inform-interpreter-is-graphical nil
  "*Controls whether `inform-interpreter-command' will be run in a buffer.
If NIL, `inform-run-project' will switch to the interpreter buffer after
running the interpreter."
  :type 'boolean
  :group 'inform-mode-build-run)

(defvar inform-compilation-error-regexp-alist
  '((inform-e1
     "^[ \t]*\\(\\(?:[a-zA-Z]:\\)?[^:(\t\n]+\\)(\\([0-9]+\\)): ?\
\\(?:\\(Error\\)\\|\\(Warning\\)\\):"
     1 2 nil (4)))
  "Alist matching compilation errors for inform -E1 style output.")



;;; Keyword definitions-------------------------------------------------------

;; These are used for syntax and font-lock purposes.
;; They combine words used in Inform 5 and Inform 6 for full compatibility.
;; You can add new keywords directly to this list as the regexps for
;; font-locking are defined when this file is byte-compiled or eval'd.

(eval-and-compile
  (defvar inform-directive-list
    '("abbreviate" "array" "attribute" "btrace" "class" "constant"
      "default" "dictionary" "end" "endif" "etrace" "extend" "fake_action"
      "global" "ifdef" "iffalse" "ifndef" "ifnot" "iftrue" "ifv3" "ifv5"
      "import" "include" "link" "listsymbols" "listdict" "listverbs"
      "lowstring" "ltrace" "message" "nearby" "nobtrace" "noetrace"
      "noltrace" "notrace" "object" "property" "release" "replace" "serial"
      "statusline" "stub" "switches" "system_file" "trace" "verb"
      "version" "zcharacter")
    "List of Inform directives that shouldn't appear embedded in code.")

  (defvar inform-defining-list
    '("[" "array" "attribute" "class" "constant" "fake_action" "global"
      "lowstring" "nearby" "object" "property")
    "List of Inform directives that define a variable/constant name.
Used to build a font-lock regexp; the name defined must follow the
keyword.")

  (defvar inform-attribute-list
    '("absent" "animate" "clothing" "concealed" "container" "door"
      "edible" "enterable" "female" "general" "light" "lockable" "locked"
      "male" "moved" "neuter" "on" "open" "openable" "pluralname" "proper"
      "scenery" "scored" "static" "supporter" "switchable" "talkable"
      "transparent" "visited" "workflag" "worn")
    "List of Inform attributes defined in the library.")

  (defvar inform-property-list
    '("n_to" "s_to" "e_to" "w_to" "ne_to" "se_to" "nw_to" "sw_to" "u_to"
      "d_to" "in_to" "out_to" "add_to_scope" "after" "article" "articles"
      "before" "cant_go" "capacity" "daemon" "describe" "description"
      "door_dir" "door_to" "each_turn" "found_in" "grammar" "initial"
      "inside_description" "invent" "life" "list_together" "name" "number"
      "orders" "parse_name" "plural" "react_after" "react_before"
      "short_name" "short_name_indef" "time_left" "time_out" "when_closed"
      "when_open" "when_on" "when_off" "with_key")
    "List of Inform properties defined in the library.")

  (defvar inform-code-keyword-list
    '("box" "break" "continue" "do" "else" "font off" "font on" "for"
      "give" "has" "hasnt" "if" "in" "inversion" "jump" "move" "new_line"
      "notin" "objectloop" "ofclass" "or" "print" "print_ret" "provides"
      "quit" "read" "remove" "restore" "return" "rfalse" "rtrue" "save"
      "spaces" "string" "style bold" "style fixed" "style reverse"
      "style roman" "style underline" "switch" "to" "until" "while")
    "List of Inform code keywords.")
  )

;; Some regular expressions are needed at compile-time too so as to
;; avoid postponing the work to load time.

;; To do the work of building the regexps we use regexp-opt with the
;; paren option which ensures the result is enclosed by a grouping
;; construct.

(eval-and-compile
  (defun inform-make-regexp (strings)
    (regexp-opt strings t)))

(eval-and-compile
  (defvar inform-directive-regexp
    (concat "\\<#?\\("
            (inform-make-regexp inform-directive-list)
            "\\)\\>")
    "Regular expression matching an Inform directive.")

  (defvar inform-defining-list-regexp
    (inform-make-regexp inform-defining-list))

  (defvar inform-object-regexp
    "#?\\<\\(object\\|nearby\\|class\\)\\>"
    "Regular expression matching start of object declaration.")

  (defvar inform-property-regexp
    (concat "\\s-*\\("
            (inform-make-regexp inform-property-list)
            "\\)")
    "Regular expression matching Inform properties."))


(defvar inform-real-object-regexp
  (eval-when-compile (concat "^" inform-object-regexp))
  "Regular expression matching the start of a real object declaration.
That is, one found at the start of a line.")

(defvar inform-label-regexp "[^]:\"!\(\n]+\\(:\\|,\\)"
  "Regular expression matching a label.")

(defvar inform-action-regexp "\\s-*\\*"
  "Regular expression matching an action line in a verb declaration.")

(defvar inform-statement-terminators '(?\; ?{ ?} ?: ?\) do else)
  "Tokens which precede the beginning of a statement.")


;;;
;;; Font-lock keywords
;;;

(defvar inform-font-lock-defaults
  '(inform-font-lock-keywords nil t ((?_ . "w") (?' . "$")) inform-prev-object)
  "Font Lock defaults for Inform mode.")

(defface inform-dictionary-word-face
  '((((class color) (background light)) (:foreground "Red"))
    (((class color) (background dark)) (:foreground "Pink"))
    (t (:italic t :bold t)))
  "Font lock mode face used to highlight dictionary words."
  :group 'inform-mode)

(defvar inform-dictionary-word-face 'inform-dictionary-word-face
  "Variable for Font lock mode face used to highlight dictionary words.")

(defvar inform-font-lock-keywords
  (eval-when-compile
    (list

     ;; Inform code keywords
     ;; Handles two keywords in a row, eg 'else return'
     (cons (concat "\\b"
                   (inform-make-regexp inform-code-keyword-list)
                   "\\b")
           'font-lock-keyword-face)

     ;; Keywords that declare variable or constant names.
     (list
      (concat "^#?"
              inform-defining-list-regexp
              "\\s-+\\(->\\s-+\\)*\\(\\(\\w\\|\\s_\\)+\\)")
      '(1 font-lock-keyword-face)
      '(3 font-lock-function-name-face))

     ;; Other directives.
     (cons inform-directive-regexp 'font-lock-keyword-face)

     ;; Single quoted strings, length > 1, are dictionary words
     '("'\\(\\(-\\|\\w\\)\\(\\(-\\|\\w\\)+\\(//\\w*\\)?\\|//\\w*\\)\\)'"
       (1 inform-dictionary-word-face append))

     ;; Double-quoted dictionary words
     '("\\(\\s-name\\s-\\|^Verb\\|^Extend\\|^\\s-+\\*\\)"
       ("\"\\(\\(-\\|\\w\\)+\\)\"" nil nil
        (1 inform-dictionary-word-face t)))

     ;; More double-quoted dictionary words
     '("^\\s-+\"\\(\\(-\\|\\w\\)+\\)\"\\s-+\"\\(\\(-\\|\\w\\)+\\)\""
       (1 inform-dictionary-word-face t)
       (3 inform-dictionary-word-face t)
       ("\"\\(\\(-\\|\\w\\)+\\)\"" nil nil
        (1 inform-dictionary-word-face t)))

     ;; `private', `class', `has' and `with' in objects.
     '("^\\s-+\\(private\\|class\\|has\\|with\\)\\(\\s-\\|$\\)"
       (1 font-lock-keyword-face))

     ;; Attributes and properties.
     (cons (concat "[^#]\\<\\("
                   (inform-make-regexp (append inform-attribute-list
                                               inform-property-list))
                   "\\)\\>")
           '(1 font-lock-variable-name-face))))
  "Expressions to fontify in Inform mode.")


;;;
;;; Inform mode
;;;

;;;###autoload
(defun inform-mode ()
  "Major mode for editing Inform programs.

* Inform syntax:

  Type \\[indent-for-tab-command] to indent the current line.
  Type \\[indent-region] to indent the region.

  Type \\[fill-paragraph] to fill strings or comments.
  This compresses multiple spaces into single spaces.

* Multi-file projects:

  The variable `inform-project-file' gives the name of the root file of
  the project \(i.e., the one that you run Inform on\)\; it is best to
  set this as a local variable in each file, for example by making
     ! -*- inform-project-file:\"game.inf\" -*-
  the first line of the file.

* Tags tables:

  Type \\[inform-retagify] to build \(and load\) a Tags table.
  Type \\[visit-tags-table] to load an existing Tags table.
  If it exists, and if the variable `inform-autoload-tags' is non-NIL,
  the Tags table is loaded on entry to Inform Mode.
  With a Tags table loaded, type \\[find-tag] to find the declaration of
  the object, class or function under point.

* Navigating in a file:

  Type \\[inform-prev-object] to go to the previous object/class declaration.
  Type \\[inform-next-object] to go to the next one.

* Compilation:

  Type \\[inform-build-project] to build the current project.
  Type \\[next-error] to go to the next error.

* Running:

  Type \\[inform-run-project] to run the current project in an
  interpreter, either as a separate process or in an Emacs terminal buffer.

* Spell checking:

  Type \\[inform-spell-check-buffer] to spell check all strings in the buffer.
  Type \\[ispell-word] to check the single word at point.

* Key definitions:

\\{inform-mode-map}
* Functions:

  inform-maybe-mode
    Looks at the contents of a file, guesses whether it is an Inform
    program, runs `inform-mode' if so, or `inform-maybe-other' if not.
    The latter defaults to `c-mode'.  Used for header files which might
    be Inform or C programs.

* Miscellaneous user options:

  inform-startup-message
    Set to nil to inhibit message first time Inform mode is used.

  inform-maybe-other
    The mode used by `inform-maybe-mode' if it guesses that the file is
    not an Inform program.

  inform-mode-hook
    This hook is run after entry to Inform Mode.

  inform-autoload-tags
    If non-nil, then a tags table will automatically be loaded when
    entering Inform mode.

  inform-auto-newline
    If non-nil, then newlines are automatically inserted before and
    after braces, and after semicolons in Inform code, and after commas
    in object declarations.

* User options controlling indentation style:

  Values in parentheses are the default indentation style.

  inform-indent-property \(8\)
    Indentation of a property or attribute in an object declaration.

  inform-indent-has-with-class \(1\)
    Indentation of has/with/class/private lines in object declaration.

  inform-indent-level \(4\)
    Indentation of line of code in a block relative to the first line of
    the block.

  inform-indent-label-offset \(-3\)
    Indentation of a line starting with a label, relative to the
    indentation if the label were absent.

  inform-indent-cont-statement \(4\)
    Indentation of second and subsequent lines of a statement, relative
    to the first.

  inform-indent-fixup-space \(T\)
    If non-NIL, fix up space after `Object', `Class', `Nearby', `has',
    `private' and `with', so that all the object's properties line up.

  inform-indent-action-column \(40\)
    Column at which action names should be placed in verb declarations.
    If NIL, then action names are not moved.

  inform-comments-line-up-p \(NIL\)
    If non-NIL, comments spread out over several lines will start on the
    same column as the first comment line.

  inform-strings-line-up-p \(NIL\)
    Variable controlling indentation of multi-line strings.
    If nil (default), string will be indented according to context.
    If a number, will always set the indentation to that column.
    If 'char', will line up with the first character of the string.
    If 'quote', or other non-nil value, will line up with open quote on
    first line.

* User options to do with compilation:

  inform-command
    The shell command with which to run the Inform compiler.

  inform-libraries-directory
    If non-NIL, gives the directory in which the Inform libraries are
    found.

  inform-command-options
    Additional options with which to call the Inform compiler.

* User options to do with an interpreter:

  inform-interpreter-command
    The command with which to run the ZCode interpreter.  Can be a
    string (a command to be run), a symbol (name of function to call)
    or a function.

  inform-interpreter-options
    Additional options with which to call the ZCode interpreter.  Only
    used if `inform-interpreter-command' is a string.

  inform-interpreter-kill-old-process
    If non-NIL, `inform-run-project' will kill any running interpreter
    process and start a new one.  If not, will switch to the interpreter's
    buffer (if necessary - see documentation for `inform-run-project' for
    details).

  inform-interpreter-is-graphical
    If NIL, `inform-run-project' will switch to the interpreter buffer
    after running the interpreter.


* Please send any bugs or comments to rupert@rupert-lane.org"

  (interactive)
  (if inform-startup-message
      (message "Emacs Inform mode version %s." inform-mode-version))
  (kill-all-local-variables)
  (use-local-map inform-mode-map)
  (set-syntax-table inform-mode-syntax-table)
  (make-local-variable 'comment-column)
  (make-local-variable 'comment-end)
  (make-local-variable 'comment-indent-function)
  (make-local-variable 'comment-start)
  (make-local-variable 'comment-start-skip)
  (make-local-variable 'fill-paragraph-function)
  (make-local-variable 'font-lock-defaults)
  (make-local-variable 'imenu-extract-index-name-function)
  (make-local-variable 'imenu-prev-index-position-function)
  (make-local-variable 'indent-line-function)
  (make-local-variable 'indent-region-function)
  (make-local-variable 'parse-sexp-ignore-comments)
  (make-local-variable 'require-final-newline)
  (setq comment-column 40
        comment-end ""
        comment-indent-function 'inform-comment-indent
        comment-start "!"
        comment-start-skip "!+\\s-*"
        fill-paragraph-function 'inform-fill-paragraph
        font-lock-defaults inform-font-lock-defaults
        imenu-extract-index-name-function 'inform-imenu-extract-name
        imenu-prev-index-position-function 'inform-prev-object
        indent-line-function 'inform-indent-line
        indent-region-function 'inform-indent-region
        local-abbrev-table inform-mode-abbrev-table
        major-mode 'inform-mode
        mode-name "Inform"
        parse-sexp-ignore-comments t
        require-final-newline t)
  (auto-fill-mode 1)
  (if inform-autoload-tags
      (inform-auto-load-tags-table))
  (run-hooks 'inform-mode-hook))

;;;###autoload
(defun inform-maybe-mode ()
  "Start Inform mode if file is in Inform; `inform-maybe-other' otherwise."
  (let ((case-fold-search t))
    (if (save-excursion
          (re-search-forward
           "^\\(!\\|object\\|nearby\\|\\[ \\)"
           nil t))
        (inform-mode)
      (funcall inform-maybe-other))))


;;;
;;; Syntax and indentation
;;;

(defun inform-beginning-of-defun ()
  "Go to the start of the current Inform definition.
Just goes to the most recent line with a function beginning [, or
a directive."
  (let ((case-fold-search t))
    (catch 'found
      (end-of-line 1)
      (while (re-search-backward "\n[[a-z]" nil 'move)
        (forward-char 1)
        (if (or (and (looking-at "\\[")
                     (eq (inform-preceding-char) ?\;))
                (looking-at inform-directive-regexp))
            (throw 'found nil))
        (forward-char -1)))))

(defun inform-preceding-char ()
  "Return preceding non-blank, non-comment character in buffer.
It is assumed that point is not inside a string or comment."
  (save-excursion
    (while (/= (point) (progn (forward-comment -1) (point))))
    (skip-syntax-backward " ")
    (if (bobp) ?\;
      (preceding-char))))

(defun inform-preceding-token ()
  "Return preceding non-blank, non-comment token in buffer.
Either the character itself, or the tokens 'do or 'else. It is
assumed that point is not inside a string or comment."
  (save-excursion
    (while (/= (point) (progn (forward-comment -1) (point))))
    (skip-syntax-backward " ")
    (if (bobp) ?\;
      (let ((p (preceding-char)))
        (cond ((and (eq p ?o)
                    (>= (- (point) 2) (point-min)))
               (goto-char (- (point) 2))
               (if (looking-at "\\<do") 'do p))
              ((and (eq p ?e)
                    (>= (- (point) 4) (point-min)))
               (goto-char (- (point) 4))
               (if (looking-at "\\<else") 'else p))
              (t p))))))

;; `inform-syntax-class' returns a list describing the syntax at point.

;; The returned list is of the form (SYNTAX IN-OBJ SEXPS STATE).
;; SYNTAX is one of

;;  directive  An Inform directive (given by `inform-directive-list')
;;  has        The "has" keyword
;;  with       The "with" keyword
;;  class      The "class" keyword
;;  private    The "private" keyword
;;  property   A property or attribute
;;  other      Any other line not in a function body
;;  string     The line begins inside a string
;;  comment    The line starts with a comment
;;  label      Line contains a label (i.e. has a colon in it)
;;  code       Any other line inside a function body
;;  blank      A blank line
;;  action     An action line in a verb declaration

;; IN-OBJ is non-NIL if the line appears to be inside an Object, Nearby,
;; or Class declaration.

;; SEXPS is a list of pairs (D . P) where P is the start of a sexp
;; containing point and D is its nesting depth.  The pairs are in
;; decreasing order of nesting depth.

;; STATE is the list returned by `parse-partial-sexp'.

;; For reasons of speed, `inform-syntax-class' looks for directives only
;; at the start of lines.  If the source contains top-level directives
;; not at the start of lines, or anything else at the start of a line
;; that might be mistaken for a directive, the wrong syntax class may be
;; returned.

;; There are circumstances in which SEXPS might not be complete (namely
;; if there were multiple opening brackets and some but not all have
;; been closed since the last call to `inform-syntax-class'), and rare
;; circumstances in which it might be wrong (namely if there are
;; multiple closing brackets and fewer, but at least two, opening
;; bracket since the last call).  I consider these cases not worth
;; worrying about - and the speed hit of checking for them is
;; considerable.

(defun inform-syntax-class (&optional defun-start data)
  "Return a list describing the syntax at point.
Optional argument DEFUN-START gives the point from which parsing
should start, and DATA is the list returned by a previous invocation
of `inform-syntax-class'. See code for details on the return type."
  (let ((line-start (point))
        in-obj state
        (case-fold-search t))
    (save-excursion
      (cond (defun-start
              (setq state (parse-partial-sexp defun-start line-start nil nil
                                              (nth 3 data)))
              (setq in-obj
                    (cond ((or (> (car state) 0) (nth 3 state) (nth 4 state))
                           (nth 1 data))
                          ((nth 1 data) (/= (inform-preceding-char) ?\;))
                          (t (looking-at inform-object-regexp)))))
            (t
             (inform-beginning-of-defun)
             (setq in-obj (looking-at inform-object-regexp)
                   state (parse-partial-sexp (point) line-start)))))

    (list
     (if (> (car state) 0)
         ;; If there's a containing sexp then it's easy.
         (cond ((nth 3 state) 'string)
               ((nth 4 state) 'comment)
               ((looking-at (concat "\\s-*" comment-start)) 'comment)
               ((looking-at inform-label-regexp) 'label)
               (t 'code))

       ;; Otherwise there are a bunch of special cases (has, with, class,
       ;; and private properties) that must be checked for.  Note that
       ;; we have to distinguish between global class declarations and
       ;; class membership in an object declaration.  This is done by
       ;; looking for a preceding semicolon.
       (cond ((nth 3 state) 'string)
             ((nth 4 state) 'comment)
             ((looking-at (concat "\\s-*" comment-start)) 'comment)
             ((and in-obj (looking-at "\\s-*class\\>")
                   (/= (inform-preceding-char) ?\;))
              'class)
             ((looking-at inform-action-regexp) 'action)
             ((looking-at inform-directive-regexp) 'directive)
             ((and (looking-at "\\[") (eq (inform-preceding-char) ?\;))
              'directive)
             ((and (not in-obj) (eq (inform-preceding-char) ?\;))
              'directive)
             ((looking-at "\\s-*$") 'blank)
             ((not in-obj) 'other)
             ((looking-at "\\s-*has\\(\\s-\\|$\\)") 'has)
             ((looking-at "\\s-*with\\(\\s-\\|$\\)") 'with)
             ((looking-at "\\s-*private\\(\\s-\\|$\\)") 'private)
             ((or (eq (inform-preceding-char) ?,)
                  (looking-at inform-property-regexp))
              'property)
             ;; This handles declarations of objects in a class eg
             ;; Bird "swallow";
             ;; It assumes that class names follow the convention of being
             ;; capitalised. This is not the most elegant way of handling
             ;; this case but in practice works well.
             ((looking-at "\\s-*[A-Z]")
              'directive)
             (t
              'other)))

     ;; Are we in an object?
     (if (and in-obj
              (not (looking-at inform-object-regexp))
              (zerop (car state))
              (eq (inform-preceding-char) ?\;))
         nil
       in-obj)

     ;; List of known enclosing sexps.
     (let ((sexps (nth 2 data))         ; the old list of sexps
           (depth (car state))          ; current nesting depth
           (sexp-start (nth 1 state)))  ; enclosing sexp, if any
       (if sexps
           ;; Strip away closed sexps.
           (let ((sexp-depth (car (car sexps))))
             (while (and sexps (or (> sexp-depth depth)
                                   (and (eq sexp-depth depth)
                                        sexp-start)))
               (setq sexps (cdr sexps)
                     sexp-depth (if sexps (car (car sexps)))))))
       (if sexp-start
           (setq sexps (cons (cons depth sexp-start) sexps)))
       sexps)

     ;; State from the parse algorithm.
     state)))

(defun inform-calculate-indentation (data)
"Return the correct indentation for the line at point.
DATA is the syntax class for the start of the line (as returned
by `inform-syntax-class'). It is assumed that point is somewhere
in the indentation for the current line (i.e., everything to the
left is whitespace)."
  (let ((syntax (car data))             ; syntax class of start of line
        (in-obj (nth 1 data))           ; inside an object?
        (depth (car (nth 3 data)))      ; depth of nesting of start of line
        (case-fold-search t))           ; searches are case-insensitive
    (cond

     ;; Directives should never be indented or else the directive-
     ;; finding code won't run fast enough.  Hence the magic
     ;; constant 0.
     ((eq syntax 'directive) 0)
     ((eq syntax 'blank) 0)

     ;; Semicolons on a line of their own will be indented per the
     ;; current syntax unless user variable inform-indent-semicolon is
     ;; nil.
     ((and (looking-at "\\s-*;$") (not inform-indent-semicolon)) 0)

     ;; Various standard indentations.
     ((eq syntax 'property) inform-indent-property)
     ((eq syntax 'other)
      (cond ((looking-at "\\s-*\\[") inform-indent-property)
            (in-obj (+ inform-indent-property inform-indent-level))
            (t inform-indent-level)))
     ((and (eq syntax 'string) (zerop depth))
      (cond (in-obj (+ inform-indent-property inform-indent-level))
            (t inform-indent-level)))
     ((and (eq syntax 'comment) (zerop depth))
      (inform-line-up-comment
       (if in-obj inform-indent-property 0)))
     ((eq syntax 'action) inform-indent-level)
     ((memq syntax '(has with class private)) inform-indent-has-with-class)

     ;; We are inside a sexp of some sort.
     (t
      (let ((indent 0)                  ; calculated indent column
            paren                       ; where the enclosing sexp begins
            string-start                ; where string (if any) starts
            (string-indent 0)           ; indentation for the current str
            cont-p                      ; true if line is a continuation
            paren-char                  ; the parenthesis character
            prec-token                  ; token preceding line
            this-char)                  ; character under consideration
        (save-excursion

          ;; Skip back to the start of a string, if any.  (Note that
          ;; we can't be in a comment since the syntax class applies
          ;; to the start of the line.)
          (if (eq syntax 'string)
              (progn
                (skip-syntax-backward "^\"")
                (forward-char -1)
                (setq string-start (point))
                (setq string-indent (current-column))
                ))

          ;; Now find the start of the sexp containing point.  Most
          ;; likely, the location was found by `inform-syntax-class';
          ;; if not, call `up-list' now and save the result in case
          ;; it's useful in future.
          (save-excursion
            (let ((sexps (nth 2 data)))
              (if (and sexps (eq (car (car sexps)) depth))
                  (goto-char (cdr (car sexps)))
                (up-list -1)
                (setcar (nthcdr 2 data)
                        (cons (cons depth (point)) (nth 2 data)))))
            (setq paren (point)
                  paren-char (following-char)))

          ;; If we were in a string, now skip back to the start of the
          ;; line.  We have to do this *after* calling `up-list' just
          ;; in case there was an opening parenthesis on the line
          ;; including the start of the string.
          (if (eq syntax 'string)
              (forward-line 0))

          ;; The indentation depends on what kind of sexp we are in.
          ;; If line is in parentheses, indent to opening parenthesis.
          (if (eq paren-char ?\()
              (setq indent (progn (goto-char paren) (1+ (current-column))))

            ;; Line not in parentheses.
            (setq prec-token (inform-preceding-token)
                  this-char (following-char))
            (cond

             ;; Each 'else' should have the same indentation as the
             ;; matching 'if'
             ((looking-at "\\s-*else")
              ;; Find the matching 'if' by counting 'if's and 'else's
              ;; in this sexp
              (let ((if-count 0) found)
                (while (and (not found)
                            (progn (forward-sexp -1) t) ; skip over sub-sexps
                            (re-search-backward "\\s-*\\(else\\|if\\)"
                                                paren t))
                  (setq if-count (+ if-count
                                    (if (string= (match-string 1) "else")
                                        -1 1)))
                  (if (eq if-count 1) (setq found t)))
                (if (not found)
                    (setq indent 0)
                  (forward-line 0)
                  (skip-syntax-forward " ")
                  (setq indent (current-column)))))

             ;; Line is an inlined directive-- always put on column 0
             ((looking-at "\\s-*#[^#]")
              (setq indent 0))

             ;; Line is in an implicit block: take indentation from
             ;; the line that introduces the block, plus one level.
             ((memq prec-token '(?\) do else))
              (forward-sexp -1)
              (forward-line 0)
              (skip-syntax-forward " ")
              (setq indent
                    (+ (current-column)
                       (if (eq this-char ?{) 0 inform-indent-level))))

             ;; Line is a continued statement.
             ((not (or (memq prec-token inform-statement-terminators)
                       (eq syntax 'label)))
              (setq cont-p t)
              (forward-line -1)
              (let ((token (inform-preceding-token)))
                ;; Is it the first continuation line?
                (if (memq token inform-statement-terminators)
                    (setq indent inform-indent-cont-statement)))
              (skip-syntax-forward " ")
              (setq indent (+ indent (current-column))))

             ;; Line is in a function, take indentation from start of
             ;; function, ignoring `with'.
             ((eq paren-char ?\[)
              (goto-char paren)
              (forward-line 0)
              (looking-at "\\(\\s-*with\\s-\\)?\\s-*")
              (goto-char (match-end 0))
              (setq indent
                    (+ (current-column)
                       (if (eq this-char ?\]) 0 inform-indent-level))))

             ;; Line is in a block: take indentation from block.
             (t
              (goto-char paren)
              (if (eq (inform-preceding-char) ?\))
                  (forward-sexp -1))
              (forward-line 0)
              (skip-syntax-forward " ")

              (setq indent
                    (+ (current-column)
                       (if (memq this-char '(?} ?{))
                           0
                         inform-indent-level)))
              ))

            ;; We calculated the indentation for the start of the
            ;; string; correct this for the remainder of the string if
            ;; appropriate.
            (cond
             ((eq syntax 'string)
              ;; do conditional line-up
              (cond
               ((numberp inform-strings-line-up-p)
                (setq indent inform-strings-line-up-p))
               ((eq inform-strings-line-up-p 'char)
                (setq indent (1+ string-indent)))
               (inform-strings-line-up-p
                (setq indent string-indent))
               ((not cont-p)
                (goto-char string-start)
                (let ((token (inform-preceding-token)))
                  (if (not (memq token inform-statement-terminators))
                      (setq indent
                            (+ indent inform-indent-cont-statement)))))))

             ;; Indent for label, if any.
             ((eq syntax 'label)
              (setq indent (+ indent inform-indent-label-offset))))))

        ;; Handle comments specially if told to line them up
        (if (looking-at (concat "\\s-*" comment-start))
            (setq indent (inform-line-up-comment indent)))

        indent)))))

(defun inform-line-up-comment (current-indent)
  "Return the indentation to line up this comment with the previous one.
If `inform-comments-line-up-p' is nil, or the preceding lines do not contain
comments, return CURRENT-INDENT."
  (if inform-comments-line-up-p
      (save-excursion
        (let ((indent current-indent)
              done limit)
          (while (and (not done)
                      (> (point) 1))
            (forward-line -1)
            (setq limit (point))
            (cond ((looking-at (concat "\\s-*" comment-start))
                   ;; a full-line comment, keep searching
                   nil)
                  ((and
                    (or (end-of-line) t)
                    (re-search-backward comment-start limit t)
                    (eq (car (inform-syntax-class)) 'comment))
                   ;; a line with a comment char at the end
                   ;; that is not part of the code
                   (setq indent (current-column))
                   (setq done t))
                  (t
                   ;; a non-comment line so we do not need to change
                   (setq done t))))
          indent))
    current-indent))

(defun inform-indent-to (column)
  "Indent to COLUMN.
Modifies whitespace to the left of point so that the character
after point is at COLUMN. If this is impossible, one whitespace
character is left. Avoids changing buffer gratuitously, and
returns non-NIL if it actually changed the buffer. If a change is
made, point is moved to the end of any inserted or deleted
whitespace. (If not, it may be moved at random.)"
  (let ((col (current-column)))
    (cond ((eq col column) nil)
          ((< col column) (indent-to column) t)
          (t (let ((p (point))
                   (mincol (progn (skip-syntax-backward " ")
                                  (current-column))))
               (if (eq mincol (1- col))
                   nil
                 (delete-region (point) p)
                 (indent-to (max (if (bolp) mincol (1+ mincol)) column))
                 t))))))

(defun inform-do-indent-line (data)
  "Indent the line containing point.
DATA is assumed to have been returned from `inform-syntax-class',
called at the *start* of the current line. It is assumed that
point is at the start of the line. Fixes up the spacing on `has',
`with', `object', `nearby', `private' and `class' lines. Returns
T if a change was made, NIL otherwise. Moves point."
  (skip-syntax-forward " ")
  (let ((changed-p (inform-indent-to (inform-calculate-indentation data)))
        (syntax (car data)))

    ;; Fix up space if appropriate, return changed flag.
    (or
     (cond
      ((and (memq syntax '(directive has with class private))
            inform-indent-fixup-space
            (looking-at
             "\\(object\\|class\\|nearby\\|has\\|with\\|private\\)\\(\\s-+\\|$\\)"))
       (goto-char (match-end 0))
       (inform-indent-to inform-indent-property))
      ((and (eq syntax 'action)
            inform-indent-action-column
            (or (looking-at "\\*.*\\(->\\)")
                (looking-at "\\*.*\\($\\)")))
       (goto-char (match-beginning 1))
       (inform-indent-to inform-indent-action-column))
      (t nil))
     changed-p)))

(defun inform-comment-indent ()
  "Calculate and return the indentation for a comment.
Assume point is on the comment."
  (skip-syntax-backward " ")
  (if (bolp)
      (inform-calculate-indentation (inform-syntax-class))
    (max (1+ (current-column)) comment-column)))

(defun inform-indent-line ()
  "Indent line containing point.
Keep point at the 'logically' same place, unless point was before
new indentation, in which case place point at indentation."
  (let ((oldpos (- (point-max) (point))))
    (forward-line 0)
    (inform-do-indent-line (inform-syntax-class))
    (and (< oldpos (- (point-max) (point)))
         (goto-char (- (point-max) oldpos)))))

(defun inform-indent-region (start end)
  "Indent all the lines in region defined by START/END."
  (save-restriction
    (let ((endline (progn (goto-char (max end start))
                          (or (bolp) (end-of-line))
                          (point)))
          data linestart)
      (narrow-to-region (point-min) endline)
      (goto-char (min start end))
      (forward-line 0)
      (while (not (eobp))
        (setq data (if data (inform-syntax-class linestart data)
                     (inform-syntax-class))
              linestart (point))
        (inform-do-indent-line data)
        (forward-line 1)))))


;;;
;;; Filling paragraphs
;;;

(defun inform-fill-paragraph (&optional arg)
  "Fill quoted string or comment containing point.
To fill a quoted string, point must be between the quotes. Deals
appropriately with trailing backslashes. ARG is ignored."
  (let* ((data (inform-syntax-class))
         (syntax (car data))
         (case-fold-search t))
    (cond ((eq syntax 'comment)
           (if (save-excursion
                 (forward-line 0)
                 (looking-at "\\s-*!+\\s-*"))
               (let ((fill-prefix (match-string 0)))
                 (fill-paragraph nil)
                 t)
             (error "Can't fill comments not at start of line")))
          ((eq syntax 'string)
           (save-excursion
             (let* ((indent-col (prog2
                                    (insert ?\n)
                                    (inform-calculate-indentation data)
                                  (delete-char -1)))
                    (start (search-backward "\""))
                    (end (search-forward "\"" nil nil 2))
                    (fill-column (- fill-column 2))
                    linebeg)
               (save-restriction
                 (narrow-to-region (point-min) end)

                 ;; Fold all the lines together, removing backslashes
                 ;; and multiple spaces as we go.
                 (subst-char-in-region start end ?\n ? )
                 (subst-char-in-region start end ?\\ ? )
                 (subst-char-in-region start end ?\t ? )
                 (goto-char start)
                 (while (re-search-forward "  +" end t)
                   (delete-region (match-beginning 0) (1- (match-end 0))))

                 ;; Split this line; reindent after first split,
                 ;; otherwise indent to point where first split ended
                 ;; up.
                 (goto-char start)
                 (setq linebeg start)
                 (while (not (eobp))
                   (move-to-column (1+ fill-column))
                   (if (eobp)
                       nil
                     (skip-chars-backward "^ " linebeg)
                     (if (eq (point) linebeg)
                         (progn
                           (skip-chars-forward "^ ")
                           (skip-chars-forward " ")))
                     (insert "\n")
                     (indent-to-column indent-col 1)
                     (setq linebeg (point))))))

             ;; Return T so that `fill-paragaph' doesn't try anything.
             t))

          (t (error "Point is neither in a comment nor a string")))))


;;;
;;; Tags
;;;

(defun inform-project-file ()
  "Return the project file to which the current file belongs.
This is either the value of variable `inform-project-file' or the
current file."
  (or inform-project-file (buffer-file-name)))

(defun inform-project-file-list ()
  "Builds a list of files in the current project and return it.
It recursively searches through included files, but tries to avoid loops."
  (let* ((project-file (expand-file-name (inform-project-file)))
         (project-dir (file-name-directory project-file))
         (in-file-list (list project-file))
         out-file-list
         (temp-buffer (generate-new-buffer "*Inform temp*")))
    (message "Building list of files in project...")
    (save-excursion
      (while in-file-list
        (if (member (car in-file-list) out-file-list)
            nil
          (set-buffer temp-buffer)
          (erase-buffer)
          (insert-file-contents (car in-file-list))
          (setq out-file-list (cons (car in-file-list) out-file-list)
                in-file-list (cdr in-file-list))
          (goto-char (point-min))
          (while (re-search-forward "\\<#?include\\s-+\">\\([^\"]+\\)\"" nil t)
            (let ((file (match-string 1)))
              ;; We need to duplicate Inform's file-finding algorithm:
              (if (not (string-match "\\." file))
                  (setq file (concat file ".inf")))
              (if (not (file-name-absolute-p file))
                  (setq file (expand-file-name file project-dir)))
              (setq in-file-list (cons file in-file-list))))))
      (kill-buffer nil))
    (message "Building list of files in project...done")
    out-file-list))

(defun inform-auto-load-tags-table ()
  "Visit tags table for current project, if it exists.
Do nothing if there is no current project, or no tags table."
  (let (tf (project (inform-project-file)))
    (if project
        (progn
          (setq tf (expand-file-name "TAGS" (file-name-directory project)))
          (if (file-readable-p tf)
              ;; visit-tags-table seems to just take first parameter in XEmacs
              (visit-tags-table tf))))))

(defun inform-retagify ()
  "Create a tags table for the files in the current project.
The current project contains all the files included using Inform's
`Include \">file\";' syntax by the project file, which is that given by
the variable `inform-project-file' \(if this is set\), or the current
file \(if not\).  Files included recursively are included in the tags
table."
  (interactive)
  (let* ((project-file (inform-project-file))
         (project-dir (file-name-directory project-file))
         (files (inform-project-file-list))
         (tags-file (expand-file-name "TAGS" project-dir)))
    (message "Running external tags program...")

    ;; Uses call-process to work on windows/nt systems (not tested)
    ;; Regexp matches routines or object/class definitions
    (apply (function call-process)
           inform-etags-program
           nil nil nil
           "--regex=/\\([A-Za-z0-9_]+\\|\\[\\)\\([ \\t]*->\\)*[ \\t]*\\<\\([A-Za-z_][A-Za-z0-9_]*\\)/\\3/"
           (concat "--output=" tags-file)
           "--language=none"
           files)

    (message "Running external tags program...done")
    (inform-auto-load-tags-table)))



;;;
;;; Electric keys
;;;

(defun inform-toggle-auto-newline (arg)
  "Toggle auto-newline feature.
Optional numeric ARG, if supplied turns on auto-newline when positive,
turns it off when negative, and just toggles it when zero."
  (interactive "P")
  (setq inform-auto-newline
        (if (or (not arg)
                (zerop (setq arg (prefix-numeric-value arg))))
            (not inform-auto-newline)
          (> arg 0))))

(defun inform-electric-key (arg)
  "Insert the key typed (ARG) and correct indentation."
  (interactive "P")
  (if (and (not arg) (eolp))
      (progn
        (self-insert-command 1)
        (inform-indent-line)
        (end-of-line))
    (self-insert-command (prefix-numeric-value arg))))

(defun inform-electric-semi (arg)
  "Insert the key typed (ARG) and correct line's indentation, as for semicolon.
Special handling does not occur inside strings and comments.
Inserts newline after the character if `inform-auto-newline' is non-NIL."
  (interactive "P")
  (if (and (not arg)
           (eolp)
           (let ((data (inform-syntax-class)))
             (not (memq (car data) '(string comment)))))
      (progn
        (self-insert-command 1)
        (inform-indent-line)
        (end-of-line)
        (if inform-auto-newline (newline-and-indent)))
    (self-insert-command (prefix-numeric-value arg))))

(defun inform-electric-comma (arg)
  "Insert the key typed (ARG) and correct line's indentation, as for comma.
Special handling only occurs in object declarations.
Inserts newline after the character if `inform-auto-newline' is non-NIL."
  (interactive "P")
  (if (and (not arg)
           (eolp)
           (let ((data (inform-syntax-class)))
             (and (not (memq (car data) '(string comment)))
                  (nth 1 data)
                  (zerop (car (nth 3 data))))))
      (progn
        (self-insert-command 1)
        (inform-indent-line)
        (end-of-line)
        (if inform-auto-newline (newline-and-indent)))
    (self-insert-command (prefix-numeric-value arg))))

(defun inform-electric-brace (arg)
  "Insert the key typed (ARG) and correct line's indentation.
Insert newlines before and after if `inform-auto-newline' is non-NIL."
  ;; This logic is the same as electric-c-brace.
  (interactive "P")
  (let (insertpos)
    (if (and (not arg)
             (eolp)
             (let ((data (inform-syntax-class)))
               (memq (car data) '(code label)))
             (or (save-excursion (skip-syntax-backward " ") (bolp))
                 (if inform-auto-newline
                     (progn (inform-indent-line) (newline) t) nil)))
        (progn
          (insert last-command-event)
          (inform-indent-line)
          (end-of-line)
          (if (and inform-auto-newline (/= last-command-event ?\]))
              (progn
                (newline)
                (setq insertpos (1- (point)))
                (inform-indent-line)))
          (save-excursion
            (if insertpos (goto-char insertpos))
            (delete-char -1))))
    (if insertpos
        (save-excursion
          (goto-char (1- insertpos))
          (self-insert-command (prefix-numeric-value arg)))
      (self-insert-command (prefix-numeric-value arg)))))


;;;
;;; Miscellaneous
;;;

(defun inform-next-object (&optional arg)
  "Go to the next object or class declaration in the file.
With a prefix ARG, go forward that many declarations.
With a negative prefix ARG, search backwards."
  (interactive "P")
  (let ((fun 're-search-forward)
        (n (prefix-numeric-value arg)))
    (cond ((< n 0)
           (setq fun 're-search-backward n (- n)))
          ((looking-at inform-real-object-regexp)
           (setq n (1+ n))))
    (prog1
        (funcall fun inform-real-object-regexp nil 'move n)
      (forward-line 0))))

;; This function doubles as an `imenu-prev-name' function, so when
;; called noninteractively it must return T if it was successful and NIL
;; if not.  Argument NIL must correspond to moving backwards by 1.

(defun inform-prev-object (&optional arg)
  "Go to the previous object or class declaration in the file.
With a prefix ARG, go back many declarations.
With a negative prefix ARG, go forwards."
  (interactive "P")
  (inform-next-object (- (prefix-numeric-value arg))))

(defun inform-imenu-extract-name ()
  "Extract item name for imenu."
  (if (looking-at
       "^#?\\(object\\|nearby\\|class\\)\\s-+\\(->\\s-+\\)*\\(\\(\\w\\|\\s_\\)+\\)")
      (concat (if (string= "class" (downcase (match-string 1)))
                  "Class ")
              (buffer-substring-no-properties (match-beginning 3)
                                              (match-end 3)))))


;;;
;;; Build and run project
;;;

;; Tell Emacs how to parse inform compiler output so next-error can be
;; used to jump to any errors. This is done at load time so the regexp
;; is set up before compilation starts.
;; XEmacs compile mode's builtin regexps work OK.

(if (featurep 'emacs)
    (if (and (boundp 'compilation-error-regexp-alist-alist)
             (not (assoc 'inform-e1 compilation-error-regexp-alist-alist)))
        (mapc
         (lambda (item)
           (push (car item) compilation-error-regexp-alist)
           (push item compilation-error-regexp-alist-alist))
         inform-compilation-error-regexp-alist)))


(defun inform-build-project ()
  "Compile the current Inform project.
The current project is given by variable`inform-project-file', or the current
file if this is NIL."
  (interactive)
  (let ((project-file (file-name-nondirectory (inform-project-file))))
    (compile
     (concat inform-command
             (if (and inform-libraries-directory
                      (file-directory-p inform-libraries-directory))
                 (concat " +" inform-libraries-directory)
               "")
             ;; Note the use of Microsoft style errors.  The
             ;; Archimedes-style errors don't give the correct file
             ;; name.
             " " inform-command-options " -E1 "
             (if (string-match "\\`[^.]+\\(\\.inf\\'\\)" project-file)
                 (substring project-file 0 (match-beginning 1))
               project-file)))))

(defun inform-run-project ()
  "Run the current Inform project using `inform-interpreter-command'.
The current project is given by variable`inform-project-file', or
the current file if this is NIL. Will kill any running
interpreter if `inform-interpreter-kill-old-process' is non-NIL.
Switches to the interpreter's output buffer if
`inform-interpreter-is-graphical' is NIL."
  (interactive)
  (let* ((project-file (inform-project-file))
         (story-file-base (if (string-match "\\`[^.]+\\(\\.inf\\'\\)"
                                            project-file)
                              (substring project-file 0 (match-beginning 1))
                            project-file))
         (story-file (concat story-file-base
                             (if (string-match "-v8" inform-command-options)
                                 ".z8"
                               ".z5")))
         (name "Inform interpreter"))
    (if  (or (symbolp inform-interpreter-command)
             (functionp inform-interpreter-command))
        ;; Emacs interpreter (or custom function)
        (funcall inform-interpreter-command story-file)
      ;; inform-interpreter-command is truly a command
      (let* ((buffer (get-buffer-create (concat "*" name "*")))
             (proc (get-buffer-process buffer)))
        (and inform-interpreter-kill-old-process
             proc
             (kill-process proc))
        (if (or inform-interpreter-is-graphical
                (eq window-system 'w32)) ; Windows can't handle
                                        ; term-exec anyway
            (progn
              ;; X gets confused if an application is restarted too quickly
              ;; Assume X if not Win32
              (unless (eq window-system 'w32)
                (message "Waiting for X...")
                ;; On my system 0.1 seconds was enough - double it for safety
                (sleep-for 0.2)
                (message ""))
              (when (or inform-interpreter-kill-old-process
                        (not proc))
                (apply (function start-process)
                       name buffer inform-interpreter-command
                       ;; Some shells barf on "empty" arguments
                       (if (string-equal "" inform-interpreter-options)
                           (list story-file)
                         (list inform-interpreter-options
                               story-file)))))
          ;; Console-mode 'terp
          (require 'term)
          (when (or inform-interpreter-kill-old-process
                    (not proc))
            (set-buffer buffer)
            (term-mode)
            (erase-buffer)
            (term-exec buffer name inform-interpreter-command nil
                       (if (string-equal "" inform-interpreter-options)
                           (list story-file)
                         (list inform-interpreter-options
                               story-file)))
            (term-char-mode)
            (term-pager-disable))
          (switch-to-buffer buffer)
          (goto-char (point-max)))))))



;;;
;;; Spell checking
;;;

(defun inform-spell-check-buffer ()
  "Spellcheck all strings in the buffer using Ispell."
  (interactive)
  (let (start (spell-continue t))
    (save-excursion
      (goto-char (point-min))
      (while (and (search-forward "\"" nil t)
                  spell-continue)
        (if (and (eq (car (inform-syntax-class)) 'string)
                 ;; don't spell check include directives etc
                 (not (save-excursion
                        (forward-line 0)
                        (looking-at inform-directive-regexp))))
            (progn
              (forward-char -1)         ; move point to quotation mark
              (setq start (point))
              (forward-sexp)
              (ispell-region start (point))
              ;; If user quit out (eg by pressing q while in ispell)
              ;; don't continue looking for strings to check.
              (setq spell-continue
                    (and ispell-process
                         (eq (process-status ispell-process) 'run)))))))))



;;;###autoload
(setq auto-mode-alist
      (append '(("\\.h\\'"   . inform-maybe-mode)
                ("\\.inf\\'" . inform-mode))
              auto-mode-alist))

;;;###autoload
(add-hook 'inform-mode-hook 'turn-on-font-lock)


(provide 'inform-mode)

;;; inform-mode.el ends here
