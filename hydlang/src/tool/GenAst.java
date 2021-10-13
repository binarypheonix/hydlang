package tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

//TODO: FIX THIS!!!! (ALONG WITH ANY ISSUES IN AstPrinter;

public class GenAst {
    public static void main(String[] args) throws IOException {
        /*if(args.length != 1){
            System.err.println("USAGE: generate_ast <output_dir>");
            System.exit(64);
        }else{*/
            String output_dir = "/Users/frankenstein/IdeaProjects/hydlang/src/hyd";
            defineAst(output_dir, "Expr", Arrays.asList(
                    "Assign : Token name, Expr value",
                    "Binary : Expr left, Token oper, Expr right",
                    "Call : Expr callee, Token paren, List<Expr> arguments",
                    "Grouping : Expr expression",
                    "Literal : Object value",
                    "Logical : Expr left, Token oper, Expr right",
                    "Unary : Token oper, Expr right",
                    "Variable : Token name"
            ));

            defineAst(output_dir, "Stmt", Arrays.asList(
                    "Block : List<Stmt> statements",
                    "Expression : Expr expression",
                    "Function : Token name, List<Token> params, List<Stmt> body",
                    "If : Expr condition, Stmt thenBranch, Stmt elseBranch",
                    "Print : Expr expression",
                    "Return : Token keyword, Expr value",
                    "Var : Token name, Expr initializer",
                    "While : Expr condition, Stmt body"
            ));
        //}
    }
    private static void defineAst(String output_dir, String baseName, List<String> types) throws IOException{
        String path = output_dir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");
        writer.println("package hyd;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + "{");
        defineVisitor(writer, baseName, types);
        for(String type : types){
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineTypes(writer, baseName, className, fields);
        }
        writer.println();
        writer.println(" abstract <R> R accept(Visitor<R> visitor);" );

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types){
        writer.println("    interface Visitor<R> {");
        for(String t : types){
            String name = t.split(" : ")[0].trim();
            writer.println("    R visit" + name + baseName + "(" + name + " " + baseName.toLowerCase() + ");");
        }

        writer.println(" }");
    }

    private static void defineTypes(PrintWriter writer, String baseName, String className, String fields){
        String[] field = fields.split(", ");
        writer.println("static class " + className + " extends " + baseName + "{");

        //field definitions;
        for(String f : field){
            writer.println("    final " + f + ";");
        }

        //constructors for fields in the subclasses;
        writer.println("    " + className + "(" + fields + ") {");
        for(String f : field) {
            String name = f.split(" ")[1];
            writer.println("    this." + name + "= " + name + ";");
        }

        writer.println("    }");

        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor){");
        writer.println("        return visitor.visit"+className+baseName+"(this);");
        writer.println("    }");
        writer.println("}");
    }
}
