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
import java.util.stream.Stream;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceRequest.*;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.api.TestResult;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.java.ClasspathElement;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.java.Javadoc;
import org.jdrupes.builder.java.JavaSourceFile;
import static org.jdrupes.builder.java.JavaTypes.*;
import static org.jdrupes.builder.mvnrepo.MvnProperties.GroupId;

public class Root extends AbstractProject implements RootProject {

    @Override
    public void prepareProject(Project project) throws Exception {
        project.set(GroupId, "org.jgrapes");
        ProjectPreparation.setupVersion(project);
        ProjectPreparation.setupCommonGenerators(project);
        ProjectPreparation.setupEclipseConfigurator(project);
    }

    public Root() throws IOException {
        super(name("JGrapes"));

        dependency(Expose, project(Core.class));

//        // Build javadoc
//        generator(Javadoc::new)
//            .destination(rootProject().directory().resolve("webpages/javadoc"))
//            .tagletpath(from(new MvnRepoLookup()
//                .resolve("org.jdrupes.taglets:plantuml-taglet:3.1.0",
//                    "net.sourceforge.plantuml:plantuml:1.2023.11"))
//                        .get(requestFor(RuntimeClasspathType)))
//            .taglets(Stream.of("org.jdrupes.taglets.plantUml.PlantUml",
//                "org.jdrupes.taglets.plantUml.StartUml",
//                "org.jdrupes.taglets.plantUml.EndUml"))
//            .addSources(get(new ResourceRequest<FileTree<JavaSourceFile>>(
//                new ResourceType<>() {
//                })))
//            .options("-overview", directory().resolve("overview.md").toString())
//            .options("--add-stylesheet",
//                directory().resolve("misc/javadoc-overwrites.css").toString())
//            .options("--add-script",
//                directory().resolve("misc/highlight.min.js").toString())
//            .options("--add-script",
//                directory().resolve("misc/highlight-all.js").toString())
//            .options("--add-stylesheet",
//                directory().resolve("misc/highlight-default.css").toString())
//            .options("-bottom",
//                readString(directory().resolve("misc/javadoc.bottom.txt")))
//            .options("--allow-script-in-comments")
//            .options("-linksource")
//            .options("-link",
//                "https://docs.oracle.com/en/java/javase/21/docs/api/")
//            .options("-quiet");

        // Commands
        commandAlias("build",
            new ResourceRequest<ClasspathElement>(new ResourceType<>() {})
        /*
         * ,
         * new ResourceRequest<JavadocDirectory>(
         * new ResourceType<>() {
         * })
         */);
        commandAlias("test",
            new ResourceRequest<TestResult>(new ResourceType<>() {}));
        commandAlias("eclipse",
            new ResourceRequest<EclipseConfiguration>(new ResourceType<>() {}));
    }
}
