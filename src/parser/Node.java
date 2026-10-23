package parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Node {

    private final String symbol;
    private final List<Node> children;
    private int fila;
    private int columna;

    public Node(String symbol) {
        this.symbol = symbol;
        this.children = new ArrayList<>();
    }

    public Node(String symbol, int fila, int columna) {
        this.symbol = symbol;
        this.children = new ArrayList<>();
        this.fila = fila;
        this.columna = columna;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    public void addChild(Node child) {
        if (child != null) {
            children.add(child);
        }
    }

    public String getSymbol() {
        return symbol;
    }

    public List<Node> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public String toIndentedString() {
        //StringBuilder builder = new StringBuilder();
        //appendIndented(builder, 0);

        StringBuilder builder = new StringBuilder();
        appendTree(builder, "", true);
        System.out.println(builder.toString());
        return builder.toString();
    }

    private void appendIndented(StringBuilder builder, int depth) {

        builder.append("  ".repeat(depth)).append(symbol).append(System.lineSeparator());
        for (Node child : children) {
            child.appendIndented(builder, depth + 1);
        }
    }
    private void appendTree(StringBuilder builder, String prefix, boolean isLast) {
        builder.append(prefix);

        if (isLast) {
            builder.append("└── ");
            prefix += "    ";
        } else {
            builder.append("├── ");
            prefix += "│   ";
        }

        builder.append(symbol).append(System.lineSeparator());

        for (int i = 0; i < children.size(); i++) {
            children.get(i).appendTree(builder, prefix, i == children.size() - 1);
        }
    }

}
