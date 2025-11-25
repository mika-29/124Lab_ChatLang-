package GenZLang

class ParseError(val token: Token, message: String) : RuntimeException(message)

class Parser(private val tokens: List<Token>) {
    private var current = 0

    private fun peek() = tokens[current]
    private fun previous() = tokens[current - 1]
    private fun isAtEnd() = peek().type == TokenType.END_OF_FILE
    private fun advance(): Token { if (!isAtEnd()) current++; return previous() }
    private fun check(type: TokenType) = !isAtEnd() && peek().type == type
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) { advance(); return true }
        }
        return false
    }
    private fun consume(type: TokenType, msg: String): Token {
        if (check(type)) return advance()
        throw ParseError(peek(), msg)
    }

    fun parseProgram(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()){
            statements.add(parseStatement())
        }
        return statements
    }

    // ----------------------------
    // ENTRY POINT
    // ----------------------------
    fun parseExpression(): Expr = parseLogicalOr()

    // ----------------------------
    // RECURSIVE EXPRESSION RULES
    // ----------------------------

    private fun parseLogicalOr(): Expr {
        var expr = parseLogicalAnd()
        while (match(TokenType.OR)) {
            val op = previous().type
            val right = parseLogicalAnd()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    private fun parseLogicalAnd(): Expr {
        var expr = parseEquality()
        while (match(TokenType.AND)) {
            val op = previous().type
            val right = parseEquality()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    private fun parseEquality(): Expr {
        var expr = parseRelational()
        while (match(TokenType.EQUAL_EQUAL, TokenType.NOT_EQUAL)) {
            val op = previous().type
            val right = parseRelational()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    private fun parseRelational(): Expr {
        var expr = parseAdditive()
        while (match(TokenType.LESS, TokenType.GREATER, TokenType.L_EQUAL, TokenType.G_EQUAL)) {
            val op = previous().type
            val right = parseAdditive()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    private fun parseAdditive(): Expr {
        var expr = parseTerm()
        while (match(TokenType.ADD, TokenType.MINUS)) {
            val op = previous().type
            val right = parseTerm()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    private fun parseTerm(): Expr {
        var expr = parseFactor()
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO)) {
            val op = previous().type
            val right = parseFactor()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    private fun parseFactor(): Expr {
        if (match(TokenType.NOT, TokenType.MINUS)) {
            val op = previous().type
            val right = parseFactor()
            return Expr.Unary(op, right)
        }
        return parsePower()
    }

    private fun parsePower(): Expr {
        var base = parsePrimary()
        if (match(TokenType.EXPONENT)) {
            val op = previous().type
            val right = parseFactor() // right-associative
            base = Expr.Binary(base, op, right)
        }
        return base
    }

    private fun parsePrimary(): Expr {
        when {
            match(TokenType.NUM) -> return Expr.Number(previous())
            match(TokenType.STR) -> return Expr.Str(previous())
            match(TokenType.CHAR) -> return Expr.CharLit(previous())
            match(TokenType.TRUE) -> return Expr.Bool(previous())
            match(TokenType.FALSE) -> return Expr.Bool(previous())
            match(TokenType.NULL) -> return Expr.Ghosted
            match(TokenType.IDENTIFIER) -> return Expr.Ident(previous())
            match(TokenType.SIGIL_IDENT) -> return Expr.Ident(previous())
            match(TokenType.LPAR) -> {
                val expr = parseExpression()
                consume(TokenType.RPAR, "Expect ')' after expression.")
                return Expr.Group(expr)
            }
            else -> throw ParseError(peek(), "Expect expression.")
        }
    }

    //parse statements
    fun parseStatement(): Stmt {
        return when {
            match(TokenType.PRINT) -> parsePrintStatement()
            match(TokenType.INPUT) -> parseListenStatement()
            match(TokenType.SIGIL_IDENT) -> parseVarDeclaration()
            match(TokenType.COLON) -> Stmt.Block(parseBlock())
            (check(TokenType.IDENTIFIER) || check(TokenType.SIGIL_IDENT)) && peekNextType() == TokenType.ASSIGNMENT -> parseAssignment()

            else -> parseExpressionStatement()
        }
    }

    // helper to parse blocks until a terminator is found
    private fun parseBlock(): List<Stmt> {
        val statements = mutableListOf<Stmt>()

        // Keep parsing until we hit 'done', 'done if', 'stop when', or EOF
        while (!checkBlockEnd() && !isAtEnd()) {
            statements.add(parseStatement())
        }

        // Consume the terminator if it exists (e.g. "done")
        if (checkBlockEnd()) {
            advance()
        }

        return statements
    }

    // Helper to identify block terminators [cite: 470, 471, 474]
    private fun checkBlockEnd(): Boolean {
        return check(TokenType.END) || check(TokenType.END_IF) || check(TokenType.END_LOOP)
    }

    private fun parsePrintStatement(): Stmt {
        val value = parseExpression()
        return Stmt.Print(value)
    }

    private fun parseExpressionStatement(): Stmt {
        val expr = parseExpression()
        return Stmt.ExprStmt(expr)
    }

    private fun parseVarDeclaration(): Stmt {
        val sigilToken = previous()
        val lexeme = sigilToken.lexeme  // "@name"

        val sigilChar = lexeme[0]
        val nameStr = lexeme.substring(1)

        val nameToken = Token(
            TokenType.IDENTIFIER,
            nameStr,
            null,
            sigilToken.line
        )
        consume(TokenType.ASSIGNMENT, "Expect 'as' in variable declaration.")

        val initializer = parseExpression()

        return Stmt.VarDecl(sigilChar, nameToken, initializer)
    }

    private fun parseAssignment(): Stmt {
        val nameToken = if (match(TokenType.SIGIL_IDENT)) previous() else consume(TokenType.IDENTIFIER, "Expect variable name.")
        consume(TokenType.ASSIGNMENT, "Expect 'as' after variable name.")
        val value = parseExpression()
        return Stmt.Assign(nameToken, value)
    }

    //helper
    private fun peekNextType(): TokenType? {
        // Check if there is a next token
        return if (current + 1 < tokens.size) {
            tokens[current + 1].type
        } else {
            null
        }
    }

    private fun parseListenStatement(): Stmt {
        // Syntax: Listen "Prompt" as @name
        val promptToken = consume(TokenType.STR, "Expect prompt string after 'Listen'.")
        consume(TokenType.ASSIGNMENT, "Expect 'as' after prompt string.")

        val variable = consume(TokenType.SIGIL_IDENT, "Expect variable for input (e.g., @name).")

        return Stmt.Listen(promptToken.literal.toString(), variable)
    }
}