package hyd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

    final Environment globals = new Environment();
    private Environment env = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter(){
        globals.define("clock", new HydCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                return (double)System.currentTimeMillis()/1000.0;
            }
            @Override
            public String toString(){
                return "<native function>";
            }
        });
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.oper.type){
            case GREATER:
                checkNumberOperands(expr.oper, left, right);
                return (double)left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.oper, left, right);
                return (double)left >= (double) right;
            case LESS:
                checkNumberOperands(expr.oper, left, right);
                return (double)left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.oper, left, right);
                return (double)left <= (double) right;
            case NOT_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.oper, left, right);
                return (double)left - (double) right;
            case SLASH:
                checkNumberOperands(expr.oper, left, right);
                return (double)left / (double) right;
            case STAR:
                checkNumberOperands(expr.oper, left, right);
                return (double)left * (double) right;
            case PLUS:
                if(left instanceof Double && right instanceof Double){
                    return (double)left + (double) right;
                }
                if(left instanceof String && right instanceof String){
                    return (String)left + (String) right;
                }

                throw new RuntimeError(expr.oper, "Operands must be of the same type (double or string).");
        }

        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> args = new ArrayList<>();

        for(Expr arg : expr.arguments){
            args.add(evaluate(arg));
        }

        if(!(callee instanceof HydCallable)){
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        HydCallable function = (HydCallable)callee;

        if(args.size() != function.arity()){
            throw new RuntimeError(expr.paren, "Expected "+ function.arity() + "args but got "+
                    args.size() + ".");
        }

        return function.call(this, args);
    }

    private void checkNumberOperands(Token oper, Object left, Object right){
        if(left instanceof Double && right instanceof Double){
            return;
        }
        throw new RuntimeError(oper, "Operands must be numbers.");
    }

    private void checkNumberOperand(Token oper, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(oper, "Operand must be a number.");
    }

    private boolean isEqual(Object left, Object right){
        if(left == null && right == null) return true;
        if(left == null) return false;

        return left.equals(right);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr){
        return expr.accept(this);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if(expr.oper.type == TokenType.OR){
            if(isTheTruth(left)) return left;
        }else{
            if(!isTheTruth(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.oper.type) {
            case NOT_EQUAL:
                return !(isTheTruth(right));
            case MINUS:
                checkNumberOperand(expr.oper, right);
                return -(double) right;
        }

        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {

        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr){
        Integer dist = locals.get(expr);
        if(dist != null){
            return env.getAt(dist, name.lexeme);
        }else{
            return globals.get(name);
        }
    }

    private boolean isTheTruth(Object obj) {
        if(obj == null) return false;
        if(obj instanceof Boolean) return (boolean) obj;
        return true;
    }

    void interpret(List<Stmt> statements){
        try{
            for(Stmt stmt : statements){
                execute(stmt);
            }
        }catch (RuntimeError r){
            Hyd.runtimeError(r);
        }
    }

    private void execute(Stmt statement){
        statement.accept(this);
    }

    void resolve(Expr expr, int depth){
        locals.put(expr, depth);
    }

    private String Stringify(Object obj){
        if(obj == null) return "nil";

        if(obj instanceof Double){
            String text = obj.toString();

            if(text.endsWith(".0")){
                text = text.substring(0, text.length()-2);
            }
            return text;
        }

        return obj.toString();
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(env));
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment env){
        Environment previous = this.env;

        try{
            this.env = env;

            for(Stmt stmt : statements){
                execute(stmt);

            }
        }finally {
            this.env = previous;
        }
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        //define a hydfunc instance capturing the env present at the time
        //of declaration of the function.
        HydFunction func = new HydFunction(stmt, env);
        env.define(stmt.name.lexeme, func);

        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if(isTheTruth(evaluate(stmt.condition))){
            execute(stmt.thenBranch);
        }else if(stmt.elseBranch!= null){
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(Stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if(stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if(stmt.initializer != null){
            value = evaluate(stmt.initializer);
        }
        env.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while(isTheTruth(evaluate(stmt.condition))){
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        Integer dist = locals.get(expr);
        if(dist != null){
            env.assignAt(dist, expr.name, value);
        }else{
            globals.assign(expr.name, value);
        }
        return value;
    }
}