package app;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import controller.Controller;
import parser.Parser;

public class Main {
    public static void main(String[] args) {
        Path file;
        if (args.length >= 2) {
            boolean esValid = args[0].equalsIgnoreCase("valid");
            String numero = args[1].trim();
            file = Path.of("tests", esValid ? "valid" : "invalid", numero + ".cave");
        } else if (args.length == 1) {
            file = Path.of(args[0]);
        } else {
            // Mode manual per executar amb "play" sense passar arguments.
            // Canvia aquests valors per triar test rapidament.
            String carpetaTestManual = "valid"; // "valid" o "invalid"
            String numeroTestManual = "1";      // ex: "3", "7", "12"
            file = Path.of("tests", carpetaTestManual, numeroTestManual + ".cave");
        }

        String codi;
        try {
            codi = Files.readString(file, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("No he pogut llegir el fitxer: " + file.toAbsolutePath());
            System.err.println(e.getMessage());
            return;
        }

        codi = codi.replace("\r", "");

        Controller controller = new Controller();
        try {
            controller.compile(codi);
        } catch (Parser.ParseException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}