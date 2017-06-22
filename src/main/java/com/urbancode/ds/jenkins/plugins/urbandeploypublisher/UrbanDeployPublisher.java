/**
 * (c) Copyright IBM Corporation 2015, 2017.
 * This is licensed under the following license.
 * The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
package com.urbancode.ds.jenkins.plugins.urbandeploypublisher;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.util.Secret;

import java.io.IOException;
import java.util.Date;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * <p>
 * This class implements the UrbanDeploy publisher process by using the
 * {@link com.urbancode.ds.jenkins.plugins.urbandeploypublisher.UrbanDeploySite}
 * .
 * </p>
 */
public class UrbanDeployPublisher extends Notifier {

    // Hold an instance of the Descriptor implementation for the
    // UrbanDeployPublisher.
    @Extension
    public static final UrbanDeployPublisherDescriptor DESCRIPTOR = new UrbanDeployPublisherDescriptor();

    private String siteName;
    private String altUser;
    private Secret altPassword;
    private Boolean altAdminUser;
    private String component;
    private String baseDir;
    private String directoryOffset;
    private String fileIncludePatterns;
    private String fileExcludePatterns;
    private String version;
    private Boolean skip = false;
    private Boolean deploy = false;
    private String deployApp;
    private String deployEnv;
    private String deployProc;
    private EnvVars envVars = null;
    private String properties;
    private String description;

    /**
     * Constructor used for data-binding fields from the corresponding
     * config.jelly
     *
     * @param siteName The profile name of the UrbanDeploy site
     * @param altUser The alternative username to connect to the UCD server
     * @param altPassword The alternative password to connect to the UCD server
     * @param altAdminUser Specifies if the alternative user has administrative privileges
     * @param component The name of the component on the UCD server
     * @param versionName The name of the component version on the UCD server
     * @param directoryOffset The offset from the base directory to pull
     *            artifacts
     * @param baseDir The base directory to pull artifacts from
     * @param fileIncludePatterns A list of patterns to include
     * @param fileExcludePatterns A list of patterns to exclude
     * @param skip A boolean to specify if version publishing should be skipped
     * @param deploy A boolean to specify if the version should be deployed
     * @param deployApp The application to deploy to on the UCD server
     * @param deployEnv The environment to deploy in on the UCD server
     * @param deployProc The application process to use for deployment on the
     *            UCD server
     * @param properties Any properties to create on the new version
     * @param description A description for the new component version
     */
    @DataBoundConstructor
    public UrbanDeployPublisher(String siteName, String altUser, Secret altPassword, Boolean altAdminUser,
            String component, String versionName, String directoryOffset, String baseDir,
            String fileIncludePatterns, String fileExcludePatterns, Boolean skip, Boolean deploy,
            String deployApp, String deployEnv, String deployProc, String properties, String description) {
        this.altUser = altUser;
        this.altPassword = altPassword;
        this.altAdminUser = altAdminUser;
        this.component = component;
        this.version = versionName;
        this.baseDir = baseDir;
        this.directoryOffset = directoryOffset;
        this.fileIncludePatterns = fileIncludePatterns;
        this.fileExcludePatterns = fileExcludePatterns;
        this.siteName = siteName;
        this.skip = skip;
        this.deploy = deploy;
        this.deployApp = deployApp;
        this.deployEnv = deployEnv;
        this.deployProc = deployProc;
        this.properties = properties;
        this.description = description;
    }

    // **********************************************************************************************
    // Accessors and mutators required for config.jelly access
    // **********************************************************************************************

