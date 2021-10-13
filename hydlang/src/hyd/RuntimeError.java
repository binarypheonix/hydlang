package hyd;

public class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token tok, String msg){
        super(msg);
        this.token = tok;
    }
}
