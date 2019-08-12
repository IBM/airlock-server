package com.okta.saml;

import com.okta.saml.util.IPRange;
import org.apache.commons.lang.StringUtils;
//import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl; deprecated
import org.apache.xpath.jaxp.XPathFactoryImpl;
import org.opensaml.ws.security.SecurityPolicyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.*;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * This class is derived from a xml configuration file;
 * it is used to provide an application required to generate a SAMLRequest,
 * and to validate SAMLResponse.
 */
public class Configuration {

    private Map<String, Application> applications;
    private String defaultEntityID = null;
    private boolean suppressErrors;
    private String loginUri;

    static XPath xPath;

    private static XPathExpression configurationRootXPath;
    private static XPathExpression applicationXPath;
    private static XPathExpression entityIdXPath;
    private static XPathExpression defaultAppXPath;
    private static XPathExpression addressXPath;
    private static XPathExpression spUsernamesXPath;
    private static XPathExpression spGroupsXPath;
    private static XPathExpression suppressErrorsXPath;
    private static XPathExpression loginUriXPath;

    private IPRange oktaUsersIps;
    private IPRange spUsersIps;
    private List<String> spUsernames;
    private List<String> spGroupnames;

    public static final String SAML_RESPONSE_FORM_NAME = "SAMLResponse";
    public static final String CONFIGURATION_KEY = "okta.config.file";
    public static final String DEFAULT_ENTITY_ID = "okta.config.default_entity_id";
    public static final String REDIR_PARAM = "os_destination";
    public static final String RELAY_STATE_PARAM = "RelayState";

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    static {
        try {
            XPathFactory xPathFactory = new XPathFactoryImpl();
            xPath = xPathFactory.newXPath();
            xPath.setNamespaceContext(new MetadataNamespaceContext());

            configurationRootXPath = xPath.compile("configuration");
            applicationXPath = xPath.compile("applications/application");
            addressXPath = xPath.compile("allowedAddresses");
            spUsernamesXPath = xPath.compile("spUsers/username");
            spGroupsXPath = xPath.compile("spGroups/groupname");
            entityIdXPath = xPath.compile("md:EntityDescriptor/@entityID");
            defaultAppXPath = xPath.compile("default");
            suppressErrorsXPath = xPath.compile("suppressErrors");
            loginUriXPath = xPath.compile("loginUri");
        } catch (XPathExpressionException e) {
            LOGGER.error("Failed to create XPathFactory instance", e);
        }
    }

    public Configuration(String configuration) throws XPathExpressionException, CertificateException, UnsupportedEncodingException, SecurityPolicyException {
        InputSource source = new InputSource(new StringReader(configuration));

        Node root = (Node) configurationRootXPath.evaluate(source, XPathConstants.NODE);
        NodeList applicationNodes = (NodeList) applicationXPath.evaluate(root, XPathConstants.NODESET);

        defaultEntityID = defaultAppXPath.evaluate(root);
        applications = new HashMap<String, Application>();
        spUsernames = new ArrayList<String>();
        spGroupnames = new ArrayList<String>();

        for (int i = 0; i < applicationNodes.getLength(); i++) {
            Element applicationNode = (Element) applicationNodes.item(i);
            String entityID = entityIdXPath.evaluate(applicationNode);
            Application application = new Application(applicationNode);
            applications.put(entityID, application);
        }

        Element allowedAddresses = (Element) addressXPath.evaluate(root, XPathConstants.NODE);
        if (allowedAddresses != null) {
            String oktaFrom = (String) xPath.compile("oktaUsers/ipFrom").evaluate(allowedAddresses, XPathConstants.STRING);
            String oktaTo = (String) xPath.compile("oktaUsers/ipTo").evaluate(allowedAddresses, XPathConstants.STRING);

            String spFrom = (String) xPath.compile("spUsers/ipFrom").evaluate(allowedAddresses, XPathConstants.STRING);
            String spTo = (String) xPath.compile("spUsers/ipTo").evaluate(allowedAddresses, XPathConstants.STRING);

            if (oktaFrom != null) {
                try {
                    oktaUsersIps = new IPRange(oktaFrom, oktaTo);
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid IP specified for Okta users addresses: " + e.getMessage());
                }
            }

            if (spFrom != null) {
                try {
                    spUsersIps = new IPRange(spFrom, spTo);
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid IP specified for Service Provider users addresses: " + e.getMessage());
                }
            }
        }

        String suppress = suppressErrorsXPath.evaluate(root);
        if (suppress != null) {
            suppress = suppress.trim();
        }
        suppressErrors = (!StringUtils.isBlank(suppress)) ? Boolean.parseBoolean(suppress) : true;

        loginUri = loginUriXPath.evaluate(root);
        if (loginUri != null) {
            loginUri = loginUri.trim();
        }

        NodeList spUnames = (NodeList) spUsernamesXPath.evaluate(root, XPathConstants.NODESET);
        if (spUnames != null) {
            for (int i = 0; i < spUnames.getLength(); i++) {
                Element usernameNode = (Element) spUnames.item(i);
                if (!StringUtils.isBlank(usernameNode.getTextContent())) {
                    spUsernames.add(usernameNode.getTextContent().trim());
                }
            }
        }

        NodeList spGnames = (NodeList) spGroupsXPath.evaluate(root, XPathConstants.NODESET);
        if (spGnames != null) {
            for (int i = 0; i < spGnames.getLength(); i++) {
                Element groupnameNode = (Element) spGnames.item(i);
                if (!StringUtils.isBlank(groupnameNode.getTextContent())) {
                    spGroupnames.add(groupnameNode.getTextContent().trim());
                }
            }
        }
    }

