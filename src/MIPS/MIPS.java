
package MIPS;
import tac.Instruccio;
import tac.TAC;

import java.util.*;

public class MIPS {

    private final List<Instruccio> instruccions;
    private final List<String> liniesMips = new ArrayList<>();

    public MIPS(TAC tac) {
        this.instruccions = tac.getInstruccions();
    }

    public String generarMips() {
        liniesMips.clear();
        List<Instruccio> inicialitzacionsGlobals = new ArrayList<>();
        LinkedHashMap<String, List<Instruccio>> cosPerFuncio = new LinkedHashMap<>();
        Set<String> variablesGlobals = new LinkedHashSet<>();
        Map<String, Integer> numParametresPerFuncio = new HashMap<>();

        String nomFuncioActual = null;
        for (Instruccio instruccio : instruccions) {
            String desti = instruccio.getDesti();
            boolean esEtiquetaInterna = desti != null && desti.matches("L\\d+");
            if (instruccio.getOp() == null && !esEtiquetaInterna) {
                nomFuncioActual = desti;
                cosPerFuncio.put(nomFuncioActual, new ArrayList<>());
                continue;
            }

            if (nomFuncioActual == null) {
                inicialitzacionsGlobals.add(instruccio);
                if ("=".equals(instruccio.getOp())) {
                    variablesGlobals.add(instruccio.getDesti());
                }
                continue;
            }

            cosPerFuncio.get(nomFuncioActual).add(instruccio);
            if ("call".equals(instruccio.getOp())) {
                if (instruccio.getArg2() != null) {
                    numParametresPerFuncio.put(instruccio.getArg1(), Integer.parseInt(instruccio.getArg2()));
                } else {
                    numParametresPerFuncio.put(instruccio.getDesti(), Integer.parseInt(instruccio.getArg1()));
                }
            }
        }

        generarSeccioInicial(variablesGlobals);
        generarCodiMips(inicialitzacionsGlobals, cosPerFuncio, variablesGlobals, numParametresPerFuncio);

        StringBuilder resultat = new StringBuilder();
        for (String linia : liniesMips) {
            resultat.append(formatarLinia(linia)).append("\n");
        }
        return resultat.toString();
    }

    public void print() {
        System.out.println("========== mips ==========");
        for (String linia : liniesMips) {
            System.out.println(formatarLinia(linia));
        }
        System.out.println("==========================");
    }

    private void generarSeccioInicial(Set<String> variablesGlobals) {
        afegirInstruccioMips(".data");
        for (String variableGlobal : variablesGlobals) {
            afegirInstruccioMips(variableGlobal + ": .word 0");
        }
        afegirInstruccioMips("");
    }

    private void generarCodiMips(List<Instruccio> inicialitzacionsGlobals, Map<String, List<Instruccio>> cosPerFuncio, Set<String> variablesGlobals, Map<String, Integer> numParametresPerFuncio) {
        afegirInstruccioMips(".text");
        afegirInstruccioMips(".globl main");
        afegirInstruccioMips("");
        afegirInstruccioMips("main:");
        for (Instruccio instruccio : inicialitzacionsGlobals) {
            if ("=".equals(instruccio.getOp())) {
                String valor = instruccio.getArg1();
                afegirInstruccioMips("li $t0, " + ("true".equals(valor) ? "1" : "false".equals(valor) ? "0" : valor));
                afegirInstruccioMips("sw $t0, " + instruccio.getDesti());
            }
        }
        afegirInstruccioMips("jal OOGACHACKA");
        afegirInstruccioMips("li $v0, 10");
        afegirInstruccioMips("syscall");
        afegirInstruccioMips("");

        for (Map.Entry<String, List<Instruccio>> entradaFuncio : cosPerFuncio.entrySet()) {
            generarFuncio(entradaFuncio.getKey(), entradaFuncio.getValue(), variablesGlobals, numParametresPerFuncio);
            afegirInstruccioMips("");
        }
    }

