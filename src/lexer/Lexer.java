package lexer;

import java.util.*;

public class Lexer {

    private final String codi;
    private final List<String> errors = new ArrayList<>();
    private int currPos;
    private final int lengthCodi;

    private int linia;
    private int columna;

    private enum Estat {
        INICIAL,
        ID,
        NUMBER,
        MONOTOKENS
    }

    private static final Set<String> EXPO_TOKENS = Set.of("piedra", "roca", "montaña");

    private static final Map<String, String> TOKENS = new HashMap<>();

    static {
        TOKENS.put("ooga", "I8");
        TOKENS.put("booga", "I16");
        TOKENS.put("cabooga", "I32");
        TOKENS.put("cauabooga", "I64");
        TOKENS.put("nada", "VOID");
        TOKENS.put("OOGACHACKA", "MAIN");

        TOKENS.put("pasar", "IF");
        TOKENS.put("nopasar", "ELSE");
        TOKENS.put("mientras", "WHILE");
        TOKENS.put("acasa", "RETURN");
        TOKENS.put("parar", "BREAK");
        TOKENS.put("seguir", "CONTINUE");
        TOKENS.put("notocar", "CONST");

        TOKENS.put("sumar", "SUM");
        TOKENS.put("quitar", "MIN");
        TOKENS.put("partir", "DIV");
        TOKENS.put("veces", "MUL");
        TOKENS.put("sobra", "MOD");
        TOKENS.put("ser", "EQ");
        TOKENS.put("masuno", "INC");
        TOKENS.put("menosuno", "DEC");

        TOKENS.put("granverdad", "TRUE");
        TOKENS.put("granmentira", "FALSE");
        TOKENS.put("y", "AND");
        TOKENS.put("o", "OR");
        TOKENS.put("no", "NOT");
        TOKENS.put("cambiar", "CAST");

        TOKENS.put("pequenomismo", "ISLESSOREQTHAN");
        TOKENS.put("grandemismo", "ISMOREOREQTHAN");
        TOKENS.put("nomismo", "ISNOTEQ");
        TOKENS.put("mismo", "ISEQ");
        TOKENS.put("pequeno", "ISLESSTHAN");
        TOKENS.put("grande", "ISMORETHAN");

        TOKENS.put("todoslados", "GLOBAL");
    }

    public Lexer(String sourceCode) {
        this.codi = sourceCode == null ? "" : sourceCode;
        this.currPos = 0;
        this.lengthCodi = this.codi.length();
        this.linia = 1;
        this.columna = 1;
    }

    private void finalitzarNumero(List<Token> tokens, StringBuilder buffer, int currLinia, int currColumna) {
        String numero = buffer.toString();
        int ultimaPos = currPos;
        while (currPos < lengthCodi && codi.charAt(currPos) == ' ') {
            currPos++;
        }
        String cadena = null;
        if (currPos < lengthCodi) {
            char ch = codi.charAt(currPos);
            if (Character.isLetter(ch) || ch == '_') {
                StringBuilder expoBuffer = new StringBuilder();
                while (currPos < lengthCodi) {
                    char chExpo = codi.charAt(currPos);
                    if (Character.isLetter(chExpo) || chExpo == '_') {
                        expoBuffer.append(chExpo);
                        currPos++;
                    } else {
                        break;
                    }
                }
                cadena = expoBuffer.toString();
            }
        }
        if (cadena != null && EXPO_TOKENS.contains(cadena)) {
            tokens.add(new Token("DIGIT", numero, currLinia, currColumna));
            tokens.add(new Token("EXPO", cadena, linia, columna));
        } else {
            currPos = ultimaPos;
            tokens.add(new Token("DIGIT", numero, currLinia, currColumna));
        }
    }

