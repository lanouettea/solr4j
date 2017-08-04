/*
 * #%L
 * Solr4J
 * %%
 * Copyright (C) 2017 André Lanouette
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.andrelanouette.solr4j;

import com.andrelanouette.exec.ManagedProcess;
import com.andrelanouette.exec.ManagedProcessBuilder;
import com.andrelanouette.exec.ManagedProcessException;
import com.andrelanouette.exec.OutputStreamLogDispatcher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Pattern;

/**
 * //TODO: Document this class; Solr4j
 *
 * @author Andre Lanouette
 * @since 10/07/2017
 */
public class Solr4J {
    //region  CONSTANTS DECLARATIONS

    private final Logger logger;

    private final String FOUND_PROCESS_REGEXP_FORMAT = ".*Found Solr process \\d+ running on port %d.*";
    private static final String SOLR_VERSION_FILE = ".solr-version";

    //endregion

    //region MEMBER DECLARATIONS

    protected Solr4JConfiguration configuration;

    private File baseDir;

    protected int solrStartMaxWaitTimeMs = 30000;
    private ManagedProcess stopProcess;

    //endregion

    //region CONSTRUCTORS
    protected Solr4J(Solr4JConfiguration configuration, Logger logger) {
        if (logger == null) {
            logger = LoggerFactory.getLogger(Solr4J.class);
        }
        this.logger = logger;
        this.configuration = configuration;
    }

    public static Solr4J newEmbeddedSolr4J(Solr4JConfiguration solr4JConfiguration, Logger logger) throws ManagedProcessException {
        Solr4J solr4J = new Solr4J(solr4JConfiguration, logger);
        solr4J.prepareDirectories();
        solr4J.unpackEmbeddedSolrServer();
        return solr4J;
    }


    public static Solr4J newEmbeddedSolr4J(int port, Logger logger) throws ManagedProcessException {
        Solr4JConfigurationBuilder solr4JConfigurationBuilder = Solr4JConfigurationBuilder.newBuilder();
        solr4JConfigurationBuilder.setPort(port);
        return newEmbeddedSolr4J(solr4JConfigurationBuilder.build(), logger);
    }

    public static Solr4J newEmbeddedSolr4J(Solr4JConfiguration solr4JConfiguration) throws ManagedProcessException {
        return newEmbeddedSolr4J(solr4JConfiguration, null);
    }

    public static Solr4J newEmbeddedSolr4J(int port) throws ManagedProcessException {
        return newEmbeddedSolr4J(port, null);
    }

    //endregion

    //region BASE METHODS

    public synchronized boolean isAlreadyRunning() throws ManagedProcessException {
        File executable = newExecutableFile("bin", "solr");
        if (!executable.exists()) {     // Prevent filenotfound when checking if running before unpacking the distribution.
            return false;
        }
        ManagedProcessBuilder builder = new ManagedProcessBuilder(executable);
        builder.setOutputStreamLogDispatcher(getOutputStreamLogDispatcher("solr"));
        builder.setDestroyOnShutdown(false);
        builder.addArgument("status");
        builder.setExitValues(new int[]{0, 2, 3});

        ManagedProcess statusProcess = builder.build();
        statusProcess.start();
        statusProcess.waitForExit();

        String console = statusProcess.getConsole();
        return Pattern.compile(String.format(FOUND_PROCESS_REGEXP_FORMAT, configuration.getPort())).matcher(console).find();
    }


    /**
     * Starts up the Solr server, using the base directory and port specified in the configuration.
     *
     * @throws ManagedProcessException if something fatal went wrong
     */
    public synchronized void start(boolean restartIfAlreadyRunning) throws ManagedProcessException {
        logger.info("Starting up the Solr server...");

        boolean isAlreadyRunning = isAlreadyRunning();

        if (isAlreadyRunning) {
            if (restartIfAlreadyRunning) {
                stop();
            } else {
                throw new ManagedProcessException(String.format("Solr was already running on port %d", configuration.getPort()));
            }
        }

        boolean ready = false;
        ManagedProcess solrProcess;
        try {
            solrProcess = startPreparation();
            ready = solrProcess.startAndWaitForConsoleMessageMaxMs(getReadyForConnectionsTag(), solrStartMaxWaitTimeMs);
        } catch (Exception e) {
            logger.error("failed to start Solr server", e);
            throw new ManagedProcessException("An error occurred while starting the Solr server", e);
        }
        if (!ready) {
            throw new ManagedProcessException("Solr server does not seem to have started up correctly? Magic string not seen in "
                    + solrStartMaxWaitTimeMs + "ms: " + getReadyForConnectionsTag());
        }
        logger.info("Solr server startup complete.");
    }