    /**
     * @return the map of all the applications listed in the config file,
     *          where the entityID of the application's EntityDescriptor is the key
     *          and its representation in Application object is the value
     */
    public Map<String, Application> getApplications() {
        return applications;
    }

    /**
     * @param entityID an identifier for an EntityDescriptor
     * @return an Application whose EntityDescriptor entityID matches the given entityID
     */
    public Application getApplication(String entityID) {
        return applications.get(entityID);
    }

    /**
     * @return the Application whose EntityDescriptor entityID matches the default entityID
     */
    public Application getDefaultApplication() {
        if (StringUtils.isBlank(defaultEntityID)) {
            return null;
        }
        return applications.get(defaultEntityID);
    }

    /**
     * @return the default entityID from the configuration, which in configured under default
     */
    public String getDefaultEntityID() {
        return defaultEntityID;
    }

    /**
     * Is ip allowed for identity provider users (Okta)
     */
    public boolean isIpAllowedForOkta(String ip) {
        try {
            boolean isRejectedForOkta = oktaUsersIps != null && !oktaUsersIps.isAddressInRange(ip);
            boolean isRejectedForSP = spUsersIps != null && !spUsersIps.isAddressInRange(ip);

            //making it more explicit
            if ((isRejectedForOkta && isRejectedForSP) ||
                    (!isRejectedForOkta && !isRejectedForSP) ||
                    (oktaUsersIps == null && isRejectedForSP) ||
                    (spUsersIps == null && oktaUsersIps == null)) {
                return true;
            }

            if ((oktaUsersIps == null && !isRejectedForSP) ||
                    (isRejectedForOkta && !isRejectedForSP)) {
                return false;
            }
        } catch (Exception e) {
            LOGGER.error(e.getClass().getSimpleName() + ": " + e.getMessage());
            //something is wrong with configuration, logging this and falling back to default behaviour
            return true;
        }

        return true;
    }

    /**
     * Is username allowed for identity provider users (Okta)
     */
    public boolean isUsernameAllowedForOkta(String username) {
        if (StringUtils.isBlank(username)) {
            return true;
        }
        return !spUsernames.contains(username);
    }

    public boolean isInSPGroups(Collection<String> userGroups) {
        if (userGroups == null || userGroups.isEmpty() || spGroupnames.isEmpty()) {
            return false;
        }

        for (String atlGroup: spGroupnames) {
            for (String userGroup : userGroups) {
                if (userGroup.trim().equalsIgnoreCase(atlGroup.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSPUsernamesUsed() {
        return !spUsernames.isEmpty();
    }

    public boolean isSPGroupnamesUsed() {
        return !spGroupnames.isEmpty();
    }

    /**
     * Is ip allowed for service provider users
     */
    public boolean isIpAllowedForSP(String ip) {
       return !isIpAllowedForOkta(ip);
    }

    public boolean suppressingErrors() {
        return suppressErrors;
    }

    public String getLoginUri() {
        return loginUri;
    }
}