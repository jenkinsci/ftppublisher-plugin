package com.zanox.hudson.plugins;

import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;

/**
 * <p>
 * This class implements the ftp publisher process by using the {@link FTPSite}.
 * </p>
 * <p>
 * HeadURL: $HeadURL: http://z-bld-02:8080/zxdev/zxant_test_environment/trunk/formatting/codeTemplates.xml $<br />
 * Date: $Date: 2008-04-22 11:53:34 +0200 (Di, 22 Apr 2008) $<br />
 * Revision: $Revision: 2451 $<br />
 * </p>
 * 
 * @author $Author: ZANOX-COM\fit $
 * 
 */
public class FTPPublisher extends Notifier implements SimpleBuildStep {

	/**
	 * Hold an instance of the Descriptor implementation of this publisher.
	 */
	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	/**
	 * This is a SimpleDateFormat instance to get a directory name which include a time stamp.
	 */
	protected static final SimpleDateFormat ID_FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	private String siteName;
	public  List<Entry> entries = new ArrayList<Entry>();
	private Boolean useTimestamps = false;
	private Boolean flatten = true;
	private Boolean skip = false;

	/**
	 * The constructor which take a configured ftp site name to publishing the artifacts.
	 *
	 * @param siteName
	 *          the name of the ftp site configuration to use
	 */
	@DataBoundConstructor
	public FTPPublisher(String siteName, List<Entry> entries, boolean useTimestamps, boolean skip, boolean flatten) {
		this.skip = skip;
		this.flatten = flatten;
		this.entries = entries;
		this.useTimestamps = useTimestamps;
		this.siteName = siteName;
		this.siteName = getSiteName();
	}



	public void setUseTimestamps(boolean useTimestamps) {
		this.useTimestamps = useTimestamps;
	}

	public boolean isUseTimestamps() {
		return useTimestamps;
	}

	public void setFlatten(boolean flatten) {
		this.flatten = flatten;
	}
	public boolean getFlatten() { return flatten; }
	public boolean getUseTimestamps() { return useTimestamps;}
	public boolean getSkip() {return skip;}

	public boolean isFlatten() {
		return flatten;
	}
	
	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	public boolean isSkip() {
		return skip;
	}

	public FTPPublisher() {
		int a = 2;
	}


	/**
	 * The getter for the entries field. (this field is set by the UI part of this plugin see config.jelly file)
	 * 
	 * @return the value of the entries field
	 */
	public List<Entry> getEntries() {
		return entries;
	}


