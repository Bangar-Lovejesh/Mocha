package src.craftingInterpreters.mocha;

import java.util.HashMap;
import java.util.Map;

class MochaInstance {
    private final MochaClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    MochaInstance(MochaClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return this.klass.name + " Instance";
    }

    Object get(Token name) {
        if (this.fields.containsKey(name.lexeme)) {
            return this.fields.get(name.lexeme);
        }

        MochaFunction method = this.klass.findMethod(name.lexeme);
        if (null != method) {
            return method.bind(this);
        }

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        this.fields.put(name.lexeme, value);
    }
}
