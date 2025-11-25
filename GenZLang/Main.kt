package GenZLang

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size == 1) {

        val evaluator = Evaluator(isRepl = false)
        runFile(args[0], evaluator)
    } else {
        val evaluator = Evaluator(isRepl = true)
        runPrompt(evaluator)
    }
}

fun runFile(path: String, evaluator: Evaluator) {
    val file = File(path)
    if (!file.exists()) {
        println("Error: File '$path' not found.")
        return
    }

    // Read the whole file as a single string
    val script = file.readText()
    run(script, evaluator)
}

fun runPrompt(evaluator: Evaluator) {
    while (true) {
        print("> ")
        val line = readlnOrNull() ?: break
        if (line.isBlank()) continue
        run(line, evaluator)
    }
}

fun run(source: String, evaluator: Evaluator) {
    val scanner = Scanner(source)
    val tokens = scanner.scanTokens()
    val parser = Parser(tokens)

    try {
        val statements = parser.parseProgram()
        evaluator.executeProgram(statements)
    } catch (e: ParseError) {
        println("[line ${e.token.line}] Parse Error: ${e.message}")
    } catch (e: RuntimeException) {
        println("Runtime Error: ${e.message}")
    }
}