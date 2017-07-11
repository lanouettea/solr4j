package com.andrelanouette.solr4j;

/**
 * //TODO: Document this class; Solr4JConfiguration
 *
 * @author Andre Lanouette <andre.lanouette@lrtech.ca>
 * @since 10/07/2017
 */
public interface Solr4JConfiguration {

    int getPort();

    String getBaseDir();

    String getSolrVersion();

    boolean isUnpackFromClasspath();

    String getBinariesClasspathLocation();

    String getJavaHome();

    Integer getMemory();

    String getArgs();

    class Impl implements Solr4JConfiguration {
        private final int port;
        private final String baseDir;
        private final String solrVersion;
        private final boolean unpackFromClasspath;
        private final String binariesClasspathLocation;
        private final String javaHome;
        private final Integer memory;
        private final String args;

        public Impl(int port, String baseDir, String solrVersion, boolean unpackFromClasspath, String binariesClasspathLocation, String javaHome, Integer memory, String args) {
            this.port = port;
            this.baseDir = baseDir;
            this.solrVersion = solrVersion;
            this.unpackFromClasspath = unpackFromClasspath;
            this.binariesClasspathLocation = binariesClasspathLocation;
            this.javaHome = javaHome;
            this.memory = memory;
            this.args = args;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public String getBaseDir() {
            return baseDir;
        }

        @Override
        public String getSolrVersion() {
            return solrVersion;
        }

        public boolean isUnpackFromClasspath() {
            return unpackFromClasspath;
        }

        public String getBinariesClasspathLocation() {
            return binariesClasspathLocation;
        }

        public String getJavaHome() {
            return javaHome;
        }

        public Integer getMemory() {
            return memory;
        }

        @Override
        public String getArgs() {
            return args;
        }
    }
}
