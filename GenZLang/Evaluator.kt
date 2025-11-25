package GenZLang

class Evaluator(val isRepl: Boolean = false) {
    var env = Environment()

    fun evaluate(expr: Expr): Any? {
        return try {
            eval(expr)
        } catch (e: RuntimeError) {
            println("[ line ${e.token.line} ] Runtime error: ${e.message}")
            null
        }
    }

    private fun eval(expr: Expr): Any? {
        return when (expr) {
            is Expr.Number -> expr.token.literal
            is Expr.Str -> expr.token.literal
            is Expr.CharLit -> expr.token.literal
            is Expr.Bool -> expr.token.literal
            Expr.Ghosted -> null
            is Expr.Ident -> {
                val full = expr.token.lexeme          // "@name", "$age", "%score"
                // Logic to handle variable lookup: strip sigil if present
                val identifier = if (full.startsWith("@") || full.startsWith("$") || full.startsWith("%")) {
                    full.substring(1)
                } else {
                    full
                }
                env.get(identifier)
            }
            is Expr.Group -> eval(expr.expression)
            is Expr.Unary -> evalUnary(expr)
            is Expr.Binary -> evalBinary(expr)
        }
    }

    // ... (Your existing evalUnary, evalBinary, numeric, etc. methods go here) ...
    // Note: I am not repeating them to save space, but KEEP THEM in your file!
    // Paste your existing evalUnary, evalBinary, addValues, numeric, numericOperands, compareNumbers, isEqual, isTruthy here.

    // --- PASTE START (For context, assuming you keep your existing helper methods) ---
    private fun evalUnary(expr: Expr.Unary): Any? {
        val right = eval(expr.right)
        return when (expr.op) {
            TokenType.MINUS -> {
                if (right !is Double) throw RuntimeError(expr.right.token(), "Operand must be a number.")
                -right
            }
            TokenType.NOT -> !isTruthy(right)
            else -> right
        }
    }

    private fun evalBinary(expr: Expr.Binary): Any? {
        val left = eval(expr.left)
        val right = eval(expr.right)
        return when (expr.op) {
            TokenType.ADD -> addValues(expr, left, right)
            TokenType.MINUS -> numeric(expr, left, right) { a, b -> a - b }
            TokenType.MULTIPLY -> numeric(expr, left, right) { a, b -> a * b }
            TokenType.DIVIDE -> {
                val (a, b) = numericOperands(expr, left, right)
                if (b == 0.0) throw RuntimeError(expr.right.token(), "Division by zero.")
                a / b
            }
            TokenType.MODULO -> numeric(expr, left, right) { a, b -> a % b }
            TokenType.EXPONENT -> numeric(expr, left, right) { a, b -> Math.pow(a, b) }
            TokenType.LESS -> compareNumbers(expr, left, right) { a, b -> a < b }
            TokenType.GREATER -> compareNumbers(expr, left, right) { a, b -> a > b }
            TokenType.L_EQUAL -> compareNumbers(expr, left, right) { a, b -> a <= b }
            TokenType.G_EQUAL -> compareNumbers(expr, left, right) { a, b -> a >= b }
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            TokenType.NOT_EQUAL -> !isEqual(left, right)
            TokenType.AND -> isTruthy(left) && isTruthy(right)
            TokenType.OR -> isTruthy(left) || isTruthy(right)
            else -> null
        }
    }

    private fun addValues(expr: Expr.Binary, left: Any?, right: Any?): Any {
        return when {
            left is Double && right is Double -> left + right
            left is String && right is String -> left + right
            else -> throw RuntimeError(expr.left.token(), "Operands must be two numbers or two strings.")
        }
    }

    private fun numeric(expr: Expr.Binary, left: Any?, right: Any?, op: (Double, Double) -> Double): Double {
        val (a, b) = numericOperands(expr, left, right)
        return op(a, b)
    }

    private fun numericOperands(expr: Expr.Binary, left: Any?, right: Any?): Pair<Double, Double> {
        if (left !is Double) throw RuntimeError(expr.left.token(), "Operands must be numbers.")
        if (right !is Double) throw RuntimeError(expr.right.token(), "Operands must be numbers.")
        return Pair(left, right)
    }

