/*
 * JGrapes Event driven Framework
 * Copyright (C) 2023 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jgrapes.io.events;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapes.core.Event;
import org.jgrapes.io.process.ProcessManager;

/**
 * Starts a new process managed by the {@link ProcessManager}
 * component that handles the event.
 */
public class StartProcess extends Event<Void> {

    private final String[] command;
    private File directory;
    private Map<String, String> environment;

    /**
     * Signals that a new process should be started.
     *
     * @param command the command
     */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public StartProcess(String... command) {
        this.command = command;
    }

    /**
     * Signals that a new process should be started.
     *
     * @param command the command
     */
    public StartProcess(List<String> command) {
        this(command.toArray(new String[0]));
    }

    /**
     * Returns the command.
     * 
     * @return the command
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public String[] command() {
        return command;
    }

    /**
     * Sets the working directory for the process.
     *
     * @param directory the directory
     * @return the event for method chaining
     */
    public StartProcess directory(File directory) {
        this.directory = directory;
        return this;
    }

    /**
     * Returns the directory.
     * 
     * @return the directory
     */
    public File directory() {
        return directory;
    }

    /**
     * Sets the environment for the process to be created.
     * The values given will be merged with the defaults
     * used by the {@link ProcessBuilder}. Overrides any
     * values from a previous invocation or from invoking
     * {@link #environment(String, String)}.
     *
     * @param environment the environment
     * @return the event for method chaining
     */
    public StartProcess environment(Map<String, String> environment) {
        this.environment = environment;
        return this;
    }

    /**
     * Returns the environment.
     * 
     * @return the environment
     */
    public Map<String, String> environment() {
        return environment;
    }

    /**
     * Sets a single value in the environment of the process
     * to be created, see {@link StartProcess#environment(Map)}.
     *
     * @param key the key
     * @param value the value
     * @return the event for method chaining
     */
    public StartProcess environment(String key, String value) {
        if (environment == null) {
            environment = new HashMap<>();
        }
        environment.put(key, value);
        return this;
    }
}
