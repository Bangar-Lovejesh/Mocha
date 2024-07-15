package src.craftingInterpreters.mocha;

import java.util.Map;
import java.util.HashMap;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();
    final Environment enclosing;

    Environment(){
        this.enclosing = null;
    }
    Environment(Environment enclosing){
        this.enclosing = enclosing;
    }
    void define(String key, Object value) {
        this.values.put(key, value);
    }

    Environment ancestor(int distance){
        Environment environment = this;
        for(int i = 0; i < distance; i++){
            environment = environment.enclosing;
        }
        return environment;
    }

    Object get(Token name) {
        if (this.values.containsKey(name.lexeme)) {
            return this.values.get(name.lexeme);
        }
        if (null != enclosing) {
            return this.enclosing.get(name);
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'");
    }

    void assign(Token name, Object value) {
        if(this.values.containsKey(name.lexeme)) {
            this.values.put(name.lexeme, value);
            return;
        }
        if (null != enclosing) {
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
