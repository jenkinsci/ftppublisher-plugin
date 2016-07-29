package com.zanox.hudson.plugins;

import com.jcraft.jsch.SftpException;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Map;

public class EntryCopierPipeline {
	protected static final SimpleDateFormat ID_FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

	private Run<?, ?> build;
	private TaskListener listener;
	private Map<String, String> envVars;
	private URI workSpaceDir;
	private FTPSite ftpSite;
	private boolean flatten;
	private boolean useTimestamps;
	FilePath workspace;

	public EntryCopierPipeline(Run<?, ?> build, FilePath workspace, TaskListener listener, FTPSite ftpSite, boolean flatten, boolean useTimestamps)
	    throws IOException, InterruptedException {
		this.build = build;
		this.listener = listener;
		this.ftpSite = ftpSite;
		this.workspace = workspace;

		envVars = build.getEnvironment(listener);
		workSpaceDir = workspace.toURI().normalize();
		this.flatten = flatten;
		this.useTimestamps = useTimestamps;
	}

	public int copy(Entry entry) throws IOException, SftpException, InterruptedException {

		// prepare sources
		String expanded = Util.replaceMacro(entry.sourceFile, envVars);
		FilePath[] sourceFiles = null;
		String baseSourceDir = workSpaceDir.getPath();

		FilePath tmp = new FilePath(workspace, expanded);

		if (tmp.exists() && tmp.isDirectory()) { // Directory

			sourceFiles = tmp.list("**/*");
			baseSourceDir = tmp.toURI().normalize().getPath();
			listener.getLogger().println("Preparing to copy directory : " + baseSourceDir);

		} else { // Files
			
			sourceFiles = workspace.list(expanded);
			baseSourceDir = workSpaceDir.getPath();
			listener.getLogger().println(workSpaceDir);
			
		}

		if (sourceFiles.length == 0) { // Nothing
			listener.getLogger().println("No file(s) found: " + expanded);
			return 0;
		}

		int fileCount = 0;

		// prepare common dest
		String subRoot = Util.replaceMacro(entry.filePath, envVars);
		if (useTimestamps) {
			subRoot += "/" + ID_FORMATTER.format(build.getTimestamp().getTime());
		}
		ftpSite.changedToProjectRootDir("", listener.getLogger());
		ftpSite.mkdirs(subRoot, listener.getLogger());
		// ftpCurrentDirectory = subRoot;

		for (FilePath sourceFile : sourceFiles) {
			copyFile(sourceFile, subRoot, baseSourceDir);
			fileCount++;
		}

		listener.getLogger().println("transferred " + fileCount + " files to " + subRoot);
		return fileCount;
	}

	public void copyFile(FilePath sourceFile, String destDir, String baseSourceDir) throws IOException, SftpException, InterruptedException {
		ftpCdMkdirs(sourceFile, destDir, baseSourceDir);
		ftpSite.upload(sourceFile, envVars, listener.getLogger());
	}

	private void ftpCdMkdirs(FilePath sourceFile, String entryRootFolder, String baseSourceDir) throws IOException, SftpException,
	    InterruptedException {
		String relativeSourcePath = getRelativeToCopyBaseDirectory(baseSourceDir, sourceFile);
		// if (!ftpCurrentDirectory.equals(relativeSourcePath)) {
		ftpSite.changedToProjectRootDir(entryRootFolder, listener.getLogger());
		// ftpCurrentDirectory = entryRootFolder;
		if (!flatten) {
			ftpSite.mkdirs(relativeSourcePath, listener.getLogger());
			// ftpCurrentDirectory += relativeSourcePath;
		}
		// }
	}

	private String getRelativeToCopyBaseDirectory(String baseDir, FilePath sourceFile) throws IOException, InterruptedException {
		URI sourceFileURI = sourceFile.toURI().normalize();
		String relativeSourceFile = sourceFileURI.getPath().replaceFirst(baseDir, "");
		int lastSlashIndex = relativeSourceFile.lastIndexOf("/");
		if (lastSlashIndex == -1) {
			return ".";
		} else {
			return relativeSourceFile.substring(0, lastSlashIndex);
		}
	}

	// private String ftpCurrentDirectory;

}