    private void generarFuncio(String nomFuncio, List<Instruccio> cosFuncio, Set<String> variablesGlobals, Map<String, Integer> numParametresPerFuncio) {
        List<String> parametres = identificarParams(cosFuncio, variablesGlobals);
        int totalParametres = Math.max(numParametresPerFuncio.getOrDefault(nomFuncio, 0), parametres.size());
        List<String> variablesLocals = recollirLocals(cosFuncio, variablesGlobals, parametres);
        Map<String, Integer> desplacaments = new HashMap<>();
        for (int i = 0; i < variablesLocals.size(); i++) {
            desplacaments.put(variablesLocals.get(i), -(i + 1) * 4);
        }
        int midaLocals = variablesLocals.size() * 4;

        afegirInstruccioMips(nomFuncio + ":");
        afegirInstruccioMips("addi $sp, $sp, -8");
        afegirInstruccioMips("sw $ra, 4($sp)");
        afegirInstruccioMips("sw $fp, 0($sp)");
        afegirInstruccioMips("move $fp, $sp");
        if (midaLocals > 0) {
            afegirInstruccioMips("addi $sp, $sp, -" + midaLocals);
        }

        for (int i = 0; i < parametres.size(); i++) {
            int desplacamentCaller = 8 + (totalParametres - 1 - i) * 4;
            int desplacamentLocal = desplacaments.get(parametres.get(i));
            afegirInstruccioMips("lw $t0, " + desplacamentCaller + "($fp)");
            afegirInstruccioMips("sw $t0, " + desplacamentLocal + "($fp)");
        }

        for (Instruccio instruccio : cosFuncio) {
            generarInstruccio(instruccio, desplacaments, variablesGlobals, nomFuncio);
        }

        afegirInstruccioMips(nomFuncio + "_fi:");
        afegirInstruccioMips("move $sp, $fp");
        afegirInstruccioMips("lw $fp, 0($sp)");
        afegirInstruccioMips("lw $ra, 4($sp)");
        afegirInstruccioMips("addi $sp, $sp, 8");
        afegirInstruccioMips("jr $ra");
    }

    private List<String> identificarParams(List<Instruccio> cosFuncio, Set<String> variablesGlobals) {
        LinkedHashSet<String> variablesLlegides = new LinkedHashSet<>();
        Set<String> variablesEscrites = new HashSet<>();
        for (Instruccio instruccio : cosFuncio) {
            for (String variable : obtenirVarsLlegides(instruccio)) {
                if (variable != null && !"true".equals(variable) && !"false".equals(variable) && !esNumero(variable) && !variablesGlobals.contains(variable)) {
                    variablesLlegides.add(variable);
                }
            }
            String variableEscrita = obtenirVariableEscrita(instruccio);
            if (variableEscrita != null) {
                variablesEscrites.add(variableEscrita);
            }
        }
        List<String> parametres = new ArrayList<>();
        for (String variable : variablesLlegides) {
            if (!variablesEscrites.contains(variable)) {
                parametres.add(variable);
            }
        }
        return parametres;
    }

    private List<String> recollirLocals(List<Instruccio> cosFuncio, Set<String> variablesGlobals, List<String> parametres) {
        LinkedHashSet<String> totesLesVariables = new LinkedHashSet<>();
        for (Instruccio instruccio : cosFuncio) {
            for (String variable : obtenirTotesVars(instruccio)) {
                if (variable != null && !"true".equals(variable) && !"false".equals(variable) && !esNumero(variable) && !variablesGlobals.contains(variable)) {
                    totesLesVariables.add(variable);
                }
            }
        }
        List<String> locals = new ArrayList<>(parametres);
        for (String variable : totesLesVariables) {
            if (!locals.contains(variable)) {
                locals.add(variable);
            }
        }
        return locals;
    }


