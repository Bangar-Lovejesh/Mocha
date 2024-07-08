package src.craftingInterpreters.mocha;

import java.util.HashMap;
import java.util.Map;

class MochaInstance {
    private MochaClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    MochaInstance(MochaClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return klass.name + " Instance";
    }

    Object get(Token name){
        if(fields.containsKey(name.lexeme)){
            return fields.get(name.lexeme);
        }

        MochaFunction method = klass.findMethod(name.lexeme);
        if(method != null){return method.bind(this);}

        throw new RuntimeError(name , "Undefined property '" + name.lexeme+"'.");
    }

    void set(Token name, Object value){
        fields.put(name.lexeme, value);
    }
}
