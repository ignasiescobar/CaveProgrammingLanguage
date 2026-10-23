package semantic;

import parser.GrammaticalTree;
import parser.Node;

import java.util.*;

public class TaulaSimbols {

    private static final Set<String> TIPUS_NUM = Set.of("I8", "I16", "I32", "I64");
    private final Map<String, Simbol> globalTable = new LinkedHashMap<>();
    private final Map<String, Map<String, Simbol>> functionScopes = new LinkedHashMap<>();
    private Map<String, Simbol> currentScope = globalTable;
    private final List<String> errors = new ArrayList<>();
    private final List<String> avisosRuntime = new ArrayList<>();
    private String retornFuncio;
    private String funcioActual;
    private int profunditatBucles;

    public void analitzar(GrammaticalTree tree) {
        analitzarArbre(tree);
    }

    public void analitzarArbre(GrammaticalTree tree) {
        errors.clear();
        avisosRuntime.clear();
        globalTable.clear();
        functionScopes.clear();
        canviarScopeGlobal();
        if (tree == null || tree.getRoot() == null) {
            errors.add("L'arbre sintàctic no pot ser buit.");
            return;
        }
        if (!"PROGRAM".equals(tree.getRoot().getSymbol())) {
            errors.add("S'espera arrel PROGRAM.");
            return;
        }
        for (Node fil : tree.getRoot().getChildren()) {
            if ("DECL".equals(fil.getSymbol())) {
                processarGlobals(fil);
            } else if ("FUNCIO".equals(fil.getSymbol())) {
                processarSingatura(fil);
            } else {
                addError("Fills de PROGRAM inesperats: " + fil.getSymbol(), fil);
            }
        }
        // El programa ha de contenir exactament una funcio d'entrada MAIN.
        comprovarMainUnica(tree.getRoot().getChildren());
        // Validem tipus, assignacions, crides, returns, condicionals, casts i errors runtime.
        for (Node fil : tree.getRoot().getChildren()) {
            if ("DECL".equals(fil.getSymbol())) {
                processarVariableGlobal(fil);
            } else if ("FUNCIO".equals(fil.getSymbol())) {
                processarFuncio(fil);
            }
        }
        canviarScopeGlobal();
    }

    public void crearScopePerFuncio(String funcName) {
        Map<String, Simbol> newScope = new LinkedHashMap<>();
        functionScopes.put(funcName, newScope);
        currentScope = newScope;
    }

