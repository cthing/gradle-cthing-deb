/*
 * Copyright 2025 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.gradle.plugins.deb;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.PathEntity;
import org.apache.hc.core5.util.Timeout;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.jspecify.annotations.NonNull;


/**
 * Responsible for publishing DEB packages to an APT repository.
 */
public abstract class DebPublishTask extends DefaultTask {

    private static final Timeout REPO_TIMEOUT = Timeout.of(5, TimeUnit.MINUTES);
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    @SuppressWarnings("this-escape")
    public DebPublishTask() {
        setDescription("Publish DEB packages to an APT repository");
        setGroup("Publishing");
    }

    /**
     * Obtains the URL of the repository containing the package.
     *
     * @return URL of the repository
     */
    @Input
    @Optional
    public abstract Property<@NonNull String> getRepositoryUrl();

    /**
     * Obtains the username for accessing the repository containing the package.
     *
     * @return Username to access the repository
     */
    @Input
    @Optional
    public abstract Property<@NonNull String> getRepositoryUsername();

    /**
     * Obtains the password for accessing the repository containing the package.
     *
     * @return Password to access the repository
     */
    @Input
    @Optional
    public abstract Property<@NonNull String> getRepositoryPassword();

    /**
     * Obtains the Debian tasks whose packages need to be published.
     *
     * @return Debian tasks to publish
     */
    @Internal
    public abstract SetProperty<@NonNull DebTask> getDebTasks();

    /**
     * Performs the publishing of the DEB packages to the APT repository.
     */
    @TaskAction
    public void publish() {
        final String repoUrl = getRepositoryUrl().getOrNull();
        if (repoUrl == null) {
            getLogger().lifecycle("Repository URL not defined, publish task is a noop");
        } else {
            try {
                final URI repoUri = new URI(repoUrl.endsWith("/") ? repoUrl : (repoUrl + "/"));
                final Consumer<File> publishProc = "file".equals(repoUri.getScheme())
                                                   ? artifact -> publishLocal(artifact.toPath(), repoUri)
                                                   : artifact -> publishRemote(artifact.toPath(), repoUri);
                getDebTasks().get().forEach(debTask -> debTask.getArtifacts().forEach(publishProc));
            } catch (final URISyntaxException ex) {
                throw new TaskExecutionException(this, ex);
            }
        }
    }

    /**
     * Publishes the package to the local filesystem.
     *
     * @param file Pathname of the Debian package to publish
     * @param uri URI whose path is the destination on the local filesystem.
     */
    private void publishLocal(final Path file, final URI uri) {
        getLogger().info("Publishing {} to local destination {}", file.getFileName(), uri);

        final Path path = Path.of(uri.getPath());
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }
            Files.copy(file, path.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException ex) {
            throw new TaskExecutionException(this, ex);
        }
    }

    /**
     * Publishes the package to a remote Nexus APT repository. Nexus requires the package to be published
     * using a multipart POST. The Apache HTTP client is used because the Java HTTP client does not have
     * built-in multipart support and Nexus is very finicky about multipart uploads.
     *
     * @param file Pathname of the Debian package to publish
     * @param uri URI of the repository to which the package should be published
     */
    private void publishRemote(final Path file, final URI uri) {
        getLogger().info("Publishing {} to remote destination {}", file.getFileName(), uri);

        final HttpPost post = new HttpPost(uri);
        post.setHeader("Content-Type", MULTIPART_FORM_DATA);
        post.setEntity(new PathEntity(file, ContentType.create(MULTIPART_FORM_DATA)));
        if (getRepositoryUsername().isPresent() && getRepositoryPassword().isPresent()) {
            final String authStr = getRepositoryUsername().get() + ":" + getRepositoryPassword().get();
            final String auth = Base64.getEncoder().encodeToString(authStr.getBytes(StandardCharsets.UTF_8));
            post.setHeader("Authorization", "Basic " + auth);
        }

        final RequestConfig config = RequestConfig.custom()
                                                  .setConnectionRequestTimeout(REPO_TIMEOUT)
                                                  .setResponseTimeout(REPO_TIMEOUT)
                                                  .build();

        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build()) {
            client.execute(post, response -> {
                final int status = response.getCode();
                if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
                    final HttpEntity resEntity = response.getEntity();
                    return resEntity != null ? EntityUtils.toString(resEntity) : null;
                } else {
                    throw new IOException("Unable to upload file `" + file + "' - HTTP status " + status);
                }
            });
        } catch (final IOException ex) {
            throw new TaskExecutionException(this, ex);
        }
    }
}
