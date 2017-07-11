package com.andrelanouette.solr4j;

import org.apache.commons.lang3.SystemUtils;

/**
 * //TODO: Document this class; Solr4JConfigurationBuilder
 *
 * @author Andre Lanouette <andre.lanouette@lrtech.ca>
 * @since 10/07/2017
 */
public class Solr4JConfigurationBuilder {
    //region  CONSTANTS DECLARATIONS

    protected static final String WIN = "win";

    //endregion

    //region MEMBER DECLARATIONS

    protected String baseDir = SystemUtils.JAVA_IO_TMPDIR + "/Solr4J/base";
    protected int port = 8983;
    protected String solrVerion = "6.6.0";
    protected boolean isUnpackingFromClasspath = true;
    protected String javaHome = null;
    protected Integer memory = null;
    protected String args = null;


    //endregion

    //region CONSTRUCTORS

    private Solr4JConfigurationBuilder() {
    }

    //endregion

    //region BASE METHODS

    public static Solr4JConfigurationBuilder newBuilder() {
        return new Solr4JConfigurationBuilder();
    }

    public Solr4JConfiguration build() {
        return new Solr4JConfiguration.Impl(this.port, this.baseDir, this.solrVerion, this.isUnpackingFromClasspath, _getBinariesClassPathLocation(), _getJavaHome(), this.memory, this.args);
    }

    //endregion

    //region EXTENDED METHODS

    protected String getBinariesClassPathLocation() {
        StringBuilder binariesClassPathLocation = new StringBuilder();
        binariesClassPathLocation.append(getClass().getPackage().getName().replace(".", "/"));
        binariesClassPathLocation.append("/").append("distrib/").append("solr-").append(getSolrVerion());
        return binariesClassPathLocation.toString();
    }

    protected String _getBinariesClassPathLocation() {
        if (isUnpackingFromClasspath)
            return getBinariesClassPathLocation();
        else
            return null;
    }

    protected String _getJavaHome(){
        if (javaHome != null){
            return javaHome;
        }else{
            return System.getProperty("java.home");
        }
    }

    protected String getSolrVerion() {
        return solrVerion;
    }

    //endregion

    //region EVENTS

    //endregion

    //region GETTERS / SETTERS

    public Solr4JConfigurationBuilder setBaseDir(String baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    public Solr4JConfigurationBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public Solr4JConfigurationBuilder setSolrVersion(String solrVersion) {
        this.solrVerion = solrVersion;
        return this;
    }

    public Solr4JConfigurationBuilder setUnpackFromClassPath(boolean unpackingFromClasspath){
        this.isUnpackingFromClasspath = unpackingFromClasspath;
        return this;
    }

    public Solr4JConfigurationBuilder setJavaHome(String javaHome) {
        this.javaHome = javaHome;
        return this;
    }

    public Solr4JConfigurationBuilder setMemory(int memory) {
        this.memory = memory;
        return this;
    }

    public Solr4JConfigurationBuilder setArgs(String args) {
        this.args = args;
        return this;
    }

    //endregion
}
