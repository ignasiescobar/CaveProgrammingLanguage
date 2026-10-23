package parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lexer.Token;

public class Parser {

    private static final Set<String> TIPUS = Set.of("I8", "I16", "I32", "I64");
    private static final Set<String> ARIOP = Set.of("SUM", "MIN", "MUL", "DIV", "MOD");
    private static final Set<String> COMP = Set.of(
            "ISMORETHAN", "ISLESSTHAN", "ISEQ", "ISNOTEQ", "ISMOREOREQTHAN", "ISLESSOREQTHAN");
    private static final Set<String> LOGOP = Set.of("AND", "OR");

    private static final Set<String> FOLLOW_FUNCIO = Set.of("I8", "I16", "I32", "I64", "VOID", "EOF");
    private static final Set<String> FOLLOW_PARAMS = Set.of("RPAREN");
    private static final Set<String> FOLLOW_PARAM = Set.of("COMMA", "RPAREN");
    private static final Set<String> FOLLOW_STMT = Set.of(
            "CONST", "I8", "I16", "I32", "I64", "ID", "IF", "WHILE", "RETURN",
            "BREAK", "CONTINUE", "LBRACE", "RBRACE", "EOF");
    private static final Set<String> FOLLOW_EXPR = Set.of(
            "COMMA", "RPAREN", "RBRACE", "ELSE", "EOF", "CONST", "I8", "I16", "I32", "I64",
            "ID", "IF", "WHILE", "RETURN", "BREAK", "CONTINUE", "LBRACE");

