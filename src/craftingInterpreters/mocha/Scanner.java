package src.craftingInterpreters.mocha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static src.craftingInterpreters.mocha.TokenType.*;

public class Scanner {
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("for", FOR);
        keywords.put("if", IF);
        keywords.put("fun", FUN);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("return", RETURN);
        keywords.put("true", TRUE);
        keywords.put("false", FALSE);
        keywords.put("print", PRINT);
        keywords.put("super", SUPER);
        keywords.put("while", WHILE);
        keywords.put("var", VAR);
        keywords.put("this", THIS);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start;
    private int current;
    private int line = 1;


    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!this.isAtEnd()) {
            this.start = this.current;
            this.scanToken();
        }
        this.tokens.add(new Token(EOF, "", null, this.line));
        return this.tokens;
    }

    private void scanToken() {
        char c = this.advance();
        switch (c) {
            case '(':
                this.addToken(LEFT_PAREN);
                break;
            case ')':
                this.addToken(RIGHT_PAREN);
                break;
            case '{':
                this.addToken(LEFT_BRACE);
                break;
            case '}':
                this.addToken(RIGHT_BRACE);
                break;
            case ',':
                this.addToken(COMMA);
                break;
            case '.':
                this.addToken(DOT);
                break;
            case '-':
                this.addToken(MINUS);
                break;
            case ';':
                this.addToken(SEMICOLON);
                break;
            case '+':
                this.addToken(PLUS);
                break;
            case '*':
                this.addToken(STAR);
                break;
            case '!':
                this.addToken(this.match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                this.addToken(this.match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                this.addToken(this.match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                this.addToken(this.match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (this.match('/')) {
                    while ('\n' != this.peek() && !this.isAtEnd()) {
                        this.advance();
                    }

                } else {
                    this.addToken(SLASH);
                }
                break;
            case 'o':
                if ('r' == this.peek()) {
                    this.addToken(OR);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                this.line++;
                break;
            case '"':
                this.string();
                break;

            default:
                if (this.isDigit(c)) {
                    this.number();
                } else if (this.isAlpha(c)) {
                    this.identifier();
                } else {
                    Mocha.error(this.line, "Unexpected character " + c + ".");

                }

        }
    }


    private void identifier() {
        char curr = this.peek();
        while (this.isAlphaNumeric(curr)) {
            this.advance();
            curr = this.peek();
        }
        String text = this.source.substring(this.start, this.current);
        TokenType type = keywords.get(text);
        if (null == type) {
            type = IDENTIFIER;
        }
        this.addToken(type);
    }

    private Boolean isAlpha(char c) {
        return ('a' <= c && 'z' >= c) || ('A' <= c && 'Z' >= c) || '_' == c;
    }

    private Boolean isAlphaNumeric(char c) {
        return this.isAlpha(c) || this.isDigit(c);
    }

    private Boolean isDigit(char c) {
        return '0' <= c && '9' >= c;
    }

    private void number() {
        while (this.isDigit(this.peek())) this.advance();

        if ('.' == this.peek() && this.isDigit(this.peekNext())) {
            do this.advance();
            while (this.isDigit(this.peek()));
        }

        this.addToken(NUMBER, Double.parseDouble(this.source.substring(this.start, this.current)));
    }

    private char peekNext() {
        if (this.current + 1 >= this.source.length()) return '\0';
        return this.source.charAt(this.current + 1);
    }

    private void string() {
        while ('"' != this.peek() && !this.isAtEnd()) {
            if ('\n' == this.peek()) this.line++;
            this.advance();
        }

        if (this.isAtEnd()) {
            Mocha.error(this.line, "Unexpected end of string");
            return;
        }
        this.advance();

        String value = this.source.substring(this.start + 1, this.current - 1);
        this.addToken(STRING, value);
    }

    private char peek() {
        if (this.isAtEnd()) return '\0';
        return this.source.charAt(this.current);
    }

    private Boolean match(char expected) {
        if (this.isAtEnd()) return false;
        if (this.source.charAt(this.current) != expected) return false;
        this.current++;
        return true;
    }

    private char advance() {
        this.current++;
        return this.source.charAt(this.current - 1);
    }

    private void addToken(TokenType type) {
        this.addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = this.source.substring(this.start, this.current);
        this.tokens.add(new Token(type, text, literal, this.line));
    }

    private boolean isAtEnd() {
        return this.current >= this.source.length();
    }


}
