package hyd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Scanner {
    private final String source;
    private final ArrayList<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current =0;
    private int line = 1;

    private static final Map<String, TokenType> reserved = new HashMap<>();
    static {
        reserved.put("and",     TokenType.AND);
        reserved.put("class",   TokenType.CLASS);
        reserved.put("else",    TokenType.ELSE);
        reserved.put("if",      TokenType.IF);
        reserved.put("false",   TokenType.FALSE);
        reserved.put("or",      TokenType.OR);
        reserved.put("fun",     TokenType.FUN);
        reserved.put("for",     TokenType.FOR);
        reserved.put("nil",     TokenType.NIL);
        reserved.put("print",   TokenType.PRINT);
        reserved.put("return",  TokenType.RETURN);
        reserved.put("super",   TokenType.SUPER);
        reserved.put("this",    TokenType.THIS);
        reserved.put("true",    TokenType.TRUE);
        reserved.put("var",     TokenType.VAR);
        reserved.put("while",   TokenType.WHILE);
    }

    Scanner(String source){
        this.source = source;
    }

    ArrayList<Token> scan(){
        while(!isAtEnd()){
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));

        return tokens;
    }

    private void scanToken(){
        char c = advance();
        switch(c){
            case '(': addToken(TokenType.LEFT_PAREN);break;
            case ')': addToken(TokenType.RIGHT_PAREN);break;
            case '{': addToken(TokenType.LEFT_BRACE);break;
            case '}': addToken(TokenType.RIGHT_BRACE);break;
            case '+': addToken(TokenType.PLUS);break;
            case '-': addToken(TokenType.MINUS);break;
            case '/':
                if(match('/')){
                    while(peek() != '\n' && !isAtEnd()) advance();
                }else {
                    addToken(TokenType.SLASH);
                    break;
                }
            case '*': addToken(TokenType.STAR);break;
            case '.': addToken(TokenType.DOT);break;
            case ',': addToken(TokenType.COMMA);break;
            case ';': addToken(TokenType.SEMI_COLON);break;
            case '!': addToken(match('=') ? TokenType.NOT_EQUAL : TokenType.NOT); break;
            case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
            case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
            case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
            case '"': string(); break;
            case ' ':
            case '\r':
            case '\t': break;
            case '\n': line++; break;
            default:
                if(isDigit(c)){
                    number();
                }else if(isAlpha(c)){
                    identifier();
                }else {
                    Hyd.error(line, "Unknown Character.");
                }
        }
    }

    private void identifier(){
        while(isAlphaNumeric(peek())) advance();

        String val = source.substring(start, current);
        TokenType type = reserved.get(val);

        if(type==null) {
            type = TokenType.IDENTIFIER;
        }

        addToken(type);

    }

    private boolean isAlpha(char c){
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'z') ||
                (c=='_');
    }

    private boolean isAlphaNumeric(char c){
        return isAlpha(c) || isDigit(c);
    }

    private void number(){
        while(isDigit(peek())){
            advance();
        }

        if(peek()=='.' && isDigit(peekNext())){
            advance();
            while(isDigit(peek())) advance();
        }

        Double val = Double.parseDouble(source.substring(start, current));
        addToken(TokenType.NUMBER, val);
    }

    private char peekNext(){
        if(current+1 >= source.length()) return '\0';
        return source.charAt(current+1);
    }

    private boolean isDigit(char c){
        return c >= '0' && c <= '9';
    }

    private void string(){
        while(peek() != '"' && !isAtEnd()){
            if(peek()=='\n') line++;
            advance();
        }

        if(isAtEnd()){
            Hyd.error(line, "String not Terminated.");
            return;
        }

        advance();

        String val = source.substring(start+1, current-1);
        addToken(TokenType.STRING,val);

    }

    private char peek(){
        if(isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private boolean match(char c){
        if(isAtEnd()) return false;
        if(source.charAt(current) != c) return false;

        current++;
        return true;
    }

    private void addToken(TokenType t){
        addToken(t, null);
    }
    private void addToken(TokenType type, Object literal){
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private char advance(){
        return source.charAt(current++);
    }


    private boolean isAtEnd(){
        return current >= source.length();
    }
}
