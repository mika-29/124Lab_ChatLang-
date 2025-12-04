package GenZLang

class Scanner(private val source: String) {
    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1

    private val keywords = KeywordFactory.keywords()

    fun scanTokens(): List<Token>{
        while(!reachedEnd()){
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.END_OF_FILE,"", null, line))
        return tokens
    }

    private fun scanToken() {
        skipNoise()
        if(reachedEnd()) return

        if(tryPhraseStart()) return

        val currentChar = nextChar()
        when (currentChar) {
            '(' -> addToken(TokenType.LPAR)
            ')' -> addToken(TokenType.RPAR)
            '{' -> addToken(TokenType.LBRACE)
            '}' -> addToken(TokenType.RBRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            ':' -> addToken(TokenType.COLON)
            '*' -> addToken(TokenType.MULTIPLY)
            '%' -> addToken(TokenType.MODULO)
            '^' -> addToken(TokenType.EXPONENT)
            '&' -> addToken( type = TokenType.AND)
            '|' -> addToken( type = TokenType.OR)
            '/' -> addToken(TokenType.DIVIDE)
            '!' -> addToken(TokenType.NOT)
            '[' -> addToken(TokenType.LBRACKET)
            ']' -> addToken(TokenType.RBRACKET)

            //Multi-character operators
            '+' -> addToken(type = if (match(expected = '+')) TokenType.INC else TokenType.ADD)
            '-' -> addToken(type = if (match(expected = '>')) TokenType.ARROW else TokenType.MINUS)
            '>' -> addToken(if (match('=')) TokenType.G_EQUAL else TokenType.GREATER)
            '<' -> addToken(if (match('=')) TokenType.L_EQUAL else TokenType.LESS)

            '@', '$', '%' -> captureDatatypePrefix(currentChar)

            '"' -> {
                start = current - 1   // ensure lexeme begins at opening quote
                readString()
            }
            '\'' -> readChar()

            else -> {
                when{
                    source.startsWith("FYI.", current-1) -> {
                        current += "FYI.".length-1
                        scanComment()
                    }
                    currentChar.isDigit() -> readNumber()
                    currentChar.isLetter() -> readIdentifier()
                    else -> println("[line $line] Unexpected character: '$currentChar'")
                }
            }
        }
    }

    private fun tryPhraseStart(): Boolean {
        val remaining = source.substring(current).lowercase()

        return when {
            remaining.startsWith("repeat this") -> {
                current += "repeat this".length
                addToken(TokenType.START_LOOP, "repeat this")
                true
            }
            remaining.startsWith("stop when") -> {
                current += "stop when".length
                addToken(TokenType.END_LOOP, "stop when")
                true
            }
            remaining.startsWith("done if") -> {
                current += "done if".length
                addToken(TokenType.END_IF, "done if")
                true
            }
            remaining.startsWith("create a function") -> {
                current += "create a function".length
                addToken(TokenType.FUNCTION, "create a function")
                true
            }
            remaining.startsWith("with parameters") -> {
                current += "with parameters".length
                addToken(TokenType.WITH_PARAMS, "with parameters")
                true
            }

            remaining.startsWith("is not") -> {
                current += "is not".length
                addToken(TokenType.NOT_EQUAL, "is not") // Map 'is not' to '!='
                true
            }
            else -> false
        }
    }

    private fun readIdentifier(){
    
        val wordStart = current - 1
        while (!reachedEnd() && (peek().isLetterOrDigit() || peek() == '_')) nextChar()
        val word = source.substring(wordStart, current).lowercase()

        if (keywords.containsKey(word)) {
            when (word) {
                "true"  -> addToken(TokenType.TRUE, true)
                "false" -> addToken(TokenType.FALSE, false)
                else    -> addToken(keywords[word]!!, word)
            }
            return
        }

        // Otherwise it's an identifier
        val lexeme = source.substring(wordStart, current)
        addToken(TokenType.IDENTIFIER, lexeme)
    }

    // Peek what the next contiguous letter word is without consuming
    private fun peekNextWord(): String {
        var idx = current
        // skip whitespace
        while (idx < source.length && source[idx].isWhitespace()) {
            if (source[idx] == '\n') { /* do not increment line here; peek-only */ }
            idx++
        }
        val start = idx
        while (idx < source.length && (source[idx].isLetterOrDigit() || source[idx] == '_')) idx++
        return if (start < idx) source.substring(start, idx).lowercase() else ""
    }

    // Consume whitespace/newline between words (advances current and updates line)
    private fun consumeBetweenWords() {
        var consumed = false
        while (!reachedEnd() && source[current].isWhitespace()) {
            consumed = true
            if (source[current] == '\n') line++
            current++
        }
        // start is left as-is; remember() will use substring(start,current) so ensure start is correct at callsite
    }

    private fun captureDatatypePrefix(prefix: Char) {
        val startName = current
        while (!reachedEnd() && (peek().isLetterOrDigit() || peek() == '_')) nextChar()

        if (current == startName)
            throw RuntimeException("Variable name expected after '$prefix'")

        val nameStr = source.substring(startName, current)  // clean name, no spaces
        val fullLexeme = "$prefix$nameStr"                   // "@name"

        addToken(TokenType.SIGIL_IDENT, fullLexeme)
    }

    private fun scanComment() {
        while(!reachedEnd() && peek() != '.'){
            nextChar()
        }

        if (reachedEnd()){
            println("[line $line] Comment Incomplete")
            return
        }
        nextChar()
    }

    private fun readChar() {
        if (reachedEnd()) {
            println("[line $line] Unterminated character literal")
            return
        }
        nextChar()

        if (peek() != '\'') {
            println("[line $line] Character literal must be exactly one character")
            return
        }

        nextChar()
        val value = source.substring(start + 1, current - 1)

        addToken(TokenType.CHAR, value[0])
    }


    private fun readString() {
        // consume until closing quote
        while (!reachedEnd() && peek() != '"') {
            if (peek() == '\n') line++
            nextChar()
        }

        if (reachedEnd()) {
            println("[line $line] Unterminated string.")
            return
        }

        nextChar() // consume closing quote

        // extract JUST the inside of the quotes
        val value = source.substring(start + 1, current - 1)

        // produce correct token with literal=string, lexeme=value (no extra quotes)
        tokens.add(Token(TokenType.STR, value, value, line))

    }

    private fun readNumber() {
        while (!reachedEnd() && (source[current].isDigit())) {
            nextChar()
        }

        if(!reachedEnd() && source[current] == '.' && current+1 < source.length && source[current+1].isDigit()){
            nextChar()
            while (!reachedEnd() && (source[current].isDigit())) {
                nextChar()
            }
        }
        val numberString = source.substring(start, current)
        val numberValue = numberString.toDouble()
        addToken(TokenType.NUM, numberValue)

    }

    private fun skipNoise() {
        while (!reachedEnd()) {
            when (peek()) {
                ' ', '\r', '\t' -> nextChar()
                '\n' -> { line++; nextChar() }
                else -> return
            }
        }
    }

    private fun reachedEnd(): Boolean = current >= source.length
    private fun nextChar(): Char = source[current++]

    private fun addToken(type: TokenType, literal: Any? = null) {
        val lexeme = source.substring(start, current)
        val token = Token(type, lexeme, literal, line)
        tokens.add(token)
    }

    private fun match(expected: Char): Boolean{
        if (reachedEnd()) return false
        if (source[current] != expected) return false
        current++
        return true
    }

    private fun peek(): Char = if (reachedEnd()) '\u0000' else source[current]
}