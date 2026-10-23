package tac;

public class Instruccio {

    private final String op;
    private final String desti;
    private final String arg1;
    private final String arg2;

    public Instruccio(String op, String desti, String arg1, String arg2) {
        this.op = op;
        this.desti = desti;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public String getOp() {
        return op;
    }

    public String getDesti() {
        return desti;
    }

    public String getArg1() {
        return arg1;
    }

    public String getArg2() {
        return arg2;
    }

    @Override
    public String toString() {
        if (op == null) {
            return desti + ":";
        }
        if (op.equals("goto")) {
            return "goto " + desti;
        }
        if (op.equals("if!goto")) {
            return "if !" + arg1 + " goto " + desti;
        }
        if (op.equals("return")) {
            if (desti == null) {
                return "return";
            }
            return "return " + desti;
        }
        if (op.equals("param")) {
            return "param " + desti;
        }
        if (op.equals("call")) {
            if (arg2 == null) {
                return "call " + desti + ", " + arg1;
            }
            return desti + " = call " + arg1 + ", " + arg2;
        }
        if (op.equals("=")) {
            return desti + " = " + arg1;
        }
        if (arg2 == null) {
            return desti + " = " + op + " " + arg1;
        }
        return desti + " = " + arg1 + " " + op + " " + arg2;
    }
}
