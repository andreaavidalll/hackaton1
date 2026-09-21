package com.tuckersoft.branchengine.decision;

import org.springframework.stereotype.Component;

import java.text.Normalizer;

/**
 * Clasifica el rawInput con reglas propias (nada de IA ni servicios externos), asi que
 * el resultado es siempre el mismo para la misma entrada. El orden de las reglas
 * importa: la primera que matchea gana.
 */
@Component
public class DecisionClassifier {

    public record Resultado(String branchType, String handlerUnit, String outcomeCode) {
    }

    public Resultado clasificar(String rawInput) {
        String texto = normalizar(rawInput);
        String branchType = resolverBranchType(texto);
        return switch (branchType) {
            case "OBEDIENCIA" -> new Resultado(branchType, "Mesa de Guion", "ADVANCE_MAIN_PATH");
            case "REBELDIA" -> new Resultado(branchType, "Control de Continuidad", "FORK_TIMELINE");
            case "SOSPECHA" -> new Resultado(branchType, "Oficina de Seguridad", "INJECT_WHITE_BEAR_SYMBOL");
            case "RUPTURA_CUARTA_PARED" -> new Resultado(branchType, "Departamento Netflix", "BREAK_FOURTH_WALL");
            default -> new Resultado(branchType, "Archivo de Errores", "DISCARD_INPUT");
        };
    }

    private String resolverBranchType(String texto) {
        if (!texto.matches(".*[a-z].*")) {
            return "ENTRADA_CORRUPTA";
        }
        if (contieneAlguna(texto, "netflix", "camara", "espectador", "videojuego")) {
            return "RUPTURA_CUARTA_PARED";
        }
        if (contieneAlguna(texto, "vigilan", "simbolo", "conspiracion")) {
            return "SOSPECHA";
        }
        if (contieneAlguna(texto, "rechaza", "destruye", "desobedece", "renuncia")) {
            return "REBELDIA";
        }
        return "OBEDIENCIA";
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private boolean contieneAlguna(String texto, String... palabrasClave) {
        for (String palabra : palabrasClave) {
            if (texto.contains(palabra)) {
                return true;
            }
        }
        return false;
    }
}
