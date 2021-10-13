package hyd;

public class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line){
        this.lexeme = lexeme;
        this.type = type;
        this.line = line;
        this.literal = literal;
    }

    public String toString(){
        return "["+type+"]    ["+lexeme+"]   ["+literal +"]";
    }
}
