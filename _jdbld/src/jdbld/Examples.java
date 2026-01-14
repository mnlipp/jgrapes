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

import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class Examples extends AbstractProject implements JavaLibraryProject {

    public Examples() {
        super(name("examples"));
        dependency(Consume, project(Http.class));
        dependency(Consume, project(Mail.class));
        dependency(Consume, new MvnRepoLookup().resolve(
            "org.osgi:osgi.core:6.0.0"));

//        runtimeOnly 'org.apache.felix:org.apache.felix.framework:6.0.5'
//        runtimeOnly ('org.apache.aries.spifly:org.apache.aries.spifly.dynamic.bundle:1.3.4') {
//            transitive = true }
//        runtimeOnly (project(':examples')) { transitive = true }

    }
}