    private fun compareNumbers(expr: Expr.Binary, left: Any?, right: Any?, op: (Double, Double) -> Boolean): Boolean {
        val (a, b) = numericOperands(expr, left, right)
        return op(a, b)
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        if (a == null && b == null) return true
        if (a == null) return false
        return a == b
    }

    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }
    // --- PASTE END ---

    // [Change 2] Updated execute to handle Statements, Blocks, and Type Validation
    fun execute(stmt: Stmt) {
        when (stmt) {
            is Stmt.Block -> executeBlock(stmt.statements, Environment(env))

            is Stmt.Print -> {
                val value = eval(stmt.expr)
                println(formatResult(value)) // Uses the helper in Main.kt or similar
            }

            is Stmt.ExprStmt -> {
                val value = eval(stmt.expr)
                if (isRepl) {
                    println(formatResult(value))
                }
            }

            // [CHANGE 2: Implement Listen Logic]
            is Stmt.Listen -> {
                print(stmt.prompt + " ") // Print prompt without newline
                val input = readlnOrNull() ?: ""

                // Convert based on sigil ($ = Int, % = Float, @ = String)
                val value: Any = when {
                    stmt.variable.lexeme.startsWith("$") -> input.toIntOrNull() ?: 0
                    stmt.variable.lexeme.startsWith("%") -> input.toDoubleOrNull() ?: 0.0
                    else -> input
                }

                // Save to environment (remove sigil)
                val name = stmt.variable.lexeme.substring(1)

                // Upsert logic
                try { env.assign(name, value) }
                catch (e: RuntimeException) { env.define(name, value) }
            }

            is Stmt.VarDecl -> {
                var value = eval(stmt.initializer)

                // [Rule] Auto-convert Double to Int if the variable is '$' (Int)
                if (stmt.sigil == '$' && value is Double) {
                    value = value.toInt()
                }

                validateType(stmt.sigil, value, stmt.name)

                // Standardize name: Remove the sigil (@, $, %) so we store just "x"
                val cleanName = if(stmt.name.lexeme.startsWith(stmt.sigil)) {
                    stmt.name.lexeme.substring(1)
                } else {
                    stmt.name.lexeme
                }

                // [CRITICAL CHANGE] "Upsert" Logic (Update or Insert)
                try {
                    // 1. Try to update an existing variable.
                    // This checks the current scope, then parent, then grandparent...
                    env.assign(cleanName, value)
                } catch (e: RuntimeException) {
                    // 2. If 'assign' throws an error (variable not found),
                    // create a NEW variable in the CURRENT scope.
                    env.define(cleanName, value)
                }
            }

            is Stmt.Assign -> {
                var value = eval(stmt.value)
                val lexeme = stmt.name.lexeme

                val hasSigil = lexeme.startsWith("@") || lexeme.startsWith("$") || lexeme.startsWith("%")
                val sigil = if (hasSigil) lexeme[0] else ' '
                val cleanName = if (hasSigil) lexeme.substring(1) else lexeme

                // [Rule 1] Convert Double to Int for '$'
                if (sigil == '$' && value is Double) {
                    value = value.toInt()
                }

                // [Rule 2] Convert Char to String for '@'
                if (sigil == '@' && value is Char) {
                    value = value.toString()
                }

                // [Rule 3] Validate everything matches
                if (hasSigil) {
                    validateType(sigil, value, stmt.name)
                }

                // Upsert (Update or Insert)
                try {
                    env.assign(cleanName, value)
                } catch (e: RuntimeException) {
                    env.define(cleanName, value)
                }
            }
            else -> throw RuntimeException("Unimplemented statement type: ${stmt::class.simpleName}")
        }
    }

    // [Change 3] Helper to execute a list of statements in a new scope
    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.env
        try {
            this.env = environment
            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.env = previous // Restore the previous environment
        }
    }

    fun executeProgram(statements: List<Stmt>) {
        for (stmt in statements) {
            execute(stmt)
        }
    }

    // [Change 4] Helper to enforce The Chat Lang's strict typing rules
    private fun validateType(sigil: Char, value: Any?, token: Token) {
        if (value == null) return // Allow nulls

        when (sigil) {
            '@' -> {
                if (value !is String)
                    throw RuntimeError(token, "Type Mismatch: Variable '${token.lexeme}' expects a String (text), but got ${getTypeName(value)}.")
            }
            '$' -> {
                // We expect an Int. Scanner gives Doubles.
                if (value is Double) {
                    if (value % 1 != 0.0) {
                        throw RuntimeError(token, "Type Mismatch: Variable '${token.lexeme}' expects an Int (whole number), but got a Decimal ($value). Use '%' for decimals.")
                    }
                } else if (value !is Int) {
                    throw RuntimeError(token, "Type Mismatch: Variable '${token.lexeme}' expects an Int.")
                }
            }
            '%' -> {
                if (value !is Double && value !is Float)
                    throw RuntimeError(token, "Type Mismatch: Variable '${token.lexeme}' expects a Float (decimal).")
            }
        }
    }

    private fun getTypeName(obj: Any): String = obj::class.simpleName ?: "Unknown"

    // Helper used in Stmt.Print (needs to match Main.kt)
    private fun formatResult(value: Any?): String {
        return when (value) {
            null -> "nil"
            is Double -> {
                val intVal = value.toInt()
                if (value == intVal.toDouble()) intVal.toString()
                else value.toString()
            }
            else -> value.toString()
        }
    }
}