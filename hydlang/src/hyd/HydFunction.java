package hyd;

import com.sun.tools.doclint.Env;

import java.util.List;

public class HydFunction implements HydCallable{

    private final Stmt.Function declaration;
    private final Environment closure;

    HydFunction(Stmt.Function declaration, Environment environment){
        this.declaration = declaration;
        this.closure = environment;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment env = new Environment(closure);
        for(int i = 0;i < declaration.params.size();i++){
            env.define(declaration.params.get(i).lexeme, args.get(i));
        }
        try{
            interpreter.executeBlock(declaration.body, env);
        }catch (Return ret){
            return ret.value;
        }
        return null;
    }

    @Override
    public String toString(){
        return "<function decl := " + declaration.name.lexeme + "( params :" + this.arity() + ")";
    }
}
