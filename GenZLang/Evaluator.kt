package GenZLang

import java.lang.RuntimeException

class GenZFunction(
    val declaration: Stmt.Function,
    val closure: Environment
) : GenZCallable { // Implementing the interface

    override fun arity(): Int {
        var count = 0
        var param = declaration.parameters
        // Walk the linked list of parameters to count them
        while (param != null) {
            count++
            param = param.next
        }
        return count
    }

    override fun call(interpreter: Evaluator, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        var param = declaration.parameters
        var i = 0 // Index for the list of values passed in

        while (param != null) {
            // Get the parameter name from the declaration
            val rawName = param.name.lexeme.trim()

            val cleanName = if (rawName.startsWith("$") || rawName.startsWith("@") || rawName.startsWith("%")) {
                rawName.substring(1)
            } else {
                rawName
            }

            environment.define(cleanName, arguments[i])

            param = param.next
            i++
        }

        try {
            // Run the block using the interpreter's existing helper
            val bodyBlock = declaration.body as Stmt.Block
            interpreter.executeBlock(bodyBlock.root, environment)

        } catch (returnValue: ReturnValue) {
            return returnValue.value
        }

        return null // Return null if the function finishes without a return statement
    }
}

class ReturnValue(val value: Any?) : RuntimeException()

class Evaluator {
    var env = Environment()

    init {
        // --- NATIVE FUNCTION: clock() ---
        env.define("clock", object : GenZCallable {
            override fun arity(): Int = 0
            override fun call(interpreter: Evaluator, arguments: List<Any?>): Any? {
                return System.currentTimeMillis() / 1000.0
            }
        })
    }

    private fun eval(expr: Expr): Any? {
        return when (expr) {

            is Expr.Number -> expr.token.literal
            is Expr.Str -> expr.token.literal
            is Expr.CharLit -> expr.token.literal
            is Expr.Bool -> expr.token.literal
            Expr.Ghosted -> null
            is Expr.Ident -> {
                val name = stripSigil(expr.token.lexeme)
                env.get(name)                  // look up the value in the environment
            }
            is Expr.Group -> eval(expr.expression)

            is Expr.Unary -> evalUnary(expr)
            is Expr.Binary -> evalBinary(expr)
            is Expr.Call -> evalCall(expr)
            is Expr.ArrayLit -> {expr.elements.map { eval(it) } }
            is Expr.IndexGet -> {
                val left = eval(expr.left)
                val indexVal = eval(expr.index)

                if (indexVal !is Double) throw RuntimeError(expr.left.token(), "Index must be a number.")
                val idx = indexVal.toInt()

                return when (left) {
                    // Allow reading from existing Lists
                    is List<*> -> {
                        if (idx < 0 || idx >= left.size) return null
                        left[idx]
                    }
                    // NEW: Allow reading from Strings
                    is String -> {
                        if (idx < 0 || idx >= left.length) return null
                        left[idx].toString() // Return char as a String
                    }

                    else -> throw RuntimeError(expr.left.token(), "Can only index arrays or strings.")
                }
            }
        }
    }

    private fun evalCall(expr: Expr.Call): Any? {
        // 1. Get the callee name
        val calleeNameExpr = expr.callee as? Expr.Ident
            ?: throw RuntimeError(expr.callee.token(), "Callee must be an identifier.")

        val functionName = stripSigil(calleeNameExpr.token.lexeme)
        val callee = env.get(functionName)

        // 2. Evaluate all arguments into a list
        val arguments = mutableListOf<Any?>()
        var argNode = expr.arguments
        while (argNode != null) {
            arguments.add(eval(argNode.expression))
            argNode = argNode.next
        }

        // 3. Check if it implements the Interface (GenZFunction OR Native)
        if (callee !is GenZCallable) {
            throw RuntimeError(expr.callee.token(), "Function '$functionName' not found or is not callable.")
        }

        // 4. Check argument count (Arity)
        if (arguments.size != callee.arity()) {
            throw RuntimeError(expr.callee.token(), "Expected ${callee.arity()} arguments but got ${arguments.size}.")
        }

        // 5. Delegate execution to the object itself
        return callee.call(this, arguments)
    }

    private fun evalUnary(expr: Expr.Unary): Any? {
        val right = eval(expr.right)

        return when (expr.op) {
            TokenType.MINUS -> {
                if (right !is Double)
                    throw RuntimeError(expr.right.token(), "Operand must be a number.")
                -right
            }
            TokenType.NOT -> !isTruthy(right)
            else -> right
        }
    }

