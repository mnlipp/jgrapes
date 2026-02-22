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
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VENDOR;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION;
import static jdbld.ExtProps.GitApi;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Scm;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.InvalidPatternException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ExecResult;
import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.api.Project;
import static org.jdrupes.builder.api.Project.Properties.Version;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.api.TestResult;
import org.jdrupes.builder.bnd.BndAnalyzer;
import org.jdrupes.builder.bnd.BndBaselineEvaluation;
import org.jdrupes.builder.bnd.BndBaseliner;
import org.jdrupes.builder.core.AbstractRootProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.eclipse.EclipseConfigurator;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceCollector;
import static org.jdrupes.builder.java.JavaTypes.JavaSourceTreeType;
import org.jdrupes.builder.java.JavadocDirectory;
import org.jdrupes.builder.java.LibraryBuilder;
import org.jdrupes.builder.java.LibraryJarFile;
import org.jdrupes.builder.java.ManifestAttributes;
import org.jdrupes.builder.junit.JUnitTestRunner;
import org.jdrupes.builder.mvnrepo.JavadocJarBuilder;
import static org.jdrupes.builder.mvnrepo.MvnProperties.GroupId;
import org.jdrupes.builder.mvnrepo.MvnPublication;
import org.jdrupes.builder.mvnrepo.MvnPublisher;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.mvnrepo.PomFile;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.jdrupes.builder.mvnrepo.SourcesJarGenerator;
import org.jdrupes.gitversioning.api.VersionEvaluator;
import org.jdrupes.gitversioning.core.DefaultTagFilter;
import org.jdrupes.gitversioning.core.MavenStyleTagProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class Root extends AbstractRootProject {

    @Override
    public void prepareProject(Project project) throws Exception {
        project.set(GroupId, "org.jgrapes");
        setupVersion(project);
        setupCommonGenerators(project);
        setupEclipseConfigurator(project);
    }

    public Root() throws IOException {
        super(name("JGrapes"));

        dependency(Expose, project(Core.class));
        dependency(Expose, project(Util.class));
        dependency(Expose, project(IO.class));
        dependency(Expose, project(Http.class));
        dependency(Expose, project(HttpFreemarker.class));
        dependency(Expose, project(Mail.class));

        // Build javadoc
        generator(JGrapesJavadoc::new);

        // Publish gh-pages
        generator(GhPagesPublisher::new);

        // Commands
        commandAlias("build").projects("**")
            .resources(of(LibraryJarFile.class).using(Supply));
        commandAlias("javadoc").resources(of(JavadocDirectory.class));
        commandAlias("pomFile").resources(of(PomFile.class));
        commandAlias("mavenPublication").resources(of(MvnPublication.class));
        commandAlias("test").resources(of(TestResult.class));
        commandAlias("eclipse").resources(of(EclipseConfiguration.class));
        commandAlias("runGreeter")
            .resources(of(ExecResult.class).withName("Greeter"));
        commandAlias("runEchoUntilQuit")
            .resources(of(ExecResult.class).withName("EchoUntilQuit"));
        commandAlias("runEchoServer")
            .resources(of(ExecResult.class).withName("EchoServer"));
        commandAlias("baseline").resources(of(BndBaselineEvaluation.class));
        commandAlias("ghPagesPublication")
            .resources(of(GhPagesPublication.class));
    }

    private static void setupVersion(Project project)
            throws IOException, GitAPIException, InvalidPatternException {
        if (project instanceof RootProject) {
            project.set(GitApi, Git.open(project.directory().toFile()));
        }

        // Use shortened project name for tags
        var shortened = project.name().startsWith(project.get(GroupId) + ".")
            ? project.name()
                .substring(project.<String> get(GroupId).length() + 1)
            : project.name();
        if ("manager".equals(shortened)) {
            shortened = "manager-app";
        }
        if ("http".equals(shortened)) {
            shortened = "http-base";
        }
        var tagPrefix = shortened.replace('.', '-') + "-";

        var evaluator = VersionEvaluator
            .forRepository(project.<Git> get(GitApi).getRepository())
            .tagFilter(new DefaultTagFilter().prepend(tagPrefix))
            .tagProcessor(new MavenStyleTagProcessor()
                .ignoreBranches("testing/.*", "release/.*", "develop/.*"));
        project.set(Version, evaluator.version());
    }

    private static void setupCommonGenerators(Project project) {
        if (project instanceof JavaProject) {
            if (!(project instanceof MergedTestProject)) {
                project.generator(JavaCompiler::new)
                    .addSources(Path.of("src"), "**/*.java")
                    .options("--release", "21");
                project.generator(JavaResourceCollector::new)
                    .add(Path.of("resources"), "**/*");
            } else {
                project.generator(JavaCompiler::new).addSources(Path.of("test"),
                    "**/*.java").options("--release", "21");
                project.generator(JavaResourceCollector::new).add(Path.of(
                    "test-resources"), "**/*");
                project.dependency(Consume, new MvnRepoLookup()
                    .resolve("junit:junit:4.13.2")
                    .bom("org.junit:junit-bom:5.14.2")
                    .resolve("org.junit.jupiter:junit-jupiter-api")
                    .resolve("org.junit.jupiter:junit-jupiter-params")
                    .resolve("org.junit.jupiter:junit-jupiter-engine",
                        "org.junit.vintage:junit-vintage-engine",
                        "net.jodah:concurrentunit:0.4.2"));
                project.dependency(Supply, JUnitTestRunner::new)
                    .syncOn(Root.class);
            }
        }
        if (project instanceof JavaLibraryProject) {
            // Generate POM
            project.generator(PomFileGenerator::new).adaptPom(model -> {
                model.setDescription("See URL.");
                model.setUrl("http://mnlipp.github.io/jgrapes/");
                var scm = new Scm();
                scm.setUrl("scm:git@github.com:mnlipp/jgrapes.git");
                scm.setConnection(
                    "scm:git@github.com:mnlipp/jgrapes.git");
                scm.setDeveloperConnection(
                    "git@github.com:mnlipp/jgrapes.git");
                model.setScm(scm);
                var license = new License();
                license.setName("AGPL 3.0");
                license.setUrl("https://www.gnu.org/licenses/agpl-3.0.en.html");
                license.setDistribution("repo");
                model.setLicenses(List.of(license));
                var developer = new Developer();
                developer.setId("mnlipp");
                developer.setName("Michael N. Lipp");
                model.setDevelopers(List.of(developer));
            });

            // Build the library / bundle
            var gen = project.generator(LibraryBuilder::new)
                .addFrom(project.providers().select(Supply))
                .addAttributeValues(Map.of(
                    IMPLEMENTATION_TITLE, project.name(),
                    IMPLEMENTATION_VERSION, project.get(Version),
                    IMPLEMENTATION_VENDOR, "Michael N. Lipp (mnl@mnl.de)")
                    .entrySet().stream())
                .addManifestAttributes(project.resources(
                    project.of(ManifestAttributes.class).using(Consume)))
                .addEntries(project.resources(
                    project.of(PomFile.class).using(Supply))
                    .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                        .resolve((String) project.get(GroupId))
                        .resolve(project.name())
                        .resolve("pom.xml"), pomFile)))
                .add(Path.of("OSGI-OPT/src"), project.resources(
                    project.of(JavaSourceTreeType).using(Supply, Expose)));
            var git = project.<Git> get(GitApi);
            try {
                gen.attributes(
                    Map.entry(new Attributes.Name("Git-Descriptor"),
                        git.describe().setAll(true).call()
                            + (git.status().call().getUncommittedChanges()
                                .isEmpty() ? "" : "-dirty")),
                    Map.entry(new Attributes.Name("Git-SHA"),
                        git.getRepository().resolve("HEAD").getName()));
            } catch (GitAPIException | RevisionSyntaxException
                    | IOException e) {
                throw new BuildException().from(project).cause(e);
            }

            // Provide OSGi data
            if (project.directory().resolve("bnd.bnd").toFile().exists()) {
                String bundleVersion = project.get(Version);
                if (bundleVersion.endsWith("-SNAPSHOT")) {
                    bundleVersion = bundleVersion.replaceFirst("-", ".-")
                        .replaceAll("-SNAPSHOT$", "-\\${tstamp}-SNAPSHOT");
                } else {
                    bundleVersion += ".ga";
                }
                project.dependency(Consume, BndAnalyzer::new)
                    .instruction("Bundle-SymbolicName", project.name())
                    .instructions(project.directory().resolve("bnd.bnd"))
                    .instruction("Bundle-Version", bundleVersion)
                    .instructions(
                        Map.of("-diffignore", "Git-Descriptor, Git-SHA",
                            "Bundle-Version", bundleVersion));
                project.dependency(Supply, BndBaseliner::new)
                    .instruction("-diffignore", "Git-Descriptor, Git-SHA");
            }

            // Supply sources jar
            project.generator(SourcesJarGenerator::new).addTrees(
                project.resources(project.of(
                    JavaSourceTreeType).using(Supply, Expose)));

            // Supply javadoc jar
            project.generator(JavadocJarBuilder::new);

            // Publish (deploy). Credentials and signing information is
            // obtained through properties.
            project.generator(MvnPublisher::new);
        }
    }

    private static void setupEclipseConfigurator(Project project) {
        project.generator(new EclipseConfigurator(project)
            .adaptProjectConfiguration((Document doc,
                    Node buildSpec, Node natures) -> {
                if (project instanceof JavaProject) {
                    var cmd = buildSpec
                        .appendChild(doc.createElement("buildCommand"));
                    cmd.appendChild(doc.createElement("name"))
                        .appendChild(doc.createTextNode(
                            "net.sf.eclipsecs.core.CheckstyleBuilder"));
                    cmd.appendChild(doc.createElement("arguments"));
                    natures.appendChild(doc.createElement("nature"))
                        .appendChild(doc.createTextNode(
                            "net.sf.eclipsecs.core.CheckstyleNature"));
                    cmd = buildSpec
                        .appendChild(doc.createElement("buildCommand"));
                    cmd.appendChild(doc.createElement("name"))
                        .appendChild(doc.createTextNode(
                            "ch.acanda.eclipse.pmd.builder.PMDBuilder"));
                    cmd.appendChild(doc.createElement("arguments"));
                    natures.appendChild(doc.createElement("nature"))
                        .appendChild(doc.createTextNode(
                            "ch.acanda.eclipse.pmd.builder.PMDNature"));
                }
            }).adaptJdtCorePrefs(props -> {
                var formatterPrefs = new java.util.Properties();
                try {
                    formatterPrefs.load(Root.class.getResourceAsStream(
                        "org.eclipse.jdt.core.formatter.prefs"));
                    formatterPrefs.entrySet().stream()
                        .forEach(e -> props.put(e.getKey(), e.getValue()));
                } catch (IOException e) {
                    throw new BuildException().from(project).cause(e);
                }
            }).adaptConfiguration(() -> {
                if (!(project instanceof JavaProject)) {
                    return;
                }
                try {
                    Files.copy(
                        Root.class.getResourceAsStream("net.sf.jautodoc.prefs"),
                        project.directory()
                            .resolve(".settings/net.sf.jautodoc.prefs"),
                        StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(Root.class.getResourceAsStream("checkstyle"),
                        project.directory().resolve(".checkstyle"),
                        StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(Root.class.getResourceAsStream("eclipse-pmd"),
                        project.directory().resolve(".eclipse-pmd"),
                        StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new BuildException().from(project).cause(e);
                }
            }));
    }
}
