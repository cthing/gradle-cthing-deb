/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import com.cthing.gradle.plugins.util.FileUtils;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.time.temporal.ChronoUnit.MINUTES;


/**
 * Responsible for publishing DEB packages to an APT repository.
 */
public class DebPublishTask extends DefaultTask {

    private static final int REPO_TIMEOUT = 5;      // Minutes

    private final Property<String> repositoryUrl;
    private final Property<String> repositoryUsername;
    private final Property<String> repositoryPassword;

    public DebPublishTask() {
        setDescription("Publish DEB packages to an APT repository");
        setGroup("Publishing");

        final ObjectFactory objects = getProject().getObjects();
        this.repositoryUrl = objects.property(String.class);
        this.repositoryUsername = objects.property(String.class);
        this.repositoryPassword = objects.property(String.class);
    }

    @Input
    @Optional
    public Property<String> getRepositoryUrl() {
        return this.repositoryUrl;
    }

    @Input
    @Optional
    public Property<String> getRepositoryUsername() {
        return this.repositoryUsername;
    }

    @Input
    @Optional
    public Property<String> getRepositoryPassword() {
        return this.repositoryPassword;
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
            try {
                final URI repoUri = new URI(repoUrl.endsWith("/") ? repoUrl : (repoUrl + "/"));
                final Consumer<File> publishProc = "file".equals(repoUri.getScheme())
                                                   ? artifact -> publishLocal(artifact, repoUri)
                                                   : artifact -> publishRemote(artifact, repoUri);

                getProject().getTasks().withType(DebTask.class, debTask -> debTask.getArtifacts().forEach(publishProc));
            } catch (final URISyntaxException ex) {
                throw new TaskExecutionException(this, ex);
            }
        }
    }

    private void publishLocal(final File file, final URI uri) {
        getLogger().info("Publishing {} to {}", file.getName(), uri);

        final File path = new File(uri.getPath());
        if (!path.exists() && !path.mkdirs()) {
            throw new GradleException("Could not create directory: " + path);
        }
        FileUtils.copyFile(file, path, true);
    }

    private void publishRemote(final File file, final URI uri) {
        getLogger().info("Publishing {} to {}", file.getName(), uri);

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                                                   .uri(uri)
                                                   .POST(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                                                   .timeout(Duration.of(REPO_TIMEOUT, MINUTES))
                                                   .build();
            final PasswordAuthentication authentication =
                    new PasswordAuthentication(this.repositoryUsername.get(),
                                               this.repositoryPassword.get().toCharArray());
            final HttpResponse<String> response = HttpClient.newBuilder()
                                                            .followRedirects(HttpClient.Redirect.ALWAYS)
                                                            .authenticator(new Authenticator() {
                                                                @Override
                                                                protected PasswordAuthentication getPasswordAuthentication() {
                                                                    return authentication;
                                                                }
                                                            })
                                                            .build()
                                                            .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HTTP_OK && response.statusCode() != HTTP_CREATED) {
                throw new GradleException("Unable to upload file: " + response.body());
            }
        } catch (final IOException | InterruptedException ex) {
            throw new TaskExecutionException(this, ex);
        }
    }
}
