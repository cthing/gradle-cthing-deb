/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

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
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.PathEntity;
import org.apache.hc.core5.util.Timeout;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;


/**
 * Responsible for publishing DEB packages to an APT repository.
 */
public class DebPublishTask extends DefaultTask {

    private static final Timeout REPO_TIMEOUT = Timeout.of(5, TimeUnit.MINUTES);
    //private static final int REPO_TIMEOUT = 5;      // Minutes
    //private static final String CRLF = "\r\n";

    private final Property<String> repositoryUrl;
    private final Property<String> repositoryUsername;
    private final Property<String> repositoryPassword;

    @SuppressWarnings("this-escape")
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
                                                   ? artifact -> publishLocal(artifact.toPath(), repoUri)
                                                   : artifact -> publishRemote(artifact.toPath(), repoUri);

                getProject().getTasks().withType(DebTask.class, debTask -> debTask.getArtifacts().forEach(publishProc));
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
     * using a multipart POST.
     *
     * @param file Pathname of the Debian package to publish
     * @param uri URI of the repository to which the package should be published
     */
    private void publishRemote(final Path file, final URI uri) {
        getLogger().info("Publishing {} to remote destination {}", file.getFileName(), uri);

        final String auth = Base64.getEncoder()
                            .encodeToString((this.repositoryUsername.get() + ":" + this.repositoryPassword.get()).getBytes(
                                    StandardCharsets.UTF_8));

        final HttpPost post = new HttpPost(uri);

        post.setHeader("Authorization", "Basic " + auth);
        post.setHeader("Content-Type", "multipart/form-data"); // exactly as in curl

        // FileEntity sends file as-is in request body
        final HttpEntity entity = new PathEntity(file, ContentType.create("multipart/form-data"));
        post.setEntity(entity);

        final RequestConfig config = RequestConfig.custom()
                                                  //.setConnectTimeout(REPO_TIMEOUT)        // SUPPRESS CHECKSTYLE Connection timeout
                                                  .setConnectionRequestTimeout(REPO_TIMEOUT) // SUPPRESS CHECKSTYLE Timeout to get connection from pool
                                                  .setResponseTimeout(REPO_TIMEOUT)       // SUPPRESS CHECKSTYLE Socket timeout (read timeout)
                                                  .build();

        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build()) {
            client.execute(post, (ClassicHttpResponse response) -> {
                final int status = response.getCode();
                if (status >= 200 && status < 300) {            // SUPPRESS CHECKSTYLE ok
                    final HttpEntity resEntity = response.getEntity();
                    return resEntity != null ? EntityUtils.toString(resEntity) : null;
                } else {
                    throw new GradleException("Unable to upload file: " + file);
                }
            });
        } catch (final IOException ex) {
            throw new TaskExecutionException(this, ex);
        }
    }

    //    /**
    //     * Publishes the package to a remote Nexus APT repository. Nexus requires the package to be published
    //     * using a multipart POST.
    //     *
    //     * @param file Pathname of the Debian package to publish
    //     * @param uri URI of the repository to which the package should be published
    //     */
    //    private void publishRemote(final Path file, final URI uri) {
    //        getLogger().info("Publishing {} to remote destination {}", file.getFileName(), uri);
    //
    //        //final String boundary = UUID.randomUUID().toString();
    //        //final String preamble = "--" + boundary + CRLF
    //        //        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getFileName() + "\"" + CRLF
    //        //        + "Content-Type: application/octet-stream" + CRLF + CRLF;
    //        //final String epilogue = CRLF + "--" + boundary + "--" + CRLF;
    //
    //        try {
    //            //final InputStream preambleStream = new ByteArrayInputStream(preamble.getBytes(StandardCharsets.UTF_8));
    //            //final InputStream fileStream = Files.newInputStream(file);
    //            //final InputStream epilogueStream = new ByteArrayInputStream(epilogue.getBytes(StandardCharsets.UTF_8));
    //            //final Enumeration<InputStream> multipartBody = Collections.enumeration(List.of(preambleStream,
    //            //                                                                            fileStream,
    //            //                                                                            epilogueStream));
    //            //try (InputStream multipartStream = new SequenceInputStream(multipartBody)) {
    //            final HttpRequest request = HttpRequest.newBuilder()
    //                                                   .uri(uri)
    //                                                   .header("Content-Type", "multipart/form-data")
    //                                                   //.header("Content-Type",
    //                                                   //        "multipart/form-data; boundary=" + boundary)
    //                                                   .POST(HttpRequest.BodyPublishers.ofFile(file))
    //                                                   //.POST(HttpRequest.BodyPublishers.ofInputStream(() -> multipartStream))
    //                                                   .timeout(Duration.of(REPO_TIMEOUT, MINUTES))
    //                                                   .build();
    //            final PasswordAuthentication authentication =
    //                    new PasswordAuthentication(this.repositoryUsername.get(),
    //                                               this.repositoryPassword.get().toCharArray());
    //            final HttpResponse<String> response = HttpClient.newBuilder()
    //                                                            .followRedirects(HttpClient.Redirect.ALWAYS)
    //                                                            .authenticator(new Authenticator() {
    //                                                                @Override
    //                                                                @SuppressWarnings("MethodDoesntCallSuperMethod")
    //                                                                protected PasswordAuthentication getPasswordAuthentication() {
    //                                                                    return authentication;
    //                                                                }
    //                                                            })
    //                                                            .build()
    //                                                            .send(request, HttpResponse.BodyHandlers.ofString());
    //            final int code = response.statusCode();
    //            if (code != HTTP_NO_CONTENT && code != HTTP_CREATED && code != HTTP_OK) {
    //                throw new GradleException("Unable to upload file: " + response.body());
    //            }
    //            //}
    //
    //        } catch (final IOException | InterruptedException ex) {
    //            throw new TaskExecutionException(this, ex);
    //        }
    //    }
}