    public List<Token> tokenize() {
        errors.clear();
        List<Token> tokens = new ArrayList<>();
        Estat estat = Estat.INICIAL;
        StringBuilder buffer = new StringBuilder();

        int currLinia = 1;
        int currColumna = 1;

        while (currPos < lengthCodi) {
            char c = codi.charAt(currPos);

            switch (estat) {

                case INICIAL:

                    if (c == ' ' || c == '\t') {
                        currPos++;
                        columna++;
                    } else if (c == '\n') {
                        currPos++;
                        linia++;
                        columna = 1;
                    } else if (Character.isLetter(c) || c == '_') {
                        buffer.setLength(0);
                        buffer.append(c);
                        currLinia = linia;
                        currColumna = columna;
                        estat = Estat.ID;
                        currPos++;
                        columna++;
                    } else if (Character.isDigit(c)) {
                        buffer.setLength(0);
                        buffer.append(c);
                        currLinia = linia;
                        currColumna = columna;
                        estat = Estat.NUMBER;
                        currPos++;
                        columna++;
                    } else if (c == '/' && currPos + 1 < lengthCodi && codi.charAt(currPos + 1) == '/') {
                        currPos += 2;
                        columna += 2;
                        while (currPos < lengthCodi && codi.charAt(currPos) != '\n') {
                            currPos++;
                            columna++;
                        }
                    } else {
                        estat = Estat.MONOTOKENS;
                    }
                    break;

                case MONOTOKENS:

                    currLinia = linia;
                    currColumna = columna;
                    switch (c) {
                        case '(':
                            tokens.add(new Token("LPAREN", "(", currLinia, currColumna));
                            break;
                        case ')':
                            tokens.add(new Token("RPAREN", ")", currLinia, currColumna));
                            break;
                        case '{':
                            tokens.add(new Token("LBRACE", "{", currLinia, currColumna));
                            break;
                        case '}':
                            tokens.add(new Token("RBRACE", "}", currLinia, currColumna));
                            break;
                        case ',':
                            tokens.add(new Token("COMMA", ",", currLinia, currColumna));
                            break;
                        default:
                            errors.add("Error lèxic: token invàlid '" + c + "' a " + currLinia + ":" + currColumna);
                            break;
                    }
                    currPos++;
                    columna++;
                    estat = Estat.INICIAL;
                    break;

                case ID:

                    if ((Character.isLetter(c) || Character.isDigit(c)) || c == '_') {
                        buffer.append(c);
                        currPos++;
                        columna++;
                    } else {
                        String word = buffer.toString();
                        String type;
                        if (TOKENS.containsKey(word)) {
                            type = TOKENS.get(word);
                        } else {
                            type = "ID";//poso per default sempre el ID, al parser ja sortira error
                        }
                        tokens.add(new Token(type, word, currLinia, currColumna));
                        estat = Estat.INICIAL;
                    }
                    break;

                case NUMBER:

                    if (Character.isDigit(c)) {
                        buffer.append(c);
                        currPos++;
                        columna++;
                        while (currPos < lengthCodi && Character.isDigit(codi.charAt(currPos))) {
                            buffer.append(codi.charAt(currPos));
                            currPos++;
                            columna++;
                        }
                        errors.add("Error lèxic: número amb més d'un dígit '" + buffer.toString() + "' a " + currLinia + ":" + currColumna + " (només 0-9 permès)");
                        finalitzarNumero(tokens, buffer, currLinia, currColumna);
                        estat = Estat.INICIAL;
                    } else {
                        finalitzarNumero(tokens, buffer, currLinia, currColumna);
                        estat = Estat.INICIAL;
                    }
                    break;
            }
        }

        // Darrer token si el fitxer acaba dins d'ID o NUMBER
        if (estat == Estat.ID && buffer.length() > 0) {
            String word = buffer.toString();
            String type = TOKENS.containsKey(word) ? TOKENS.get(word) : "ID";
            tokens.add(new Token(type, word, currLinia, currColumna));
        } else if (estat == Estat.NUMBER && buffer.length() > 0) {
            finalitzarNumero(tokens, buffer, currLinia, currColumna);
        }

        tokens.add(new Token("EOF", "", linia, columna));
        return tokens;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void printErrors() {
        for (String error : errors) {
            System.out.println(error);
        }
    }
}