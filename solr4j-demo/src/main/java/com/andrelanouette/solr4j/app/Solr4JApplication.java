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

package com.andrelanouette.solr4j.app;

import com.andrelanouette.solr4j.Solr4J;
import com.andrelanouette.solr4j.Solr4JConfigurationBuilder;

/**
 * //TODO: Document this class; Solr4JApplication
 *
 * @author Andre Lanouette <andre.lanouette@lrtech.ca>
 * @since 10/07/2017
 */
public class Solr4JApplication {
    //region  CONSTANTS DECLARATIONS

    //endregion

    //region MEMBER DECLARATIONS

    //endregion

    //region CONSTRUCTORS

    //endregion

    //region BASE METHODS

    public static void main(String[] args) throws Exception {
        final Solr4J solr4J = Solr4J.newEmbeddedSolr4J(Solr4JConfigurationBuilder.newBuilder().setBaseDir("./solr4j-test").build());

//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            @Override
//            public void run() {
//                System.out.println("Shutdown hook ran!");
//                try {
//                    solr4J.stop();
//                } catch (ManagedProcessException e) {
//                    e.printStackTrace();
//                }
//            }
//        });

        solr4J.start();
        solr4J.unpackConfigSet("test_configset", true);

        while (true) {
            Thread.sleep(250);
        }
    }

    //endregion

    //region EXTENDED METHODS

    //endregion

    //region EVENTS

    //endregion

    //region GETTERS / SETTERS

    //endregion
}
