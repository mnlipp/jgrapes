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
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class Mail extends AbstractProject implements JavaLibraryProject {

    public Mail() {
        super(name("org.jgrapes.mail"));
        dependency(Expose, project(Core.class));
        dependency(Expose, project(Util.class));
        dependency(Expose, project(IO.class));
        dependency(Expose, new MvnRepoLookup().resolve(
            "jakarta.mail:jakarta.mail-api:[2.1.4,2.2.0)"));
        dependency(Reveal, new MvnRepoLookup().resolve(
            "org.eclipse.angus:imap:[2.0.4,2.1.0)"));
    }
}
