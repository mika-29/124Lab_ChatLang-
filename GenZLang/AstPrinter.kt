package GenZLang

object AstPrinter {
    fun printExpr(expr: Expr) {
        println(print(expr))
    }

    fun print(expr: Expr): Any {
        return when (expr) {
            is Expr.Number -> expr.token.lexeme.toDouble()
            is Expr.Str -> expr.token.lexeme
            is Expr.CharLit -> expr.token.lexeme
            is Expr.Bool -> expr.token.lexeme
            is Expr.Ghosted -> "nil"
            is Expr.Ident -> expr.token.lexeme
            is Expr.Group -> "(group ${print(expr.expression)})"
            is Expr.Unary -> "(${opName(expr.op)} ${print(expr.right)})"
            is Expr.Binary -> return "(${opName(expr.op)} ${print(expr.left)} ${print(expr.right)})"
            is Expr.Call -> "(${print(expr.callee)} ${printArgumentChain(expr.arguments)})"
            is Expr.ArrayLit -> "(${print(expr.elements)})"
            is Expr.IndexGet -> "(${print(expr.left)} ${print(expr.index)})" }
    }

    // Helper to print the recursive Argument chain
    fun printArgumentChain(arg: Stmt.Argument?): String {
        if (arg == null) {
            return ""
        }
        // Prints the current argument expression, followed by the rest of the chain
        val current = print(arg.expression)
        val next = printArgumentChain(arg.next)

        // Concatenate the current argument with the rest of the arguments
        return if (next.isEmpty()) current.toString() else "$current $next"
    }

    private fun opName(op: TokenType): String = when (op) {
        TokenType.ADD -> "+"
        TokenType.MINUS -> "-"
        TokenType.MULTIPLY -> "*"
        TokenType.DIVIDE -> "/"
        TokenType.MODULO -> "%"
        TokenType.EXPONENT -> "^"
        TokenType.EQUAL_EQUAL -> "=="
        TokenType.NOT_EQUAL -> "!="
        TokenType.LESS -> "<"
        TokenType.GREATER -> ">"
        TokenType.L_EQUAL -> "<="
        TokenType.G_EQUAL -> ">="
        TokenType.AND -> "&"
        TokenType.OR -> "|"
        TokenType.NOT -> "!"
        else -> op.name
    }
}



