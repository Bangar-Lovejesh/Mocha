package src.craftingInterpreters.mocha;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Object> {
    final Environment globals = new Environment();
    private Environment environment = this.globals;
    private final Map<Expr, Integer> locals  = new HashMap<>();

    Interpreter() {
        this.globals.define("clock", new MochaCallable() {
            @Override
            public int arity() { return 0; }
            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return System.currentTimeMillis() / 1000.0;
            }
            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return this.evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = this.evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !this.isTruthy(right);
            case MINUS:
                this.checkNumberOperand(expr.operator, right);
                return -(double) right;

        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return this.lookUpVariable(expr.name, expr);
    }

    public Object lookUpVariable(Token name, Expr expr) {
        Integer distance = this.locals.get(expr);
        if (null != distance) {
            return this.environment.getAt(distance, name.lexeme);
        }
        else{
            return this.globals.get(name);
        }
    }

    private boolean isTruthy(Object object) {
        if (null == object) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = this.evaluate(expr.left);
        Object right = this.evaluate(expr.right);
        switch (expr.operator.type) {
            case GREATER:
                this.checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                this.checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                this.checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                this.checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case BANG_EQUAL:
                return !this.isEqual(left, right);
            case EQUAL_EQUAL:
                return this.isEqual(left, right);
            case MINUS:
                this.checkNumberOperands(expr.operator, left, right);

                return (double) left - (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                } else if (left instanceof String && right instanceof String) {
                    return left + (String) right;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case SLASH:
                return (double) left / (double) right;
            case STAR:
                return (double) left * (double) right;
        }
        return null;
    }


    private Boolean isEqual(Object left, Object right) {
        if (null == left && null == right) return true;
        if (null == left) return false;
        return left.equals(right);
    }

    public void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand Must Be A Number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operand Must Be A Numbers.");
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                this.execute(statement);
            }
        } catch (RuntimeError error) {
            Mocha.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private String stringify(Object object) {
        if (null == object) return "null";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                return text.substring(0, text.length() - 2);
            }
        }
        return object.toString();
    }

    void executeBlock(List<Stmt> statements,
                      Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                this.execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        this.executeBlock(stmt.statements, new Environment(this.environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;
        if(null != stmt.superclass){
            superclass = this.evaluate(stmt.superclass);
            if(!(superclass instanceof MochaClass)){
                throw new RuntimeError(stmt.superclass.name, "Superclass must be a class");
            }
        }
        this.environment.define(stmt.name.lexeme, null);
        if(null != stmt.superclass){
            this.environment =  new Environment(this.environment);
            this.environment.define("super",superclass);
        }
        Map<String, MochaFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            MochaFunction function = new MochaFunction(method, this.environment,method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }
        MochaClass klass = new MochaClass(stmt.name.lexeme,(MochaClass)superclass, methods);
        if(null != superclass){
            this.environment = this.environment.enclosing;
        }
        this.environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        this.evaluate(stmt.expression);
        return null;
    }

    @Override
    public Object visitFunctionStmt(Stmt.Function stmt) {
        MochaFunction function = new MochaFunction(stmt, this.environment,false);
        this.environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = this.evaluate(stmt.expression);
        System.out.println(this.stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (null != stmt.value) value = this.evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (null != stmt.initializer) {
            value = this.evaluate(stmt.initializer);
            this.environment.define(stmt.name.lexeme, value);
        }
        this.environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = this.evaluate(expr.value);
        Integer distance = this.locals.get(expr);
        if(null != distance){
            this.environment.assignAt(distance, expr.name, value);
        }else{
            this.globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (this.isTruthy(this.evaluate(stmt.condition))) {
            this.execute(stmt.thenBranch);
        } else if (null != stmt.elseBranch) {
            this.execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = this.evaluate(expr.left);
        if (TokenType.OR == expr.operator.type) {
            if (this.isTruthy(left)) return left;
        } else {
            if (!this.isTruthy(left)) return left;
        }
        return this.evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = this.evaluate(expr.object);

        if(!(object instanceof MochaInstance)){
            throw new RuntimeError(expr.name, "Only instances have fields.");

        }

        Object value = this.evaluate(expr.value);
        ((MochaInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = this.locals.get(expr);
        MochaClass superclass = (MochaClass) this.environment.getAt(distance, "super");
        MochaInstance object = (MochaInstance) this.environment.getAt(distance-1,"this");
        MochaFunction method = superclass.findMethod(expr.method.lexeme);
        if(null == method){
            throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
        }
        return method.bind(object);

    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return this.lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (this.isTruthy(this.evaluate(stmt.condition))) {
            this.execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = this.evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(this.evaluate(argument));
        }
        if (!(callee instanceof MochaCallable function)) {
            throw new RuntimeError(expr.paren,
                    "Can only call functions and classes.");
        }
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }
        return function.call(this, arguments);

    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = this.evaluate(expr.object);
        if(object instanceof MochaInstance){
            return ((MochaInstance) object).get(expr.name);
        }
        throw new RuntimeError(expr.name, "Only instances have property");
    }

    void resolve(Expr expr, int depth){
        this.locals.put(expr, depth);
    }
}