package com.andrelanouette.solr4j;

import com.andrelanouette.exec.ManagedProcess;
import com.andrelanouette.exec.ManagedProcessBuilder;
import com.andrelanouette.exec.ManagedProcessException;
import com.andrelanouette.exec.OutputStreamLogDispatcher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * //TODO: Document this class; Solr4j
 *
 * @author Andre Lanouette <andre.lanouette@lrtech.ca>
 * @since 10/07/2017
 */
public class Solr4J {
    //region  CONSTANTS DECLARATIONS

    private static final Logger logger = LoggerFactory.getLogger(Solr4J.class);

    //endregion

    //region MEMBER DECLARATIONS

    protected Solr4JConfiguration configuration;

    private File baseDir;

    private ManagedProcess solrProcess;

    protected int solrStartMaxWaitTimeMs = 30000;

    //endregion

    //region CONSTRUCTORS

    protected Solr4J(Solr4JConfiguration configuration) {
        this.configuration = configuration;
    }

    public static Solr4J newEmbeddedSolr4J(Solr4JConfiguration solr4JConfiguration) throws ManagedProcessException {
        Solr4J solr4J = new Solr4J(solr4JConfiguration);
        solr4J.prepareDirectories();
        solr4J.unpackEmbeddedSolrServer();
        return solr4J;
    }

    public static Solr4J newEmbeddedSolr4J(int port) throws ManagedProcessException {
        Solr4JConfigurationBuilder solr4JConfigurationBuilder = Solr4JConfigurationBuilder.newBuilder();
        solr4JConfigurationBuilder.setPort(port);
        return newEmbeddedSolr4J(solr4JConfigurationBuilder.build());
    }

    //endregion

    //region BASE METHODS

    /**
     * Starts up the Solr server, using the base directory and port specified in the configuration.
     *
     * @throws ManagedProcessException if something fatal went wrong
     */
    public synchronized void start() throws ManagedProcessException {
        logger.info("Starting up the Solr server...");
        boolean ready = false;
        try {
            solrProcess = startPreparation();
            ready = solrProcess.startAndWaitForConsoleMessageMaxMs(getReadyForConnectionsTag(), solrStartMaxWaitTimeMs);
        } catch (Exception e) {
            logger.error("failed to start Solr server", e);
            throw new ManagedProcessException("An error occurred while starting the Solr server", e);
        }
        if (!ready) {
            if (solrProcess.isAlive())
                solrProcess.destroy();
            throw new ManagedProcessException("Solr server does not seem to have started up correctly? Magic string not seen in "
                    + solrStartMaxWaitTimeMs + "ms: " + getReadyForConnectionsTag() + solrProcess.getLastConsoleLines());
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
        if (solrProcess.isAlive()) {
            logger.debug("Stopping the Solr server...");
            solrProcess.destroy();
            logger.info("Solr server stopped.");
        } else {
            logger.debug("Solr server was already stopped.");
        }
    }

    public synchronized void unpackConfigSet(String configSetName, boolean overwrite) {
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
            Util.extractFromClasspathToFile(configSetNameClassPathLocation.toString(), configSetsDir);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error unpacking the configset %s", configSetName), e);
        }
    }

    private ManagedProcessBuilder getSolrManagedProcessBuilder() throws ManagedProcessException {
        ManagedProcessBuilder builder = new ManagedProcessBuilder(newExecutableFile("bin", "solr"));
        builder.setOutputStreamLogDispatcher(getOutputStreamLogDispatcher("solr"));
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
    protected void unpackEmbeddedSolrServer() {
        if (configuration.getBinariesClasspathLocation() == null) {
            logger.info("Not unpacking any embedded Solr Server (as BinariesClasspathLocation configuration is null)");
            return;
        }

        try {
            Util.extractFromClasspathToFile(configuration.getBinariesClasspathLocation(), baseDir);
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
                    if (solrProcess != null && solrProcess.isAlive()) {
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
