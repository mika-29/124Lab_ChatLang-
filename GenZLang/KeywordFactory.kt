package GenZLang

object KeywordFactory {
    fun keywords(): Map<String, TokenType> {
        return mapOf(
            "bet" to TokenType.IF,
            "deadass" to TokenType.ELSE,
            "lowkey" to TokenType.FOR,
            "cap" to TokenType.FALSE,
            "fr" to TokenType.TRUE,
            "tea" to TokenType.VAR,
            "ghosted" to TokenType.NULL,
            "summon" to TokenType.FUN,
            "spill" to TokenType.PRINT,
            "slay" to TokenType.RETURN,
            "super" to TokenType.SUPER,
            "dis" to TokenType.THIS,
            "highkey" to TokenType.WHILE,
            "squad" to TokenType.CLASS,
            "cancelledt" to TokenType.BREAK,
            "yeet" to TokenType.CONTINUE,
            "pullup" to TokenType.IMPORT
        )
    }
}