    synchronized ManagedProcess startPreparation() throws ManagedProcessException, IOException {
        ManagedProcessBuilder builder = getSolrManagedProcessBuilder();
        builder.addArgument("start");
        builder.addArgument("-p").addArgument(Integer.toString(configuration.getPort()));
        builder.addArgument("-f"); // Start in foreground, since we want to keep an handle on the process.

        if (configuration.getMemory() != null) {
            builder.addArgument("-m").addArgument(String.format("%dm", configuration.getMemory()));
        }
        if (configuration.getArgs() != null) {
            builder.addArgument("-a").addArgument(configuration.getArgs());
        }

        if (configuration.getJavaHome() != null) {
            builder.getEnvironment().put("JAVA_HOME", configuration.getJavaHome());
        }

        cleanupOnExit();

        // because cleanupOnExit() just installed our (class Solr4J) own
        // Shutdown hook, we don't need the one from ManagedProcess:
        builder.setDestroyOnShutdown(false);
        logger.info("Solr server executable: " + builder.getExecutable());
        return builder.build();
    }


    /**
     * Stops the Solr server.
     *
     * @throws ManagedProcessException if something fatal went wrong
     */
    public synchronized void stop() throws ManagedProcessException {
        if (isAlreadyRunning() && (stopProcess == null || !stopProcess.isAlive())) {
            logger.debug("Stopping the Solr server...");
            ManagedProcessBuilder stopProcessBuilder = getSolrManagedProcessBuilder();
            stopProcessBuilder.addArgument("stop");
            stopProcessBuilder.addArgument("-p").addArgument(Integer.toString(configuration.getPort()));
            stopProcessBuilder.setDestroyOnShutdown(false);
            stopProcess = stopProcessBuilder.build();
            stopProcess.start();
            stopProcess.waitForExit();

            if (isAlreadyRunning()) {
                logger.error("Solr server did not stop as expected. It was still running after calling the stop process.");
            } else {
                logger.info("Solr server stopped.");
            }
        } else {
            logger.debug("Solr server was already stopped.");
        }
    }

    public synchronized void unpackCoreFromClasspath(String coreName, boolean overwrite) {
        unpackCoreFromClasspath(coreName, overwrite, null);
    }

    public synchronized void unpackCoreFromClasspath(String coreName, boolean overwrite, Class referenceClass) {
        StringBuilder configSetNameClassPathLocation = new StringBuilder();
        configSetNameClassPathLocation.append(getClass().getPackage().getName().replace(".", "/"));
        configSetNameClassPathLocation.append("/").append("core/").append(coreName);

        File coreDir = new File(String.format("%s/server/solr/%s", configuration.getBaseDir(), coreName));

        try {
            if (coreDir.exists()) {
                if (overwrite) {
                    FileUtils.deleteDirectory(coreDir);
                } else {
                    throw new RuntimeException("A Solr core with the same name already exists.");
                }
            }
            Util.extractFromClasspathToFile(configSetNameClassPathLocation.toString(), coreDir, referenceClass);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error unpacking the core %s from the classpath", coreName), e);
        }
    }

    public synchronized void unpackConfigSet(String configSetName, boolean overwrite) {
        unpackConfigSet(configSetName, overwrite, null);
    }

    public synchronized void unpackConfigSet(String configSetName, boolean overwrite, Class referenceClass) {
        StringBuilder configSetNameClassPathLocation = new StringBuilder();
        configSetNameClassPathLocation.append(getClass().getPackage().getName().replace(".", "/"));
        configSetNameClassPathLocation.append("/").append("configset/").append(configSetName);

        File configSetsDir = new File(String.format("%s/server/solr/configsets/%s", configuration.getBaseDir(), configSetName));

        try {
            if (configSetsDir.exists()) {
                if (overwrite) {
                    FileUtils.deleteDirectory(configSetsDir);
                } else {
                    throw new RuntimeException("A Solr configset with the same name already exists.");
                }
            }
            Util.extractFromClasspathToFile(configSetNameClassPathLocation.toString(), configSetsDir, referenceClass);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error unpacking the configset %s", configSetName), e);
        }
    }

    private ManagedProcessBuilder getSolrManagedProcessBuilder() throws ManagedProcessException {
        ManagedProcessBuilder builder = new ManagedProcessBuilder(newExecutableFile("bin", "solr"));
        builder.setOutputStreamLogDispatcher(getOutputStreamLogDispatcher("solr"));
        builder.setLogger(this.logger);
        return builder;
    }

