// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads native libraries from the fat JAR's embedded resources.
 *
 * <p>At runtime, detects OS + architecture, extracts the matching native
 * libraries from {@code natives/<os-arch>/} inside the JAR to a temp
 * directory, then calls {@link System#load(String)}.</p>
 *
 * <p>Supports:
 * <ul>
 *   <li>linux-x86_64 (.so)</li>
 *   <li>macos-aarch64 (.dylib)</li>
 *   <li>macos-x86_64 (.dylib)</li>
 *   <li>windows-x86_64 (.dll)</li>
 * </ul>
 *
 * <p>This class is used by {@link CosmosNativeBridge} in its static initializer
 * as a fallback when {@code System.loadLibrary} cannot find the native libs
 * on {@code java.library.path}.</p>
 */
final class NativeLoader {

    private static volatile boolean loaded = false;

    private NativeLoader() {}

    /**
     * Loads both native libraries (Layer 2 C-ABI + Layer 3 JNI glue).
     * Safe to call multiple times — only loads once.
     */
    static synchronized void load() {
        if (loaded) {
            return;
        }

        String platform = detectPlatform();
        String ext = detectExtension();

        // Load Layer 2 first (C-ABI), then Layer 3 (JNI glue that depends on it)
        loadFromJar(platform, "azurecosmos", ext);
        loadFromJar(platform, "azurecosmos_jni", ext);

        loaded = true;
    }

    private static void loadFromJar(String platform, String libName, String ext) {
        String prefix = isWindows() ? "" : "lib";
        String fileName = prefix + libName + ext;
        String resourcePath = "/natives/" + platform + "/" + fileName;

        try (InputStream is = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                // Not in JAR — fall back to java.library.path
                System.loadLibrary(libName);
                return;
            }

            // Extract to temp directory
            Path tempDir = Files.createTempDirectory("cosmos-native-");
            tempDir.toFile().deleteOnExit();

            Path tempFile = tempDir.resolve(fileName);
            tempFile.toFile().deleteOnExit();

            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            System.load(tempFile.toAbsolutePath().toString());

        } catch (IOException e) {
            throw new UnsatisfiedLinkError(
                    "Failed to extract native library " + resourcePath + ": " + e.getMessage());
        }
    }

    private static String detectPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        String osName;
        if (os.contains("linux")) {
            osName = "linux";
        } else if (os.contains("mac") || os.contains("darwin")) {
            osName = "macos";
        } else if (os.contains("win")) {
            osName = "windows";
        } else {
            throw new UnsatisfiedLinkError("Unsupported OS: " + os);
        }

        String archName;
        if (arch.equals("amd64") || arch.equals("x86_64")) {
            archName = "x86_64";
        } else if (arch.equals("aarch64") || arch.equals("arm64")) {
            archName = "aarch64";
        } else {
            throw new UnsatisfiedLinkError("Unsupported architecture: " + arch);
        }

        return osName + "-" + archName;
    }

    private static String detectExtension() {
        if (isWindows()) return ".dll";
        if (isMac()) return ".dylib";
        return ".so";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static boolean isMac() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac") || os.contains("darwin");
    }
}
