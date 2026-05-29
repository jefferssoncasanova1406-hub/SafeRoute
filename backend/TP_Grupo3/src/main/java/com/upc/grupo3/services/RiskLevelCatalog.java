package com.upc.grupo3.services;

import com.upc.grupo3.exceptions.ApplicationConfigurationException;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RiskLevelCatalog {

    public static final int LOW = 1;
    public static final int MEDIUM = 2;
    public static final int HIGH = 3;

    private static final Map<Integer, RiskLevel> LEVELS = Map.of(
            LOW, new RiskLevel("bajo", "#22C55E"),
            MEDIUM, new RiskLevel("medio", "#F59E0B"),
            HIGH, new RiskLevel("alto", "#DC2626"));

    public int defaultLevel() {
        return LOW;
    }

    public boolean isSupported(Integer riskLevel) {
        return LEVELS.containsKey(riskLevel);
    }

    public String name(Integer riskLevel) {
        return resolve(riskLevel).name();
    }

    public String color(Integer riskLevel) {
        return resolve(riskLevel).color();
    }

    private RiskLevel resolve(Integer riskLevel) {
        RiskLevel level = LEVELS.get(riskLevel);
        if (level == null) {
            throw new ApplicationConfigurationException("El nivel de riesgo no esta soportado");
        }
        return level;
    }

    private record RiskLevel(String name, String color) {
    }
}
