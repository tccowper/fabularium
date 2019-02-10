/*
 * Copyright (C) 2018 Tim Cadogan-Cowper.
 *
 * This file is part of Fabularium.
 *
 * Fabularium is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Fabularium; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.luxlunae.glk.model.html;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * This tokeniser implements a simplified version of the HTML 5 tokeniser state machine
 * defined at https://www.w3.org/TR/html5/syntax.html#tokenization.  At some point in
 * the future if performance appears to be too much of an issue, we may reimplement it
 * in C and use JNI calls.  But at the moment it seems to be working well.
 */
final class Tokeniser {

    public static final int EOF = -1;
    private static final int NULL = 0x0000;
    private static final int TAB = 0x0009;
    private static final int LF = 0x000A;
    private static final int FF = 0x000C;
    private static final int SPACE = 0x0020;
    private static final int QUOTATION_MARK = 0x0022;
    private static final int AMPERSAND = 0x0026;
    private static final int LESS_THAN_SIGN = 0x003C;
    private static final int LATIN_SMALL_LETTER_X = 0x0078;
    private static final int LATIN_CAPITAL_LETTER_X = 0x0058;
    private static final int SEMICOLON = 0x003B;
    private static final int SOLIDUS = 0x002F;
    private static final int REPLACEMENT_CHARACTER = 0xFFFD;
    private static final int LATIN_CAPITAL_LETTER_A = 0x0041;
    private static final int LATIN_CAPITAL_LETTER_F = 0x0046;
    private static final int LATIN_SMALL_LETTER_A = 0x0061;
    private static final int LATIN_SMALL_LETTER_F = 0x0066;
    private static final int ASCII_ZERO = 0x0030;
    private static final int ASCII_NINE = 0x0039;

    private Tokeniser() {
        // prevent instantiation
    }

    // This is the only entry point into this class
    static void parseNextToken(@NonNull com.luxlunae.glk.model.html.State c) {
        if (c.mCRState != CRState.CRNone) {
            // we're in the middle of consuming a character reference
            // keep it up
            switch (c.mCRState) {
                case CRNumberType:
                    CRNumberType(c);
                    break;
                case CRNumber:
                    CRNumber(c);
                    break;
                case CRNumberHex:
                    CRNumberHex(c);
                    break;
                case CRSymbol:
                    CRSymbol(c);
                    break;
                case CREndSemicolon:
                    CREndSemicolon(c);
                    break;
            }
        } else {
            switch (c.mState) {
                case Data:
                    data(c);
                    break;
                case CRInData:
                    CRinData(c);
                    break;
                case TagOpen:
                    tagOpen(c);
                    break;
                case EndTagOpen:
                    endTagOpen(c);
                    break;
                case TagName:
                    tagName(c);
                    break;
                case Before_Attr_Name:
                    beforeAttributeName(c);
                    break;
                case Attr_Name:
                    attributeName(c);
                    break;
                case After_Attr_Name:
                    afterAttributeName(c);
                    break;
                case Before_Attr_Value:
                    beforeAttributeValue(c);
                    break;
                case Attr_Value_DQuot:
                    attributeValueDQuote(c);
                    break;
                case Attr_Value_SQuot:
                    attributeValueSQuote(c);
                    break;
                case Attr_Value_UQuot:
                    attributeValueUQuote(c);
                    break;
                case CRInAttrValue:
                    CRInAttributeValue(c);
                    break;
                case After_Attr_Value_Quoted:
                    afterAttributeValueQuoted(c);
                    break;
                case Self_Closing_Start_Tag:
                    selfClosingTag(c);
                    break;
                case BogusComment:
                    bogusComment(c);
                    break;
                default:
                    c.parseError("TADS Html Tokeniser: could not emit token as in unrecognised state: " + c.mState);
                    c.emitEOF();
                    c.mState = State.Data;
                    break;
            }
        }
    }

