/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.util;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.io.File;
import java.io.IOException;
import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.events.Start;
import org.jgrapes.util.events.ConfigurationUpdate;
import org.jgrapes.util.events.FileChanged;
import org.jgrapes.util.events.InitialPreferences;

/**
 * This component provides a store for an application's configuration
 * backed by a TOML file. 
 * 
 * The component reads the initial values from {@link File} passed
 * to the constructor. During application bootstrap, it 
 * intercepts the {@link Start} event using a handler with  priority 
 * 999999. When receiving this event, it fires all known preferences 
 * values on the channels of the start event as a 
 * {@link InitialPreferences} event, using a new {@link EventPipeline}
 * and waiting for its completion. Then, allows the intercepted 
 * {@link Start} event to continue. 
 * 
 * Components that depend on configuration values define handlers
 * for {@link ConfigurationUpdate} events and adapt themselves to the values 
 * received. Note that due to the intercepted {@link Start} event, the initial
 * preferences values are received before the {@link Start} event, so
 * components' configurations can be rearranged before they actually
 * start doing something.
 *
 * Besides initially publishing the stored preferences values,
 * the component also listens for {@link ConfigurationUpdate} events
 * on its channel and updates the TOML file (may be suppressed).
 */
@SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.AvoidDuplicateLiterals",
    "PMD.GodClass" })
public class TomlConfigurationStore extends NightConfigStore {

    /**
     * Creates a new component with its channel set to the given 
     * channel and the given file. The component handles
     * {@link ConfigurationUpdate} events and {@link FileChanged}
     * events for the configuration file (see
     * @link #NightConfigStore(Channel, File, boolean, boolean)}
     * 
     * @param componentChannel the channel 
     * @param file the file used to store the configuration
     * @throws IOException
     */
    public TomlConfigurationStore(Channel componentChannel, File file)
            throws IOException {
        this(componentChannel, file, true);
    }

    /**
     * Creates a new component with its channel set to the given 
     * channel and the given file. The component handles
     * {@link FileChanged} events for the configuration file (see
     * @link #NightConfigStore(Channel, File, boolean, boolean)}
     * 
     * If `update` is `true`, the configuration file is updated
     * when {@link ConfigurationUpdate} events are received.  
     *
     * @param componentChannel the channel
     * @param file the file used to store the configuration
     * @param update if the configuration file is to be updated
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("PMD.ShortVariable")
    public TomlConfigurationStore(Channel componentChannel, File file,
            boolean update) throws IOException {
        this(componentChannel, file, update, true);
    }

    /**
     * Creates a new component with its channel set to the given 
     * channel and the given file.
     * 
     * If `update` is `true`, the configuration file is updated
     * when {@link ConfigurationUpdate} events are received.  
     * 
     * If `watch` is `true`, {@link FileChanged} events are processed
     * and the configuration file is reloaded when it changes. Note
     * that the generation of the {@link FileChanged} events must
     * be configured independently (see {@link FileSystemWatcher}).
     *
     * @param componentChannel the channel
     * @param file the file used to store the configuration
     * @param update if the configuration file is to be updated
     * @param watch if {@link FileChanged} events are to be processed
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("PMD.ShortVariable")
    public TomlConfigurationStore(Channel componentChannel, File file,
            boolean update, boolean watch) throws IOException {
        super(componentChannel, file, update, watch);
        config = CommentedFileConfig.builder(file.getAbsolutePath()).sync()
            .concurrent().build();
        config.load();
    }

}
