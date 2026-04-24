/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2026 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package jdbld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Intent;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.java.JavadocDirectory;
import static org.jdrupes.builder.java.JavaTypes.*;

/// The Class GhPagesPublisher.
///
public class GhPagesPublisher extends AbstractGenerator {

    /**
     * Instantiates a new gh pages publisher.
     *
     * @param project the project
     */
    public GhPagesPublisher(Project project) {
        super(project);
    }

    @Override
    protected <T extends Resource> Collection<T>
            doProvide(ResourceRequest<T> request) {
        if (!request.accepts(new ResourceType<GhPagesPublication>() {})) {
            return Collections.emptyList();
        }

        // Prepare javadocs
        var javadocs = project().rootProject()
            .resources(of(JavadocDirectoryType).using(Intent.Supply));

        // Clone, copy, publish
        var workDir = project().buildDirectory().resolve("gh-pages");
        try {
            var git = cloneRepository(workDir);
            copyJavadocs(javadocs, workDir);
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Update JGrapes javadoc").call();
            git.push()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                    "mnlipp",
                    context().property("jgrapes.repo.access.token", "")))
                .setProgressMonitor(new TextProgressMonitor(
                    context().statusLine().writer(this.toString() + ": ")))
                .call();
        } catch (GitAPIException | IOException e) {
            throw new BuildException().from(this).cause(e);
        }

        @SuppressWarnings("unchecked")
        var result = (T) GhPagesPublication.create();
        return List.of(result);
    }

    private Git cloneRepository(Path workDir)
            throws GitAPIException, InvalidRemoteException, TransportException {
        // Start with a clean Clone
        workDir.toFile().mkdir();
        var files = FileTree.of(project(), workDir, "**/*");
        context().statusLine().update("%s cleaning %s", this, workDir);
        files.cleanup();
        var repoUri = "https://github.com/mnlipp/jgrapes.git";
        var branch = "gh-pages";
        var git = Git.cloneRepository().setURI(repoUri).setBranch(branch)
            .setDirectory(workDir.toFile()).setDepth(1)
            .setProgressMonitor(new TextProgressMonitor(
                context().statusLine().writer(this.toString() + ": ")))
            .call();
        return git;
    }

    private void copyJavadocs(Stream<JavadocDirectory> javadocs, Path workDir)
            throws IOException {
        // Copy javadocs
        context().statusLine().update("%s copying javadocs", this);
        var javadocDir = javadocs.findFirst().map(r -> r.root());
        if (javadocDir.isEmpty()) {
            throw new BuildException().from(this)
                .message("Javadoc not available");
        }
        var javadocFiles = FileTree.of(project(), javadocDir.get(), "**/*");
        for (var iter = javadocFiles.entries().iterator();
                iter.hasNext();) {
            var relPath = iter.next();
            Files.copy(javadocFiles.root().resolve(relPath),
                workDir.resolve("javadoc").resolve(relPath),
                StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
