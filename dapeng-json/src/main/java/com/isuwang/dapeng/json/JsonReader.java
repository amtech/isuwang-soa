package com.isuwang.dapeng.json;

// see https://github.com/wangzaixiang/spray-json/blob/master/src/main/scala/spray/json/JsonParser.scala
public class JsonReader {

    ParserInput input;
    char cursorChar;

    public JsonReader(CharSequence input, JsonCallback callback) {
       // TODO
    }

    class Line {
        private final int lineNr;
        private final int column;
        private final String text;

        // case class Line(lineNr: Int, column: Int, text: String)
        public Line(int lineNr, int column, String text){
            this.lineNr = lineNr;
            this.column = column;
            this.text = text;
        }
    }
    interface ParserInput {
        /**
         * Advance the cursor and get the next char.
         * Since the char is required to be a 7-Bit ASCII char no decoding is required.
         */
        char nextChar();

        /**
         * Advance the cursor and get the next char, which could potentially be outside
         * of the 7-Bit ASCII range. Therefore decoding might be required.
         */
        char nextUtf8Char();

        //def currentArgument(): JsValue

        int cursor();
        //def length: Int
        //def sliceString(start: Int, end: Int): String
        char[] sliceCharArray(int start, int end);

        Line getLine(int pos);

    }

    void parseJsValue() {
        ws();
//        value();
//        require(EOI);
    }

    // ' \n\r\t'
    void ws() {
        while( ( (1L << cursorChar) &
                ((cursorChar - 64) >> 31) &     // cursorChar < 64
                0x100002600L)
                != 0L) {
            advance();
        }
    }

