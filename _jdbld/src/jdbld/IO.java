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

import static org.jdrupes.builder.api.Intend.*;

import org.jdrupes.builder.api.MergedTestProject;
import static org.jdrupes.builder.api.ResourceRequest.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class IO extends AbstractProject implements JavaLibraryProject {

    public IO() {
        super(name("org.jgrapes.io"));
        dependency(Expose, project(Core.class));
        dependency(Expose, project(Util.class));
        dependency(Expose, new MvnRepoLookup().resolve(
            "com.fasterxml.jackson.core:jackson-databind:[2.13,3)"));
    }

    public static class IOTest extends AbstractProject
            implements JavaProject, MergedTestProject {

        public IOTest() {
            super(parent(IO.class));
            dependency(Consume, parentProject().get());
            dependency(Consume, new MvnRepoLookup().resolve(
                "jakarta.json:jakarta.json-api:2.1.2",
                "jakarta.json.bind:jakarta.json.bind-api:3.0.1",
                "org.apache.johnzon:johnzon-jsonb:2.0.1"));
        }
    }
}
