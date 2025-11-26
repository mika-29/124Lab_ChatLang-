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
            match(TokenType.IF) -> parseIfStatement()
            match(TokenType.PRINT) -> parsePrintStatement()
            match(TokenType.SIGIL_IDENT) -> parseVarDeclaration()
            check(TokenType.SIGIL_IDENT) && peekNextType() == TokenType.EQUAL -> parseAssignment()
            else -> parseExpressionStatement()
        }
    }
    private fun parseIfStatement(): Stmt {
        val condition = parseExpression()

        consume(TokenType.COLON, "Expect ':' after then.")

        // IMPORTANT: We use a helper to grab all statements inside the block
        val thenStmts = parseBlockStatements(listOf(TokenType.ELSE, TokenType.END_IF))

        // IMPORTANT: We wrap them in Stmt.Block.
        // The Evaluator sees this and creates a NEW Scope (Environment).
        val thenBranch = Stmt.Block(thenStmts)

        var elseBranch: Stmt? = null
        if (match(TokenType.ELSE)) {
            consume(TokenType.COLON, "Expect ':' after otherwise.")
            val elseStmts = parseBlockStatements(listOf(TokenType.END_IF))
            elseBranch = Stmt.Block(elseStmts)
        }

        consume(TokenType.END_IF, "Expect 'done if' to close condition.")
        match(TokenType.DOT) // Optional: Consumes dot if present after Done if.
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    // Helper: Collects statements until it hits a keyword (Else or Done if)
    private fun parseBlockStatements(terminators: List<TokenType>): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd() && terminators.none { check(it) }) {
            statements.add(parseStatement())
        }
        return statements
    }

    private fun parsePrintStatement(): Stmt {
        val value = parseExpression()
        match(TokenType.DOT)
        return Stmt.Print(value)
    }

    private fun parseExpressionStatement(): Stmt {
        val expr = parseExpression()
        match(TokenType.DOT)
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

        match(TokenType.DOT)
        return Stmt.VarDecl(sigilChar, nameToken, initializer)
    }

    private fun parseAssignment(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
        consume(TokenType.ASSIGNMENT, "Expect 'as' after variable name.")
        val value = parseExpression()
        match(TokenType.DOT)
        return Stmt.Assign(name, value)
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
}