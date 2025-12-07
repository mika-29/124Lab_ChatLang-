package GenZLang

interface GenZCallable {
    fun arity(): Int // How many arguments does it expect?
    fun call(interpreter: Evaluator, arguments: List<Any?>): Any?
}
