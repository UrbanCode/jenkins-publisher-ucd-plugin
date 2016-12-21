/*
 * Licensed Materials - Property of IBM Corp.
 * IBM UrbanCode Deploy
 * IBM AnthillPro
 * (c) Copyright IBM Corporation 2002, 2016. All Rights Reserved.
 *
 * U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
 * GSA ADP Schedule Contract with IBM Corp.
 */
package com.urbancode.ds.jenkins.plugins.urbandeploypublisher;

import hudson.AbortException;
import hudson.model.BuildListener;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.util.UUID;

import org.jenkinsci.remoting.RoleChecker;

/**
 * This class is called on a channel to publish artifacts to a component version
 *
 */
public class PublishArtifactsCallable implements Callable<Boolean, Exception> {

    //**********************************************************************************************
    // CLASS
    //**********************************************************************************************
    private static final long serialVersionUID = 34598734957L;

    //**********************************************************************************************
    // INSTANCE
    //**********************************************************************************************
    final private RestClientHelper clientHelper;
    final private String baseDir;
    final private String dirOffset;
    final private String fileIncludePatterns;
    final private String fileExcludePatterns;
    final private String component;
    final private String version;
    final private String description;
    final private BuildListener listener;


    /**
     * Construct a Callable task
     * @param clientHelper the helper object to run UCD rest commands
     * @param baseDir
     * @param dirOffset
     * @param fileIncludePatterns
     * @param fileExcludePatterns
     * @param component
     * @param version
     * @param description
     * @param listener object to receive events that happen during a build
     */
    public PublishArtifactsCallable(
        RestClientHelper clientHelper,
        String baseDir,
        String dirOffset,
        String fileIncludePatterns,
        String fileExcludePatterns,
        String component,
        String version,
        String description,
        BuildListener listener)
    {
        if (fileIncludePatterns == null) {
            fileIncludePatterns = "";
        }
        if (fileExcludePatterns == null) {
            fileExcludePatterns = "";
        }

        this.clientHelper = clientHelper;
        this.baseDir = baseDir;
        this.dirOffset = dirOffset;
        this.fileIncludePatterns = fileIncludePatterns;
        this.fileExcludePatterns = fileExcludePatterns;
        this.component = component;
        this.version = version;
        this.description = description;
        this.listener = listener;
    }

    /**
     * Call task on remote node, otherwise call would default to master node
     * @param channel the name of the node to call a task on
     * @throws AbortException
     */
    public void callOnChannel(VirtualChannel channel) throws AbortException {
        try {
            channel.call(this);
        }
        catch (Exception ex) {
            throw new AbortException("Failed to run build on channel: "
                    + channel + " : " + ex.getMessage());
        }
    }

    /**
     * Run this callable task on the defined channel
     * @return A boolean to represent the task success
     * @throws AbortException
     */
    @Override
    public Boolean call() throws AbortException {
        File workDir = new File(baseDir);

        if (!workDir.exists()) {
            throw new AbortException("Base artifact directory '" + workDir.getAbsolutePath()
                    + "' does not exist!");
        }

        if (dirOffset != null && dirOffset.trim().length() > 0) {
            workDir = new File(workDir, dirOffset.trim());
        }

        listener.getLogger().println("Creating new version: " + version + " on component: " + component);

        UUID versionId = clientHelper.createComponentVersion(version, component, description);

        listener.getLogger().println("Successfully created new component version.");

        listener.getLogger().println("Working Directory: " + workDir.getPath());
        listener.getLogger().println("Includes: " + fileIncludePatterns);
        listener.getLogger().println("Excludes: " + fileExcludePatterns);

        listener.getLogger().println("Adding files to component version.");

        try {
            clientHelper.uploadVersionFiles(
                    workDir,
                    component,
                    version,
                    fileIncludePatterns,
                    fileExcludePatterns);

            listener.getLogger().println("Successfully uploaded files to version.");

        }
        catch (AbortException ex) {
            try {
                listener.getLogger().println("Deleting component version '" + versionId
                        + "' due to failed artifact upload.");
                clientHelper.deleteComponentVersion(versionId);
            }
            catch (AbortException e) {
                listener.error("Failed to delete component version :" + e.getMessage());
            }

            throw ex;
        }

        return true;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        this.checkRoles(checker);
    }
}