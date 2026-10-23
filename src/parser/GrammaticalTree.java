package parser;


public class GrammaticalTree {

    private final Node root;

    public GrammaticalTree(Node root) {
        this.root = root;
    }

    public Node getRoot() {
        return root;
    }

    public String printTree() {
        return root.toIndentedString();
    }
}
