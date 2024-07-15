package src.craftingInterpreters.mocha;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static src.craftingInterpreters.mocha.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Expr expression() {
        return this.assignment();
    }

    private Expr assignment() {
        Expr expr = this.or();
        if(this.match(EQUAL)){
            Token equals = this.previous();
            Expr value = this.assignment();

            if (expr instanceof Expr.Variable){
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name,value);
            }
            else if(expr instanceof Expr.Get get) {
                return new Expr.Set(get.object, get.name, value);
            }
            throw this.error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = this.and();
        while(this.match(OR)){
            Token operator = this.previous();
            Expr right = this.and();
            expr = new Expr.Logical(expr,operator,right);
        }
        return expr;
    }
    private Expr and() {
        Expr expr = this.equality();

        while(this.match(AND)){
            Token operator = this.previous();
            Expr right = this.equality();
            expr = new Expr.Logical(expr,operator,right);
        }
        return expr;
    }
    private Stmt statement() {
        if(this.match(FOR)) return this.forStatement();
        if(this.match(IF)) return this.ifStatement();
        if(this.match(PRINT)) return this.printStatement();
        if(this.match(RETURN)) return this.returnStatement();
        if(this.match(WHILE)) return this.whileStatement();
        if(this.match(LEFT_BRACE)) return new Stmt.Block(this.block());
        return this.expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = this.previous();
        Expr value = null;
        if (!this.check(SEMICOLON)){
            value = this.expression();
        }
        this.consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword,value);
    }

    private Stmt forStatement(){
        this.consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if(this.match(SEMICOLON)){
            initializer = null;
        }
        else if (this.match(VAR)){
            initializer = this.varDeclaration();
        }
        else{
            initializer = this.expressionStatement();
        }

        Expr condition = null;
        if(!this.check(SEMICOLON)){
            condition = this.expression();
        }
        this.consume(SEMICOLON, "Expect ';' after the loop condition");

        Expr increment = null;
        if (!this.check(RIGHT_PAREN)){
            increment = this.expression();
        }
        this.consume(RIGHT_PAREN, "Expect ')' after the loop condition");

        Stmt body = this.statement();

        if (null != increment){
            body = new Stmt.Block(
                    Arrays.asList(body, new Stmt.Expression(increment))
            );
        }
        if(null == condition) {
            condition = new Expr.Literal(true);
        }
        body = new Stmt.While(condition, body);

        if(null != initializer){
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        return body;
    }
    private Stmt ifStatement() {
        this.consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = this.expression();
        this.consume(RIGHT_PAREN, "Expect ')' after 'if'.");
        Stmt thenBranch = this.statement();
        Stmt elseBranch = this.statement();
        if(this.match(ELSE)){
            elseBranch = this.statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }
    private Stmt printStatement() {
        Expr value = this.expression();
        this.consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }
    private Stmt whileStatement(){
        this.consume(LEFT_PAREN, "Expect '(' after while.");
        Expr condition = this.expression();
        this.consume(RIGHT_PAREN, "Expect ')' after while.");
        Stmt body = this.statement();

        return new Stmt.While(condition, body);
    }
    private Stmt expressionStatement() {
        Expr expr = this.expression();
        this.consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!this.check(RIGHT_BRACE) && !this.isAtEnd()) {
            statements.add(this.declaration());
        }
        this.consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr equality() {
        Expr expr = this.comparison();
        while (this.match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = this.previous();
            Expr right = this.comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = this.term();
        while (this.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = this.previous();
            Expr right = this.term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = this.factor();

        while (this.match(MINUS,PLUS)){
            Token operator = this.previous();
            Expr right = this.factor();
            expr = new Expr.Binary(expr, operator, right);

        }
        return expr;
    }

    private Stmt varDeclaration() {
        Token name = this.consume(IDENTIFIER, "Except Variable name.");
        Expr initializer = null;
        if(this.match(EQUAL)){
            initializer = this.expression();
        }

        this.consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }


    private boolean match(TokenType... types) {
        for (TokenType type: types) {
            if (this.check(type)){
                this.advance();
                return true;
            }

        }
        return false;
    }

    private Expr factor() {
        Expr expr = this.unary();
        while (this.match(SLASH, STAR)) {
            Token operator = this.previous();
            Expr right = this.unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (this.match(BANG, MINUS)) {
            Token operator = this.previous();
            Expr right = this.unary();
            return new Expr.Unary(operator, right);
        }
        return this.call();
    }

    private Expr call() {
        Expr expr = this.primary();
        while (true) {
            if (this.match(LEFT_PAREN)) {
                expr = this.finishCall(expr);
            } else if (this.match(DOT)){
                Token name = this.consume(IDENTIFIER,
                        "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            }else {
                break;}
        }
        return expr;
    }
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!this.check(RIGHT_PAREN)) {
            do {
                if (255 <= arguments.size()) {
                    throw this.error(this.peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(this.expression());
            } while (this.match(COMMA));
        }
        Token paren = this.consume(RIGHT_PAREN,
                "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (this.match(FALSE)) return new Expr.Literal(false);
        if (this.match(TRUE)) return new Expr.Literal(true);
        if (this.match(NIL)) return new Expr.Literal(null);
        if (this.match(NUMBER, STRING)) {
            return new Expr.Literal(this.previous().literal);}
        if (this.match(THIS)) return new Expr.This(this.previous());
        if (this.match(SUPER)){
            Token keyword = this.previous();
            this.consume(DOT, "Expect '.' after super.");
            Token method = this.consume(IDENTIFIER, "Expect superclass method name.");
            return new Expr.Super(keyword,method);
        }
        if (this.match(IDENTIFIER)){
            return new Expr.Variable(this.previous());
        }
        if (this.match(LEFT_PAREN)) {
            Expr expr = this.expression();
            this.consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw this.error(this.peek(), "Expect expression.");
    }


    private boolean check(TokenType type) {
        if (this.isAtEnd()) return false;
        return this.peek().type == type;
    }
    private Token consume(TokenType type, String message) {
        if (this.check(type)) return this.advance();
        throw this.error(this.peek(), message);
    }


    private ParseError error(Token token, String message) {
        Mocha.error(token, message);
        return new ParseError();
    }



    List<Stmt> parse(){
        List<Stmt> statements = new ArrayList<>();
        while (!this.isAtEnd()){
            statements.add(this.declaration());
        }

        return statements;
    }

    private Token advance() {
        if (!this.isAtEnd()) this.current++;
        return this.previous();
    }

    private Stmt declaration() {
        try {
            if (this.match(CLASS)) return this.classDeclaration();
            if (this.match(FUN)) return this.function("function");
            if (this.match(VAR)) return this.varDeclaration();
            return this.statement();
        }catch(ParseError error){
            this.synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = this.consume(IDENTIFIER, "Expect class name.");
        Expr.Variable superclass = null;
        if(this.match(LESS)){
            this.consume(IDENTIFIER, "Expect superclass name.");
            superclass = new Expr.Variable(this.previous());
        }
        this.consume(LEFT_BRACE, "Expect '{' before class body.");
        List<Stmt.Function> methods = new ArrayList<>();
        while (!this.check(RIGHT_BRACE) && !this.isAtEnd()) {
            methods.add(this.function("method"));
        }
        this.consume(RIGHT_BRACE, "Expect '}' after class body.");
        return new Stmt.Class(name,superclass, methods);
    }

    private Stmt.Function function(String kind) {
        Token name = this.consume(IDENTIFIER, "Expect " + kind + " name.");
        this.consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!this.check(RIGHT_PAREN)) {
            do {
                if (255 <= parameters.size()) {
                    throw this.error(this.peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(
                        this.consume(IDENTIFIER, "Expect parameter name."));
            } while (this.match(COMMA));
        }
        this.consume(RIGHT_PAREN, "Expect ')' after parameters.");
        this.consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = this.block();
        return new Stmt.Function(name, parameters, body);
    }

    private boolean isAtEnd() {
        return EOF == this.peek().type;
    }
    private Token peek() {
        return this.tokens.get(this.current);
    }
    private Token previous() {
        return this.tokens.get(this.current - 1);
    }

    private void synchronize() {
        this.advance();
        while (!this.isAtEnd()) {
            if (SEMICOLON == this.previous().type) return;
            switch (this.peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            this.advance();
        }
    }
}
