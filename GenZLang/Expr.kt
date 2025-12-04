package GenZLang

import GenZLang.Stmt.Argument

sealed class Expr { //this is our nodes
    data class Number(val token: Token) : Expr()
    data class Str(val token: Token) : Expr()
    data class CharLit(val token: Token) : Expr()
    data class Bool(val token: Token) : Expr()
    object Ghosted : Expr() // represents null
    data class Ident(val token: Token) : Expr()
    data class Group(val expression: Expr) : Expr()
    data class Unary(val op: TokenType, val right: Expr) : Expr()
    data class Binary(val left: Expr, val op: TokenType, val right: Expr) : Expr()
    data class Call(val callee: Expr, val arguments: Argument?) : Expr()
    data class ArrayLit(val elements: List<Expr>) : Expr()
    data class IndexGet(val left: Expr, val index: Expr) : Expr()
}