    private void generarInstruccio(Instruccio instruccio, Map<String, Integer> desplacaments, Set<String> variablesGlobals, String nomFuncio) {
        String op = instruccio.getOp();

        if (op == null) {
            afegirInstruccioMips(instruccio.getDesti() + ":");
            return;
        }

        switch (op) {
            case "=":
                carregarValor(instruccio.getArg1(), "$t0", desplacaments, variablesGlobals);
                guardarValor("$t0", instruccio.getDesti(), desplacaments, variablesGlobals);
                break;
            case "goto":
                afegirInstruccioMips("j " + instruccio.getDesti());
                break;
            case "if!goto":
                carregarValor(instruccio.getArg1(), "$t0", desplacaments, variablesGlobals);
                afegirInstruccioMips("beq $t0, $zero, " + instruccio.getDesti());
                break;
            case "return":
                if (instruccio.getDesti() != null) {
                    carregarValor(instruccio.getDesti(), "$v0", desplacaments, variablesGlobals);
                }
                afegirInstruccioMips("j " + nomFuncio + "_fi");
                break;
            case "param":
                carregarValor(instruccio.getDesti(), "$t0", desplacaments, variablesGlobals);
                afegirInstruccioMips("addi $sp, $sp, -4");
                afegirInstruccioMips("sw $t0, 0($sp)");
                break;
            case "call":
                generarCall(instruccio, desplacaments, variablesGlobals);
                break;
            case "!":
                carregarValor(instruccio.getArg1(), "$t0", desplacaments, variablesGlobals);
                afegirInstruccioMips("xori $t1, $t0, 1");
                guardarValor("$t1", instruccio.getDesti(), desplacaments, variablesGlobals);
                break;
            case "+":
                generarOperacio("add", instruccio, desplacaments, variablesGlobals);
                break;
            case "-":
                generarOperacio("sub", instruccio, desplacaments, variablesGlobals);
                break;
            case "*":
                generarOperacio("mul", instruccio, desplacaments, variablesGlobals);
                break;
            case "/":
                carregarValor(instruccio.getArg1(), "$t0", desplacaments, variablesGlobals);
                carregarValor(instruccio.getArg2(), "$t1", desplacaments, variablesGlobals);
                afegirInstruccioMips("div $t0, $t1");
                afegirInstruccioMips("mflo $t2");
                guardarValor("$t2", instruccio.getDesti(), desplacaments, variablesGlobals);
                break;
            case "%":
                carregarValor(instruccio.getArg1(), "$t0", desplacaments, variablesGlobals);
                carregarValor(instruccio.getArg2(), "$t1", desplacaments, variablesGlobals);
                afegirInstruccioMips("div $t0, $t1");
                afegirInstruccioMips("mfhi $t2");
                guardarValor("$t2", instruccio.getDesti(), desplacaments, variablesGlobals);
                break;
            case "<":
                generarOperacio("slt", instruccio, desplacaments, variablesGlobals);
                break;
            case ">":
                generarOperacio("sgt", instruccio, desplacaments, variablesGlobals);
                break;
            case "==":
                generarOperacio("seq", instruccio, desplacaments, variablesGlobals);
                break;
            case "!=":
                generarOperacio("sne", instruccio, desplacaments, variablesGlobals);
                break;
            case ">=":
                generarOperacio("sge", instruccio, desplacaments, variablesGlobals);
                break;
            case "<=":
                generarOperacio("sle", instruccio, desplacaments, variablesGlobals);
                break;
            case "&&":
                generarOperacio("and", instruccio, desplacaments, variablesGlobals);
                break;
            case "||":
                generarOperacio("or", instruccio, desplacaments, variablesGlobals);
                break;
            default:
                if (op.startsWith("cast")) {
                    carregarValor(instruccio.getArg1(), "$t0", desplacaments, variablesGlobals);
                    guardarValor("$t0", instruccio.getDesti(), desplacaments, variablesGlobals);
                }
                break;
        }
    }

    private void generarOperacio(String operacioMips, Instruccio instruccio, Map<String, Integer> desplacaments, Set<String> variablesGlobals) {
        carregarValor(instruccio.getArg1(), "$t0", desplacaments, variablesGlobals);
        carregarValor(instruccio.getArg2(), "$t1", desplacaments, variablesGlobals);
        afegirInstruccioMips(operacioMips + " $t2, $t0, $t1");
        guardarValor("$t2", instruccio.getDesti(), desplacaments, variablesGlobals);
    }

