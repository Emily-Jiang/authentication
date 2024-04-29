/*
 * Copyright (c) 2024 Contributors to Eclipse Foundation.
 * Copyright (c) 2007, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package ee.jakarta.tck.authentication.test.basic.sam.config;

import static ee.jakarta.tck.authentication.test.basic.servlet.JASPICData.DEFAULT_LOG_FILE;
import static ee.jakarta.tck.authentication.test.basic.servlet.JASPICData.LOGGER_NAME;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

import ee.jakarta.tck.authentication.test.basic.sam.ProviderConfigurationEntry;
import ee.jakarta.tck.authentication.test.basic.sam.ProviderConfigurationXMLFileProcessor;
import ee.jakarta.tck.authentication.test.basic.sam.TSAuthConfigProviderServlet;
import ee.jakarta.tck.authentication.test.basic.sam.TSFileHandler;
import ee.jakarta.tck.authentication.test.common.logging.server.TSLogger;
import ee.jakarta.tck.authentication.test.common.logging.server.TSXMLFormatter;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.security.auth.message.config.AuthConfigProvider;
import jakarta.security.auth.message.config.RegistrationListener;
import jakarta.security.auth.message.module.ServerAuthModule;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 *
 * @author Raja Perumal
 *
 * This is an AuthConfigFactory implementation class that will get loaded by the MessageProcessingRuntime (MPR). Once
 * this is loaded into the MPR and the the constructor is called, the constructor will call a series of methods that
 * will ultimately be performing the assertion tests and logging results. It will be the responsibility of the client
 * code to verify results.
 *
 * Important: It is very likely that the logged messages of this class are being searched for in the logfile by the
 * client code. Because of this, refrain from changing log messages in this file.
 *
 */
public class TSAuthConfigFactoryForStandalone extends AuthConfigFactory {

    private static TSLogger logger = null;
    private static Map<String, AuthConfigProvider> authConfigProviderMap = new Hashtable<>();
    private static Map<AuthConfigProvider, RegistrationListener> registrationListenerMap = new Hashtable<>();
    private static Map registrationContextMap = new Hashtable();
    private static ProviderConfigurationXMLFileProcessor configFileProcessor = null;

    public TSAuthConfigFactoryForStandalone() {
        initializeTSLogger();
        logger.log(INFO, "Initialized TSLogger");
        readProviderConfigurationXMLFile();
    }

