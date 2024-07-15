package src.craftingInterpreters.mocha;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String key, Object value) {
        this.values.put(key, value);
    }

    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            assert null != environment;
            environment = environment.enclosing;
        }
        return environment;
    }

    Object get(Token name) {
        if (this.values.containsKey(name.lexeme)) {
            return this.values.get(name.lexeme);
        }
        if (null != this.enclosing) {
            return this.enclosing.get(name);
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'");
    }

    void assign(Token name, Object value) {
        if (this.values.containsKey(name.lexeme)) {
            this.values.put(name.lexeme, value);
            return;
        }
        if (null != this.enclosing) {
            this.enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'");
    }

    Object getAt(Integer distance, String name) {
        return this.ancestor(distance).values.get(name);
    }

    void assignAt(Integer distance, Token name, Object value) {
        this.ancestor(distance).values.put(name.lexeme, value);
    }
}
