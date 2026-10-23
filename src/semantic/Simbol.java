package semantic;

import java.util.List;

public class Simbol {

    public enum CategoriaSimbol {
        VARIABLE, PARAMETER, FUNCTION
    }

    private final String name;
    private final CategoriaSimbol categoria;
    private final String tipus;
    private final boolean constant;
    private boolean initialized;
    private final int fila;
    private final int columna;

    private List<String> tipusParams;
    private List<Boolean> tipusConst;

    public Simbol(String name, CategoriaSimbol categoria, String tipus, boolean constant, boolean initialized) {
        this(name, categoria, tipus, constant, initialized, -1, -1);
    }

    public Simbol(String name, CategoriaSimbol categoria, String tipus, boolean constant, boolean initialized, int fila, int columna) {
        this.name = name;
        this.categoria = categoria;
        this.tipus = tipus;
        this.constant = constant;
        this.initialized = initialized;
        this.fila = fila;
        this.columna = columna;
    }

    public String getName() {
        return name;
    }

    public CategoriaSimbol getCategoria() {
        return categoria;
    }

    public String getTipus() {
        return tipus;
    }

    public boolean isConstant() {
        return constant;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public List<String> getTipusParams() {
        return tipusParams;
    }

    public void setTipusParams(List<String> tipusParams) {
        this.tipusParams = tipusParams;
    }

    public List<Boolean> getTipusConst() {
        return tipusConst;
    }

    public void setTipusConst(List<Boolean> tipusConst) {
        this.tipusConst = tipusConst;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }
}
