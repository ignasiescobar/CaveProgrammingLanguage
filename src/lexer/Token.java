package lexer;

public class Token {

    private final String tokenType;
    private final String lexema;
    private final int fila;
    private final int columna;

    public Token(String tokenType, String lexema, int fila, int columna) {
        this.tokenType = tokenType;
        this.lexema = lexema;
        this.fila = fila;
        this.columna = columna;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getLexema() {
        return lexema;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    public String toString() {
        final boolean USE_COLOR = System.getenv("NO_COLOR") == null;
        final String RESET = "\u001B[0m";
        final String MAGENTA = "\u001B[35m";
        final String CYAN = "\u001B[36m";
        final String YELLOW = "\u001B[33m";
        String tokenCol = tokenType;
        String lexCol = lexema;
        String posCol = String.format("(%d,%d)", fila, columna);
        if (USE_COLOR) {
            tokenCol = MAGENTA + tokenType + RESET;
            lexCol = CYAN + lexema + RESET;
            posCol = YELLOW + posCol + RESET;
        }
        return "TokenID:" + tokenCol + " Lexema:[" + lexCol + "] " + posCol;
    }
}