    private static void data(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case AMPERSAND:
                c.mState = Tokeniser.State.CRInData;
                break;
            case '<':
                c.mState = Tokeniser.State.TagOpen;
                break;
            case NULL:
                c.parseError("TADS HTML parse error in data");
                c.appendText((char) 0);
                break;
            case EOF:
                c.emitEOF();
                break;
            default:
                c.appendText((char) ch);
                break;
        }
    }

    private static void CRinData(@NonNull com.luxlunae.glk.model.html.State c) {
        c.mState = Tokeniser.State.Data;
        consumeCR(c, null);
    }

    private static void tagOpen(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case '!':
                c.mState = Tokeniser.State.MarkupDeclOpen;
                break;
            case '/':
            case '\\':  // HTML TADS also allows a backslash to indicate an end tag
                c.mState = Tokeniser.State.EndTagOpen;
                break;
            case '?':
                c.parseError("TADS HTML parse error at tag open");
                c.mState = Tokeniser.State.BogusComment;
                break;
            default:
                if (Character.isLetter(ch)) {
                    if (Character.isUpperCase(ch)) {
                        ch = Character.toLowerCase(ch);
                    }
                    c.newStartTag();
                    c.appendCurTag((char) ch);
                    c.mState = Tokeniser.State.TagName;
                } else {
                    c.parseError("TADS HTML parse error at tag open");
                    c.mState = Tokeniser.State.Data;
                    c.appendText((char) LESS_THAN_SIGN);
                    c.unconsume();
                }
                break;
        }
    }

    private static void endTagOpen(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case '>':
                c.parseError("TADS HTML parse error at end tag open");
                c.mState = Tokeniser.State.Data;
                break;
            case EOF:
                c.parseError("TADS HTML parse error at end tag open");
                c.mState = Tokeniser.State.Data;
                c.appendText((char) LESS_THAN_SIGN);
                c.appendText((char) SOLIDUS);
                c.unconsume();
                break;
            default:
                if (Character.isLetter(ch)) {
                    if (Character.isUpperCase(ch)) {
                        ch = Character.toLowerCase(ch);
                    }
                    c.newEndTag();
                    c.appendCurTag((char) ch);
                    c.mState = Tokeniser.State.TagName;
                } else {
                    c.parseError("TADS HTML parse error at end tag open");
                    c.mState = Tokeniser.State.BogusComment;
                }
                break;
        }
    }

    private static void tagName(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case TAB:
            case LF:
            case FF:
            case SPACE:
                c.mState = Tokeniser.State.Before_Attr_Name;
                break;
            case '/':
                c.mState = Tokeniser.State.Self_Closing_Start_Tag;
                break;
            case '>':
                c.mState = Tokeniser.State.Data;
                c.emitTag();
                break;
            case NULL:
                c.parseError("TADS HTML parse error at tag name");
                c.appendCurTag((char) REPLACEMENT_CHARACTER);
                break;
            case EOF:
                c.parseError("TADS HTML parse error at tag name");
                c.mState = Tokeniser.State.Data;
                c.unconsume();
                break;
            default:
                if (Character.isUpperCase(ch)) {
                    ch = Character.toLowerCase(ch);
                }
                c.appendCurTag((char) ch);
                break;
        }
    }

    private static void beforeAttributeName(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case TAB:
            case LF:
            case FF:
            case SPACE:
                // Ignore the character.
                break;
            case '/':
                c.mState = Tokeniser.State.Self_Closing_Start_Tag;
                break;
            case '>':
                c.mState = Tokeniser.State.Data;
                c.emitTag();
                break;
            case NULL:
                c.parseError("TADS HTML parse error before attribute name");
                c.newAttribute();
                c.appendCurAttrName((char) REPLACEMENT_CHARACTER);
                c.mState = Tokeniser.State.Attr_Name;
                break;
            case QUOTATION_MARK:
            case '\'':
            case '<':
            case '=':
                c.parseError("TADS HTML parse error before attribute name");
                c.newAttribute();
                c.appendCurAttrName((char) ch);
                c.mState = Tokeniser.State.Attr_Name;
                break;
            case EOF:
                c.parseError("TADS HTML parse error before attribute name");
                c.mState = Tokeniser.State.Data;
                c.unconsume();
                break;
            default:
                if (Character.isUpperCase(ch)) {
                    ch = Character.toLowerCase(ch);
                }
                c.newAttribute();
                c.appendCurAttrName((char) ch);
                c.mState = Tokeniser.State.Attr_Name;
                break;
        }
    }

    private static void attributeName(@NonNull com.luxlunae.glk.model.html.State c) {
        // TODO:  When the user agent leaves the attribute name state (and before emitting
        // the tag token, if appropriate), the complete attribute's name must be compared
        // to the other attributes on the same token; if there is already an attribute on
        // the token with the exact same name, then this is a parse error and the
        // new attribute must be removed from the token.
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case TAB:
            case LF:
            case FF:
            case SPACE:
                c.mState = Tokeniser.State.After_Attr_Name;
                break;
            case '/':
                c.mState = Tokeniser.State.Self_Closing_Start_Tag;
                break;
            case '=':
                c.mState = Tokeniser.State.Before_Attr_Value;
                break;
            case '>':
                c.mState = Tokeniser.State.Data;
                c.emitTag();
                break;
            case NULL:
                c.parseError("TADS HTML parse error at attribute name");
                c.appendCurAttrName((char) REPLACEMENT_CHARACTER);
                break;
            case QUOTATION_MARK:
            case '\'':
            case '<':
                c.parseError("TADS HTML parse error at attribute name");
                c.appendCurAttrName((char) ch);
                break;
            case EOF:
                c.parseError("TADS HTML parse error at attribute name");
                c.mState = Tokeniser.State.Data;
                c.unconsume();
                break;
            default:
                if (Character.isUpperCase(ch)) {
                    ch = Character.toLowerCase(ch);
                }
                c.appendCurAttrName((char) ch);
                break;
        }
    }

    private static void afterAttributeName(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case TAB:
            case LF:
            case FF:
            case SPACE:
                // Ignore the character
                break;
            case '/':
                c.mState = Tokeniser.State.Self_Closing_Start_Tag;
                break;
            case '=':
                c.mState = Tokeniser.State.Before_Attr_Value;
                break;
            case '>':
                c.mState = Tokeniser.State.Data;
                c.emitTag();
                break;
            case NULL:
                c.parseError("TADS HTML parse error after attribute name");
                c.newAttribute();
                c.appendCurAttrName((char) REPLACEMENT_CHARACTER);
                c.mState = Tokeniser.State.Attr_Name;
                break;
            case QUOTATION_MARK:
            case '\'':
            case '<':
                c.parseError("TADS HTML parse error after attribute name");
                c.newAttribute();
                c.appendCurAttrName((char) ch);
                c.mState = Tokeniser.State.Attr_Name;
                break;
            case EOF:
                c.parseError("TADS HTML parse error after attribute name");
                c.mState = Tokeniser.State.Data;
                c.unconsume();
                break;
            default:
                if (Character.isUpperCase(ch)) {
                    ch = Character.toLowerCase(ch);
                }
                c.newAttribute();
                c.appendCurAttrName((char) ch);
                c.mState = Tokeniser.State.Attr_Name;
                break;
        }
    }

    private static void beforeAttributeValue(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case TAB:
            case LF:
            case FF:
            case SPACE:
                // Ignore the character
                break;
            case QUOTATION_MARK:
                c.mState = Tokeniser.State.Attr_Value_DQuot;
                break;
            case AMPERSAND:
                c.mState = Tokeniser.State.Attr_Value_UQuot;
                c.unconsume();
                break;
            case '\'':
                c.mState = Tokeniser.State.Attr_Value_SQuot;
                break;
            case NULL:
                c.parseError("TADS HTML parse error before attribute value");
                c.appendCurAttrValue((char) REPLACEMENT_CHARACTER);
                c.mState = Tokeniser.State.Attr_Value_UQuot;
                break;
            case '>':
                c.parseError("TADS HTML parse error before attribute value");
                c.mState = Tokeniser.State.Data;
                c.emitTag();
                break;
            case '<':
            case '=':
            case '`':
                c.parseError("TADS HTML parse error before attribute value");
                c.appendCurAttrValue((char) ch);
                c.mState = Tokeniser.State.Attr_Value_UQuot;
                break;
            case EOF:
                c.parseError("TADS HTML parse error before attribute value");
                c.mState = Tokeniser.State.Data;
                c.unconsume();
                break;
            default:
                c.appendCurAttrValue((char) ch);
                c.mState = Tokeniser.State.Attr_Value_UQuot;
                break;
        }
    }

    private static void attributeValueDQuote(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case QUOTATION_MARK:
                c.mState = Tokeniser.State.After_Attr_Value_Quoted;
                break;
            case AMPERSAND:
                c.mAdditionalChar = QUOTATION_MARK;
                c.mLastState = c.mState;
                c.mState = Tokeniser.State.CRInAttrValue;
                break;
            case NULL:
                c.parseError("TADS HTML parse error at (double-quoted) attribute value");
                c.appendCurAttrValue((char) REPLACEMENT_CHARACTER);
                break;
            case EOF:
                c.parseError("TADS HTML parse error at (double-quoted) attribute value");
                c.mState = Tokeniser.State.Data;
                c.unconsume();
                break;
            default:
                c.appendCurAttrValue((char) ch);
                break;
        }
    }

    private static void attributeValueSQuote(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case '\'':
                c.mState = Tokeniser.State.After_Attr_Value_Quoted;
                break;
            case AMPERSAND:
                c.mAdditionalChar = '\'';
                c.mLastState = c.mState;
                c.mState = Tokeniser.State.CRInAttrValue;
                break;
            case NULL:
                c.parseError("TADS HTML parse error at (single-quoted) attribute value");
                c.appendCurAttrValue((char) REPLACEMENT_CHARACTER);
                break;
            case EOF:
                c.parseError("TADS HTML parse error at (single-quoted) attribute value");
                c.mState = Tokeniser.State.Data;
                c.unconsume();
                break;
            default:
                c.appendCurAttrValue((char) ch);
                break;
        }
    }

    private static void attributeValueUQuote(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case TAB:
            case LF:
            case FF:
            case SPACE:
                c.mState = Tokeniser.State.Before_Attr_Name;
                break;
            case AMPERSAND:
                c.mAdditionalChar = '>';
                c.mLastState = c.mState;
                c.mState = Tokeniser.State.CRInAttrValue;
                break;
            case '>':
                c.mState = Tokeniser.State.Data;
                c.emitTag();
                break;
            case NULL:
                c.parseError("TADS HTML parse error at (unquoted) attribute value");
                c.mState = Tokeniser.State.Data;
                break;
            case QUOTATION_MARK:
            case '\'':
            case '<':
            case '=':
            case '`':
                c.parseError("TADS HTML parse error at (unquoted) attribute value");
                c.appendCurAttrValue((char) ch);
                break;
            case EOF:
                c.parseError("TADS HTML parse error at (unquoted) attribute value");
                c.mState = Tokeniser.State.Data;
                c.unconsume();
                break;
            default:
                c.appendCurAttrValue((char) ch);
                break;
        }
    }

    private static void CRInAttributeValue(@NonNull com.luxlunae.glk.model.html.State c) {
        c.mState = c.mLastState;
        consumeCR(c, c.mAdditionalChar);
    }

    private static void afterAttributeValueQuoted(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case TAB:
            case LF:
            case FF:
            case SPACE:
                c.mState = Tokeniser.State.Before_Attr_Name;
                break;
            case '/':
                c.mState = Tokeniser.State.Self_Closing_Start_Tag;
                break;
            case '>':
                c.mState = Tokeniser.State.Data;
                c.emitTag();
                break;
            case EOF:
                c.parseError("TADS HTML parse error after (quoted) attribute value");
                c.mState = Tokeniser.State.Data;
                c.unconsume();
                break;
            default:
                c.parseError("TADS HTML parse error after (quoted) attribute value");
                c.mState = Tokeniser.State.Before_Attr_Name;
                c.unconsume();
                break;
        }
    }

    private static void selfClosingTag(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case '>':
                // Usually we would set a self-closing flag here as well, but
                // we don't need that in this implementation.
                c.mState = Tokeniser.State.Data;
                c.emitTag();
                break;
            case EOF:
                c.parseError("TADS HTML parse error at self closing start tag");
                c.mState = Tokeniser.State.Data;
                c.unconsume();
                break;
            default:
                c.parseError("TADS HTML parse error at self closing start tag");
                c.mState = Tokeniser.State.Before_Attr_Name;
                c.unconsume();
                break;
        }
    }

    private static void bogusComment(@NonNull com.luxlunae.glk.model.html.State c) {
        // Consume every character up to and including the first ">" (U+003E) character
        // or the end of the file (EOF), whichever comes first.
        int ch = c.consumeNextInputChar();
        switch (ch) {
            case EOF:
                c.mState = Tokeniser.State.Data;
                c.unconsume();
                break;
            case '>':
                c.mState = Tokeniser.State.Data;
                break;
            default:
                // do nothing - currently we just ignore comments
                break;
        }
    }

    /**
     * Attempts to consume a character reference, optionally with an additional allowed character, which,
     * if specified where the algorithm is invoked, adds a character to the list of characters that
     * cause there to not be a character reference.
     *
     * @param additional - additional character, or 0 if not allowed.
     */
    private static void consumeCR(@NonNull com.luxlunae.glk.model.html.State c, @Nullable Integer additional) {
        int ch = c.peekNextChar();
        switch (ch) {
            case TAB:
            case LF:
            case FF:
            case SPACE:
            case LESS_THAN_SIGN:
            case AMPERSAND:
            case EOF:
                // Not a character reference. No characters are consumed, and nothing is returned.
                return;
            case '#':
                // Consume the U+0023 NUMBER SIGN.
                c.mark();
                c.consumeNextInputChar();
                c.mCRState = CRState.CRNumberType;
                break;
            default:
                if (additional != null && ch == additional) {
                    // Not a character reference. No characters are consumed, and nothing is returned.
                    return;
                }
                c.mark();
                c.appendCurCharRef((char) c.consumeNextInputChar());  // N.B. one char is not enough for a match - so don't test
                c.mCRState = CRState.CRSymbol;
                break;
        }
    }

    private static boolean isASCIIHexDigit(int ch) {
        return (ch >= LATIN_CAPITAL_LETTER_A && ch <= LATIN_CAPITAL_LETTER_F) ||
                (ch >= LATIN_SMALL_LETTER_A && ch <= LATIN_SMALL_LETTER_F) ||
                (ch >= ASCII_ZERO && ch <= ASCII_NINE);
    }

    private static boolean isASCIIDigit(int ch) {
        return (ch >= ASCII_ZERO && ch <= ASCII_NINE);
    }

    private static void CRNumberType(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.peekNextChar();
        switch (ch) {
            case LATIN_CAPITAL_LETTER_X:
            case LATIN_SMALL_LETTER_X:
                c.consumeNextInputChar();
                c.mCRState = CRState.CRNumberHex;
                break;
            default:
                c.mCRState = CRState.CRNumber;
                break;
        }
    }

    private static void CRNumberHex(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        if (isASCIIHexDigit(ch)) {
            c.appendCurCharRef((char) ch);
        } else if (c.matchCRNumber(true)) {
            // Emit the number
            if (ch != SEMICOLON) {
                c.parseError("TADS HTML parse error - hex number character reference is missing closing semicolon");
                c.unconsume();
            }
            c.flushCRSubstitute();
            c.mCRState = CRState.CRNone;
        } else {
            c.parseError("TADS HTML parse error at character reference (hex number)");
            c.clearCRText();
            c.reset();
            c.mCRState = CRState.CRNone;
            if (c.mState == Tokeniser.State.Data) {
                c.appendText((char) AMPERSAND);
            } else {
                c.appendCurAttrValue((char) AMPERSAND);
            }
        }
    }

    private static void CRNumber(@NonNull com.luxlunae.glk.model.html.State c) {
        int ch = c.consumeNextInputChar();
        if (isASCIIDigit(ch)) {
            c.appendCurCharRef((char) ch);
        } else if (c.matchCRNumber(false)) {
            // Emit the number
            if (ch != SEMICOLON) {
                c.parseError("TADS HTML parse error - number character reference is missing closing semicolon");
                c.unconsume();
            }
            c.flushCRSubstitute();
            c.mCRState = CRState.CRNone;
        } else {
            c.parseError("TADS HTML parse error at character reference (number)");
            c.clearCRText();
            c.reset();
            c.mCRState = CRState.CRNone;
            if (c.mState == Tokeniser.State.Data) {
                c.appendText((char) AMPERSAND);
            } else {
                c.appendCurAttrValue((char) AMPERSAND);
            }
        }
    }

    private static void CRSymbol(@NonNull com.luxlunae.glk.model.html.State c) {
        // Consume the maximum number of characters possible, with the consumed characters
        // matching one of the identifiers in the first column of the named character references table
        // (in a case-sensitive manner)
        int ch = c.consumeNextInputChar();

        // If we have reached the end of the file or already consumed
        // the maximum number of chars in a character reference, there's an
        // error - unconsume everything
        if (ch == EOF || c.getCRTextLength() > CharRef.MAX_CREF_LENGTH) {
            // TODO: check if there's a parser error (i.e. if the characters after the AMPERSAND
            c.parseError("TADS HTML parse error: did not recognise character reference '" + c.getCRText() + "'");
            c.clearCRText();
            c.reset();
            c.mCRState = CRState.CRNone;
            if (c.mState == Tokeniser.State.Data) {
                c.appendText((char) AMPERSAND);
            } else {
                c.appendCurAttrValue((char) AMPERSAND);
            }
            return;
        }

        // OK we've got some more - do we now have a match?
        c.appendCurCharRef((char) ch);
        if (c.matchCRSymbol()) {
            // Success!
            c.flushCRSubstitute();
            c.mCRState = (ch == SEMICOLON) ? CRState.CRNone : CRState.CREndSemicolon;
        }
    }

    private static void CREndSemicolon(@NonNull com.luxlunae.glk.model.html.State c) {
        // Consume the semi-colon at the end of a character reference
        if (c.consumeNextInputChar() != SEMICOLON) {
            c.parseError("TADS HTML parse error - character reference is missing closing semicolon");
            c.unconsume();
        }
        c.mCRState = CRState.CRNone;
    }

    // HTML TADS is based on the HTML 3.2 reference specification
    // HTML TADS doesn't recognise SCRIPT tags, so we don't bother implementing
    // those states.  We also currently ignore DOCTYPE tags.
    enum State {
        Data, CRInData, RCDATA, CRInRCDATA, RAWTEXT,
        PLAINTEXT, TagOpen, EndTagOpen, TagName, RCDATA_LT, RCDATA_EndTagOpen,
        RCDATA_EndTagName, RAWTEXT_LT, RAWTEXT_EndTagOpen, RAWTEXT_EndTagName,
        Before_Attr_Name, Attr_Name, After_Attr_Name, Before_Attr_Value, Attr_Value_DQuot,
        Attr_Value_SQuot, Attr_Value_UQuot, CRInAttrValue, After_Attr_Value_Quoted,
        Self_Closing_Start_Tag, BogusComment, MarkupDeclOpen, CommentStart, CommentStartDash,
        CommentState, CommentEndDash, CommentEnd, CommentEndBang, CDATA
    }

    enum CRState {
        CRNone, CRNumberType, CRNumberHex, CRNumber, CRSymbol, CREndSemicolon
    }

}
