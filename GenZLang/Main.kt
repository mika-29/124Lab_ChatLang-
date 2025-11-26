package GenZLang

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    // Check if the user passed a file path argument
    if (args.size > 1) {
        println("Usage: GenZLang [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        // SCRIPT MODE: Run the file provided in args[0]
        runFile(args[0])
    } else {
        // REPL MODE: Run interactively (Your original code)
        runPrompt()
    }
}

// NEW: Function to read a file and run it
fun runFile(path: String) {
    val file = File(path)
    if (!file.exists()) {
        println("Error: File '$path' not found.")
        return
    }

    // Read the whole file into a String
    val script = file.readText()

    // Create an evaluator (Maintains state for the script duration)
    val evaluator = Evaluator()

    // Execute
    run(script, evaluator)
}

// MOVED: Your original interactive loop
fun runPrompt() {
    val evaluator = Evaluator()
    println("GenZ Lang REPL (Type empty line to quit)")

    while (true) {
        print("> ")
        val line = readlnOrNull() ?: break
        if (line.isBlank()) break // Exit on empty line

        // Execute the single line
        run(line, evaluator)
    }
}

// SHARED: The logic that actually processes code
fun run(source: String, evaluator: Evaluator) {
    val scanner = Scanner(source)
    val tokens = scanner.scanTokens()

    val parser = Parser(tokens)

    try {
        val statements = parser.parseProgram()

        // This triggers your Scoping/Assignment/Printing logic
        evaluator.executeProgram(statements)

    } catch (e: ParseError) {
        println("[line 1] Parse Error: ${e.message}")
    } catch (e: RuntimeError) {
        println("[line ${e.token.line}] Runtime Error: ${e.message}")
    }
}