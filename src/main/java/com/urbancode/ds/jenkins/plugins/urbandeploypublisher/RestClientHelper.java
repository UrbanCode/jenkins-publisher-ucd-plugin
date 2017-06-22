/**
 * (c) Copyright IBM Corporation 2015, 2017.
 * This is licensed under the following license.
 * The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
package com.urbancode.ds.jenkins.plugins.urbandeploypublisher;

import hudson.AbortException;
import hudson.model.BuildListener;
import hudson.util.Secret;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.net.URI;
import java.util.UUID;

import org.apache.http.impl.client.DefaultHttpClient;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.urbancode.ud.client.ApplicationClient;
import com.urbancode.ud.client.ComponentClient;
import com.urbancode.ud.client.PropertyClient;
import com.urbancode.ud.client.SystemClient;
import com.urbancode.ud.client.VersionClient;
import com.urbancode.ud.client.UDRestClient;

/**
 * This class is used to provide access to the UrbanCode Deploy rest client
 *
 */
public class RestClientHelper implements Serializable {
    private URI ucdUrl;
    private UrbanDeploySite udSite;
    private String altUser;
    private Secret altPassword;

    public RestClientHelper(URI ucdUrl, UrbanDeploySite udSite, String altUser, Secret altPassword) {
        this.ucdUrl = ucdUrl;
        this.udSite = udSite;
        this.altUser = altUser != null ? altUser.trim() : "";
        this.altPassword = altPassword;
    }

    /**
     * Creates the component version
     *
     * @return UUID of the new version
     *
     * @throws AbortException
     */
    public UUID createComponentVersion(
            String version,
            String component,
            String description)
    throws AbortException {
        VersionClient versionClient = new VersionClient(ucdUrl, getUdClient());

        if (version == null || version.isEmpty() || version.length() > 255) {
            throw new AbortException(String.format("Failed to create version '%s' in UrbanCode Deploy. "
                    + "UrbanCode Deploy version names' length must be between 1 and  255 characters "
                    + "long. (Current length: %s)", version, version.length()));
        }

        UUID versionId;

        try {
            versionId = versionClient.createVersion(component, version, description);
        }
        catch (Exception ex) {
            throw new AbortException("Failed to create component version '"
                    + version + "' on component '" + component + "' : " + ex.getMessage());
        }

        return versionId;
    }

    /**
     * Upload files to component version
     * @throws AbortException
     */
    public void uploadVersionFiles(
            File workDir,
            String component,
            String version,
            String includePatterns,
            String excludePatterns)
    throws AbortException {
        VersionClient versionClient = new VersionClient(ucdUrl, getUdClient());
        String[] includes  = splitFiles(includePatterns);
        String[] excludes = splitFiles(excludePatterns);

        try {
            versionClient.addVersionFiles(
                    component,
                    version,
                    workDir,
                    "",
                    includes,
                    excludes,
                    true,
                    true);
        }
        catch (Exception ex) {
            throw new AbortException("Failed to upload files to version '" + version + "' : " +  ex.getMessage());
        }
    }

    public void deleteComponentVersion(UUID id)
    throws AbortException {
        VersionClient versionClient = new VersionClient(ucdUrl, getUdClient());

        try {
            versionClient.deleteVersion(id);
        }
        catch (Exception ex) {
            throw new AbortException("Failed to delete component version with id '" + id + "' : " + ex.getMessage());
        }
    }

