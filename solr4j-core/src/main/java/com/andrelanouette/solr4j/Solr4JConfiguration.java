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

/**
 * //TODO: Document this class; Solr4JConfiguration
 *
 * @author Andre Lanouette
 * @since 10/07/2017
 */
public interface Solr4JConfiguration {

    int getPort();

    String getBaseDir();

    String getSolrVersion();

    boolean isUnpackFromClasspath();

    boolean isUnpackForceOverwrite();

    String getBinariesClasspathLocation();

    String getJavaHome();

    Integer getMemory();

    String getArgs();

    class Impl implements Solr4JConfiguration {
        private final int port;
        private final String baseDir;
        private final String solrVersion;
        private final boolean unpackFromClasspath;
        private final boolean unpackForceOverwrite;
        private final String binariesClasspathLocation;
        private final String javaHome;
        private final Integer memory;
        private final String args;

        public Impl(int port, String baseDir, String solrVersion, boolean unpackFromClasspath, boolean unpackForceOverwrite, String binariesClasspathLocation, String javaHome, Integer memory, String args) {
            this.port = port;
            this.baseDir = baseDir;
            this.solrVersion = solrVersion;
            this.unpackFromClasspath = unpackFromClasspath;
            this.unpackForceOverwrite = unpackForceOverwrite;
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

        @Override
        public boolean isUnpackForceOverwrite() {
            return unpackForceOverwrite;
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
