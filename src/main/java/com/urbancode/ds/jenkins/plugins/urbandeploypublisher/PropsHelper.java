/**
 * (c) Copyright IBM Corporation 2015, 2017.
 * This is licensed under the following license.
 * The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
 package com.urbancode.ds.jenkins.plugins.urbandeploypublisher;

import com.urbancode.ud.client.PropertyClient;
import com.urbancode.ud.client.ComponentClient;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.apache.http.client.methods.HttpGet;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import hudson.model.BuildListener;

public class PropsHelper {
    PropertyClient propClient;
    ComponentClient compClient;

    public void setComponentVersionProperties(String url, List<String> componentNames, String versionName,
            String properties, String user, String pass, BuildListener listener) {

        if (properties.length() > 0) {
            URI ucdUrl = UriBuilder.fromUri(url).build();
            propClient = new PropertyClient(ucdUrl, user, pass);
            compClient = new ComponentClient(ucdUrl, user, pass);
            try {
                Properties propertiesToSet = new Properties();
                propertiesToSet.load(new StringReader(properties));

                for (String componentName : componentNames) {
                    setComponentVersionProperties(url, componentName, versionName, propertiesToSet, listener);
                }
            } catch (Exception e) {
                listener.getLogger().println("An error occured while parsing the properties: " + e.getMessage());
            }
        }
    }

    public void setComponentVersionProperties(String url, String componentName, String versionName,
            Properties propertiesToSet, BuildListener listener) {

        if (!propertiesToSet.isEmpty()) {
            // get the component version
            try {
                JSONObject propSheetDef = compClient.getComponentVersionPropSheetDef(componentName);
                String propSheetDefId = (String)propSheetDef.get("id");
                String propSheetDefPath = (String)propSheetDef.get("path");
                JSONArray existingPropDefs = propClient.getPropSheetDefPropDefs(propSheetDefPath);

                for (int i = 0; i < existingPropDefs.length(); i++) {
                    JSONObject propDef = existingPropDefs.getJSONObject(i);
                    String name = propDef.getString("name");
                    String value = (String)propertiesToSet.getProperty(name);
                    propDef.put("value", value);
                    propertiesToSet.remove(name);
                }

                if (!propertiesToSet.isEmpty()) {
                    listener.getLogger().println("Creating non-existent property definitions.");
                    UUID propSheetDefUUID = UUID.fromString(propSheetDefId);

                    for (Map.Entry<Object, Object> property : propertiesToSet.entrySet()) {
                        String name = (String)property.getKey();
                        String description = "";
                        String label = "";
                        Boolean required = false;
                        String type = "TEXT";
                        String value = (String)property.getValue();

                        try {
                            propClient.createPropDef(propSheetDefUUID, propSheetDefPath, name, description, label,
                                    required, type, value);
                            listener.getLogger().println("Property: " + name + " created successfully.");
                        } catch (IOException ex) {
                            listener.getLogger().println(
                                    "An error occurred while creating a new property definition on property sheet with id "
                                            + propSheetDefUUID + ":" + ex.getMessage());
                        }
                    }
                }

                if (existingPropDefs.length() > 0) {
                    listener.getLogger().println("Updating existing property definitions");

                    try {
                        propClient.updatePropDefs(propSheetDefPath, existingPropDefs, false);
                    } catch (IOException ex) {
                        listener.getLogger().println(
                                "An error occurred while updating an existing property definition on property sheet with path "
                                        + propSheetDefPath + ":" + ex.getMessage());
                    }
                }
            } catch (Exception e) {
                listener.getLogger().println(
                        "An error occured while adding properties to the version: " + e.getMessage());
            }
        }
    }
}
