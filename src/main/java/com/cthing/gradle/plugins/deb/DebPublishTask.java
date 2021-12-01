/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.io.File;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.repository.Repository;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;


/**
 * Responsible for publishing DEB packages to an APT repository.
 */
public class DebPublishTask extends DefaultTask {

    private final Property<String> repositoryUrl;
    private final Property<String> repositoryPath;
    private final Property<AuthenticationInfo> authenticationInfo;

    public DebPublishTask() {
        setDescription("Publish DEB packages to an APT repository");
        setGroup("Publishing");

        final ObjectFactory objects = getProject().getObjects();
        this.repositoryUrl = objects.property(String.class);
        this.repositoryPath = objects.property(String.class).convention("");
        this.authenticationInfo = objects.property(AuthenticationInfo.class);
    }

    @Input
    @Optional
    public Property<String> getRepositoryUrl() {
        return this.repositoryUrl;
    }

    @Input
    @Optional
    public Property<String> getRepositoryPath() {
        return this.repositoryPath;
    }

    @Input
    @Optional
    public Property<AuthenticationInfo> getAuthenticationInfo() {
        return this.authenticationInfo;
    }

    /**
     * Performs the publishing of the DEB packages to the APT repository.
     */
    @TaskAction
    public void publish() {
        final String repoUrl = this.repositoryUrl.getOrNull();
        if (repoUrl == null) {
            getLogger().lifecycle("Repository URL not defined, publish task is a noop");
        } else {
            final Repository repository = new Repository("debs", repoUrl);

            final Wagon wagon = lookupWagonForRepository(repository);
            connectWagonToRepository(wagon, repository);

            try {
                getProject().getTasks().withType(DebTask.class, debTask ->
                        debTask.getArtifacts().forEach(artifact -> uploadFile(wagon, artifact)));
            } finally {
                disconnectWagonFromRepository(wagon);
            }
        }
    }

    /**
     * Attempts to find a Wagon instance that can handle the specified repository.
     *
     * @param repository Repository for which a Wagon instance is desired
     * @return Wagon instance for the specified repository. If a Wagon instance cannot be found, a
     *      {@link GradleException} is thrown
     */
    private Wagon lookupWagonForRepository(final Repository repository) {
        final Wagon wagon;
        switch (repository.getProtocol()) {
            case "http":
            case "https":
                wagon = new HttpWagon();
                break;
            case "file":
                wagon = new FileWagon();
                break;
            default:
                throw new GradleException("Unsupported repository protocol: " + repository.getProtocol());
        }

        final WagonListener listener = new WagonListener();
        wagon.addSessionListener(listener);
        wagon.addTransferListener(listener);
        return wagon;
    }

    /**
     * Associates the specified repository with the specified Wagon instance.
     *
     * @param wagon Wagon instance to associate with the specified repository
     * @param repository Repository to attach to the specified Wagon instance
     */
    private void connectWagonToRepository(final Wagon wagon, final Repository repository) {
        try {
            if (this.authenticationInfo.isPresent()) {
                wagon.connect(repository, this.authenticationInfo.get());
            } else {
                wagon.connect(repository);
            }
        } catch (final WagonException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }
    }

    /**
     * Disconnects the repository connected to the specified Wagon instance.
     *
     * @param wagon Wagon instance whose connected repository is to be disconnected
     */
    private void disconnectWagonFromRepository(final Wagon wagon) {
        try {
            wagon.disconnect();
        } catch (final ConnectionException ex) {
            getLogger().warn("Error while disconnecting from {}", this.repositoryUrl.get(), ex);
        }
    }

    /**
     * Perform the actual upload of the specified file to the repository connected to the specified Wagon instance.
     *
     * @param wagon Instance of wagon connection to a repository into which the specified file will be uploaded
     * @param file File to upload to a repository
     */
    protected void uploadFile(final Wagon wagon, final File file) {
        getLogger().info("Uploading {}", file.getName());

        final String repoPathname = String.format("%s/%s", this.repositoryPath.get(), file.getName());

        try {
            wagon.put(file, repoPathname);
        } catch (final WagonException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }
    }
}
