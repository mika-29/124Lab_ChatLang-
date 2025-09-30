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
        val currentChar = nextChar()
        when (currentChar) {
            '(' -> addToken(TokenType.LPAR)
            ')' -> addToken(TokenType.RPAR)
            '{' -> addToken(TokenType.LBRACE)
            '}' -> addToken(TokenType.RBRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)
            '%' -> addToken(TokenType.MODULO)
            '^' -> addToken(TokenType.EXPONENT)
            '&' -> addToken( type = TokenType.AND)
            '|' -> addToken( type = TokenType.OR)

            //Multi-character operators
            '+' -> addToken(type = if (match(expected = '+')) TokenType.INC else TokenType.ADD)
            '-' -> addToken(type = if (match(expected = '-')) TokenType.DEC else TokenType.MINUS)
            '!' -> addToken(if (match('=')) TokenType.NOT_EQUAL else TokenType.NOT)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.ASSIGN)
            '>' -> addToken(if (match('=')) TokenType.G_EQUAL else TokenType.GREATER)
            '<' -> addToken(if (match('=')) TokenType.L_EQUAL else TokenType.LESS)
            '/' -> addToken(TokenType.SLASH)

            ' ','\r','\t' -> {}

            else -> {
                when{
                    source.startsWith("FYI.", current-1) -> {
                        current += "FYI.".length-1
                        scanComment()
                    }
                    currentChar.isLetter() -> readIdentifier()
                    currentChar.isDigit() -> readNumber()
                    currentChar == '"' -> readString()
                    currentChar == '\'' -> readChar()
                    else -> println("[line $line] Unexpected character: '$currentChar'")
                }
            }
        }
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
        while (!reachedEnd() && peek() != '"'){
            if (peek() == '\n') line++
            nextChar()
        }

        if (reachedEnd()) {
            println("[line $line] Unexpected character: '$current'")
            return
        }

        nextChar()

        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.STR, value)
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

    private fun readIdentifier() {
        while (!reachedEnd() && (source[current].isLetterOrDigit() || source[current] == '_')) {
            nextChar()
        }
        val text = source.substring(start, current)
        val type = keywords[text] ?: TokenType.IDENTIFIER
        addToken(type)
    }

    private fun reachedEnd(): Boolean = current >= source.length
    private fun nextChar(): Char = source[current++]

    private fun addToken(type: TokenType, literal: Any? = null) {
        val lexeme = source.substring(start, current)
        tokens.add(Token(type, lexeme, literal, line))
    }

    private fun match(expected: Char): Boolean{
        if (reachedEnd()) return false
        if (source[current] != expected) return false
        current++
        return true
    }

    private fun peek(): Char = if (reachedEnd()) '\u0000' else source[current]
}
