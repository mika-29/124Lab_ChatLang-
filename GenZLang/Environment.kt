package GenZLang

class Environment(val enclosing: Environment? = null)  {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(name: String, value: Any?) {
        if (values.containsKey(name)) {
            values[name] = value
            return
        }

        // If not found in this scope, try to update it in the outer scope
        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }

        throw RuntimeException("Undefined variable '$name'.")
    }

    fun get(name: String): Any? {
        if (values.containsKey(name)) {
            return values[name]
        }

        // If not found here, ask the parent environment
        if (enclosing != null) {
            return enclosing.get(name)
        }

        throw RuntimeException("Undefined variable '$name'.")
    }
}