    /**
     * Trigger application deployment process with latest versions of each component.
     * @param app
     * @param env
     * @param proc
     * @param componentName
     * @param versionName
     * @param listener
     * @return The id of the application process request
     * @throws AbortException
     */
    public String createDefaultProcessRequest(
            String app,
            String env,
            String proc,
            String componentName,
            String versionName,
            BuildListener listener)
    throws AbortException {
        ApplicationClient appClient = new ApplicationClient(ucdUrl, getUdClient());
        List<String> versions = new ArrayList<String>();
        versions.add(versionName);

        Map<String, List<String>> compVersions = new HashMap<String, List<String>>();
        compVersions.put(componentName, versions);

        listener.getLogger().println("Creating application process deployment request.");

        UUID appProc;

        try {
            appProc = appClient.requestApplicationProcess(app, proc, "", env, "", false, compVersions);
        }
        catch (Exception ex) {
            throw new AbortException("Failed to create application process request '" + proc + "' : "
                    + ex.getMessage());
        }

        listener.getLogger().println("Successfully created application process deployment request.");

        return appProc.toString();
    }

    /**
     * Add the link on the component to the component version
     * @param udSite
     * @param compName
     * @param versionName
     * @param linkName
     * @param linkUrl
     * @throws AbortException
     */
    public void addLinkToComp(
            String compName,
            String versionName,
            String linkName,
            String linkUrl)
    throws AbortException
    {
        ComponentClient compClient = new ComponentClient(ucdUrl, getUdClient());
        try {
            compClient.addComponentVersionLink(compName, versionName, linkName, linkUrl);
        }
        catch (Exception ex) {
            throw new AbortException("Failed to add a version link to the component '" + compName + "' : "
                    +  ex.getMessage());
        }
    }

    /**
     * Check the result of an application process
     * @param appClient
     * @param procId
     * @return A boolean value stating whether the process is finished
     * @throws AbortException
     */
    public String checkDeploymentProcessResult(String procId)
    throws AbortException {
        ApplicationClient appClient = new ApplicationClient(ucdUrl, getUdClient());
        String deploymentResult;

        try {
            deploymentResult = appClient.getApplicationProcessStatus(procId);
        }
        catch(Exception ex) {
            throw new AbortException("Failed to acquire status of application process '" + procId + "' : "
                    + ex.getMessage());
        }

        return deploymentResult;
    }

