package controller;

import MIPS.MIPS;
import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.GrammaticalTree;
import semantic.TaulaSimbols;
import tac.TAC;
import java.util.List;

public class Controller {
    private static final String RESET = "\u001B[0m";
    private static final String COLOR_LEXER = "\u001B[33m";
    private static final String COLOR_PARSER = "\u001B[35m";
    private static final String COLOR_SEMANTICA = "\u001B[31m";

    public Controller() {
    }

    public void compile(String codi) {
        Lexer activeLexer = new Lexer(codi);
        List<Token> tokens = activeLexer.tokenize();
        List<String> errorsLexer = activeLexer.getErrors();

        Parser parser = new Parser(tokens);
        GrammaticalTree tree = parser.parseProgram();
        List<String> errorsParser = parser.getErrors();
        TaulaSimbols taulaSimbols = new TaulaSimbols();
        taulaSimbols.analitzar(tree);
        List<String> errorsSemantica = taulaSimbols.getErrors();

        boolean senseErrorsAbansTac = errorsLexer.isEmpty() && errorsParser.isEmpty() && errorsSemantica.isEmpty();
        if (!senseErrorsAbansTac) {
            if (!errorsLexer.isEmpty()) {
                System.out.println(COLOR_LEXER + "[Errors LEXER]" + RESET);
            }
            activeLexer.printErrors();
            if (!errorsParser.isEmpty()) {
                System.out.println(COLOR_PARSER + "[Errors PARSER]" + RESET);
            }
            parser.printErrors();
            if (!errorsSemantica.isEmpty()) {
                System.out.println(COLOR_SEMANTICA + "[Errors SEMANTICA]" + RESET);
            }
            taulaSimbols.printErrors();
            return;
        }
        taulaSimbols.printAll();

        tree.printTree();
        TAC tac = new TAC();
        tac.generarTAC(tree);
        tac.print();

        MIPS generadorMips = new MIPS(tac);
        generadorMips.generarMips();
        generadorMips.print();
    }

}
