package src.craftingInterpreters.mocha;

import java.util.List;

interface MochaCallable {
    Object call(Interpreter interpreter, List<Object> arguments);
}
