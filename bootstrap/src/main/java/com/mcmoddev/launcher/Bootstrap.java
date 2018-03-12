package com.mcmoddev.launcher;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mcmoddev.launcher.version.Version;
import com.mcmoddev.launcher.version.VersionAdapter;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Bootstrap {
    public static final String URL = "https://raw.githubusercontent.com/MinecraftModDevelopment/MMD-Launcher/gh-pages/update.json";

    public File dataDir;
    public JsonParser jsonParser;
    public Gson gson;

    public Version currentVersion;
    public String currentSHA256;

    public File bootstrapFile;
    public File launcherFile;

    public Version newerVersion;
    public String newerURL;

    public ProgressBar progressbar;
    public String[] args;

    public static void main(String[] args) {
        final List<String> argumentList = Arrays.asList(args);
        final Bootstrap bootstrap = new Bootstrap(argumentList.contains("--portable") || argumentList.contains("-p"));
        bootstrap.args = args;

        try {
            bootstrap.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bootstrap(boolean portable) {
        this.dataDir = portable ? new File(".") : this.getDataFolder();
        this.jsonParser = new JsonParser();
        this.gson = new GsonBuilder().registerTypeAdapter(Version.class, new VersionAdapter()).setPrettyPrinting().create();

        final File bootstrapDir = new File(this.dataDir, "bootstrap");
        this.bootstrapFile = new File(bootstrapDir, "bootstrap.json");
        this.launcherFile = new File(bootstrapDir, "launcher.jar");

        if (bootstrapDir.exists()) {
            if (this.bootstrapFile.exists()) {
                try {
                    final JsonObject object = this.jsonParser.parse(new FileReader(this.bootstrapFile)).getAsJsonObject();
                    this.currentVersion = new Version(object.get("version").getAsString());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (this.launcherFile.exists()) {
                try {
                    this.currentSHA256 = Files.asByteSource(this.launcherFile).hash(Hashing.sha256()).toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (!bootstrapDir.mkdirs()) {
                throw new RuntimeException("Unable to create data directory");
            }
        }

        this.progressbar = new ProgressBar();
    }

    public void start() throws IOException {
        final Map<Version, JsonObject> map = this.gson.fromJson(new InputStreamReader(new URL(Bootstrap.URL).openStream()), new TypeToken<Map<Version, JsonObject>>() {}.getType());
        for (Map.Entry<Version, JsonObject> entry : map.entrySet()) {
            int compare = entry.getKey().compareTo(this.currentVersion);
            if (compare > 0) {
                if (this.newerVersion != null) {
                    if (entry.getKey().compareTo(this.newerVersion) > 0) {
                        this.newerVersion = entry.getKey();
                        this.newerURL = entry.getValue().get("url").getAsString();
                    }
                } else {
                    this.newerVersion = entry.getKey();
                    this.newerURL = entry.getValue().get("url").getAsString();
                }
            } else if (compare == 0) {
                final String actualSHA256 = entry.getValue().get("sha256").getAsString();
                if (!this.currentSHA256.equals(actualSHA256)) {
                    this.newerVersion = entry.getKey();
                    this.newerURL = entry.getValue().get("url").getAsString();
                }
            }
        }

        if (this.newerVersion != null) {
            System.out.println("Found newer version: " + this.newerVersion + " (currently " + this.currentVersion + ")");
            if (this.launcherFile.exists()) {
                System.out.println("Removing old jar");
                if (!this.launcherFile.delete()) {
                    throw new RuntimeException("Failed to remove old jar");
                }
            }
            this.progressbar.display(this.newerURL, this.launcherFile, () -> {
                final JsonObject object = new JsonObject();
                object.addProperty("version", Bootstrap.this.newerVersion.get());
                final String json = Bootstrap.this.gson.toJson(object);
                try {
                    FileUtils.writeStringToFile(Bootstrap.this.bootstrapFile, json, Charsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Update complete!");
                Bootstrap.this.launch();
            });
        } else {
            this.launch();
        }
    }

    public void launch() {
        String[] arguments = {"java", "-jar", this.launcherFile.getAbsolutePath()};
        arguments = this.concat(arguments, this.args);
        final ProcessBuilder process = new ProcessBuilder(arguments);
        process.directory(this.dataDir);
        try {
            process.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] concat(String[] a, String[] b) {
        final int aLength = a.length;
        final int bLength = b.length;
        final String[] array = new String[aLength + bLength];
        System.arraycopy(a, 0, array, 0, aLength);
        System.arraycopy(b, 0, array, aLength, bLength);
        return array;
    }

    public File getDataFolder() {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return new File(System.getenv("APPDATA"), ".mmd-launcher");
        } else if (osName.contains("mac")) {
            return new File(System.getProperty("user.home"), "/Library/Application Support/mmd-launcher");
        } else {
            return new File(System.getProperty("user.home"), ".mmd-launcher");
        }
    }
}
