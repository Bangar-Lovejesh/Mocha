package src.craftingInterpreters.mocha;

import java.util.List;
import java.util.Map;

class MochaClass implements MochaCallable {
    final String name;
    final MochaClass superclass;
    private final Map<String, MochaFunction> methods;

    MochaClass(String name, MochaClass superclass, Map<String, MochaFunction> methods) {
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int arity() {
        MochaFunction initializer = this.findMethod("init");
        if (null == initializer) return 0;
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        MochaInstance instance = new MochaInstance(this);
        MochaFunction initializer = this.findMethod("init");
        if (null != initializer) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    public MochaFunction findMethod(String name) {
        if (this.methods.containsKey(name)) {
            return this.methods.get(name);
        }
        if (null != this.superclass) {
            return this.superclass.findMethod(name);
        }
        return null;
    }
}
