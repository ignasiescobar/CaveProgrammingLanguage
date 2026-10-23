package tac;

import parser.GrammaticalTree;
import parser.Node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class TAC {

    private final List<Instruccio> instruccions = new ArrayList<>();
    private int numTemp = 0;
    private int numEtiqueta = 0;
    private final Deque<String> etiquetesBreak = new ArrayDeque<>();
    private final Deque<String> etiquetesContinue = new ArrayDeque<>();

    public void generarTAC(GrammaticalTree tree) {
        if (tree == null || tree.getRoot() == null) {
            return;
        }
        for (Node fil : tree.getRoot().getChildren()) {
            if ("DECL".equals(fil.getSymbol())) {
                generarDecl(fil);
            } else if ("FUNCIO".equals(fil.getSymbol())) {
                generarFuncio(fil);
            }
        }
    }

    private void generarFuncio(Node func) {
        String nom = "";
        Node bloc = null;
        for (Node c : func.getChildren()) {
            if ("FUNCIO_ID".equals(c.getSymbol()) && !c.getChildren().isEmpty()) {
                nom = c.getChildren().get(0).getSymbol();
            }
            if ("BLOC".equals(c.getSymbol())) {
                bloc = c;
            }
        }
        afegirInstruccio(null, nom, null, null);
        if (bloc != null) {
            for (Node s : bloc.getChildren()) {
                generarSentencia(s);
            }
        }
    }

    private void generarSentencia(Node n) {
        String s = n.getSymbol();
        if ("DECL".equals(s)) {
            generarDecl(n);
        } else if ("ID_STMT".equals(s)) {
            generarId(n);
        } else if ("IF".equals(s)) {
            generarIf(n);
        } else if ("WHILE".equals(s)) {
            generarWhile(n);
        } else if ("RETURN_STMT".equals(s)) {
            generarReturn(n);
        } else if ("BREAK".equals(s)) {
            if (!etiquetesBreak.isEmpty()) {
                afegirInstruccio("goto", etiquetesBreak.peek(), null, null);
            }
        } else if ("CONTINUE".equals(s)) {
            if (!etiquetesContinue.isEmpty()) {
                afegirInstruccio("goto", etiquetesContinue.peek(), null, null);
            }
        } else if ("BLOC".equals(s)) {
            for (Node c : n.getChildren()) {
                generarSentencia(c);
            }
        }
    }

    private void generarDecl(Node n) {
        List<Node> c = n.getChildren();
        if (c.isEmpty()) {
            return;
        }
        String nom;
        Node expr = null;
        if (c.get(0).getSymbol().equals("CONST")) {
            if (c.size() < 4) {
                return;
            }
            nom = c.get(2).getSymbol();
            expr = c.get(3);
        } else {
            nom = c.get(1).getSymbol();
            if (c.size() > 2) {
                expr = c.get(2);
            }
        }
        if (expr != null) {
            String r = generarExpr(expr);
            afegirInstruccio("=", nom, r, null);
        } else {
            afegirInstruccio("=", nom, "0", null);
        }
    }

    private void generarId(Node n) {
        List<Node> c = n.getChildren();
        if (c.size() < 2) {
            return;
        }
        String nom = c.get(0).getSymbol();
        String tipus = c.get(1).getSymbol();
        if ("ASSIGN".equals(tipus) && c.size() >= 3) {
            String r = generarExpr(c.get(2));
            afegirInstruccio("=", nom, r, null);
        } else if ("ARGS".equals(tipus)) {
            List<String> args = new ArrayList<>();
            for (Node a : c.get(1).getChildren()) {
                args.add(generarExpr(a));
            }
            for (String a : args) {
                afegirInstruccio("param", a, null, null);
            }
            afegirInstruccio("call", nom, String.valueOf(args.size()), null);
        } else if ("INC".equals(tipus)) {
            afegirInstruccio("+", nom, nom, "1");
        } else if ("DEC".equals(tipus)) {
            afegirInstruccio("-", nom, nom, "1");
        }
    }

    private void generarIf(Node n) {
        List<Node> c = n.getChildren();
        if (c.size() < 2) {
            return;
        }
        String cond = generarExpr(c.get(0));
        String l1 = novaEtiqueta();
        afegirInstruccio("if!goto", l1, cond, null);
        Node bloc = c.get(1);
        for (Node s : bloc.getChildren()) {
            generarSentencia(s);
        }
        if (c.size() >= 3 && "ELSE".equals(c.get(2).getSymbol()) && !c.get(2).getChildren().isEmpty()) {
            String l2 = novaEtiqueta();
            afegirInstruccio("goto", l2, null, null);
            afegirInstruccio(null, l1, null, null);
            Node blocElse = c.get(2).getChildren().get(0);
            for (Node s : blocElse.getChildren()) {
                generarSentencia(s);
            }
            afegirInstruccio(null, l2, null, null);
        } else {
            afegirInstruccio(null, l1, null, null);
        }
    }

    private void generarWhile(Node n) {
        List<Node> c = n.getChildren();
        if (c.size() < 2) {
            return;
        }
        String linici = novaEtiqueta();
        String lfi = novaEtiqueta();
        afegirInstruccio(null, linici, null, null);
        String cond = generarExpr(c.get(0));
        afegirInstruccio("if!goto", lfi, cond, null);
        etiquetesBreak.push(lfi);
        etiquetesContinue.push(linici);
        Node bloc = c.get(1);
        for (Node s : bloc.getChildren()) {
            generarSentencia(s);
        }
        etiquetesBreak.pop();
        etiquetesContinue.pop();
        afegirInstruccio("goto", linici, null, null);
        afegirInstruccio(null, lfi, null, null);
    }

    private void generarReturn(Node n) {
        List<Node> c = n.getChildren();
        if (c.size() >= 2) {
            String r = generarExpr(c.get(1));
            afegirInstruccio("return", r, null, null);
        } else {
            afegirInstruccio("return", null, null, null);
        }
    }

    private String generarExpr(Node n) {
        if (n == null) {
            return null;
        }
        String s = n.getSymbol();
        if ("NUM".equals(s)) {
            return valorNum(n);
        }
        if ("TRUE".equals(s)) {
            return "true";
        }
        if ("FALSE".equals(s)) {
            return "false";
        }
        if ("ID".equals(s)) {
            if (!n.getChildren().isEmpty()) {
                return n.getChildren().get(0).getSymbol();
            }
            return "";
        }
        if ("NOT".equals(s)) {
            String a = generarExpr(n.getChildren().get(0));
            String t = nouTemp();
            afegirInstruccio("!", t, a, null);
            return t;
        }
        if ("INC".equals(s) || "DEC".equals(s)) {
            String nom = "";
            if (!n.getChildren().isEmpty()) {
                Node id = n.getChildren().get(0);
                if (!id.getChildren().isEmpty()) {
                    nom = id.getChildren().get(0).getSymbol();
                }
            }
            String op = "INC".equals(s) ? "+" : "-";
            afegirInstruccio(op, nom, nom, "1");
            return nom;
        }
        if ("CAST".equals(s) && n.getChildren().size() >= 2) {
            String tipus = n.getChildren().get(0).getSymbol();
            String a = generarExpr(n.getChildren().get(1));
            String t = nouTemp();
            afegirInstruccio("cast " + tipus, t, a, null);
            return t;
        }
        if ("CALL".equals(s) && n.getChildren().size() >= 2) {
            String nom = "";
            Node id = n.getChildren().get(0);
            if (!id.getChildren().isEmpty()) {
                nom = id.getChildren().get(0).getSymbol();
            }
            List<String> args = new ArrayList<>();
            for (Node a : n.getChildren().get(1).getChildren()) {
                args.add(generarExpr(a));
            }
            for (String a : args) {
                afegirInstruccio("param", a, null, null);
            }
            String t = nouTemp();
            afegirInstruccio("call", t, nom, String.valueOf(args.size()));
            return t;
        }
        String op = simbolOp(s);
        if (op != null && n.getChildren().size() >= 2) {
            String a1 = generarExpr(n.getChildren().get(0));
            String a2 = generarExpr(n.getChildren().get(1));
            String t = nouTemp();
            afegirInstruccio(op, t, a1, a2);
            return t;
        }
        return s;
    }

    private String valorNum(Node n) {
        if (n.getChildren().isEmpty()) {
            return "0";
        }
        String digit = n.getChildren().get(0).getSymbol();
        if (n.getChildren().size() < 2) {
            return digit;
        }
        String expo = n.getChildren().get(1).getSymbol();
        int factor;
        switch (expo) {
            case "piedra":
                factor = 10;
                break;
            case "roca":
                factor = 100;
                break;
            case "montaña":
                factor = 1000;
                break;
            default:
                return digit;
        }
        try {
            long valor = Long.parseLong(digit) * factor;
            return String.valueOf(valor);
        } catch (NumberFormatException e) {
            return digit;
        }
    }

    private String simbolOp(String s) {
        switch (s) {
            case "SUM":
                return "+";
            case "MIN":
                return "-";
            case "MUL":
                return "*";
            case "DIV":
                return "/";
            case "MOD":
                return "%";
            case "ISMORETHAN":
                return ">";
            case "ISLESSTHAN":
                return "<";
            case "ISEQ":
                return "==";
            case "ISNOTEQ":
                return "!=";
            case "ISMOREOREQTHAN":
                return ">=";
            case "ISLESSOREQTHAN":
                return "<=";
            case "AND":
                return "&&";
            case "OR":
                return "||";
            default:
                return null;
        }
    }

    private String nouTemp() {
        numTemp++;
        return "t" + numTemp;
    }

    private String novaEtiqueta() {
        numEtiqueta++;
        return "L" + numEtiqueta;
    }

    private void afegirInstruccio(String op, String desti, String arg1, String arg2) {
        instruccions.add(new Instruccio(op, desti, arg1, arg2));
    }

    public List<Instruccio> getInstruccions() {
        return instruccions;
    }

    public void print() {
        System.out.println("========== TAC ==========");
        for (Instruccio ins : instruccions) {
            System.out.println(ins.toString());
        }
        System.out.println("=========================");
    }
}
