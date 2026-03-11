package com.tobyink.millionhorses.config;

import com.google.gson.*;
import dev.architectury.platform.Platform;

import java.io.*;
import java.nio.file.*;

/**
 * Configuración del mod — lee/escribe config/millionhorses.json
 *
 * Ejemplo de millionhorses.json:
 * {
 *   "allowVanillaHorseSpawns": false
 * }
 */
public class ModConfig {

    private static final Path CONFIG_PATH =
            Platform.getConfigFolder().resolve("millionhorses.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ── Valores con defaults ─────────────────────────────────────────────────
    public static boolean allowVanillaHorseSpawns = false;

    // ── Carga / guardado ─────────────────────────────────────────────────────

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save(); // Crear con defaults si no existe
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("allowVanillaHorseSpawns")) {
                allowVanillaHorseSpawns = obj.get("allowVanillaHorseSpawns").getAsBoolean();
            }
        } catch (Exception e) {
            System.err.println("[MillionHorses] Error leyendo config, usando defaults: " + e.getMessage());
            save(); // Sobreescribir con defaults si el archivo está corrupto
        }
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("allowVanillaHorseSpawns", allowVanillaHorseSpawns);
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(obj, writer);
            }
        } catch (Exception e) {
            System.err.println("[MillionHorses] Error guardando config: " + e.getMessage());
        }
    }
}