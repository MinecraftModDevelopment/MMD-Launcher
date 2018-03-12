package com.mcmoddev.launcher.mod;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class Mod {
    private String name;
    private String fileName;
    private String url;
    private String sha256;
    private ModType modType;

    private boolean hasConfig;
    private ModConfig[] configs;

    public Mod(String name, JsonObject object) {
        this.name = name;
        this.fileName = object.get("file").getAsString();
        this.url = object.has("url") ? object.get("url").getAsString() : null;
        this.sha256 = object.has("sha256") ? object.get("sha256").getAsString().toLowerCase(Locale.ENGLISH) : null;
        this.modType = object.has("type") ? ModType.valueOf(object.get("type").getAsString().toUpperCase(Locale.ENGLISH)) : ModType.MOD;

        this.hasConfig = object.has("config");
        if (this.hasConfig) {
            final JsonArray array = object.get("config").getAsJsonArray();
            this.configs = new ModConfig[array.size()];
            for (int i = 0; i < array.size(); i++) {
                this.configs[i] = new ModConfig(array.get(i).getAsJsonObject());
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public String getURL() {
        return url;
    }

    public String getSHA256() {
        return sha256;
    }

    public ModType getModType() {
        return modType;
    }

    public boolean hasConfig() {
        return hasConfig;
    }

    public ModConfig[] getConfigs() {
        return configs;
    }

    public boolean doDownload(File file) {
        if (this.getURL() == null) {
            return false;
        } else if (!file.exists()) {
            return true;
        } else {
            try {
                final String obtained_sha256 = Files.asByteSource(file).hash(Hashing.sha256()).toString();
                return this.getSHA256() != null && !this.getSHA256().equals(obtained_sha256);
            } catch (IOException e) {
                e.printStackTrace();
                return true;
            }
        }
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
