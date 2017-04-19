/**
 * © Copyright IBM Corporation 2015, 2017.
 * This is licensed under the following license.
 * The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
package com.urbancode.ds.jenkins.plugins.urbandeploypublisher;

import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import hudson.util.FormFieldValidator;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UrbanDeployPublisherDescriptor extends BuildStepDescriptor<Publisher> {
    /**
     * <p> This class holds the metadata for the UrbanDeployPublisher. </p>
     */
    private final CopyOnWriteList<UrbanDeploySite> sites = new CopyOnWriteList<UrbanDeploySite>();
    /**
     * The default constructor.
     */
    public UrbanDeployPublisherDescriptor() {
        super(UrbanDeployPublisher.class);
        load(); // load serializable fields of this instance from persisted storage
    }

    /**
     * The name of the plugin to display them on the project configuration web page.
     * <p/>
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @see hudson.model.Descriptor#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return "Publish artifacts to IBM UrbanCode Deploy";
    }

    /**
     * Return the location of the help document for this publisher.
     * <p/>
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @see hudson.model.Descriptor#getHelpFile()
     */
    @Override
    public String getHelpFile() {
        return "/plugin/ibm-ucdeploy-publisher/help.html";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }


    /**
     * The getter of the sites field.
     *
     * @return the value of the sites field.
     */
    public UrbanDeploySite[] getSites() {
        return sites.toArray(new UrbanDeploySite[sites.size()]);
    }

    /**
     * Replace sites with user defined site values from repeatable global property
     * {@inheritDoc}
     *
     * @param req {@inheritDoc}
     * @return {@inheritDoc}
     * @see hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest)
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        //sites.replaceBy(req.bindParametersToList(UrbanDeploySite.class, "ud."));
        sites.replaceBy(req.bindJSONToList(UrbanDeploySite.class, formData.get("sites")));
        save(); // save serializable fields of this instance to persisted storage
        return super.configure(req, formData);
    }


    /**
     * Verify connectivity to the UCD site
     * @param req
     * @param rsp
     * @param url
     * @param user
     * @param password
     * @param trustAllCerts
     * @throws IOException
     * @throws ServletException
     * @deprecated FormFieldValidator
     */
    @Deprecated
    public void doTestConnection(StaplerRequest req, StaplerResponse rsp, @QueryParameter("url") final String url,
                                 @QueryParameter("user") final String user,
                                 @QueryParameter("password") final String password,
                                 @QueryParameter("adminUser") final boolean adminUser,
                                 @QueryParameter("trustAllCerts") final boolean trustAllCerts)
    throws IOException, ServletException {
        new FormFieldValidator(req, rsp, true) {
            protected void check()
                    throws IOException, ServletException {
                try {
                    UrbanDeploySite site = new UrbanDeploySite(null, url, user, password, adminUser, trustAllCerts);
                    site.verifyConnection();
                    ok("Success");
                }
                catch (Exception e) {
                    error(e.getMessage());
                }
            }
        }.process();
    }
}