    /*
     * Read the provider configuration XML file and registers each provider with TSAuthConfigFactoryForStandalone
     */
    private void readProviderConfigurationXMLFile() {
        String providerConfigFileLocation = System.getProperty("provider.configuration.file");

        try {
            // Given the provider configuration xml file
            // This reader parses the xml file and stores the configuration entries as a collection.
            configFileProcessor = new ProviderConfigurationXMLFileProcessor(providerConfigFileLocation);

            // Retrieve the ProviderConfigurationEntries collection
            Collection<ProviderConfigurationEntry> providerConfigurationEntries =
                configFileProcessor.getProviderConfigurationEntriesCollection();

            // Obtain each ProviderConfigurationEntry and register it with TSAuthConfigFactoryForStandalone
            for (ProviderConfigurationEntry providerConfigurationEntry : providerConfigurationEntries) {

                if (providerConfigurationEntry != null) {
                    registerConfigProvider(
                        providerConfigurationEntry.getProviderClassName(),
                        getCleanACPProps(providerConfigurationEntry.getProperties()),
                        providerConfigurationEntry.getMessageLayer(),
                        providerConfigurationEntry.getApplicationContextId(),
                        providerConfigurationEntry.getRegistrationDescription());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if ((e.getMessage() != null) && (!e.getMessage().equals(""))) {
                logger.log(SEVERE, e.getMessage());
            } else {
                logger.log(SEVERE, "Error in readProviderConfigurationXMLFile()");
            }
        }

    }

    /*
     * When registering a config provider, the properties passed to the ACF.registerConfigProvider() call can ONLY contain
     * props t hat are string types. Non-String types must NOT be passed to the registerConfigProvider() method. This method
     * will 'clean' a Property map so that its props can be passed to the registerConfigProvider() method.
     */
    private Map<String, String> getCleanACPProps(Map<String, String> origProps) {
        if (origProps == null) {
            return null;
        }

        HashMap<String, String> props = new HashMap<>();

        // loop thru passed in props and remove anything that is
        // not of type String since only String type Props are allowed in our
        // calls to registerConfigProvider()
        for (String key : origProps.keySet()) {
            if (key != null) {
                Object val = origProps.get(key);
                if (val instanceof String) {
                    // we found entry that is not String so remove it
                    props.put(key, (String) val);
                    System.out.println("TSAuthConfigFactoryForStandalone:  added key=" + key + " with value = " + val);
                } else {
                    System.out.println("TSAuthConfigFactoryForStandalone: found non-string value for key=" + key);
                }
            }
        }

        return props;
    }

    /**
     * Get a registered AuthConfigProvider from the factory.
     *
     * Get the provider of ServerAuthConfig and/or ClientAuthConfig objects registered for the identified message layer and
     * application context.
     *
     * @param layer a String identifying the message layer for which the registered AuthConfigProvider is to be returned.
     * This argument may be null.
     *
     * @param appContext a String that identifys the application messaging context for which the registered
     * AuthConfigProvider is to be returned. This argument may be null.
     *
     * @param listener the RegistrationListener whose <code>notify</code> method is to be invoked if the corresponding
     * registration is unregistered or replaced. The value of this argument may be null.
     *
     * @return the implementation of the AuthConfigProvider interface registered at the factory for the layer and appContext
     * or null if no AuthConfigProvider is selected.
     *
     * <p>
     * All factories shall employ the following precedence rules to select the registered AuthConfigProvider that matches
     * the layer and appContext arguments:
     * <ul>
     * <li>The provider that is specifically registered for both the corresponding message layer and appContext shall be
     * selected.
     * <li>if no provider is selected according to the preceding rule, the provider specifically registered for the
     * corresponding appContext and for all message layers shall be selected.
     * <li>if no provider is selected according to the preceding rules, the provider specifically registered for the
     * corresponding message layer and for all appContexts shall be selected.
     * <li>if no provider is selected according to the preceding rules, the provider registered for all message layers and
     * for all appContexts shall be selected.
     * <li>if no provider is selected according to the preceding rules, the factory shall terminate its search for a
     * registered provider.
     * </ul>
     */
    @Override
    public AuthConfigProvider getConfigProvider(String layer, String appContext, RegistrationListener listener) {
        AuthConfigProvider localAuthConfigProvider = null;

        // Runtime calls getConfigProvider() after calling
        // AuthConfigFactory.getFactory()
        logger.log(INFO, "getConfigProvider called for Layer : " + layer + " and AppContext :" + appContext);

        if (authConfigProviderMap != null) {
            localAuthConfigProvider = (AuthConfigProvider) authConfigProviderMap.get(layer + appContext);

            // register the listener for AuthConfigProvider
            if ((listener != null) && (localAuthConfigProvider != null)) {
                registrationListenerMap.put(localAuthConfigProvider, listener);
            }

            String logMsg = "getConfigProvider returned non-null provider for";
            logMsg += " Layer : " + layer + " and AppContext :" + appContext;
            logger.log(INFO, logMsg);
            return localAuthConfigProvider;
        } else {
            String logMsg = "getConfigProvider returned null provider for";
            logMsg += " Layer : " + layer + " and AppContext :" + appContext;
            logger.log(INFO, logMsg);
            return null;
        }

    }

    /**
     * Get the the registration context for the identified registration.
     *
     * @param registrationID a String that identifies a provider registration at the factory
     *
     * @return a RegistrationContext or null. When a Non-null value is returned, it is a copy of the registration context
     * corresponding to the registration. Null is returned when the registration identifier does not correpond to an active
     * registration
     */
    @Override
    public RegistrationContext getRegistrationContext(String registrationID) {
        return (RegistrationContext) registrationContextMap.get(registrationID);
    }

    /**
     * Get the registration identifiers for all registrations of the provider instance at the factory.
     *
     * @param provider the AuthConfigurationProvider whose registration identifiers are to be returned. This argument may be
     * null, in which case, it indicates that the the id's of all active registration within the factory are returned.
     *
     * @return an array of String values where each value identifies a provider registration at the factory. This method
     * never returns null; it returns an empty array when their are no registrations at the factory for the identified
     * provider.
     */
    @Override
    public String[] getRegistrationIDs(AuthConfigProvider provider) {

        Vector keyMatchVector = new Vector();

        if (provider != null) {
            Set entries = authConfigProviderMap.entrySet();
            Iterator iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                // Add all the matching keys to keyMatchVector
                if (entry.getValue().equals(provider)) {
                    keyMatchVector.add(entry.getKey());
                }
            }
        } else {
            // if provider=null then return all keys from authConfigProviderMap
            Set authConfigProviderMapKeySet = authConfigProviderMap.keySet();

            Iterator iterator = authConfigProviderMapKeySet.iterator();
            while (iterator.hasNext()) {
                // Add all keys to keyMatchVector
                keyMatchVector.add(iterator.next());
            }
        }

        // create the result string array using keyMatchVector
        String result[] = new String[keyMatchVector.size()];
        int index = 0;

        Iterator keyMatchVectorIterator = keyMatchVector.iterator();

        while (keyMatchVectorIterator.hasNext()) {
            // populate result string array with the contents of keyMatchVector
            result[index++] = (String) keyMatchVectorIterator.next();
        }

        return result;
    }

    /**
     * Disassociate the listener from all the provider registrations whose layer and appContext values are matched by the
     * corresponding arguments to this method.
     *
     * @param listener the RegistrationListener to be detached.
     *
     * @param layer a String identifying the message layer or null.
     *
     * @param appContext a String value identifying the application contex or null.
     *
     * @return an array of String values where each value identifies a provider registration from which the listener was
     * removed. This method never returns null; it returns an empty array if the listener was not removed from any
     * registrations.
     *
     * @exception SecurityException if the caller does not have permission to detach the listener from the factory.
     *
     */
    @Override
    public String[] detachListener(RegistrationListener listener, String layer, String appContext) {
        return null;
    }

    /**
     * Remove the identified provider registration from the factory and invoke any listeners associated with the removed
     * registration.
     *
     * @param registrationID a String that identifies a provider registration at the factory
     *
     * @return true if there was a registration with the specified identifier and it was removed. Return false if the
     * registraionID was invalid.
     *
     * @exception SecurityException if the caller does not have permission to unregister the provider at the factory.
     *
     */
    @Override
    public boolean removeRegistration(String registrationID) {
        // get the corresponding ConfigProvider for registrationID
        // and lookup any listeners associated with that provider, if so
        // invoke notify method on them
        registrationContextMap.remove(registrationID);
        return (authConfigProviderMap.remove(registrationID) != null);
    }

    /**
     * Registers within the factory, a provider of ServerAuthConfig and/or ClientAuthConfig objects for a message layer and
     * application context identifier.
     *
     * <P>
     * At most one registration may exist within the factory for a given combination of message layer and appContext. Any
     * pre-existing registration with identical values for layer and appContext is replaced by a subsequent registration.
     * When replacement occurs, the registration identifier, layer, and appContext identifier remain unchanged, and the
     * AuthConfigProvider (with initialization properties) and description are replaced.
     *
     * <p>
     * Within the lifetime of its Java process, a factory must assign unique registration identifiers to registrations, and
     * must never assign a previously used registration identifier to a registration whose message layer and or appContext
     * identifier differ from the previous use.
     *
     * <p>
     * Programmatic registrations performed via this method must update (according to the replacement rules described
     * above), the persistent declarative representation of provider registrations employed by the factory constructor.
     *
     * @param className the fully qualified name of an AuthConfigProvider implementation class. This argument must not be
     * null.
     *
     * @param properties a Map object containing the initialization properties to be passed to the provider constructor.
     * This argument may be null. When this argument is not null, all the values and keys occuring in the Map must be of
     * type String.
     *
     * @param layer a String identifying the message layer for which the provider will be registered at the factory. A null
     * value may be passed as an argument for this parameter, in which case, the provider is registered at all layers.
     *
     * @param appContext a String value that may be used by a runtime to request a configuration object from this provider.
     * A null value may be passed as an argument for this parameter, in which case, the provider is registered for all
     * configuration ids (at the indicated layers).
     *
     * @param description a text String descripting the provider. this value may be null.
     *
     * @return a String identifier assigned by the factory to the provider registration, and that may be used to remove the
     * registration from the provider.
     *
     * @exception SecurityException if the caller does not have permission to register a provider at the factory.
     *
     * @exception AuthException if the provider construction or registration fails.
     */
    @Override
    public String registerConfigProvider(String className, Map properties, String layer, String appContext, String description) {
        String result = null;
        AuthConfigProvider authConfigProvider = null;
        String theClass = TSAuthConfigProviderStandalone.class.getName();

        logger.log(INFO, "registerConfigProvider() called for layer " + layer + " and appContext " + appContext);
        try {
            // Here we instantiate only TSAuthConfigProvider
            // this needs to be revisited.
            if (className.equals(TSAuthConfigProvider.class.getName())) {
                // instantiate CTS AuthConfigProviderImpl with logger
                authConfigProvider = new TSAuthConfigProvider(properties, null, logger);

            } else if (className.equals(TSAuthConfigProviderServlet.class.getName())) {
                authConfigProvider = new TSAuthConfigProviderServlet(properties, logger, null);

            } else if (className.equals(theClass)) {
                authConfigProvider = new TSAuthConfigProviderStandalone(properties, logger, null);

            } else {
                throw new RuntimeException("Unknown class : " + className);
            }

            RegistrationContext previousRegistrationContext = null;
            AuthConfigProvider previousAuthConfigProvider = null;
            previousAuthConfigProvider = (AuthConfigProvider) authConfigProviderMap.get(layer + appContext);
            previousRegistrationContext = (RegistrationContext) registrationContextMap.get(layer + appContext);

            if (previousAuthConfigProvider == null) {
                authConfigProviderMap.put(layer + appContext, authConfigProvider);
                registrationContextMap.put(layer + appContext, new RegistrationContextImpl(layer, appContext, description, true));

                // Add new provider to the persistent store(ProviderConfiguration.xml)
                ProviderConfigurationXMLFileProcessor.addProviderConfigEntry(className, properties, layer, appContext, description);

            } else if ((previousAuthConfigProvider != null) && (previousRegistrationContext.isPersistent() == false)) {
                authConfigProviderMap.put(layer + appContext, authConfigProvider);
                registrationContextMap.put(layer + appContext, new RegistrationContextImpl(layer, appContext, description, true));

                // Add new provider to the persistent store(ProviderConfiguration.xml)
                ProviderConfigurationXMLFileProcessor.addProviderConfigEntry(className, properties, layer, appContext, description);
            }
            result = layer + appContext;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception :" + e.getMessage());
        }
        return result;

    }

    /**
     * Registers within the (in-memory) factory, a provider of ServerAuthConfig and/or ClientAuthConfig objects for a
     * message layer and application context identifier. This method does NOT effect the factory's persistent declarative
     * representation of provider registrations, and intended to be used by Providers to perform self-Registration.
     *
     * <P>
     * At most one registration may exist within the factory for a given combination of message layer and appContext. Any
     * pre-existing registration with identical values for layer and appContext is replaced by a subsequent registration.
     * When replacement occurs, the registration identifier, layer, and appContext identifier remain unchanged, and the
     * AuthConfigProvider (with initialization properties) and description are replaced.
     *
     * <p>
     * Within the lifetime of its Java process, a factory must assign unique registration identifiers to registrations, and
     * must never assign a previously used registration identifier to a registration whose message layer and or appContext
     * identifier differ from the previous use.
     *
     * @param provider the AuthConfigProvider to be registered at the factory (or null). Calling this method with a null
     * value for this parameter shall cause <code>getConfigProvider</code> to return null when it iscalled with layer and
     * appContext values for which the resulting registration is the best match.
     *
     * @param layer a String identifying the message layer for which the provider will be registered at the factory. A null
     * value may be passed as an argument for this parameter, in which case, the provider is registered at all layers.
     *
     * @param appContext a String value that may be used by a runtime to request a configuration object from this provider.
     * A null value may be passed as an argument for this parameter, in which case, the provider is registered for all
     * configuration ids (at the indicated layers).
     *
     * @param description a text String descripting the provider. this value may be null.
     *
     * @return a String identifier assigned by the factory to the provider registration, and that may be used to remove the
     * registration from the provider.
     *
     * @exception SecurityException if the caller does not have permission to register a provider at the factory.
     *
     * @exception AuthException if the provider registration fails.
     */
    @Override
    public String registerConfigProvider(AuthConfigProvider provider, String layer, String appContext, String description) {

        String result = null;
        String providerClassName = null;

        logger.log(INFO, "registerConfigProvider() called for layer " + layer + " and appContext " + appContext);

        if (provider == null) {
            return result;
        }

        try {
            RegistrationContext previousRC = null;
            AuthConfigProvider previousACP = null;
            previousACP = (AuthConfigProvider) authConfigProviderMap.get(layer + appContext);
            previousRC = (RegistrationContext) registrationContextMap.get(layer + appContext);

            if (previousACP == null) {
                authConfigProviderMap.put(layer + appContext, provider);
                registrationContextMap.put(layer + appContext, new RegistrationContextImpl(layer, appContext, description, false));

            } else if ((previousACP != null) && (previousRC.isPersistent() == true)) {
                // update registration context
                registrationContextMap.put(layer + appContext, new RegistrationContextImpl(layer, appContext, description, false));

                if (provider != null) {
                    providerClassName = provider.getClass().getName();
                }

                // delete existing provider from its persistent state
                configFileProcessor.deleteProviderConfigEntry(providerClassName, layer, appContext, description);
            }
            result = layer + appContext;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception :" + e.getMessage());
        }

        return result;
    }

    /**
     * Registers within the (in-memory) factory, an instance of a <code>ServerAuthModule</code> for a message layer and
     * application context identifier as identified by a profile specific context object.
     *
     * <p>
     * This will override any other modules that have already been registered, either via proprietary means or using the
     * standard API. The <code>ServerAuthModule</code> is removed, via a call to <code>removeServerAuthModule</code> when
     * the context associated with the profile specific context object ends.
     *
     * <p>
     * Note that this method is a convenience method that can be used instead of <code>registerConfigProvider</code>, but
     * should ultimately have the same effect. That is, the <code>layer</code> and <code>appContext</code> parameters are
     * generated from the context object, and the <code>ServerAuthModule</code> is wrapped by an implementation specific
     * <code>AuthConfigProvider</code>, which are then used to call <code>registerConfigProvider</code> or an internal
     * method with the same effect. The returned registration ID is then associated with the profile specific context
     * object, and also returned from this method.
     *
     * <p>
     * A "profile specific context object" is for example the <code>ServletContext</code> in the Servlet Container Profile.
     * The context associated with this <code>ServletContext</code> ends when for example the application corresponding to
     * it is undeployed. Association of the registration ID with the <code>ServletContext</code> simply means calling the
     * <code>setAttribute</code> method on the <code>ServletContext</code>, with the registration ID as value. (The name
     * attribute has not been standardised in this version of the specification)
     *
     * @param serverAuthModule the <code>ServerAuthModule</code> instance to be registered
     * @param context the profile specific context of the application for which the module is registered
     * @return A String identifier assigned by the factory to the provider registration, and that may be used to remove the
     * registration from the factory.
     */
    @Override
    public String registerServerAuthModule(ServerAuthModule serverAuthModule, Object context) {
        logger.log(INFO, "registerServerAuthModule() called for serverAuthModule " + serverAuthModule + " and context " + context);
        return context.toString();
    }

    /**
     * Remove the <code>ServerAuthModule</code> (and potentially encompassing wrappers/factories) that was previously
     * registered via a call to <code>registerServerAuthModule</code>.
     *
     * <p>
     * Note that this method is a convenience method that can be used instead of <code>removeRegistration</code>, but should
     * ultimately have the same effect. That is calling <code>removeRegistration</code> with the return value from
     * <code>registerServerAuthModule</code> must have the same effect in that the <code>ServerAuthModule</code> is removed.
     *
     * @param context the profile specific context of the application for which the module is removed.
     */
    @Override
    public void removeServerAuthModule(Object context) {
        logger.log(INFO, "removeServerAuthModule() called for context " + context);
    }

    /**
     * Cause the factory to reprocess its persisent declarative representation of provider registrations.
     *
     * <p>
     * A factory should only replace an existing registration when a change of provider implementation class or
     * initialization properties has occured.
     *
     * @exception AuthException if an error occured during the reinitialization.
     *
     * @exception SecurityException if the caller does not have permission to refresh the factory.
     */
    @Override
    public void refresh() {
    }

    private static void initializeTSLogger() {
        String logFileLocation = null;
        if (logger != null) {
            return;
        }

        try {
            logFileLocation = System.getProperty("log.file.location");
            System.out.println("logFileLocation = " + logFileLocation);
            if (logFileLocation == null) {
                throw new RuntimeException("log.file.location not set");
            }

            logger = TSLogger.getTSLogger(LOGGER_NAME);
            boolean appendMode = true;

            // Create a new file
            TSFileHandler fileHandler = new TSFileHandler(logFileLocation + "/" + DEFAULT_LOG_FILE, appendMode);
            fileHandler.setFormatter(new TSXMLFormatter());
            logger.addHandler(fileHandler);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("TSLogger Initialization failed", e);
        }

    }

    private static class RegistrationContextImpl implements RegistrationContext {
        private String messageLayer;
        private String appContext;
        private String description;

        private RegistrationContextImpl(String messageLayer, String appContext, String description, boolean isPersistent) {
            this.messageLayer = messageLayer;
            this.appContext = appContext;
            this.description = description;
        }

        @Override
        public String getMessageLayer() {
            return messageLayer;
        }

        @Override
        public String getAppContext() {
            return appContext;
        }

        @Override
        public String getDescription() {
            return description;
        }

        /**
         * Get the persisted status from the registration context.
         *
         * @return a boolean indicating whether the registration is the result of a className based registration, or an instance
         * based (e.g. self-) registration. Only registrations performed by Class name are persistent.
         */
        @Override
        public boolean isPersistent() {
            return false;
        }

    }

}