    private fun evalBinary(expr: Expr.Binary): Any? {   //so may mali pa sa equality if using boolean like (5>3) == true stuff like that
        val left = eval(expr.left)
        val right = eval(expr.right)
        //println("EVAL BINARY: left=$left (${left?.javaClass}), right=$right (${right?.javaClass}), op=${expr.op}")

        return when (expr.op) {

            TokenType.ADD -> addValues(expr, left, right)

            TokenType.MINUS -> numeric(expr, left, right) { a, b -> a - b }
            TokenType.MULTIPLY -> numeric(expr, left, right) { a, b -> a * b }
            TokenType.DIVIDE -> {
                val (a, b) = numericOperands(expr, left, right)
                if (b == 0.0)
                    throw RuntimeError(expr.right.token(), "Division by zero.")
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
            left is String && right is Double -> left + right
            left is List<*> && right is List<*> -> {
                val combined = mutableListOf<Any?>()
                combined.addAll(left)
                combined.addAll(right)
                combined
            } else -> throw RuntimeError(expr.left.token(),
                "Operands must be two numbers or two strings.")
        }
    }

    private fun numeric(
        expr: Expr.Binary,
        left: Any?,
        right: Any?,
        op: (Double, Double) -> Double
    ): Double {
        val (a, b) = numericOperands(expr, left, right)
        return op(a, b)
    }

    private fun numericOperands(expr: Expr.Binary, left: Any?, right: Any?): Pair<Double, Double> {
        if (left !is Double)
            throw RuntimeError(expr.left.token(), "Operands must be numbers.")
        if (right !is Double)
            throw RuntimeError(expr.right.token(), "Operands must be numbers.")
        return Pair(left, right)
    }

    private fun compareNumbers(
        expr: Expr.Binary,
        left: Any?,
        right: Any?,
        op: (Double, Double) -> Boolean
    ): Boolean {
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

    fun executeProgram(program: Stmt.Program) {
        if (program.root != null) {
            execute(program.root)
        }
    }

    //printing
    fun execute(stmt:Stmt){
        when (stmt) {
            // THE NODE TRAVERSAL
            is Stmt.Sequence -> {
                execute(stmt.statement) // 1. Do the current thing
                if (stmt.next != null) {
                    execute(stmt.next!!)  // 2. Recursively do the next thing
                }
            }

            is Stmt.Print -> {
                val value = eval(stmt.expr)
                if(value is List<*>){
                    println(value.joinToString(""))
                }else{
                    println(value)
                }
            }

            is Stmt.ExprStmt -> {
                eval(stmt.expr) // ignore result
            }

            is Stmt.VarDecl -> {
                val value = eval(stmt.initializer)
                val name = stripSigil(stmt.name.lexeme)
                env.define(name, value)
            }

            is Stmt.Assign -> {
                val value = eval(stmt.value)
                val name = stripSigil(stmt.name.lexeme)
                try {
                    env.assign(name, value)
                } catch (e: Exception) {
                    env.define(name, value)
                }
            }

            is Stmt.Block -> {
                // Create a new inner scope, run code, then restore outer scope
                executeBlock(stmt.root, Environment(env))
            }

            is Stmt.If -> {
                if (isTruthy(eval(stmt.condition))) {
                    execute(stmt.thenBranch)
                } else if (stmt.elseBranch != null) {
                    execute(stmt.elseBranch)
                }
            }

            is Stmt.Loop -> {
                while (!isTruthy(eval(stmt.condition))) {
                    execute(stmt.body)
                }
            }

            is Stmt.HybridLoop -> {
                val countValue = eval(stmt.count)
                if (countValue !is Double) {
                    throw RuntimeError(stmt.count.token(), "Loop count must be a number.")
                }
                val count = countValue.toInt()

                for (i in 0 until count) {
                    if (stmt.stopCondition != null) {
                        val conditionResult = eval(stmt.stopCondition)
                        if (isTruthy(conditionResult)) {
                            break
                        }
                    }
                    execute(stmt.body)
                }
            }

            is Stmt.Function -> {
                val function = GenZFunction(stmt, env)
                val funName = stripSigil(stmt.name.lexeme)
                env.define(funName, function)
            }

            is Stmt.Return -> {
                val value = eval(stmt.value)
                throw ReturnValue(value)
            }

            is Stmt.Input -> {
                val promptValue = eval(stmt.prompt)
                print(promptValue)
                val input = readlnOrNull() ?: ""
                val varName = stripSigil(stmt.variable.lexeme)
                env.define(varName, input)
            }

            is Stmt.ArraySet -> {
                val cleanName = stripSigil(stmt.name.lexeme)
                val list = env.get(cleanName)

                val indexVal = eval(stmt.index)
                val newVal = eval(stmt.value)

                if (list !is MutableList<*>) {
                    throw RuntimeError(stmt.name, "Only arrays can be modified by index.")
                }

                if (indexVal !is Double) {
                    throw RuntimeError(stmt.name, "Array index must be a number.")
                }

                val mutableList = list as MutableList<Any?>
                val idx = indexVal.toInt()

                if (idx < 0 || idx >= mutableList.size) {
                    throw RuntimeError(stmt.name, "Index out of bounds.")
                }

                mutableList[idx] = newVal
            }

            else -> throw RuntimeException("Unimplemented statement type: ${stmt::class.simpleName}")
        }
    }

    fun executeBlock(rootStmt: Stmt?, environment: Environment) {
        val previous = this.env // Save outer scope
        try {
            this.env = environment // Enter inner scope
            if (rootStmt != null) {
                execute(rootStmt)
            }
        } finally {
            this.env = previous // Restore outer scope
        }
    }

    private fun stripSigil(lexeme: String): String {
        val clean = lexeme.trim()
        if (clean.startsWith("@") || clean.startsWith("$") || clean.startsWith("%")) {
            return clean.substring(1)
        }
        return clean
    }
}

class RuntimeError(val token: Token, msg: String) : RuntimeException(msg)
