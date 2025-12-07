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
            match(TokenType.SIGIL_IDENT) -> {
                var expr: Expr = Expr.Ident(previous())
                while (match(TokenType.LBRACKET)) {
                    val index = parseExpression()
                    consume(TokenType.RBRACKET, "Expect ']' after index.")
                    expr = Expr.IndexGet(expr, index)
                }
                return expr
            }
            match(TokenType.CALL) -> return parseCallExpression()
            match(TokenType.LBRACKET) -> return parseArrayLiteral()
            match(TokenType.LPAR) -> {
                val expr = parseExpression()
                consume(TokenType.RPAR, "Expect ')' after expression.")
                return Expr.Group(expr)
            }
            else -> throw ParseError(peek(), "Expect expression.")
        }
    }

    fun parseProgram(): Stmt.Program {
        // Start parsing the chain until EOF
        val root = parseChain()
        return Stmt.Program(root)
    }

    // NEW: Recursive function to build linked nodes
    private fun parseChain(terminator: TokenType? = null): Stmt? {
        if (isAtEnd()) return null

        // Stop if we hit the specific terminator (like 'ELSE' or 'DONE IF')
        if (terminator != null && check(terminator)) return null
        val currentStmt = parseStatement()

        // Recursively parse the NEXT statement
        val nextStmt = parseChain(terminator)
        return Stmt.Sequence(currentStmt, nextStmt)
    }

    // Modified helper for If-Else that might have two terminators
    private fun parseBlockChain(vararg terminators: TokenType): Stmt? {
        if (isAtEnd() || terminators.any {check(it)}) return null

        val currentStmt = parseStatement()
        val nextStmt = parseBlockChain(*terminators)

        return Stmt.Sequence(currentStmt, nextStmt)
    }

    //parse statements
    fun parseStatement(): Stmt {
        return when {
            match(TokenType.IF) -> parseIfStatement()
            match(TokenType.PRINT) -> parsePrintStatement()
            match(TokenType.START_LOOP) -> parseRepeatStatement()
            match(TokenType.FUNCTION) -> parseFunctionDefinition()
            match(TokenType.RETURN) -> parseReturnStatement()
            match(TokenType.INPUT) -> parseInputStatement()

            check(TokenType.SIGIL_IDENT) && tokens.getOrNull(current +1)?.type == TokenType.ASSIGNMENT -> {
                parseAssignmentOrDeclaration()
            }
            check(TokenType.SIGIL_IDENT) && tokens.getOrNull(current +1)?.type == TokenType.LBRACKET -> {
                parseArrayAssignment()
            }
            else -> parseExpressionStatement()
        }
    }

    private fun parseInputStatement(): Stmt {

        val prompt = parseExpression()
        consume(TokenType.ASSIGNMENT, "Expect 'as' after input prompt.")

        val variable = consume(TokenType.SIGIL_IDENT, "Expect variable to store input.")
        consume(TokenType.DOT, "Expect '.' after input statement.")

        return Stmt.Input(prompt, variable)
    }

    private fun parseIfStatement(): Stmt {
        val condition = parseExpression()

        consume(TokenType.COLON, "Expect ':' after then.")

        var thenRoot = parseBlockChain(TokenType.NESTED_IF,TokenType.ELSE, TokenType.END_IF)

        if(match(TokenType.NESTED_IF)){
            consume(TokenType.COLON, "Expect ':' after then.")

            val secondRoot = parseBlockChain(TokenType.ELSE, TokenType.END_IF)

            if (thenRoot == null){
                thenRoot = secondRoot
            } else if (secondRoot != null) {
                var current = thenRoot
                while(current is Stmt.Sequence && current.next != null){
                    current = current.next!!
                }
                if (current is Stmt.Sequence){
                    current.next = secondRoot
                } else {
                    thenRoot = Stmt.Sequence(thenRoot, secondRoot)
                }
            }
        }
        val thenBranch = Stmt.Block(thenRoot)

        var elseBranch: Stmt? = null
        if (match(TokenType.ELSE)) {
            consume(TokenType.COLON, "Expect ':' after otherwise.")
            val elseRoot = parseChain(TokenType.END_IF)
            elseBranch = Stmt.Block(elseRoot)
        }

        consume(TokenType.END_IF, "Expect 'done if' to close condition.")
        match(TokenType.DOT) 
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun parseLoopStatement(): Stmt {
        consume(TokenType.COLON, "Expect ':' after 'repeat this'.")
        val bodyRoot = parseChain(TokenType.END_LOOP)
        val body = Stmt.Block(bodyRoot)

        consume(TokenType.END_LOOP, "Expect 'stop when' to close the loop.")
        val condition = parseExpression()

        consume(TokenType.DOT, "Expect '.' after loop condition.")

        return Stmt.Loop(condition, body)
    }

    private fun parseRepeatStatement(): Stmt {
        if (check(TokenType.COLON)) {
            return parseLoopStatement()
        }

        val count = parseExpression()

        consume(TokenType.ITERATION, "Expect 'times' after loop count.")
        consume(TokenType.COLON, "Expect ':' after 'times'.")

        return parseHybridLoop(count)
    }

    private fun parseHybridLoop(countExpr: Expr): Stmt{

        val bodyRoot = parseChain(TokenType.END_LOOP)
        val body = Stmt.Block(bodyRoot)

        consume(TokenType.END_LOOP, "Expect 'stop when' to close the loop.")

        var stopCondition: Expr? = null

        if (!check(TokenType.DOT)) {
            stopCondition = parseExpression()
        }

        consume(TokenType.DOT, "Expect '.' after loop.")

        return Stmt.HybridLoop(countExpr, body, stopCondition)
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

    private fun parseAssignmentOrDeclaration(): Stmt {
        val sigilToken = advance()
        val lexeme = sigilToken.lexeme 

        //val sigilChar = lexeme[0]
        val nameStr = lexeme.substring(1)

        val nameToken = Token(
            TokenType.IDENTIFIER,
            nameStr,
            null,
            sigilToken.line
        )

        consume(TokenType.ASSIGNMENT, "Expect 'as' in variable declaration.")
        val initializer = parseExpression()

        consume(TokenType.DOT, "Expect '.' after assignment or declaration.")
        return Stmt.Assign(nameToken, initializer)
    }

    private fun parseFunctionDefinition(): Stmt.Function{
        val nameToken = consume(TokenType.IDENTIFIER, "Expect function name (in quotes) after 'crete a function'.")
        var parametersHead: Stmt.Param? = null

        if(match(TokenType.WITH_PARAMS)){
            consume(TokenType.ARROW, "Expect '->' after parameter phrase.")
            parametersHead = parseParameterChain()
        }

        consume(TokenType.COLON, "Expect ':' before function body starts")

        val bodyRoot = parseChain(TokenType.END)
        val bodyBlock = Stmt.Block(bodyRoot)

        consume(TokenType.END, "Expect 'done' to close the function body.")
        match(TokenType.DOT)

        return Stmt.Function(nameToken, parametersHead, bodyBlock)
    }

    private fun parseParameterChain(): Stmt.Param? {
        if(check(TokenType.COLON)){
            return null
        }

        val paramToken = consume(TokenType.SIGIL_IDENT, "Expect parameter name.")
        var nextParam: Stmt.Param? = null
        if(match(TokenType.COMMA)){
            nextParam = parseParameterChain()
        }
        return Stmt.Param(paramToken, nextParam)
    }

    private fun parseCallExpression(): Expr{
        val calleeName = if(check(TokenType.SIGIL_IDENT)) {
            consume(TokenType.SIGIL_IDENT, "expect function variable.")
        } else {
            consume(TokenType.IDENTIFIER, "expect function name after 'use'.")
        }
        //change to STR, if string
        val calleeExpr = Expr.Ident(calleeName)
        var argumentsHead: Stmt.Argument? = null

        if(match(TokenType.WITH)){
            argumentsHead = parserArgumentChainWithKeywords()
        }
        //add match(tokentype.dot) if needed
        return Expr.Call(calleeExpr, argumentsHead)
    }

    private fun parserArgumentChainWithKeywords(): Stmt.Argument? {
        val argExpr = parseExpression()
        var nextArg: Stmt.Argument? = null

        if(match(TokenType.COMMA)){
            nextArg = parserArgumentChainWithKeywords()
        }
        return Stmt.Argument(argExpr, nextArg)
    }
    private fun parseReturnStatement(): Stmt.Return {
        // We already consumed the RETURN token to get here.
        val value = if(check(TokenType.DOT)){
            Expr.Ghosted
        } else {
            parseExpression()
        }
        consume(TokenType.DOT, "Expect '.' after return value.")
        return Stmt.Return(value)
    }

    private fun parseArrayLiteral(): Expr {
        val elements = mutableListOf<Expr>()
        if (!check(TokenType.RBRACKET)) {
            do {
                elements.add(parseExpression())
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RBRACKET, "Expect ']' after array elements.")
        return Expr.ArrayLit(elements)
    }

    private fun parseArrayAssignment(): Stmt {
        val nameToken = consume(TokenType.SIGIL_IDENT, "Expect array variable name.")

        consume(TokenType.LBRACKET, "Expect '[' after variable name.")
        val indexExpr = parseExpression()
        consume(TokenType.RBRACKET, "Expect ']' after index.")

        consume(TokenType.ASSIGNMENT, "Expect 'as' to assign a value to the index.")
        val valueExpr = parseExpression()

        consume(TokenType.DOT, "Expect '.' after array assignment.")

        return Stmt.ArraySet(nameToken, indexExpr, valueExpr)
    }
}