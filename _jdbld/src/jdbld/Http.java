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

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class Http extends AbstractProject implements JavaLibraryProject {

    public Http() {
        super(name("org.jgrapes.http"));
        dependency(Expose, project(Core.class));
        dependency(Expose, project(IO.class));
        dependency(Expose, new MvnRepoLookup().resolve(
            "org.jdrupes.httpcodec:httpcodec:[3.1.0,4.0.0)"));
    }

    public static class HttpTest extends AbstractProject
            implements JavaProject, MergedTestProject {

        public HttpTest() {
            super(parent(Http.class));
            dependency(Consume, parentProject().get());
            dependency(Consume, new MvnRepoLookup().resolve(
                "org.eclipse.angus:angus-activation:[1.0.0,2.0.0)"));
        }
    }
}
