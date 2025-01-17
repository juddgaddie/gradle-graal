/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.graal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Downloads GraalVM binaries. */
public class DownloadGraalTask extends DefaultTask {

    private static final String ARTIFACT_PATTERN = "[url]/vm-[version]/graalvm-ce-[os]-[arch]-[version].tar.gz";
    private static final String FILENAME_PATTERN = "graalvm-ce-[arch]-[version].tar.gz";

    private final Property<String> graalVersion = getProject().getObjects().property(String.class);
    private final Property<String> downloadBaseUrl = getProject().getObjects().property(String.class);
    private final Property<Path> cacheDir = getProject().getObjects().property(Path.class);

    public DownloadGraalTask() {
        setGroup(GradleGraalPlugin.TASK_GROUP);
        setDescription("Downloads and caches GraalVM binaries.");

        onlyIf(task -> !getTgz().get().getAsFile().exists());
    }

    @TaskAction
    public final void downloadGraal() throws IOException {
        Path cache = getCacheSubdirectory().get();
        Files.createDirectories(cache);
        try (InputStream in = new URL(render(ARTIFACT_PATTERN)).openStream()) {
            Files.copy(in, getTgz().get().getAsFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @OutputFile
    public final Provider<RegularFile> getTgz() {
        return getProject().getLayout()
                .file(getCacheSubdirectory().map(dir -> dir.resolve(render(FILENAME_PATTERN)).toFile()));
    }

    @Input
    public final Provider<String> getGraalVersion() {
        return graalVersion;
    }

    public final void setGraalVersion(Provider<String> provider) {
        graalVersion.set(provider);
    }

    @Input
    public final Provider<String> getDownloadBaseUrl() {
        return downloadBaseUrl;
    }

    public final void setDownloadBaseUrl(Provider<String> provider) {
        downloadBaseUrl.set(provider);
    }

    private Provider<Path> getCacheSubdirectory() {
        return cacheDir.map(dir -> dir.resolve(graalVersion.get()));
    }

    private String render(String pattern) {
        return pattern
                .replaceAll("\\[url\\]", downloadBaseUrl.get())
                .replaceAll("\\[version\\]", graalVersion.get())
                .replaceAll("\\[os\\]", getOperatingSystem())
                .replaceAll("\\[arch\\]", getArchitecture());
    }

    private String getOperatingSystem() {
        switch (Platform.operatingSystem()) {
            case MAC:
                return "macos";
            case LINUX:
                return "linux";
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem());
        }
    }

    private String getArchitecture() {
        switch (Platform.architecture()) {
            case AMD64:
                return "amd64";
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.architecture());
        }
    }

    final void setCacheDir(Path value) {
        cacheDir.set(value);
    }
}
