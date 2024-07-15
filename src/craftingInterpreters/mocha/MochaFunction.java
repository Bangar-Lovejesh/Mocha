package src.craftingInterpreters.mocha;

import java.util.List;

class MochaFunction implements MochaCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    MochaFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.closure = closure;
        this.declaration = declaration;
        this.isInitializer = isInitializer;
    }

    @Override
    public int arity() {
        return this.declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter,
                       List<Object> arguments) {
        Environment environment = new Environment(this.closure);
        for (int i = 0; i < this.declaration.params.size(); i++) {
            environment.define(this.declaration.params.get(i).lexeme,
                    arguments.get(i));
        }
        try {
            interpreter.executeBlock(this.declaration.body, environment);
        } catch (Return returnValue) {
            if (this.isInitializer) return this.closure.getAt(0, "this");
            return returnValue.value;
        }
        if (this.isInitializer) return this.closure.getAt(0, "this");
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + this.declaration.name.lexeme + ">";
    }

    MochaFunction bind(MochaInstance instance) {
        Environment environment = new Environment(this.closure);
        environment.define("this", instance);
        return new MochaFunction(this.declaration, environment, this.isInitializer);

    }
}
