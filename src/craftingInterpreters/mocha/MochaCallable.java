package src.craftingInterpreters.mocha;

import java.util.List;

interface MochaCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
