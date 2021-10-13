package hyd;

import java.util.List;

interface HydCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> args);
    public String toString();

}