    /**
     * Create and set properties on the component version
     *
     * @param component
     * @param version
     * @param properties
     * @param listener
     * @throws AbortException
     */
    public void setComponentVersionProperties(
            String component,
            String version,
            String properties,
            BuildListener listener)
    throws AbortException {
        Map<String, String> propertiesToSet = readProperties(properties);
        DefaultHttpClient udClient = getUdClient();
        if (!propertiesToSet.isEmpty()) {
            ComponentClient compClient = new ComponentClient(ucdUrl, udClient);
            PropertyClient propClient = new PropertyClient(ucdUrl, udClient);
            VersionClient versionClient = new VersionClient(ucdUrl, udClient);
            JSONObject propSheetDef;
            String propSheetDefId;
            String propSheetDefPath;
            JSONArray existingPropDefJsonArray;

            // acquire prop sheet definition and it's existing propDefs
            try {
                propSheetDef = compClient.getComponentVersionPropSheetDef(component);
                propSheetDefId = (String) propSheetDef.get("id");
                propSheetDefPath = (String) propSheetDef.get("path");
                existingPropDefJsonArray = propClient.getPropSheetDefPropDefs(propSheetDefPath);
            }
            catch (IOException ex) {
                throw new AbortException("An error occurred acquiring component object for component '"
                        + component + "' : " + ex.getMessage());
            }
            catch (JSONException e) {
                throw new AbortException("An error occurred acquiring property definitions of the "
                        + "version property sheet for component '" + component + "' : " + e.getMessage());
            }

            // update existing properties
            for (int i = 0; i < existingPropDefJsonArray.length(); i++) {
                JSONObject propDef;
                String propName;

                try {
                    propDef = existingPropDefJsonArray.getJSONObject(i);
                    propName = propDef.getString("name");
                }
                catch (JSONException ex) {
                    throw new AbortException("An error occurred acquiring an existing property definition "
                            + "for component '" + component + "' : " + ex.getMessage());
                }

                String propValue = propertiesToSet.get(propName);

                if (propValue != null) {
                    try {
                        listener.getLogger().println("Setting version property " + propName);
                        versionClient.setVersionProperty(version, component, propName, propValue, false);
                        listener.getLogger().println("Successfully updated version property " + propName);
                    }
                    catch (IOException ex) {
                        throw new AbortException("An error occurred while setting the value of an existing property '"
                                + propName + "' : " + ex.getMessage());
                    }
                }

                propertiesToSet.remove(propName);
            }

            // create new properties
            if (!propertiesToSet.isEmpty()) {
                listener.getLogger().println("Creating new property definitions.");
                UUID propSheetDefUUID = UUID.fromString(propSheetDefId);

                for (Map.Entry<String, String> property : propertiesToSet.entrySet()) {
                    String propName = property.getKey();
                    String propDescription = "";
                    String propLabel = "";
                    Boolean required = false;
                    String propType = "TEXT";
                    String propValue = property.getValue();

                    try {
                        listener.getLogger().println("Creating property definition for: " + propName);
                        propClient.createPropDef(
                                propSheetDefUUID,
                                propSheetDefPath,
                                propName,
                                propDescription,
                                propLabel,
                                required,
                                propType,
                                "");
                        versionClient.setVersionProperty(version, component, propName, propValue, false);
                        listener.getLogger().println("Successfully created version property " + propName);
                    }
                    catch (IOException ex) {
                        throw new AbortException("An error occurred while creating a new version property '" + propName
                                + "' for version '" + version + "' : " + ex.getMessage());
                    }
                    catch (JSONException ex) {
                        throw new AbortException("An error occurred creating the property definition '" + propName
                                + "' on property sheet with UUID '" + propSheetDefUUID + "' : " + ex.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Load properties into a properties map
     *
     * @param properties The unparsed properties to load
     * @return The loaded properties map
     * @throws AbortException
     */
    private Map<String, String> readProperties(String properties) throws AbortException {
        Map<String, String> propertiesToSet = new HashMap<String, String>();

        for (String line : properties.split("\n")) {
            String[] propDef = line.split("=");

            if (propDef.length >= 2) {
                String propName = propDef[0].trim();
                String propVal = propDef[1].trim();
                propertiesToSet.put(propName, propVal);
            }
            else {
                throw new AbortException("Missing property delimiter '=' in property definition '" + line + "'");
            }
        }

        return propertiesToSet;
    }

    /**
     * Split String of filenames by newline and remove empty/null entries
     * @param Newline separated list of filenames
     * @return Array of filenames
     */
    private String[] splitFiles(String patterns) {
        List<String> newList = new ArrayList<String>();

        String[] patternList = patterns.split("\n");

        for (String pattern : patternList) {
            if (pattern != null && pattern.trim().length() > 0) {
                newList.add(pattern.trim());
            }
        }

        return newList.toArray(new String[newList.size()]);
    }

    /**
     * Check if maintenance mode is enabled on the UCD server
     * @return boolean representing whether or not maintenance mode is enabled
     * @throws AbortException
     */
    public boolean isMaintenanceEnabled() throws AbortException {
        SystemClient sysClient = new SystemClient(ucdUrl, getUdClient());
        boolean maintenanceEnabled;

        try {
            JSONObject systemConfig = sysClient.getSystemConfiguration();
            maintenanceEnabled = systemConfig.getBoolean("enableMaintenanceMode");
        }
        catch (IOException ex) {
            throw new AbortException("Invalid http response code returned when acquiring UCD system configuration:"
                    + ex.getMessage());
        }
        catch (JSONException ex) {
            throw new AbortException("Failed to acquire UCD system configuration: " + ex.getMessage());
        }

        return maintenanceEnabled;
    }

    private DefaultHttpClient getUdClient() {
        DefaultHttpClient udClient;

        if (altUser.isEmpty()) {
            udClient = udSite.getClient();
        }
        else {
            udClient = udSite.getTempClient(altUser, altPassword);
        }

        return udClient;
    }
}
