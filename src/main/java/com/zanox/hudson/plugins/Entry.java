package com.zanox.hudson.plugins;

import hudson.Extension;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * <p>
 * This class implements the data object for the ftp plugin. The fields can be configured in the job
 * configuration page in hudson.
 * </p>
 * <p>
 * HeadURL: $HeadURL:
 * http://z-bld-02:8080/zxdev/zxant_test_environment/trunk/formatting/codeTemplates.xml $<br />
 * Date: $Date: 2008-04-22 11:51:32 +0200 (Di, 22 Apr 2008) $<br />
 * Revision: $Revision: 2447 $<br />
 * </p>
 * 
 * @author $Author: ZANOX-COM\fit $
 * 
 */
public final class Entry implements Describable<Entry> {

    /**
     * Destination folder for the copy. May contain macros.
     */
    public String filePath;

    /**
     * File name relative to the workspace root to upload. If the sourceFile is directory then all
     * files in that directory will be copied to remote filePath directory recursively
     * <p>
     * May contain macro, wildcard.
     */
    public String sourceFile;

    @DataBoundConstructor
    public Entry(String filePath, String sourceFile) {
        this.filePath = filePath;
        this.sourceFile = sourceFile;
    }

    @Override
    public Descriptor<Entry> getDescriptor() {
        return new DescriptorImpl();
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Entry> {
        @Override
        public String getDisplayName() {
            return "hello";
        }
    }

}
