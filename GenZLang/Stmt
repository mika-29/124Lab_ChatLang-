package GenZLang

sealed class Stmt {
    data class Sequence (val statement: Stmt, var next: Stmt?) : Stmt()
    data class Program(val root: Stmt?) : Stmt()
    data class Block(val root: Stmt?) : Stmt()
    data class VarDecl(val sigil: Char, val name: Token, val initializer: Expr) : Stmt()
    data class Assign(val name: Token, val value: Expr) : Stmt()
    data class Print(val expr: Expr) : Stmt()
    data class ExprStmt(val expr: Expr) : Stmt()
    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt()
    data class Loop( val condition: Expr, val body: Stmt) : Stmt()
    data class HybridLoop(val count: Expr, val body: Stmt, val stopCondition: Expr?) : Stmt()
    data class Function(val name: Token, val parameters: Param?, val body: Stmt) : Stmt()
    data class Param(val name: Token, val next: Param?)
    data class Argument(val expression: Expr, val next: Argument?)
    data class Return(val value: Expr) : Stmt()
    data class Input(val prompt: Expr, val variable: Token) : Stmt()
    data class ArraySet(val name: Token, val index: Expr, val value: Expr) : Stmt()
}
