package com.mcmoddev.launcher.mod;

import java.io.File;

import com.mcmoddev.launcher.Launcher;

public enum ModType {
    MOD(Launcher.INSTANCE.modsDir),
    COREMOD(Launcher.INSTANCE.coreModsDir),
    ROOT(Launcher.INSTANCE.dataDir);

    private File file;

    ModType(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }
}
