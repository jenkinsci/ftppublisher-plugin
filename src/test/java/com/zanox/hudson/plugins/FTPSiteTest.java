package com.zanox.hudson.plugins;

import junit.framework.TestCase;

public class FTPSiteTest extends TestCase {

	private String host = "host-name";
	private String profile = "profile-name";

	public void testGetDisplayNameBothPresent() {
		FTPSite site = new FTPSite();
		site.setHostname(host);
		site.setProfileName(profile);
		
		String display = site.getDisplayName();
		assertEquals(profile, display);
	}
	
	public void testGetDisplayNameNoName() {
		FTPSite site = new FTPSite();
		site.setHostname(host);
		site.setProfileName(null);
		
		String display = site.getDisplayName();
		assertEquals(host, display);
		
		site.setProfileName("");
		
		display = site.getDisplayName();
		assertEquals(host, display);
	}
}
