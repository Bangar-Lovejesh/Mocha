package src.craftingInterpreters.mocha;

import static src.craftingInterpreters.mocha.TokenType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;

    private int line = 1;
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
        keywords.put("super",SUPER);
        keywords.put("while", WHILE);
        keywords.put("var", VAR);
        keywords.put("this",THIS);
    }





    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()){
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null,line));
        return tokens;
    }

    private void scanToken(){
        char c = advance();
        switch (c){
            case '(': addToken(LEFT_PAREN);break;
            case ')': addToken(RIGHT_PAREN);break;
            case '{': addToken(LEFT_BRACE);break;
            case '}': addToken(RIGHT_BRACE);break;
            case ',': addToken(COMMA);break;
            case '.': addToken(DOT);break;
            case '-': addToken(MINUS);break;
            case ';': addToken(SEMICOLON);break;
            case '+': addToken(PLUS);break;
            case '*': addToken(STAR);break;
            case '!' :
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')){
                    while(peek() != '\n' && !isAtEnd()){
                        advance();
                    }

            }
                else{
                    addToken(SLASH);
                }
                break;
            case 'o':
                if(peek() == 'r'){
                     addToken(OR);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line ++;
                break;
            case '"':string();break;

            default:
                if (isDigit(c)){
                    number();
                }
                else if(isAlpha(c)){
                    identifier();
                }
                else{
                    Mocha.error(line, "Unexpected character " +c+ ".");

                }

        }
    }


    private void identifier(){
        char curr = peek();
        while (isAlphaNumeric(curr)) {
            advance();
            curr = peek();
        }
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null){
            type = IDENTIFIER;
        }
        addToken(type);
    }

    private Boolean isAlpha(char c){
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private Boolean isAlphaNumeric(char c){
        return isAlpha(c) || isDigit(c);
    }

    private Boolean isDigit(char c){
        return c >= '0' && c <= '9';
    }

    private void number(){
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())){
            advance();
            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private char peekNext(){
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private void string(){
        while (peek() != '"' && !isAtEnd()){
            if(peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()){
            Mocha.error(line, "Unexpected end of string");
            return ;
        }
        advance();

        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private char peek(){
        if(isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private Boolean match(char expected){
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char advance(){
        current++;
        return source.charAt(current-1);
    }

    private void addToken(TokenType type){
        addToken(type,null);
    }

    private void addToken(TokenType type, Object literal){
        String text = source.substring(start,current);
        tokens.add(new Token(type,text,literal,line));
    }

    private boolean isAtEnd(){
        return current >= source.length();
    }


}