    private final List<Token> tokens;
    private final List<String> errors;
    private int currentIndex;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.errors = new ArrayList<>();
        this.currentIndex = 0;
    }

    public GrammaticalTree parseProgram() {
        errors.clear();
        Node root = new Node("PROGRAM", tokenActual().getFila(), tokenActual().getColumna());
        while (!check("EOF")) {
            try {
                while (check("GLOBAL")){
                    root.addChild(parseGlobal());
                }
                root.addChild(parseFuncio());
            } catch (ParseException e) {
                errors.add(e.getMessage());
                sincornitzarFollow(FOLLOW_FUNCIO);
            }
        }
        consumirTokenEsperat("EOF");
        return new GrammaticalTree(root);
    }

    public List<String> getErrors() {
        return errors;
    }

    public void printErrors() {
        for (String error : errors) {
            System.out.println(error);
        }
    }

    private Node parseGlobal() {
        seguentToken();
        return parserDecl();
    }

    private Node parseFuncio() {
        Node n = new Node("FUNCIO", tokenActual().getFila(), tokenActual().getColumna());
        n.addChild(parseTipusRetorn());
        n.addChild(parseFuncioId());
        consumirTokenEsperat("LPAREN");
        if (!check("RPAREN")) {
            n.addChild(parseParams());
        }
        consumirTokenEsperat("RPAREN");
        n.addChild(parseBloc());
        return n;
    }

    private Node parseTipusRetorn() {
        if (check("VOID")) {
            Token voidTok = seguentToken();
            Node n = new Node("TIPUS_RETORN", voidTok.getFila(), voidTok.getColumna());
            n.addChild(new Node(voidTok.getLexema(), voidTok.getFila(), voidTok.getColumna()));
            return n;
        }
        return parseTipus();
    }

    private Node parseFuncioId() {
        Node n = new Node("FUNCIO_ID", tokenActual().getFila(), tokenActual().getColumna());
        if (check("MAIN")) {
            Token main = seguentToken();
            n.addChild(new Node(main.getLexema(), main.getFila(), main.getColumna()));
            return n;
        }
        Token id = consumirTokenEsperat("ID");
        n.addChild(new Node(id.getLexema(), id.getFila(), id.getColumna()));
        return n;
    }

    private Node parseParams() {
        Node n = new Node("PARAMS", tokenActual().getFila(), tokenActual().getColumna());
        try {
            n.addChild(parseParam());
        } catch (ParseException e) {
            errors.add(e.getMessage());
            sincornitzarFollow(FOLLOW_PARAMS);
            return n;
        }
        while (match("COMMA")) {
            try {
                n.addChild(parseParam());
            } catch (ParseException e) {
                errors.add(e.getMessage());
                sincornitzarFollow(FOLLOW_PARAM);
                if (check("RPAREN")) {
                    break;
                }
            }
        }
        return n;
    }

    private Node parseParam() {
        Node n = new Node("PARAM", tokenActual().getFila(), tokenActual().getColumna());
        if (check("CONST")) {
            Token constTok = seguentToken();
            n.addChild(new Node("notocar", constTok.getFila(), constTok.getColumna()));
        }
        n.addChild(parseTipus());
        Token id = consumirTokenEsperat("ID");
        n.addChild(new Node(id.getLexema(), id.getFila(), id.getColumna()));
        return n;
    }

    private Node parseBloc() {
        Token lbraceTok = consumirTokenEsperat("LBRACE");
        Node n = new Node("BLOC", lbraceTok.getFila(), lbraceTok.getColumna());
        while (!check("RBRACE") && !check("EOF")) {
            try {
                int liniaIniciStmt = tokenActual().getFila();
                n.addChild(parseStmt());
                if (!check("RBRACE") && !check("EOF")) {
                    int liniaSeguentToken = tokenActual().getFila();
                    if (liniaSeguentToken == liniaIniciStmt) {
                        errors.add("Les sentències s'han de separar obligatòriament amb salt de línia.");
                        sincornitzarFollow(FOLLOW_STMT);
                    }
                }
            } catch (ParseException e) {
                errors.add(e.getMessage());
                sincornitzarFollow(FOLLOW_STMT);
            }
        }
        consumirTokenEsperat("RBRACE");
        return n;
    }

    private Node parseStmt() {
        if (check("CONST")) {
            return parserDecl();
        }
        if (isTipus(tokenActual().getTokenType())) {
            return parserDecl();
        }
        if (check("ID")) {
            return parserId();
        }
        if (check("IF")) {
            return parserIf();
        }
        if (check("WHILE")) {
            return parserWhile();
        }
        if (check("RETURN")) {
            return parserReturn();
        }
        if (check("BREAK")) {
            return parserBreak();
        }
        if (check("CONTINUE")) {
            return parserContinue();
        }
        if (check("LBRACE")) {
            return parseBloc();
        }
        throw error("Sentència no vàlida");
    }


    private Node parserDecl() {
        Node n = new Node("DECL", tokenActual().getFila(), tokenActual().getColumna());
        if (check("CONST")) {
            Token constTok = seguentToken();
            n.addChild(new Node("CONST", constTok.getFila(), constTok.getColumna()));
            n.addChild(parseTipus());
            Token id = consumirTokenEsperat("ID");
            n.addChild(new Node(id.getLexema(), id.getFila(), id.getColumna()));
            if (check("EQ")) {
                seguentToken();
                n.addChild(parseExpr());
            }
            return n;
        }
        n.addChild(parseTipus());
        Token id = consumirTokenEsperat("ID");
        n.addChild(new Node(id.getLexema(), id.getFila(), id.getColumna()));
        if (check("EQ")) {
            seguentToken();
            n.addChild(parseExpr());
        }
        return n;
    }

    private Node parserId() {
        Node n = new Node("ID_STMT", tokenActual().getFila(), tokenActual().getColumna());
        Token id = consumirTokenEsperat("ID");
        n.addChild(new Node(id.getLexema(), id.getFila(), id.getColumna()));

        if (check("EQ")) {
            Token eqTok = seguentToken();
            n.addChild(new Node("ASSIGN", eqTok.getFila(), eqTok.getColumna()));
            n.addChild(parseExpr());
            return n;
        }
        if (check("LPAREN")) {
            consumirTokenEsperat("LPAREN");

            Node args;
            if (!check("RPAREN")) {
                args = parseArgs();
            } else {
                args = new Node("ARGS", tokenActual().getFila(), tokenActual().getColumna());
            }

            consumirTokenEsperat("RPAREN");
            n.addChild(args);
            return n;
        }
        if (check("INC") || check("DEC")) {
            Token incDecTok = seguentToken();
            n.addChild(new Node(incDecTok.getTokenType(), incDecTok.getFila(), incDecTok.getColumna()));
            return n;
        }
        throw error("S'esperava '=', crida '(' o masuno/menosuno després de l'identificador");
    }

    private Node parserIf() {
        Token ifTok = consumirTokenEsperat("IF");
        Node n = new Node("IF", ifTok.getFila(), ifTok.getColumna());
        consumirTokenEsperat("LPAREN");
        try {
            n.addChild(parseExpr());
        } catch (ParseException e) {
            errors.add(e.getMessage());
            sincornitzarFollow(Set.of("RPAREN"));
        }
        consumirTokenEsperat("RPAREN");
        n.addChild(parseBloc());
        if (check("ELSE")) {
            Token elseTok = seguentToken();
            Node elseBr = new Node("ELSE", elseTok.getFila(), elseTok.getColumna());
            elseBr.addChild(parseBloc());
            n.addChild(elseBr);
        }
        return n;
    }

    private Node parserWhile() {
        Token whileTok = consumirTokenEsperat("WHILE");
        Node n = new Node("WHILE", whileTok.getFila(), whileTok.getColumna());
        consumirTokenEsperat("LPAREN");
        try {
            n.addChild(parseExpr());
        } catch (ParseException e) {
            errors.add(e.getMessage());
            sincornitzarFollow(Set.of("RPAREN"));
        }
        consumirTokenEsperat("RPAREN");
        n.addChild(parseBloc());
        return n;
    }

    private Node parserReturn() {
        Token retTok = consumirTokenEsperat("RETURN");
        Node n = new Node("RETURN_STMT", retTok.getFila(), retTok.getColumna());
        if (!isExprStart(tokenActual().getTokenType())) {
            return n;
        }
        try {
            Node n_return = new Node("RETURN", retTok.getFila(), retTok.getColumna());
            n.addChild(n_return);
            n.addChild(parseExpr());
        } catch (ParseException e) {
            errors.add(e.getMessage());
            sincornitzarFollow(FOLLOW_EXPR);
        }
        return n;
    }

    private Node parserBreak() {
        Token breakTok = consumirTokenEsperat("BREAK");
        return new Node("BREAK", breakTok.getFila(), breakTok.getColumna());
    }

    private Node parserContinue() {
        Token contTok = consumirTokenEsperat("CONTINUE");
        return new Node("CONTINUE", contTok.getFila(), contTok.getColumna());
    }

    private Node parseArgs() {
        Node n = new Node("ARGS", tokenActual().getFila(), tokenActual().getColumna());

        n.addChild(parseExpr());

        while (match("COMMA")) {
            try {
                n.addChild(parseExpr());
            } catch (ParseException e) {
                errors.add(e.getMessage());
                sincornitzarFollow(Set.of("COMMA", "RPAREN"));
                if (check("RPAREN")) {
                    break;
                }
            }
        }

        return n;
    }

    private Node parseExpr() {
        Node left = parseTerm();
        while (isOp(tokenActual().getTokenType())) {
            Token opTok = seguentToken();
            Node opNode = new Node(opTok.getTokenType(), opTok.getFila(), opTok.getColumna());
            opNode.addChild(left);
            opNode.addChild(parseTerm());
            left = opNode;
        }
        return left;
    }

    private Node parseTerm() {
        if (check("NOT")) {
            Token notTok = seguentToken();
            Node n = new Node("NOT", notTok.getFila(), notTok.getColumna());
            n.addChild(parseTerm());
            return n;
        }
        if (check("LPAREN")) {
            consumirTokenEsperat("LPAREN");
            Node inner = parseExpr();
            consumirTokenEsperat("RPAREN");
            return inner;
        }
        if (check("CAST")) {
            return parseCast();
        }
        if (check("DIGIT")) {
            return parseNum();
        }
        if (check("TRUE") || check("FALSE")) {
            return parseBool();
        }
        if (check("ID")) {
            Token idTok = seguentToken();
            Node idNode = new Node("ID", idTok.getFila(), idTok.getColumna());
            idNode.addChild(new Node(idTok.getLexema(), idTok.getFila(), idTok.getColumna()));
            return parsePostfix(idNode);
        }
        throw error("Terme no vàlid");
    }

    private Node parsePostfix(Node idNode) {
        if (check("LPAREN")) {
            consumirTokenEsperat("LPAREN");

            Node call = new Node("CALL", idNode.getFila(), idNode.getColumna());
            call.addChild(idNode);

            Node args;
            if (!check("RPAREN")) {
                args = parseArgs();
            } else {
                args = new Node("ARGS", tokenActual().getFila(), tokenActual().getColumna());
            }

            call.addChild(args);

            consumirTokenEsperat("RPAREN");
            return call;
        }
        if (check("INC") || check("DEC")) {
            Token incDecTok = seguentToken();
            Node n = new Node(incDecTok.getTokenType(), incDecTok.getFila(), incDecTok.getColumna());
            n.addChild(idNode);
            return n;
        }
        return idNode;
    }

    private Node parseCast() {
        Token castTok = consumirTokenEsperat("CAST");
        Node n = new Node("CAST", castTok.getFila(), castTok.getColumna());
        n.addChild(parseTipus());
        n.addChild(parseTerm());
        return n;
    }

    private Node parseNum() {
        Token d = consumirTokenEsperat("DIGIT");
        Node n = new Node("NUM", d.getFila(), d.getColumna());
        n.addChild(new Node(d.getLexema(), d.getFila(), d.getColumna()));
        if (check("EXPO")) {
            Token expo = seguentToken();
            n.addChild(new Node(expo.getLexema(), expo.getFila(), expo.getColumna()));
        }
        return n;
    }

    private Node parseBool() {
        Token t = seguentToken();
        return new Node(t.getTokenType(), t.getFila(), t.getColumna());
    }

    private Node parseTipus() {
        String tt = tokenActual().getTokenType();
        if (!TIPUS.contains(tt)) {
            throw error("S'esperava un tipus (ooga, booga, cabooga, cauabooga)");
        }
        Token tipusTok = seguentToken();
        return new Node(tt, tipusTok.getFila(), tipusTok.getColumna());
    }

    private Token tokenActual() {
        return tokens.get(currentIndex);
    }

    private boolean check(String type) {
        if (esFinalTokens()) {
            return type.equals("EOF");
        }
        return tokenActual().getTokenType().equals(type);
    }

    private boolean esFinalTokens() {
        return tokenActual().getTokenType().equals("EOF");
    }

    private Token seguentToken() {
        Token t = tokens.get(currentIndex);
        if (!esFinalTokens()) {
            currentIndex++;
        }
        return t;
    }

    private boolean match(String type) {
        if (check(type)) {
            seguentToken();
            return true;
        }
        return false;
    }

    private Token consumirTokenEsperat(String type) {
        if (check(type)) {
            return seguentToken();
        }
        throw error("S'esperava " + tokenName(type));
    }

    private String tokenName(String type) {
        switch (type) {
            case "LPAREN":
                return "'('";
            case "RPAREN":
                return "')'";
            case "LBRACE":
                return "'{'";
            case "RBRACE":
                return "'}'";
            case "COMMA":
                return "','";
            case "EQ":
                return "'ser (=) ' ";
            case "ID":
                return "un identificador";
            case "DIGIT":
                return "un numero";
            default:
                return type;
        }
    }

    private void sincornitzarFollow(Set<String> syncTokens) {
        while (!check("EOF") && !syncTokens.contains(tokenActual().getTokenType())) {
            seguentToken();
        }
    }

    private static boolean isTipus(String tt) {
        return TIPUS.contains(tt);
    }

    private static boolean isOp(String tt) {
        return ARIOP.contains(tt) || COMP.contains(tt) || LOGOP.contains(tt);
    }

    private static boolean isExprStart(String tt) {
        return tt.equals("ID")
                || tt.equals("DIGIT")
                || tt.equals("TRUE")
                || tt.equals("FALSE")
                || tt.equals("CAST")
                || tt.equals("NOT")
                || tt.equals("LPAREN");
    }

    private ParseException error(String message) {
        Token t = tokenActual();
        return new ParseException(message + " (token actual: " + t.getTokenType() + " \"" + t.getLexema() + "\" a " + t.getFila() + ":" + t.getColumna() + ")");
    }

    public static class ParseException extends RuntimeException {
        ParseException(String msg) {
            super(msg);
        }
    }
}
