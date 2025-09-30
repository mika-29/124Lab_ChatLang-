package GenZLang

object LexScanner {
    fun start() {
        while (true){
            print("> ")
            val line = readLine() ?: break
            val scanner = Scanner(line)
            val tokens = scanner.scanTokens()
            tokens.forEach { println(it) }
        }
    }
}

fun main() {
    LexScanner.start()
}