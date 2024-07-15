package src.craftingInterpreters.mocha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
//import java.util.Scanner;


public class Mocha {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError;
    static boolean hadRuntimeError;

    public static void main(String[] args) throws IOException {
        if (1 < args.length) {
            System.out.println("Usage: mocha [script]");
            System.exit(64);
        } else if (1 == args.length) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) System.exit(64);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        for (; ; ) {
            System.out.print("> ");
            String line = reader.readLine();
            if (null == line) break;
            run(line);
            hadError = false;
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }


    private static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (TokenType.EOF == token.type) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
// Stop if there was a syntax error.
        if (hadError) return;
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        if (hadError) return;
        interpreter.interpret(statements);
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}