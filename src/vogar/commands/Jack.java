/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vogar.commands;

import com.google.common.collect.Lists;
import vogar.Log;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import vogar.util.Strings;

/**
 * Runs the Jack compiler to generate dex files.
 */
public class Jack {

    private static final File JACK_JAR;
    private static final File JILL_JAR;

    // Initialise the jar files for jack and jill, letting them be null if the files
    // cannot be found.
    static {
        String sdkTop = System.getenv("ANDROID_BUILD_TOP");

        final File jackJar = new File(sdkTop + "/prebuilts/sdk/tools/jack.jar");
        final File jillJar = new File(sdkTop + "/prebuilts/sdk/tools/jill.jar");

        // If the system environment variable JACK_JAR is set then use that,
        // otherwise find the jar relative to the AOSP source.
        String jackJarEnv = System.getenv("JACK_JAR");

        final File jackJarFromEnv = (jackJarEnv != null) ? new File(jackJarEnv) : null;

        if (jackJarEnv != null && jackJarFromEnv.exists()) {
            JACK_JAR = jackJarFromEnv;
        } else {
            if (!jackJar.exists()) {
                JACK_JAR = null;
            } else {
                JACK_JAR = jackJar;
            }
        }

        if (!jillJar.exists()) {
            JILL_JAR = null;
        } else {
            JILL_JAR = jillJar;
        }
    }

    /**
     * Get an instance of the jack compiler with appropriate path settings.
     *
     * @return an instance of a jack compiler with appropriate paths to its dependencies if needed.
     * @throws IllegalStateException when the jack library cannot be found.
     */
    public static Jack getJackCompiler(Log log) throws IllegalStateException {
        if (JACK_JAR != null) {
            // Configure jack compiler with right JACK_JAR path.
            return new Jack(log, Lists.newArrayList("java", "-jar", JACK_JAR.getAbsolutePath()));
        } else {
            throw new IllegalStateException("Jack library not found, cannot use jack.");
        }
    }

    /**
     * Converts a jar file to a jack library.
     * @param jarPath The jar file to convert.
     * @return A string with the path to the newly converted jack library.
     * @throws IllegalArgumentException If the given jarPath parameter isn't there.
     * @throws CommandFailedException   If there was an error during the conversion.
     */
    public static String convertJarToJackLib(Log log, String jarPath) {
        File jar = new File(jarPath);
        if (!jar.exists()) {
            throw new IllegalArgumentException("No such jar file to convert: " + jarPath);
        }

        String outPath = jarPath.replaceAll("\\.jar$", ".jack");
        if (JILL_JAR != null) {
            try {
                new Command.Builder(log).args("java", "-jar", JILL_JAR.getAbsolutePath(), jarPath,
                        "--output", outPath).execute();
            } catch (CommandFailedException cfe) {
                System.out.println("There was an error converting " + jarPath + " to a jack library: "
                        + cfe.getMessage());
                throw cfe;
            }
        } else {
            throw new CommandFailedException(
                    Collections.<String>emptyList(),
                    Collections.singletonList("Jill could not be found, did you run lunch?"));
        }

        return outPath;
    }

    private final Command.Builder builder;

    public Jack(Log log, String jackArgs) {
        this.builder = new Command.Builder(log);
        builder.args(jackArgs);
    }

    public Jack(Log log, Collection<String> jackArgs) {
        this.builder = new Command.Builder(log);
        builder.args(jackArgs);
    }

    public Jack importFile(String path) {
        builder.args("--import", path);
        return this;
    }

    public Jack importMeta(String dir) {
        builder.args("--import-meta", dir);
        return this;
    }

    public Jack importResource(String dir) {
        builder.args("--import-resource", dir);
        return this;
    }

    public Jack incrementalFolder(String dir) {
        builder.args("--incremental--folder", dir);
        return this;
    }

    public Jack multiDex(String mode) {
        builder.args("--multi-dex", mode);
        return this;
    }

    public Jack outputDex(String dir) {
        builder.args("--output-dex", dir);
        return this;
    }

    public Jack outputJack(String path) {
        builder.args("--output-jack", path);
        return this;
    }

    public Jack processor(String names) {
        builder.args("--processor", names);
        return this;
    }

    public Jack processorPath(String path) {
        builder.args("--processorpath", path);
        return this;
    }

    public Jack verbose(String mode) {
        builder.args("--verbose", mode);
        return this;
    }

    public Jack addAnnotationProcessor(String processor) {
        builder.args("-A", processor);
        return this;
    }

    public Jack setProperty(String property) {
        builder.args("-D", property);
        return this;
    }

    public Jack setClassPath(String classPath) {
        builder.args("-cp", classPath);
        return this;
    }

    public Jack setDebug() {
        builder.args("-g");
        return this;
    }

    public Jack setEnvVar(String key, String value) {
        builder.env(key, value);
        return this;
    }

    /**
     * Runs the command with the preconfigured options on Jack, and returns the outcome.
     * This method does not dirty the existing Jack instance, and can be safely reused
     * to compile other files.
     * @param files The files to compile.
     * @return A list of output lines from running the command.
     */
    public List<String> compile(Collection<File> files) {
        return new Command.Builder(builder)
                .args((Object[]) Strings.objectsToStrings(files))
                .execute();
    }

}