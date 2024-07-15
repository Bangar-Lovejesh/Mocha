package src.craftingInterpreters.mocha;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private ClassType currentClass = ClassType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        this.beginScope();
        this.resolve(stmt.statements);
        this.endScope();
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        this.declare(stmt.name);
        if (null != stmt.initializer) {
            this.resolve(stmt.initializer);
        }
        this.define(stmt.name);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!this.scopes.isEmpty() && this.scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Mocha.error(expr.name, "Can't read local variable in its own initializer.");
        }
        this.resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        this.resolve(expr.value);
        this.resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        this.resolve(expr.left);
        this.resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        this.resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            this.resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        this.resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        this.resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        this.resolve(expr.left);
        this.resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        this.resolve(expr.value);
        this.resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (ClassType.NONE == this.currentClass) {
            Mocha.error(expr.keyword, "Can't use 'super' outside of a class.");
        } else if (ClassType.SUBCLASS != this.currentClass) {
            Mocha.error(expr.keyword, "Can't use 'super' in a class with no subclass.");
        }
        this.resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (ClassType.NONE == this.currentClass) {
            Mocha.error(expr.keyword, "Can't use 'THIS' outside of a class.");
            return null;
        }
        this.resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        this.resolve(expr.right);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        this.declare(stmt.name);
        this.define(stmt.name);
        this.resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        this.resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        this.resolve(stmt.condition);
        this.resolve(stmt.thenBranch);
        if (null != stmt.elseBranch) {
            this.resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        this.resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (FunctionType.NONE == this.currentFunction) {
            Mocha.error(stmt.keyword, "Can't return from top-level code.");
        }
        if (null != stmt.value) {
            if (FunctionType.INITIALIZER == this.currentFunction) {
                Mocha.error(stmt.keyword,
                        "Can't return a value from an initializer.");
            }
            this.resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        this.resolve(stmt.condition);
        this.resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = this.currentClass;
        this.currentClass = ClassType.CLASS;
        this.declare(stmt.name);
        this.define(stmt.name);
        if (null != stmt.superclass && stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
            Mocha.error(stmt.superclass.name, "A class cannot inherit from itself.");
        }
        if (null != stmt.superclass) {
            this.currentClass = ClassType.SUBCLASS;
            this.resolve(stmt.superclass);
        }
        if (null != stmt.superclass) {
            this.beginScope();
            this.scopes.peek().put("super", true);
        }
        this.beginScope();
        this.scopes.peek().put("this", true);
        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;
            if (method.name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }
            this.resolveFunction(method, declaration);
        }
        this.endScope();
        if (null != stmt.superclass) {
            this.endScope();
        }
        this.currentClass = enclosingClass;
        return null;
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = this.currentFunction;
        this.currentFunction = type;
        this.beginScope();
        for (Token param : function.params) {
            this.declare(param);
            this.define(param);
        }
        this.resolve(function.body);
        this.endScope();
        this.currentFunction = enclosingFunction;

    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            this.resolve(statement);
        }

    }

    private void resolve(Stmt statement) {
        statement.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void beginScope() {
        this.scopes.push(new HashMap<>());
    }

    private void endScope() {
        this.scopes.pop();
    }

    private void declare(Token name) {
        if (this.scopes.isEmpty()) return;

        Map<String, Boolean> scope = this.scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Mocha.error(name, "Already variable with this name in this scope");
        }
        scope.put(name.lexeme, false);

    }

    private void define(Token name) {
        if (this.scopes.isEmpty()) return;
        this.scopes.peek().put(name.lexeme, true);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = this.scopes.size() - 1; 0 <= i; i--) {
            if (this.scopes.get(i).containsKey(name.lexeme)) {
                this.interpreter.resolve(expr, this.scopes.size() - 1 - i);
                return;
            }
        }
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }
}
