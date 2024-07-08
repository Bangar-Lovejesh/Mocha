package src.craftingInterpreters.mocha;

import java.util.List;
import java.util.Map;

class MochaClass implements MochaCallable{
    final String name;
    private final Map<String, MochaFunction> methods;

    MochaClass(String name, Map<String, MochaFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        MochaFunction initializer = findMethod("init");
        if(initializer == null) return 0;
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        MochaInstance instance = new MochaInstance(this);
        MochaFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    public MochaFunction findMethod(String name) {
        if(methods.containsKey(name)){
            return methods.get(name);
        }
        return null;
    }
}