    public String getSiteName() {
        String name = siteName;
        if (name == null) {
            UrbanDeploySite[] sites = DESCRIPTOR.getSites();
            if (sites.length > 0) {
                name = sites[0].getProfileName();
            }
        }
        return name;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getAltUser() {
        return altUser;
    }

    public void setAltUser(String altUser) {
        this.altUser = altUser;
    }

    public Secret getAltPassword() {
        return altPassword;
    }

    public void setAltPassword(Secret altPassword) {
        this.altPassword = altPassword;
    }

    public boolean isAltAdminUser() {
        return altAdminUser;
    }

    public void setAltAdminUser(boolean altAdminUser) {
        this.altAdminUser = altAdminUser;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getDirectoryOffset() {
        return directoryOffset;
    }

    public void setDirectoryOffset(String directoryOffset) {
        this.directoryOffset = directoryOffset;
    }

    public String getFileIncludePatterns() {
        if (fileIncludePatterns == null || fileIncludePatterns.trim().length() == 0) {
            fileIncludePatterns = "**/*";
        }
        return fileIncludePatterns;
    }

    public void setFileIncludePatterns(String fileIncludePatterns) {
        this.fileIncludePatterns = fileIncludePatterns;
    }

    public String getFileExcludePatterns() {
        return fileExcludePatterns;
    }

    public void setFileExcludePatterns(String fileExcludePatterns) {
        this.fileExcludePatterns = fileExcludePatterns;
    }

    public String getVersionName() {
        return version;
    }

    public void setVersionName(String versionName) {
        this.version = versionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setDeploy(boolean deploy) {
        this.deploy = deploy;
    }

    public boolean isDeploy() {
        return deploy;
    }

    public void setDeployApp(String deployApp) {
        this.deployApp = deployApp;
    }

    public String getDeployApp() {
        return deployApp;
    }

    public void setDeployEnv(String deployEnv) {
        this.deployEnv = deployEnv;
    }

    public String getDeployEnv() {
        return deployEnv;
    }

    public void setDeployProc(String deployProc) {
        this.deployProc = deployProc;
    }

    public String getDeployProc() {
        return deployProc;
    }

    /**
     * Obtain the configured UrbanDeploySite object which matches the siteName
     * of the UrbanDeployPublisher instance. (see Manage Hudson and System
     * Configuration point UrbanDeploy)
     *
     * @return the matching UrbanDeploySite or null
     */
    public UrbanDeploySite getSite() {
        UrbanDeploySite[] sites = DESCRIPTOR.getSites();

        if (siteName == null && sites.length > 0) {
            // default
            return sites[0];
        }

        for (UrbanDeploySite site : sites) {
            if (site.getDisplayName().equals(siteName)) {
                return site;
            }
        }

        return null;
    }

    /**
     * Ensures that the outcome of the previous build will be available
     *
     * @return the setting for concurrent builds
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        // BUILD means this step will only be run after the previous build is
        // fully completed
        return BuildStepMonitor.BUILD;
    }

    /**
     * {@inheritDoc}
     *
     * @param build
     * @param launcher
     * @param listener
     * @return whether or not the build can continue
     * @throws AbortException
     * @throws InterruptedException
     * @throws java.io.IOException {@inheritDoc}
     * @see hudson.tasks.BuildStep#perform(hudson.model.Build, hudson.Launcher,
     *      hudson.model.BuildListener)
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws AbortException, InterruptedException, IOException {

        if (build.getResult() == Result.FAILURE || build.getResult() == Result.ABORTED) {
            throw new AbortException("Skip version deployment in IBM UrbanCode Deploy - build failed or aborted.");
        }

        RestClientHelper clientHelper;
        UrbanDeploySite udSite = getSite();
        boolean adminUser;

        if (getAltUser().isEmpty()) {
            adminUser = udSite.isAdminUser();

            clientHelper = new RestClientHelper(
                    udSite.getUri(),
                    udSite, udSite.getUser(),
                    udSite.getPassword());
        }
        else {
            listener.getLogger().println("Running job as alternative user '" + getAltUser() + "'.");

            adminUser = isAltAdminUser();

            clientHelper = new RestClientHelper(
                    udSite.getUri(),
                    udSite, getAltUser(),
                    getAltPassword());
        }

        if (adminUser) {
            if (clientHelper.isMaintenanceEnabled()) {
                throw new AbortException("UrbanCode Deploy is in maintenance mode, "
                        + "and no processes may be run.");
            }
        }


        envVars = build.getEnvironment(listener); // used to resolve environment
                                                  // variables in the build
                                                  // environment

        String resolvedComponent = envVars.expand(component);
        String resolvedVersion = envVars.expand(version);

        if (skip) {
            listener.getLogger().println("Skip artifacts upload to IBM UrbanCode Deploy - step disabled.");
        }
        else {
            String resolvedBaseDir = envVars.expand(baseDir);
            String resolvedFileIncludePatterns = envVars.expand(fileIncludePatterns);
            String resolvedFileExcludePatterns = envVars.expand(fileExcludePatterns);
            String resolvedDirectoryOffset = envVars.expand(directoryOffset);
            String resolvedProperties = envVars.expand(properties);
            String resolvedDescription = envVars.expand(description);

            // create version and upload files
            PublishArtifactsCallable task = new PublishArtifactsCallable(
                    clientHelper,
                    resolvedBaseDir,
                    resolvedDirectoryOffset,
                    resolvedFileIncludePatterns,
                    resolvedFileExcludePatterns,
                    resolvedComponent,
                    resolvedVersion,
                    resolvedDescription,
                    listener);

            // task must run on the correct channel
            listener.getLogger().println(launcher.getChannel().toString());
            task.callOnChannel(launcher.getChannel());

            // create properties on version
            if (resolvedProperties.length() > 0) {
                clientHelper.setComponentVersionProperties(resolvedComponent,
                                                           resolvedVersion,
                                                           resolvedProperties,
                                                           listener);
            }

            // add component version link
            String linkName = "Jenkins Job " + build.getDisplayName();
            String linkUrl = Hudson.getInstance().getRootUrl() + build.getUrl();
            listener.getLogger().println("Adding Jenkins job link " + linkUrl);
            try {
                clientHelper.addLinkToComp(resolvedComponent, resolvedVersion, linkName, linkUrl);
            } catch (Exception ex){
                // If link cannot be added to the component version, the entire import shouldn't crash
                listener.getLogger().println("[Warning] " +  ex.getMessage());
                listener.getLogger().println("\t View the server logs for a complete stack trace.");
            }
        }

        if (deploy) {
            String resolvedDeployApp = envVars.expand(deployApp);
            String resolvedDeployEnv = envVars.expand(deployEnv);
            String resolvedDeployProc = envVars.expand(deployProc);

            if (resolvedDeployApp == null || resolvedDeployApp.trim().length() == 0) {
                throw new AbortException("Deploy Application is a required field if Deploy is selected.");
            }
            if (resolvedDeployEnv == null || resolvedDeployEnv.trim().length() == 0) {
                throw new AbortException("Deploy Environment is a required field if Deploy is selected.");
            }
            if (resolvedDeployProc == null || resolvedDeployProc.trim().length() == 0) {
                throw new AbortException("Deploy Process is a required field if Deploy is selected.");
            }

            listener.getLogger().println("Starting deployment process " + resolvedDeployProc + " of application "
                    + deployApp + " in environment " + resolvedDeployEnv);

            String requestId = clientHelper.createDefaultProcessRequest(
                    resolvedDeployApp,
                    resolvedDeployEnv,
                    resolvedDeployProc,
                    resolvedComponent,
                    resolvedVersion,
                    listener);

            listener.getLogger().println("Deployment request created with id: " + requestId);
            listener.getLogger().println("Deployment of application request " + requestId
                    + " of application " + resolvedDeployApp + " is running.");
            long startTime = new Date().getTime();

            boolean processFinished = false;
            String deploymentResult = "";

            while (!processFinished) {
                deploymentResult = clientHelper.checkDeploymentProcessResult(requestId);

                if (!deploymentResult.equalsIgnoreCase("NONE")
                        && !deploymentResult.isEmpty()
                        && !deploymentResult.equalsIgnoreCase("SCHEDULED FOR FUTURE")) {
                    processFinished = true;

                    if (deploymentResult.equalsIgnoreCase("FAULTED")
                            || deploymentResult.equalsIgnoreCase("FAILED TO START")) {
                        throw new AbortException("Deployment process failed with result " + deploymentResult);
                    }
                }

                // give application process more time to complete
                Thread.sleep(3000);
            }

            long duration = (new Date().getTime() - startTime) / 1000;

            listener.getLogger().println("Finished deployment of application request " + requestId
                    + " for application " + resolvedDeployApp + " in environment " + resolvedDeployEnv
                    + " in " + duration + " seconds");
            listener.getLogger().println("The deployment " + deploymentResult
                    + ". See the UrbanCode Deploy deployment logs for details.");
        }
        else {
            listener.getLogger().println("Skip deploy application to IBM UrbanCode Deploy - step disabled.");
        }
        return true;
    }
}