    public String getSiteName() {
		String name = siteName;
		if (name == null) {
			FTPSite[] sites = DESCRIPTOR.getSites();
			if (sites.length > 0) {
				name = sites[0].getName();
			}
		}
		return name;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	/**
	 * This method returns the configured FTPSite object which match the siteName of the FTPPublisher instance. (see Manage Hudson and System
	 * Configuration point FTP)
	 * 
	 * @return the matching FTPSite or null
	 */
	public FTPSite getSite() {
		FTPSite[] sites = DESCRIPTOR.getSites();
		if (siteName == null && sites.length > 0) {
			// default
			return sites[0];
		}
		for (FTPSite site : sites) {
			if (site.getDisplayName().equals(siteName)) {
				return site;
			}
		}
		return null;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	@Override
	public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
		performHelper(run, filePath, run.getEnvironment(listener), launcher, listener.getLogger());
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param build
	 * @param launcher
	 * @param listener
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 *           {@inheritDoc}
	 * @see hudson.tasks.BuildStep
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		return performHelper(build, build.getWorkspace(), build.getEnvironment(listener), launcher, listener.getLogger());
	}

	public boolean performHelper(@Nonnull Run<?, ?> build, @Nonnull FilePath filePath, EnvVars envVars, @Nonnull Launcher launcher, @Nonnull PrintStream logger) throws InterruptedException, IOException {
		if (skip != null && skip) {
			logger.println("Publish artifacts to FTP - Skipping... ");
			return true;
		}
		if (build.getResult() == Result.FAILURE || build.getResult() == Result.ABORTED) {
			// build failed. don't post
			return true;
		}

		FTPSite ftpsite = null;
		try {
			ftpsite = getSite();
			logger.println("Connecting to " + ftpsite.getHostname());
			ftpsite.createSession();



			EntryCopier copier = new EntryCopier(build, filePath, logger, ftpsite, envVars, flatten, useTimestamps);

			int copied = 0;

			for (Entry e : entries) {
				copied += copier.copy(e);
			}

			logger.println("Transfered " + copied + " files.");

		} catch (Throwable th) {
			logger.println("Failed to upload files");
			build.setResult(Result.UNSTABLE);
		} finally {
			if (ftpsite != null) {
				ftpsite.closeSession();
			}
		}

		return true;
	}



	/**
	 * <p>
	 * This class holds the metadata for the FTPPublisher.
	 * </p>
	 * 
	 * @author $Author: ZANOX-COM\fit $
	 * @see Descriptor
	 */
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		private final CopyOnWriteList<FTPSite> sites = new CopyOnWriteList<FTPSite>();

		/** Whether to flatten files by default */
		private boolean flattenFilesSelectedByDefault;

		/**
		 * The default constructor.
		 */
		public DescriptorImpl() {
			super(FTPPublisher.class);
			load();
		}
		
		public void setFlattenFilesSelectedByDefault(
				boolean flattenFilesSelectedByDefault) {
			this.flattenFilesSelectedByDefault = flattenFilesSelectedByDefault;
		}
		
		public boolean isFlattenFilesSelectedByDefault() {
			return flattenFilesSelectedByDefault;
		}

		/**
		 * The name of the plugin to display them on the project configuration web page.
		 * 
		 * {@inheritDoc}
		 * 
		 * @return {@inheritDoc}
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return "Publish artifacts to FTP";
		}

		/**
		 * Return the location of the help document for this publisher.
		 * 
		 * {@inheritDoc}
		 * 
		 * @return {@inheritDoc}
		 * @see hudson.model.Descriptor#getHelpFile()
		 */
		@Override
		public String getHelpFile() {
			return "/plugin/ftppublisher/help.html";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		/**
		 * This method is called by hudson if the user has clicked the add button of the FTP repository hosts point in the System Configuration
		 * web page. It's create a new instance of the {@link FTPPublisher} class and added all configured ftp sites to this instance by calling
		 * the method {@link FTPPublisher#getEntries()} and on it's return value the addAll method is called.
		 * 
		 * {@inheritDoc}
		 * 
		 * @param req
		 *          {@inheritDoc}
		 * @return {@inheritDoc}
		 * @see hudson.model.Descriptor#newInstance(org.kohsuke.stapler.StaplerRequest)
		 */
		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) {
			FTPPublisher pub = new FTPPublisher();
			pub.setFlatten(formData.getBoolean("flatten"));
			pub.setUseTimestamps(formData.getBoolean("useTimestamps"));
			req.bindParameters(pub, "publisher.");
			req.bindParameters(pub, "ftp.");
			pub.getEntries().addAll(req.bindParametersToList(Entry.class, "ftp.entry."));
			return pub;
		}

		public boolean isFlatten(FTPPublisher pub) {
			if (pub != null) {
				return pub.isFlatten();
			} else {
				return flattenFilesSelectedByDefault;
			}
		}
		
		/**
		 * The getter of the sites field.
		 * 
		 * @return the value of the sites field.
		 */
		public FTPSite[] getSites() {
			Iterator<FTPSite> it = sites.iterator();
			int size = 0;
			while (it.hasNext()) {
				it.next();
				size++;
			}
			return sites.toArray(new FTPSite[size]);
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param req
		 *          {@inheritDoc}
		 * @return {@inheritDoc}
		 * @see hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest)
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) {
			sites.replaceBy(req.bindParametersToList(FTPSite.class, "ftp."));
			flattenFilesSelectedByDefault = formData.getBoolean("flattenFilesSelectedByDefault");
			save();
			return true;
		}

		/**
		 * This method validates the current entered ftp configuration data. That is made by create a ftp connection.
		 * 
		 * @param request
		 *          the current {@link javax.servlet.http.HttpServletRequest}
		 */
		public FormValidation doLoginCheck(StaplerRequest request) {
			String hostname = Util.fixEmpty(request.getParameter("hostname"));
			if (hostname == null) { // hosts is not entered yet
				return FormValidation.ok();
			}
			FTPSite site = new FTPSite(hostname, request.getParameter("port"), request.getParameter("timeOut"), request.getParameter("user"),
			    request.getParameter("pass"));
			site.setFtpDir(request.getParameter("ftpDir"));
			try {
				site.createSession();
				site.closeSession();

				return FormValidation.ok();
			} catch (Exception e) {
				return FormValidation.error(e.getMessage());
			}
		}
	}
}
