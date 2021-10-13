package hyd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private static class ParseError extends RuntimeException{ }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens){
        this.tokens = tokens;
    }

    List<Stmt> parse(){
       List<Stmt> statements = new ArrayList<>();
       while(!isAtEnd()){
           statements.add(declaration());
       }
       return statements;
    }

    private Stmt declaration(){
        try{
            if(match(TokenType.FUN)) return function("function");
            if(match(TokenType.VAR)) return varDeclaration();
            return statement();
        }catch(ParseError pe){
            synchronize();
            return null;
        }
    }

    private Stmt.Function function(String type){
        Token identity = consume(TokenType.IDENTIFIER, "Expected " + type + "name.");
        consume(TokenType.LEFT_PAREN, "Expected '(' after " + type + "name.");
        List<Token> params = new ArrayList<>();
        if(!check(TokenType.RIGHT_PAREN)){
            do{
                if(params.size() >= 255){
                    error(peek(), "Can't have more than 255 parameters.");
                }

                params.add(consume(TokenType.IDENTIFIER, "Expected Parameter name."));
            }while(match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "')' expected after parameter list.");

        consume(TokenType.LEFT_BRACE, "'{' expected after parameter list.");
        List<Stmt> body = block();
        return new Stmt.Function(identity, params, body);
    }

    private Stmt varDeclaration(){
        Token name = consume(TokenType.IDENTIFIER, " variable name expected.");

        Expr init = null;
        if(match(TokenType.EQUAL)){
            init = expression();
        }
        consume(TokenType.SEMI_COLON, "; expected after statement.");
        return new Stmt.Var(name, init);
    }

    private Stmt statement(){
        if(match(TokenType.FOR)) return forStatement();
        if(match(TokenType.IF)) return ifStatement();
        if(match(TokenType.PRINT)) return printStatement();
        if(match(TokenType.RETURN)) return returnStatement();
        if(match(TokenType.WHILE)) return whileStatement();
        if(match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());

        return exprStatement();
    }

    private Stmt returnStatement(){
        Token keyword = previous();
        Expr value = null;
        if(!check(TokenType.SEMI_COLON)){
            value = expression();
        }

        consume(TokenType.SEMI_COLON, "';' expected after return statement");
        return new Stmt.Return(keyword, value);
    }

    private Stmt forStatement(){
        consume(TokenType.LEFT_PAREN, "'(' expected after for.");

        Stmt init;

        if(match(TokenType.SEMI_COLON)){
            init = null;
        }else if(match(TokenType.VAR)){
            init = varDeclaration();
        }else{
            init = exprStatement();
        }

        Expr condition = null;

        if(!check(TokenType.SEMI_COLON)){
            condition = expression();
        }
        consume(TokenType.SEMI_COLON, "';' expected after condition.");

        Expr inc = null;

        if(!check(TokenType.RIGHT_PAREN)){
            inc = expression();
        }
        consume(TokenType.RIGHT_PAREN, "')' expected after expression.");

        Stmt body = statement();

        if(inc != null){
            body = new Stmt.Block(Arrays.asList(
               body, new Stmt.Expression(inc)));
        }

        if(condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if(init != null){
            body = new Stmt.Block(Arrays.asList(init, body));
        }

        return body;
    }

    private Stmt whileStatement(){
        consume(TokenType.LEFT_PAREN, "'(' expected after while.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "')' expected after condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt ifStatement(){
        consume(TokenType.LEFT_PAREN, "Expected '(' after if.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after condition.");
        Stmt branch = statement();
        Stmt elseBranch = null;

        if(match(TokenType.ELSE)){
            elseBranch = statement();
        }

        return new Stmt.If(condition, branch, elseBranch);
    }

    private List<Stmt> block(){
        List<Stmt> statements = new ArrayList<>();

        while(!check(TokenType.RIGHT_BRACE) && !isAtEnd()){
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, " } expected after block.");
        return statements;
    }

    private Stmt printStatement(){
        Expr value = expression();
        consume(TokenType.SEMI_COLON, ":= ; expected after statement.");
        return new Stmt.Print(value);
    }

    private Stmt exprStatement(){
        Expr value = expression();
        consume(TokenType.SEMI_COLON, ":= ; expected after statement.");
        return new Stmt.Expression(value);
    }

    private Expr expression(){
        return assignment();
    }

    private Expr assignment(){
        Expr expr = or();

        if(match(TokenType.EQUAL)){
            Token equals = previous();
            Expr value = assignment();

            if(expr instanceof Expr.Variable){
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid Assignment.");
        }

        return expr;
    }

    private Expr or(){
        Expr expr = and();

        while(match(TokenType.OR)){
            Token oper = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, oper, right);
        }

        return expr;
    }

    private Expr and(){
        Expr expr = equality();

        while (match(TokenType.AND)){
            Token oper = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, oper, right);
        }

        return expr;
    }

    private Expr equality(){
        Expr expr = comparison();

        while(match(TokenType.NOT_EQUAL, TokenType.EQUAL_EQUAL)){
            Token oper = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, oper, right);
        }
        return expr;
    }

    private Expr comparison(){
        Expr expr = term();

        while(match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)){
            Token oper = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, oper, right);
        }

        return expr;
    }

    private Expr term(){
        Expr expr = factor();

        while(match(TokenType.PLUS, TokenType.MINUS)){
            Token oper = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, oper, right);
        }

        return expr;
    }
    private Expr factor(){
        Expr expr = unary();

        while(match(TokenType.SLASH, TokenType.STAR)){
            Token oper = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, oper, right);
        }

        return expr;
    }

    private Expr unary(){
        if(match(TokenType.NOT, TokenType.MINUS)){
            Token oper = previous();
            Expr right = unary();
            return new Expr.Unary(oper, right);
        }

        return call();
    }

    private Expr call(){
        Expr expr = primary();

        while(true){
            if(match(TokenType.LEFT_PAREN)){
                expr = finishCall(expr);
            }else{
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee){
        List<Expr> args = new ArrayList<>();

        if(!match(TokenType.RIGHT_PAREN)){
            do{
                if(args.size() >= 255){
                    error(peek(), "No. of args must be <= 255.");
                }
                args.add(expression());
            }while(match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "')' expected after function call arguments.");

        return new Expr.Call(callee, paren, args);
    }

    private Expr primary(){
        if(match(TokenType.FALSE)) return new Expr.Literal(false);
        if(match(TokenType.TRUE)) return new Expr.Literal(true);
        if(match(TokenType.NIL)) return new Expr.Literal(null);

        if(match(TokenType.NUMBER, TokenType.STRING)){
            return new Expr.Literal(previous().literal);
        }

        if(match(TokenType.IDENTIFIER)){
            return new Expr.Variable(previous());
        }

        if(match(TokenType.LEFT_PAREN)){
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "expected ')' after expression!");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), ", expression expected!");
    }

    //utility methods:
    private boolean match(TokenType... types){
        for(TokenType token : types){
            if(check(token)){
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType tok){
        if(isAtEnd()) return false;
        return peek().type == tok;
    }

    private Token advance(){
        if(!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd(){
        return peek().type == TokenType.EOF;
    }

    private Token peek(){
        return tokens.get(current);
    }

    private Token previous(){
        return tokens.get(current-1);
    }

    //error handling:
    private Token consume(TokenType type, String msg){
        if(check(type)) return advance();
        throw error(peek(), msg);
    }

    private ParseError error(Token token, String msg){
        Hyd.error(token, msg);
        return new ParseError();
    }
    private void synchronize(){
        advance();

        while(!isAtEnd()){
            if(previous().type==TokenType.SEMI_COLON){
                return;
            }

            switch (peek().type){
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
            advance();
        }
    }

}