    private void generarCall(Instruccio instruccio, Map<String, Integer> desplacaments, Set<String> variablesGlobals) {
        if (instruccio.getArg2() != null) {
            afegirInstruccioMips("jal " + instruccio.getArg1());
            int numArgs = Integer.parseInt(instruccio.getArg2());
            if (numArgs > 0) {
                afegirInstruccioMips("addi $sp, $sp, " + (numArgs * 4));
            }
            guardarValor("$v0", instruccio.getDesti(), desplacaments, variablesGlobals);
        } else {
            afegirInstruccioMips("jal " + instruccio.getDesti());
            int numArgs = Integer.parseInt(instruccio.getArg1());
            if (numArgs > 0) {
                afegirInstruccioMips("addi $sp, $sp, " + (numArgs * 4));
            }
        }
    }

    private void carregarValor(String valor, String registre, Map<String, Integer> desplacaments, Set<String> variablesGlobals) {
        if (esNumero(valor)) {
            afegirInstruccioMips("li " + registre + ", " + valor);
        } else if ("true".equals(valor)) {
            afegirInstruccioMips("li " + registre + ", 1");
        } else if ("false".equals(valor)) {
            afegirInstruccioMips("li " + registre + ", 0");
        } else if (desplacaments.containsKey(valor)) {
            afegirInstruccioMips("lw " + registre + ", " + desplacaments.get(valor) + "($fp)");
        } else {
            afegirInstruccioMips("lw " + registre + ", " + valor);
        }
    }

    private void guardarValor(String registre, String variable, Map<String, Integer> desplacaments, Set<String> variablesGlobals) {
        if (desplacaments.containsKey(variable)) {
            afegirInstruccioMips("sw " + registre + ", " + desplacaments.get(variable) + "($fp)");
        } else {
            afegirInstruccioMips("sw " + registre + ", " + variable);
        }
    }

    private List<String> obtenirVarsLlegides(Instruccio instruccio) {
        List<String> variables = new ArrayList<>();
        String op = instruccio.getOp();
        if (op == null) {
            return variables;
        }

        switch (op) {
            case "goto":
                break;
            case "if!goto":
                variables.add(instruccio.getArg1());
                break;
            case "return":
            case "param":
                variables.add(instruccio.getDesti());
                break;
            case "call":
                break;
            default:
                variables.add(instruccio.getArg1());
                if (instruccio.getArg2() != null) {
                    variables.add(instruccio.getArg2());
                }
                break;
        }
        return variables;
    }

    private String obtenirVariableEscrita(Instruccio instruccio) {
        String op = instruccio.getOp();
        if (op == null) {
            return null;
        }
        switch (op) {
            case "goto":
            case "if!goto":
            case "return":
            case "param":
                return null;
            case "call":
                return (instruccio.getArg2() != null) ? instruccio.getDesti() : null;
            default:
                return instruccio.getDesti();
        }
    }

    private List<String> obtenirTotesVars(Instruccio instruccio) {
        List<String> variables = new ArrayList<>();
        String op = instruccio.getOp();
        if (op == null) {
            return variables;
        }

        switch (op) {
            case "goto":
                break;
            case "if!goto":
                variables.add(instruccio.getArg1());
                break;
            case "return":
            case "param":
                variables.add(instruccio.getDesti());
                break;
            case "call":
                if (instruccio.getArg2() != null) {
                    variables.add(instruccio.getDesti());
                }
                break;
            default:
                variables.add(instruccio.getDesti());
                variables.add(instruccio.getArg1());
                if (instruccio.getArg2() != null) {
                    variables.add(instruccio.getArg2());
                }
                break;
        }
        return variables;
    }

    private boolean esNumero(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        int inici = (text.charAt(0) == '-') ? 1 : 0;
        if (inici >= text.length()) {
            return false;
        }
        for (int i = inici; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String formatarLinia(String linia) {
        if (linia.isEmpty() || linia.startsWith(".") || linia.endsWith(":")) {
            return linia;
        }
        return "    " + linia;
    }

    private void afegirInstruccioMips(String linia) {
        liniesMips.add(linia);
    }
}