    /**
     * If the data directory specified in the configuration is a temporary directory, this deletes
     * any previous version. It also makes sure that the directory exists.
     *
     * @throws ManagedProcessException if something fatal went wrong
     */
    protected void prepareDirectories() throws ManagedProcessException {
        baseDir = Util.getDirectory(configuration.getBaseDir());
    }

    /**
     * Based on the current OS, unpacks the appropriate version of Solr to the file system based
     * on the configuration.
     */
    protected void unpackEmbeddedSolrServer() throws ManagedProcessException {
        unpackEmbeddedSolrServer(null);
    }

    /**
     * Based on the current OS, unpacks the appropriate version of Solr to the file system based
     * on the configuration.
     */
    protected void unpackEmbeddedSolrServer(Class referenceClass) throws ManagedProcessException {
        if (configuration.getBinariesClasspathLocation() == null) {
            logger.info("Not unpacking any embedded Solr Server (as BinariesClasspathLocation configuration is null)");
            return;
        }

        File solrVersionFile = new File(String.format("%s/%s", configuration.getBaseDir(), SOLR_VERSION_FILE));
        String currentVersion = null;
        if (solrVersionFile.exists()) {
            try(BufferedReader solrVersionReader = new BufferedReader(new FileReader(solrVersionFile))) {
                currentVersion = solrVersionReader.readLine().trim();
            } catch (IOException e) {
               logger.error("Error while attempting to read the Solr version from the version file");
            }
        }

        if (currentVersion != null && currentVersion.equals(configuration.getSolrVersion()) && !configuration.isUnpackForceOverwrite()){
            logger.info("Will not unpack Solr version {} from classpath into {} as it is already at this version", configuration.getSolrVersion(), configuration.getBaseDir());
            return;
        }

        logger.info("Will unpack Solr version {} from classpath into {}...", configuration.getSolrVersion(), configuration.getBaseDir());

        if (isAlreadyRunning()) {
            logger.info("Solr was already running when attempting to unpack. Stopping Solr first...");
            stop();
        }

        try {
            Util.extractFromClasspathToFile(configuration.getBinariesClasspathLocation(), baseDir, referenceClass);
            if (!SystemUtils.IS_OS_WINDOWS) {
                Util.forceExecutable(newExecutableFile("bin", "post"));
                Util.forceExecutable(newExecutableFile("bin", "solr"));
                Util.forceExecutable(newExecutableFile("bin", "solr.in.sh"));
                Util.forceExecutable(newExecutableFile("bin", "oom_solr.sh"));
                Util.forceExecutable(newExecutableFile("bin", "install_solr_service.sh"));
                Util.forceExecutable(newExecutableFile("bin/init.d", "solr"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error unpacking embedded Solr server", e);
        }
    }

    /**
     * Adds a shutdown hook to ensure that when the JVM exits, the Solr server is stopped gracefully.
     */
    protected void cleanupOnExit() {
        String threadName = "Shutdown Hook Deletion Thread for Temporary Solr " + baseDir.toString();
        final Solr4J solr4j = this;
        Runtime.getRuntime().addShutdownHook(new Thread(threadName) {

            @Override
            public void run() {
                // ManagedProcess DestroyOnShutdown ProcessDestroyer does
                // something similar, but it shouldn't hurt to better be save
                // than sorry and do it again ourselves here as well.
                try {
                    // Shut up and don't log if it was already stop() before
                    if (isAlreadyRunning()) {
                        logger.info("cleanupOnExit() ShutdownHook now stopping Solr server");
                        solr4j.stop();
                    }
                } catch (ManagedProcessException e) {
                    logger.warn("cleanupOnExit() ShutdownHook: An error occurred while stopping the Solr server", e);
                }
                if (baseDir.exists() && Util.isTemporaryDirectory(baseDir.getAbsolutePath())) {
                    logger.info("cleanupOnExit() ShutdownHook quietly deleting temporary Solr server base directory: " + baseDir);
                    FileUtils.deleteQuietly(baseDir);
                }
            }
        });
    }
    //endregion

    //region EXTENDED METHODS

    //endregion

    //region EVENTS

    //endregion

    //region GETTERS / SETTERS

    protected String getReadyForConnectionsTag() {
        return "o.e.j.s.Server Started";
    }

    protected OutputStreamLogDispatcher getOutputStreamLogDispatcher(@SuppressWarnings("unused") String exec) {
        return new SolrOutputStreamLogDispatcher();
    }

    protected File newExecutableFile(String dir, String exec) {
        return new File(baseDir, dir + "/" + exec + getWinCmdExt());
    }

    protected String getWinCmdExt() {
        return SystemUtils.IS_OS_WINDOWS ? ".cmd" : "";
    }

    public Solr4JConfiguration getConfiguration() {
        return configuration;
    }


    //endregion
}
