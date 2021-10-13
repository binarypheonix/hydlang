package hyd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

enum TokenType{
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, SEMI_COLON, PLUS, MINUS, SLASH, STAR,

    IDENTIFIER, STRING, NUMBER,

    NOT, NOT_EQUAL, EQUAL, EQUAL_EQUAL,
    GREATER_EQUAL, LESS_EQUAL, GREATER, LESS,

    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    EOF
    //TODO: more on this later
}

public class Hyd {
    static Boolean hadError = false;
    static Boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException{
        if(args.length > 1){
            System.out.println("USGAE: hyd <script>");
            System.exit(64);
        }else if(args.length==1){
            runScript("/Users/frankenstein/IdeaProjects/hydlang/src/test/hi_func.hyd");
        }else{
            //runPrompt();
            runScript("/Users/frankenstein/IdeaProjects/hydlang/src/test/fibo_rec.hyd");
        }
    }

    //for running hyd-scripts
    private static void runScript(String path) throws IOException{
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if(hadError){
            System.exit(65);
        }
        if(hadRuntimeError){
            System.exit(70);
        }
    }

    //for when the user runs hyd interactively:
    private static void runPrompt() throws IOException{
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for(;;){
            System.out.print("> ");
            String line = reader.readLine();
            if(line==null) break;
            run(line);
            hadError = false;
        }
    }

    //the actual func initialising the scanning process for hyd-scripts:
    private static void run(String src){
        Scanner scanner = new Scanner(src);
        ArrayList<Token> tokens = scanner.scan();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        if(hadError) return;

        interpreter.interpret(statements);
    }

    static void error(int line, String msg){
        report(line, msg);
    }

    private static void report(int line, String msg){
        System.err.println("\nLine: " + line + " |\tError: "+  msg);
        hadError = true;
    }

    static void error(Token token, String msg){
        if(token.type==TokenType.EOF){
            report(token.line, " at end"+msg);
        }else{
            report(token.line, " at '" + token.lexeme + "'"+ msg);
        }
    }

    static void runtimeError(RuntimeError err){
        System.out.println(err.getMessage() + "\n[Line: " + err.token.line + "]");
        hadRuntimeError = true;
    }
}
