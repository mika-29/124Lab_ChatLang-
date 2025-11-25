package GenZLang

// [Requirement] Support nested scopes by accepting an 'enclosing' (parent) environment
class Environment(val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Any?>()

    // Define always sets the variable in the *current* scope (shadowing) [cite: 149]
    fun define(name: String, value: Any?) {
        values[name] = value
    }

    // Assign updates the variable. If not in current, check parent. [cite: 151]
    fun assign(name: String, value: Any?) {
        if (values.containsKey(name)) {
            values[name] = value
            return
        }

        // If we have a parent, ask the parent to assign it
        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }

        throw RuntimeException("Undefined variable '$name'")
    }

    // Get looks for the variable. If not in current, check parent. [cite: 150]
    fun get(name: String): Any? {
        if (values.containsKey(name)) {
            return values[name]
        }

        // If we have a parent, ask the parent to find it
        if (enclosing != null) {
            return enclosing.get(name)
        }

        throw RuntimeException("Undefined variable '$name'")
    }
}