    public void canviarScopeGlobal() {
        currentScope = globalTable;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void printErrors() {
        for (String error : errors) {
            System.out.println(error);
        }
    }

    private void processarGlobals(Node decl) {
        // Les constants s'han d'inicialitzar en la declaracio i no es poden duplicar noms al mateix ambit.
        List<Node> c = decl.getChildren();
        if (c.isEmpty()) {
            return;
        }
        if (c.get(0).getSymbol().equals("CONST")) {
            if (c.size() < 4) {
                addError("Decl const global incompleta.", decl);
                return;
            }
            String t = c.get(1).getSymbol();
            Node nNode = c.get(2);
            String n = nNode.getSymbol();
            if (jaExisteixEnAquestNivell(n)) {
                addError("Re-declaració: '" + n + "' a l'àmbit global.", nNode);
                return;
            }
            Simbol s = new Simbol(n, Simbol.CategoriaSimbol.VARIABLE, t, true, true, nNode.getFila(), nNode.getColumna());
            afegirSimbol(s);
        } else {
            String t = c.get(0).getSymbol();
            Node nNode = c.get(1);
            String n = nNode.getSymbol();
            if (jaExisteixEnAquestNivell(n)) {
                addError("Re-declaració: '" + n + "' a l'àmbit global.", nNode);
                return;
            }
            boolean init = c.size() > 2;
            afegirSimbol(new Simbol(n, Simbol.CategoriaSimbol.VARIABLE, t, false, init, nNode.getFila(), nNode.getColumna()));
        }
    }

    private void processarSingatura(Node func) {
        String nom = nomFuncio(func);
        Node nodeNomFuncio = nodeNomFuncio(func);
        if (jaExisteixEnAquestNivell(nom)) {
            addError("Re-declaració: funció '" + nom + "' a l'àmbit global.", nodeNomFuncio);
            return;
        }
        String r = tipusRetorn(func);
        List<String> tipParams = llistaTipusParams(func);
        List<Boolean> cParam = llistaParamConstFlags(func);
        int fila = nodeNomFuncio != null ? nodeNomFuncio.getFila() : -1;
        int columna = nodeNomFuncio != null ? nodeNomFuncio.getColumna() : -1;
        Simbol s = new Simbol(nom, Simbol.CategoriaSimbol.FUNCTION, r, false, true, fila, columna);
        s.setTipusParams(tipParams);
        s.setTipusConst(cParam);
        afegirSimbol(s);
    }

    private void processarVariableGlobal(Node decl) {
        List<Node> c = decl.getChildren();
        if (c.get(0).getSymbol().equals("CONST") && c.size() >= 4) {
            String tDes = c.get(1).getSymbol();
            Node nNode = c.get(2);
            String nom = nNode.getSymbol();
            String te = processarExpr(c.get(3), true, false);
            if (te != null && !sonCompatiblesAssign(tDes, te)) {
                addError("Inicialització de '" + nom + "': s'espera " + tDes + " i s'obté " + te, nNode);
            }
        } else if (c.size() > 2) {
            String tDes = c.get(0).getSymbol();
            Node nNode = c.get(1);
            String nom = nNode.getSymbol();
            String te = processarExpr(c.get(2), true, false);
            if (te != null && !sonCompatiblesAssign(tDes, te)) {
                addError("Inicialització de '" + nom + "': s'espera " + tDes + " i s'obté " + te, nNode);
            }
        }
    }

    private void processarFuncio(Node func) {
        String nom = nomFuncio(func);
        String retT = tipusRetorn(func);
        List<Node> fillsFunc = func.getChildren();
        Node body = fillsFunc.isEmpty() ? null : fillsFunc.get(fillsFunc.size() - 1);
        if (body == null) {
            addError("BLOC faltant a " + nom, nodeNomFuncio(func));
            return;
        }
        retornFuncio = retT;
        funcioActual = nom;
        profunditatBucles = 0;

        crearScopePerFuncio(nom);
        Node pnode = abansDarrerFillsEsParams(func);
        if (pnode != null && "PARAMS".equals(pnode.getSymbol())) {
            for (Node p : pnode.getChildren()) {
                if (!"PARAM".equals(p.getSymbol())) {
                    continue;
                }
                int off = 0;
                if (p.getChildren().size() == 3) {
                    off = 1;
                }
                List<Node> ch = p.getChildren();
                String tPar = ch.get(off).getSymbol();
                String idP = ch.get(off + 1).getSymbol();
                boolean cst = p.getChildren().size() == 3 && p.getChildren().get(0).getSymbol().equals("notocar");
                if (jaExisteixEnAquestNivell(idP)) {
                    addError("Paramètre duplicat '" + idP + "' a " + nom, ch.get(off + 1));
                } else {
                    Node idParNode = ch.get(off + 1);
                    afegirSimbol(new Simbol(idP, Simbol.CategoriaSimbol.PARAMETER, tPar, cst, true, idParNode.getFila(), idParNode.getColumna()));
                }
            }
        }
        processarBloc(body.getChildren(), false);

        if (!"VOID".equals(retT) && !blocPotTornar(body, retT)) {
            addError("La funció amb tipus " + retT + " requereix 'return' en tots els camins d'execució: " + nom, cerca(nom));
        }

        retornFuncio = null;
        funcioActual = null;
        canviarScopeGlobal();
    }

    private boolean blocPotTornar(Node bloc, String retT) {
        for (Node s : bloc.getChildren()) {
            if (stmtPotTornar(s, retT)) {
                return true;
            }
        }
        return false;
    }

    private boolean stmtPotTornar(Node s, String retT) {
        if ("RETURN".equals(s.getSymbol())) {
            if ("VOID".equals(retT)) {
                return true;
            }
            return !s.getChildren().isEmpty();
        }
        if ("RETURN_STMT".equals(s.getSymbol())) {
            if (s.getChildren().isEmpty()) {
                return "VOID".equals(retT);
            }
            if ("VOID".equals(retT)) {
                return true;
            }
            return s.getChildren().size() >= 2;
        }
        if ("IF".equals(s.getSymbol()) && s.getChildren().size() >= 2) {
            if (s.getChildren().size() < 3) {
                return false;
            }
            Node el = s.getChildren().get(2);
            if (!"ELSE".equals(el.getSymbol()) || el.getChildren().isEmpty()) {
                return false;
            }
            return blocPotTornar(s.getChildren().get(1), retT) && blocPotTornar(el.getChildren().get(0), retT);
        }
        if ("BLOC".equals(s.getSymbol())) {
            return blocPotTornar(s, retT);
        }
        return false;
    }

    private void processarDeclGlobal(Node decl) {
        List<Node> c = decl.getChildren();
        if (c.get(0).getSymbol().equals("CONST")) {
            if (c.size() < 4) {
                addError("const local incompleta a " + contextSemanticaActual(), decl);
                return;
            }
            String t = c.get(1).getSymbol();
            Node nNode = c.get(2);
            String n = nNode.getSymbol();
            if (jaExisteixEnAquestNivell(n)) {
                addError("Re-declaració '" + n + "' a " + contextSemanticaActual(), nNode);
                return;
            }
            String te = processarExpr(c.get(3), true, false);
            if (te != null && !sonCompatiblesAssign(t, te)) {
                addError("Inicial de const " + n + " incompatible: " + te + " però es declara " + t, nNode);
            }
            afegirSimbol(new Simbol(n, Simbol.CategoriaSimbol.VARIABLE, t, true, true, nNode.getFila(), nNode.getColumna()));
        } else {
            String t = c.get(0).getSymbol();
            Node nNode = c.get(1);
            String n = nNode.getSymbol();
            if (jaExisteixEnAquestNivell(n)) {
                addError("Re-declaració '" + n + "' a " + contextSemanticaActual(), nNode);
                return;
            }
            if (c.size() > 2) {
                String te = processarExpr(c.get(2), true, false);
                if (te != null && !sonCompatiblesAssign(t, te)) {
                    addError("Inicial a declaració " + n + " incompatible: " + te, nNode);
                }
            }
            boolean init = c.size() > 2;
            afegirSimbol(new Simbol(n, Simbol.CategoriaSimbol.VARIABLE, t, false, init, nNode.getFila(), nNode.getColumna()));
        }
    }

    private void processarIdStmt(Node id) {
        // En assignacions i crides, l'identificador ha d'existir i els tipus han de ser compatibles.
        List<Node> c = id.getChildren();
        if (c.isEmpty()) {
            return;
        }
        Node nomNode = c.get(0);
        String nom = nomNode.getSymbol();
        Simbol s = cerca(nom);
        if (s == null) {
            addError("Identificador no declarat: '" + nom + "' a " + contextSemanticaActual(), nomNode);
        }
        if (c.size() < 2) {
            return;
        }
        Node t2 = c.get(1);
        if (t2.getSymbol().equals("ASSIGN") && c.size() >= 3) {
            if (s == null) {
                return;
            }
            if (s.getCategoria() == Simbol.CategoriaSimbol.FUNCTION) {
                addError("No es pot assignar a una funció: " + nom, nomNode);
                return;
            }
            if (s.isConstant()) {
                addError("L'identificador '" + nom + "' no es pot reassignar (const/notocar).", nomNode);
                return;
            }
            String tr = processarExpr(c.get(2), true, true);
            if (tr != null && !sonCompatiblesAssign(s.getTipus(), tr)) {
                addError("Assignació incompatible per a " + nom + " (" + s.getTipus() + " / " + tr + ")", nomNode);
            }
        } else if (t2.getSymbol().equals("ARGS")) {
            comprovarCridadaIExpr(nom, t2, false, true, true);
        } else if (t2.getSymbol().equals("INC") || t2.getSymbol().equals("DEC")) {
            if (s == null) {
                return;
            }
            if (s.getCategoria() == Simbol.CategoriaSimbol.FUNCTION) {
                addError("++/-- no aplica a funcions: " + nom, nomNode);
            } else if (!esNumeric(s.getTipus())) {
                addError("++/-- només a variables numèriques: " + nom, nomNode);
            }
        }
    }

    private void comprovarExprAbans(Node e) {
        // La condicio de if i while ha de ser de tipus boolea.
        String t = processarExpr(e, true, true);
        if (t == null) {
            return;
        }
        if (!"BOOL".equals(t)) {
            addError("La condició d'if/while ha de ser de tipus BOOL, no " + t, e);
        }
    }

    private String processarExpr(Node n, boolean errorReceptor, boolean allowVoidCall) {
        // Cada expressio te tipus i els operadors/casts nomes s'apliquen a tipus compatibles.
        if (n == null) {
            return null;
        }
        String s = n.getSymbol();
        if ("TRUE".equals(s) || "FALSE".equals(s)) {
            return "BOOL";
        }
        if ("NUM".equals(s)) {
            return "I32";
        }
        if (esTipusLletra(s) && n.getChildren().isEmpty()) {
            return s;
        }
        if ("ID".equals(s)) {
            if (n.getChildren().isEmpty()) {
                return null;
            }
            String nId = n.getChildren().get(0).getSymbol();
            return mirarTipus(nId, n, errorReceptor, allowVoidCall);
        }
        if (s.equals("NOT")) {
            String t = n.getChildren().isEmpty() ? null : processarExpr(n.getChildren().get(0), true, true);
            if (t != null && !"BOOL".equals(t) && errorReceptor) {
                addError("NOT només a BOOL, no " + t, n);
            }
            return "BOOL";
        }
        if (s.equals("AND") || s.equals("OR")) {
            for (int i = 0; i < 2; i++) {
                String t = processarExpr(n.getChildren().get(i), true, true);
                if (t != null && !"BOOL".equals(t) && errorReceptor) {
                    addError("Operador " + s + " només amb BOOL, no " + t, n);
                }
            }
            return "BOOL";
        }
        if (s.equals("SUM") || s.equals("MIN") || s.equals("MUL") || s.equals("DIV") || s.equals("MOD")) {
            if (n.getChildren().size() < 2) {
                return null;
            }
            String a1 = processarExpr(n.getChildren().get(0), true, true);
            String a2 = processarExpr(n.getChildren().get(1), true, true);
            comprovarOpArithOComp(s, a1, a2, n.getChildren().get(1), errorReceptor);
            if (a1 == null || a2 == null) {
                return tipusResultantNumeric(a1, a2);
            }
            if (!esNumeric(a1) || !esNumeric(a2)) {
                if (errorReceptor) {
                    addError("Operador " + s + " només amb tipus numèrics (" + a1 + " i " + a2 + ")", n);
                }
            }
            return tipusResultantNumeric(a1, a2);
        }
        if (s.equals("ISMORETHAN") || s.equals("ISLESSTHAN") || s.equals("ISEQ") || s.equals("ISNOTEQ") || s.equals("ISMOREOREQTHAN") || s.equals("ISLESSOREQTHAN")) {
            if (n.getChildren().size() < 2) {
                return "BOOL";
            }
            String a1 = processarExpr(n.getChildren().get(0), true, true);
            String a2 = processarExpr(n.getChildren().get(1), true, true);
            comprovarTipusOp(a1, a2, s, n.getChildren().get(0), n.getChildren().get(1), errorReceptor);
            return "BOOL";
        }
        if ("CALL".equals(s) && n.getChildren().size() >= 1) {
            return tipusCridadaDesDeCall(n, errorReceptor, allowVoidCall);
        }
        if ("INC".equals(s) || "DEC".equals(s)) {
            if (!n.getChildren().isEmpty()) {
                return processarExpr(n.getChildren().get(0), true, true);
            }
        }
        if (s.equals("CAST") && n.getChildren().size() >= 2) {
            return comprovarCast(n, errorReceptor);
        }
        if (n.getChildren().isEmpty() && cerca(s) != null) {
            return cerca(s).getTipus();
        }
        return null;
    }

    private void comprovarTipusOp(String t1, String t2, String op, Node e1, Node e2, boolean err) {
        if (t1 == null || t2 == null) {
            return;
        }
        if (esNumeric(t1) && esNumeric(t2)) {
            return;
        }
        if ("BOOL".equals(t1) && "BOOL".equals(t2)) {
            if (!"ISEQ".equals(op) && !"ISNOTEQ".equals(op) && err) {
                addError("Comparació de booleans: només == o !=, no " + op, e1);
            }
            return;
        }
        if (err) {
            addError("Comparació incompatible: " + t1 + " i " + t2, e1);
        }
    }

    private void comprovarOpArithOComp(String op, String t1, String t2, Node dreta, boolean err) {
        if (op.equals("DIV") || op.equals("MOD")) {
            // La divisio o modul per 0 es considera error en temps d'execucio (runtime).
            comprovarDiv0(dreta, op, err);
        }
    }

    private void comprovarDiv0(Node dret, String op, boolean err) {
        if (literalIntZero(dret)) {
            if (err) {
                avisosRuntime.add("[RT] " + op + " per 0: error d'execució possible (" + contextSemanticaActual() + ").");
            }
        }
    }

    private static boolean literalIntZero(Node d) {
        if (!"NUM".equals(d.getSymbol())) {
            return false;
        }
        if (d.getChildren().isEmpty()) {
            return false;
        }
        String k = d.getChildren().get(0).getSymbol();
        if (!"0".equals(k)) {
            return false;
        }
        return d.getChildren().size() == 1;
    }

    private String comprovarCast(Node cst, boolean err) {
        // cambiar T expr nomes es valid si la conversio expr -> T esta permesa.
        String tDest = cst.getChildren().get(0).getSymbol();
        String tOr = processarExpr(cst.getChildren().get(1), true, true);
        if (tOr != null && !castPermes(tOr, tDest) && err) {
            addError("Conversió (cambiar) no permesa: " + tOr + " -> " + tDest, cst);
        }
        return tDest;
    }

    private boolean castPermes(String from, String to) {
        if (from == null) {
            return true;
        }
        if (from.equals(to)) {
            return true;
        }
        if (esNumeric(from) && esNumeric(to)) {
            return true;
        }
        return "BOOL".equals(from) && "BOOL".equals(to);
    }

    private static boolean esNumeric(String t) {
        return t != null && TIPUS_NUM.contains(t);
    }

    private static boolean esTipusLletra(String t) {
        return t != null && (TIPUS_NUM.contains(t) || "BOOL".equals(t) || "VOID".equals(t));
    }

    private static boolean sonCompatiblesAssign(String decl, String obt) {
        if (decl == null || obt == null) {
            return true;
        }
        // En assignacio, els tipus numerics son compatibles entre ells.
        if (esNumeric(decl) && esNumeric(obt)) {
            return true;
        }
        return decl.equals(obt);
    }

    private void processarBloc(List<Node> stmts, boolean dinsB) {
        for (Node s : stmts) {
            switch (s.getSymbol()) {
                case "DECL" -> {
                    processarDeclGlobal(s);
                }
                case "ID_STMT" -> processarIdStmt(s);
                case "IF" -> {
                    comprovarExprAbans(s.getChildren().get(0));
                    if (s.getChildren().size() > 1) {
                        processarBloc(
                                s.getChildren().get(1).getChildren(), dinsB);
                    }
                    if (s.getChildren().size() > 2) {
                        Node e = s.getChildren().get(2);
                        if (e.getSymbol().equals("ELSE") && !e.getChildren().isEmpty()) {
                            processarBloc(
                                    e.getChildren().get(0).getChildren(), dinsB);
                        }
                    }
                }
                case "WHILE" -> {
                    comprovarExprAbans(s.getChildren().get(0));
                    profunditatBucles++;
                    processarBloc(s.getChildren().get(1).getChildren(), true);
                    profunditatBucles--;
                }
                case "BLOC" -> {
                    processarBloc(s.getChildren(), dinsB);
                }
                case "BREAK" -> {
                    if (profunditatBucles == 0) {
                        addError("BREAK fora d'un bucle a " + contextSemanticaActual(), s);
                    }
                }
                case "CONTINUE" -> {
                    if (profunditatBucles == 0) {
                        addError("CONTINUE fora d'un bucle a " + contextSemanticaActual(), s);
                    }
                }
                case "RETURN" -> {
                    processarReturn(s, retornFuncio);
                }
                case "RETURN_STMT" -> {
                    processarReturnStmt(s, retornFuncio);
                }
                default -> {
                }
            }
        }
    }

    private void processarReturnStmt(Node stmt, String tipFunc) {
        Node ret = new Node("RETURN");
        if (stmt.getChildren().size() >= 2) {
            ret.addChild(stmt.getChildren().get(1));
        }
        processarReturn(ret, tipFunc);
    }

    private String tipusCridadaDesDeCall(Node c, boolean err, boolean allowVoid) {
        if (c.getChildren().size() < 2 || c.getChildren().get(0).getChildren().isEmpty()) {
            return null;
        }
        String f = c.getChildren().get(0).getChildren().get(0).getSymbol();
        comprovarCridadaIExpr(f, c.getChildren().get(1), true, true, allowVoid);
        Simbol fn = cerca(f);
        if (fn == null) {
            return null;
        }
        return fn.getTipus();
    }

    private void processarReturn(Node retNode, String tipFunc) {
        List<Node> ch = retNode.getChildren();
        if (tipFunc == null) {
            return;
        }
        if ("VOID".equals(tipFunc)) {
            if (!ch.isEmpty()) {
                addError("A funció 'void' el return no pot portar expressió a " + contextSemanticaActual(), retNode);
            }
        } else {
            if (ch.isEmpty()) {
                addError("A funció amb tipus " + tipFunc + " cal 'return' amb expressió: " + contextSemanticaActual(), retNode);
            } else {
                String t = processarExpr(ch.get(0), true, true);
                if (t != null && !t.equals(tipFunc)) {
                    addError("Tipus de 'return' (" + t + ") no coincideix amb el tipus de la funció (" + tipFunc + ") a " + contextSemanticaActual(), retNode);
                }
            }
        }
    }

    private String tipusResultantNumeric(String t1, String t2) {
        if (t1 == null) {
            return t2;
        }
        if (t2 == null) {
            return t1;
        }
        if (!esNumeric(t1) && !esNumeric(t2)) {
            return t1;
        }
        if (!esNumeric(t1)) {
            return t2;
        }
        if (!esNumeric(t2)) {
            return t1;
        }
        if (t1.equals(t2)) {
            return t1;
        }
        if (t1.equals("I64") || t2.equals("I64")) {
            return "I64";
        }
        if (t1.equals("I32") || t2.equals("I32")) {
            return "I32";
        }
        if (t1.equals("I16") || t2.equals("I16")) {
            return "I16";
        }
        return "I8";
    }

    private void comprovarCridadaIExpr(String fName, Node argNode, boolean enContextExpr, boolean err, boolean allowVoid) {
        // La funcio cridada ha d'existir i coincidir en nombre i tipus de parametres.
        Simbol fn = cerca(fName);
        if (fn == null) {
            if (err) {
                addError("Cridada: funció no declarada: " + fName, argNode);
            }
            return;
        }
        if (fn.getCategoria() != Simbol.CategoriaSimbol.FUNCTION) {
            if (err) {
                addError("'" + fName + "' no és una funció (és " + fn.getCategoria() + ").", argNode);
            }
            return;
        }
        if (argNode == null || !"ARGS".equals(argNode.getSymbol())) {
            if (err) {
                addError("Estructura de crida invàlida: " + fName, argNode);
            }
            return;
        }
        List<String> tesp = fn.getTipusParams() == null ? List.of() : fn.getTipusParams();
        List<Node> eargs = argNode.getChildren();
        if (eargs.size() != tesp.size()) {
            if (err) {
                addError("Nombre d'arguments incorrecte a " + fName + " (s'esperen " + tesp.size() + " i n'hi ha " + eargs.size() + ").", argNode);
            }
        }
        for (int i = 0; i < eargs.size() && i < tesp.size(); i++) {
            String t = processarExpr(eargs.get(i), err, false);
            if (t != null && !t.equals(tesp.get(i)) && err) {
                addError("Tipus d'argument " + (i + 1) + " a " + fName + ": s'espera " + tesp.get(i) + " i s'obté " + t, eargs.get(i));
            }
        }
        if (enContextExpr && !allowVoid && "VOID".equals(fn.getTipus()) && err) {
            addError("Cridada a funció void a context d'expressió: " + fName, argNode);
        }
    }

    private String mirarTipus(String nom, Node dinsDe, boolean err, boolean allowVoidCall) {
        Simbol s = cerca(nom);
        if (s == null) {
            if (err) {
                addError("ID no declarat: " + nom, dinsDe);
            }
            return null;
        }
        if (s.getCategoria() == Simbol.CategoriaSimbol.FUNCTION && "ID".equals(dinsDe.getSymbol()) && err) {
            addError("Cal cridar la funció '" + nom + "' amb parèntesis, no com a variable simple.", dinsDe);
            return null;
        }
        return s.getTipus();
    }

    private Simbol cerca(String nom) {
        if (currentScope.containsKey(nom)) {
            return currentScope.get(nom);
        }
        if (currentScope != globalTable && globalTable.containsKey(nom)) {
            return globalTable.get(nom);
        }
        return null;
    }

    private String contextSemanticaActual() {
        return funcioActual != null ? funcioActual : "(fora de funció)";
    }



    private void addError(String m, Node n) {
        String msg = m;
        if (n != null && n.getFila() > 0) {
            msg = m + " (línia " + n.getFila() + ", columna " + n.getColumna() + ")";
        }
        if (!errors.contains(msg)) {
            errors.add(msg);
        }
    }

    private void addError(String m, Simbol simbol) {
        String msg = m;
        if (simbol != null && simbol.getFila() > 0) {
            msg = m + " (línia " + simbol.getFila() + ", columna " + simbol.getColumna() + ")";
        }
        if (!errors.contains(msg)) {
            errors.add(msg);
        }
    }

    public void printAll() {
        final boolean useColor = System.getenv("NO_COLOR") == null;
        final String R = useColor ? "\u001B[0m" : "";
        final String B = useColor ? "\u001B[1m" : "";
        final String CY = useColor ? "\u001B[36m" : "";
        final String MG = useColor ? "\u001B[35m" : "";
        final String YL = useColor ? "\u001B[33m" : "";
        final String RD = useColor ? "\u001B[31m" : "";
        final String DM = useColor ? "\u001B[2m" : "";

        System.out.println(B + CY + "========== TAULA GLOBAL ==========" + R);
        for (Simbol s : globalTable.values()) {
            System.out.println(DM + formatSimbolTaula(s) + R);
        }
        for (Map.Entry<String, Map<String, Simbol>> entry : functionScopes.entrySet()) {
            System.out.println();
            System.out.println(B + MG + "---------- ÀMBIT LOCAL: " + entry.getKey() + " ----------" + R);
            for (Simbol s : entry.getValue().values()) {
                System.out.println(DM + formatSimbolTaula(s) + R);
            }
        }
        System.out.println(B + CY + "==================================" + R);
        if (!avisosRuntime.isEmpty()) {
            System.out.println();
            System.out.println(B + YL + "Avisos (runtime):" + R);
            for (String a : avisosRuntime) {
                System.out.println(YL + a + R);
            }
        }
        if (!errors.isEmpty()) {
            System.out.println();
            System.out.println(B + RD + "Errors semàntics:" + R);
            for (String e : errors) {
                System.out.println(RD + e + R);
            }
        }
    }

    private static String formatSimbolTaula(Simbol s) {
        String etiquetaCategoria = s.getCategoria() == Simbol.CategoriaSimbol.FUNCTION ? "FUNCTION" : (s.getCategoria() == Simbol.CategoriaSimbol.PARAMETER ? "PARAMETER" : "VARIABLE");
        boolean init = s.getCategoria() == Simbol.CategoriaSimbol.FUNCTION ? true : s.isInitialized();
        return String.format("[%s] Nom: %-10s | Tipus: %-5s | Init: %b", etiquetaCategoria, s.getName(), s.getTipus(), init);
    }

    private String nomFuncio(Node func) {
        for (Node x : func.getChildren()) {
            if ("FUNCIO_ID".equals(x.getSymbol()) && !x.getChildren().isEmpty()) {
                return x.getChildren().get(0).getSymbol();
            }
        }
        return "";
    }

    private Node nodeNomFuncio(Node func) {
        for (Node x : func.getChildren()) {
            if ("FUNCIO_ID".equals(x.getSymbol()) && !x.getChildren().isEmpty()) {
                return x.getChildren().get(0);
            }
        }
        return null;
    }

    private String tipusRetorn(Node func) {
        Node t = func.getChildren().get(0);
        if ("TIPUS_RETORN".equals(t.getSymbol())) {
            if (!t.getChildren().isEmpty()) {
                if (t.getChildren().get(0).getSymbol().equals("nada")) {
                    return "VOID";
                }
            }
        }
        return t.getSymbol();
    }

    private Node abansDarrerFillsEsParams(Node func) {
        List<Node> c = func.getChildren();
        if (c.size() < 2) {
            return null;
        }
        if (c.size() >= 4) {
            return c.get(2);
        }
        return null;
    }

    private static List<String> llistaTipusParams(Node f) {
        for (Node x : f.getChildren()) {
            if (x.getSymbol().equals("PARAMS")) {
                List<String> r = new ArrayList<>();
                for (Node p : x.getChildren()) {
                    if (!p.getSymbol().equals("PARAM")) {
                        continue;
                    }
                    if (p.getChildren().size() == 2) {
                        r.add(p.getChildren().get(0).getSymbol());
                    } else {
                        r.add(p.getChildren().get(1).getSymbol());
                    }
                }
                return r;
            }
        }
        return List.of();
    }

    private static List<Boolean> llistaParamConstFlags(Node f) {
        for (Node x : f.getChildren()) {
            if (x.getSymbol().equals("PARAMS")) {
                List<Boolean> r = new ArrayList<>();
                for (Node p : x.getChildren()) {
                    if (p.getSymbol().equals("PARAM") && p.getChildren().size() == 3) {
                        r.add(true);
                    } else if (p.getSymbol().equals("PARAM")) {
                        r.add(false);
                    }
                }
                return r;
            }
        }
        return List.of();
    }

    private boolean afegirSimbol(Simbol e) {
        if (e == null || e.getName() == null) {
            return false;
        }
        if (jaExisteixEnAquestNivell(e.getName())) {
            return false;
        }
        currentScope.put(e.getName(), e);
        return true;
    }

    private boolean jaExisteixEnAquestNivell(String n) {
        if (n == null) {
            return false;
        }
        return currentScope.containsKey(n);
    }

    private void comprovarMainUnica(List<Node> fillsProgram) {
        int mains = 0;
        for (Node fill : fillsProgram) {
            if (!"FUNCIO".equals(fill.getSymbol())) {
                continue;
            }
            String nom = nomFuncio(fill);
            if ("OOGACHACKA".equals(nom) || "MAIN".equals(nom)) {
                mains++;
            }
        }
        if (mains != 1) {
            addError("El programa ha de contenir exactament una funció MAIN.", (Node) null);
        }
    }
}