    boolean advance(){
         cursorChar = input.nextChar();
         return true;
    }

//    @tailrec private def ws(): Unit =
//            // fast test whether cursorChar is one of " \n\r\t"
//            if (((1L << cursorChar) & ((cursorChar - 64) >> 31) & 0x100002600L) != 0L) { advance(); ws() }


//    class JsonParser(input: ParserInput, jsonExtensionSupport: Boolean = false) {
//  import JsonParser.{ParsingException, EOI, EOS }
//
//        private[this] val sb = new JStringBuilder
//        private[this] var cursorChar: Char = input.nextChar()
//        private[this] var jsValue: JsValue = _
//
//        def parseJsValue(): JsValue = {
//                ws()
//    `value`()
//        require(EOI)
//        jsValue
//  }
//
//
//        ////////////////////// GRAMMAR ////////////////////////
//
//
//        // http://tools.ietf.org/html/rfc4627#section-2.1
//        private def `value`(): Unit = {
//                val mark = input.cursor
//                def simpleValue(matched: Boolean, value: JsValue) = if (matched) jsValue = value else fail("JSON Value", mark)
//
//        (cursorChar: @switch) match {
//            case 'f' => simpleValue(`false`(), JsFalse)
//            case 'n' => simpleValue(`null`(), JsNull)
//            case 't' => simpleValue(`true`(), JsTrue)
//            case '{' => advance(); `object`()
//            case '[' => advance(); `array`()
//            case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '-' => `number`()
//            case '"' => `string`(); jsValue = if (sb.length == 0) JsString.empty else JsString(sb.toString)
//            case '\'' =>
//                if(jsonExtensionSupport) {
//                    single_quoted_string(); jsValue = if(sb.length == 0) JsString.empty else JsString(sb.toString)
//                }
//                else fail("JSON Value")
//            case EOS =>  jsValue = input.currentArgument; advance(); ws();
//            case _ => fail("JSON Value")
//        }
//  }
//
//        private def `false`() = advance() && ch('a') && ch('l') && ch('s') && ws('e')
//        private def `null`() = advance() && ch('u') && ch('l') && ws('l')
//        private def `true`() = advance() && ch('r') && ch('u') && ws('e')
//
//        // http://tools.ietf.org/html/rfc4627#section-2.2
//        private def `object`(): Unit = {
//                ws()
//                jsValue = if (cursorChar != '}') {
//            @tailrec def members(map: Map[String, JsValue]): Map[String, JsValue] = {
//                if(jsonExtensionSupport) IDorString()
//                else `string`()
//
//                require(':')
//                ws()
//                val key = sb.toString
//        `value`()
//                val nextMap = map.updated(key, jsValue)
//                if (ws(',')) members(nextMap) else nextMap
//            }
//            var map = Map.empty[String, JsValue]
//            map = members(map)
//            require('}')
//            JsObject(map)
//        } else {
//            advance()
//            JsObject.empty
//        }
//        ws()
//  }
//
//        // http://tools.ietf.org/html/rfc4627#section-2.3
//        private def `array`(): Unit = {
//                ws()
//                jsValue = if (cursorChar != ']') {
//            val list = Vector.newBuilder[JsValue]
//            @tailrec def values(): Unit = {
//        `value`()
//            list += jsValue
//            if (ws(',')) values()
//      }
//            values()
//            require(']')
//            JsArray(list.result())
//        } else {
//            advance()
//            JsArray.empty
//        }
//        ws()
//  }
//
//        // http://tools.ietf.org/html/rfc4627#section-2.4
//        private def `number`() = {
//                val start = input.cursor
//                val startChar = cursorChar
//                ch('-')
//    `int`()
//    `frac`()
//    `exp`()
//        jsValue =
//        if (startChar == '0' && input.cursor - start == 1) JsNumber.zero
//        else JsNumber(input.sliceCharArray(start, input.cursor))
//        ws()
//  }
//
//        private def `int`(): Unit = if (!ch('0')) oneOrMoreDigits()
//        private def `frac`(): Unit = if (ch('.')) oneOrMoreDigits()
//        private def `exp`(): Unit = if (ch('e') || ch('E')) { ch('-') || ch('+'); oneOrMoreDigits() }
//
//    private def oneOrMoreDigits(): Unit = if (DIGIT()) zeroOrMoreDigits() else fail("DIGIT")
//    @tailrec private def zeroOrMoreDigits(): Unit = if (DIGIT()) zeroOrMoreDigits()
//
//    private def DIGIT(): Boolean = cursorChar >= '0' && cursorChar <= '9' && advance()
//
//    private def IDorString(): Unit = {
//        if(cursorChar == '"') `string`()
//    else if(cursorChar == '\'') single_quoted_string()
//        else if(cursorChar == '_' || cursorChar == '$' ||
//                (cursorChar >= 'a' && cursorChar <= 'z') || (cursorChar >= 'A' && cursorChar <= 'Z')) ID()
//        else fail("id or string")
//    }
//
//    private def `ID`(): Unit = {
//        val start = input.cursor
//        if(ID1()) {
//            var cont = true
//            do {
//                cont = ID2()
//            } while(cont)
//
//            sb.setLength(0)
//            sb.append(input.sliceCharArray(start, input.cursor))
//        }
//        else fail("ID")
//    }
//
//    private def `ID1`(): Boolean = (cursorChar == '_' || cursorChar == '$' ||
//            (cursorChar >= 'a' && cursorChar <= 'z') || (cursorChar >= 'A' && cursorChar <= 'Z')) && advance()
//
//    private def `ID2`(): Boolean = (cursorChar == '_' || cursorChar == '$' || (cursorChar >= '0' && cursorChar <= '9') ||
//            (cursorChar >= 'a' && cursorChar <= 'z') || (cursorChar >= 'A' && cursorChar <= 'Z')) && advance()
//
//    // http://tools.ietf.org/html/rfc4627#section-2.5
//    // TODO support single-quoted string
//    private def `string`(): Unit = {
//        if (cursorChar == '"') cursorChar = input.nextUtf8Char() else fail("'\"'")
//        sb.setLength(0)
//        while (`char`()) cursorChar = input.nextUtf8Char()
//        require('"')
//        ws()
//    }
//
//    private def single_quoted_string(): Unit = {
//        if(cursorChar == '\'') cursorChar = input.nextUtf8Char() else fail("'")
//        sb.setLength(0)
//        while(sqs_char()) cursorChar = input.nextUtf8Char()
//        require('\'')
//        ws
//    }
//
//    private def `char`() =
//            // simple bloom-filter that quick-matches the most frequent case of characters that are ok to append
//            // (it doesn't match control chars, EOI, '"', '?', '\', 'b' and certain higher, non-ASCII chars)
////    if (((1L << cursorChar) & ((31 - cursorChar) >> 31) & 0x7ffffffbefffffffL) != 0L) appendSB(cursorChar)
//            if (((1L << cursorChar) & ((31 - cursorChar) >> 31) & 0x3ffffffbefffffffL) != 0L) appendSB(cursorChar)
//    else cursorChar match {
//        case '"' | EOI | EOS => false
//        case '\\' => advance(); `escaped`()
//        case c => (c >= ' ') && appendSB(c)
//    }
//
//    // single-quoted-char
//    private def sqs_char() =
//    cursorChar match {
//        case '\'' | EOI | EOS => false
//        case '\\' => advance(); `escaped`()
//        case c => (c >= ' ') && appendSB(c)
//    }
//
//    private def `escaped`() = {
//        def hexValue(c: Char): Int =
//        if ('0' <= c && c <= '9') c - '0'
//        else if ('a' <= c && c <= 'f') c - 87
//        else if ('A' <= c && c <= 'F') c - 55
//        else fail("hex digit")
//        def unicode() = {
//                var value = hexValue(cursorChar)
//                advance()
//                value = (value << 4) + hexValue(cursorChar)
//                advance()
//                value = (value << 4) + hexValue(cursorChar)
//                advance()
//                value = (value << 4) + hexValue(cursorChar)
//                appendSB(value.toChar)
//        }
//        (cursorChar: @switch) match {
//            case '"' | '/' | '\\' | '\'' => appendSB(cursorChar)
//            case 'b' => appendSB('\b')
//            case 'f' => appendSB('\f')
//            case 'n' => appendSB('\n')
//            case 'r' => appendSB('\r')
//            case 't' => appendSB('\t')
//            case 'u' => advance(); unicode()
//            case _ => fail("JSON escape sequence")
//        }
//    }
//
//    @tailrec private def ws(): Unit =
//            // fast test whether cursorChar is one of " \n\r\t"
//            if (((1L << cursorChar) & ((cursorChar - 64) >> 31) & 0x100002600L) != 0L) { advance(); ws() }
//
//    ////////////////////////// HELPERS //////////////////////////
//
//    private def ch(c: Char): Boolean = if (cursorChar == c) { advance(); true } else false
//    private def ws(c: Char): Boolean = if (ch(c)) { ws(); true } else false
//    private def advance(): Boolean = { cursorChar = input.nextChar(); true }
//    private def appendSB(c: Char): Boolean = { sb.append(c); true }
//    private def require(c: Char): Unit = if (!ch(c)) fail(s"'$c'")
//
//    private def fail(target: String, cursor: Int = input.cursor, errorChar: Char = cursorChar): Nothing = {
//        val ParserInput.Line(lineNr, col, text) = input.getLine(cursor)
//        val summary = {
//                val unexpected =
//        if (errorChar != EOI) {
//            val c = if (Character.isISOControl(errorChar)) "\\u%04x" format errorChar.toInt else errorChar.toString
//            s"character '$c'"
//        } else "end-of-input"
//        val expected = if (target != "'\uFFFF'") target else "end-of-input"
//        s"Unexpected $unexpected at input index $cursor (line $lineNr, position $col), expected $expected"
//    }
//        val detail = {
//                val sanitizedText = text.map(c ⇒ if (Character.isISOControl(c)) '?' else c)
//        s"\n$sanitizedText\n${" " * (col-1)}^\n"
//    }
//        throw new ParsingException(summary, detail)
//    }

}
