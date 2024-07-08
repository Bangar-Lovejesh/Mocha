package src.craftingInterpreters.mocha;

import java.util.Map;
import java.util.HashMap;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();
    final Environment enclosing;

    Environment(){
        enclosing = null;
    }
    Environment(Environment enclosing){
        this.enclosing = enclosing;
    }
    void define(String key, Object value) {
        values.put(key, value);
    }

    Environment ancestor(int distance){
        Environment environment = this;
        for(int i = 0; i < distance; i++){
            environment = environment.enclosing;
        }
        return environment;
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        if (enclosing != null) {
            return enclosing.get(name);
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'");
    }

    void assign(Token name, Object value) {
        if(values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'");
    }

    Object getAt(Integer distance, String name) {
        return ancestor(distance).values.get(name);
    }

    void assignAt(Integer distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }
}
