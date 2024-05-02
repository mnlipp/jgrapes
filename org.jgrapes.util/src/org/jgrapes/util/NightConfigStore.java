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

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import org.jgrapes.core.Channel;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.util.events.ConfigurationUpdate;
import org.jgrapes.util.events.FileChanged;
import org.jgrapes.util.events.InitialConfiguration;

/**
 * A base class for configuration stored based on the 
 * [night config library](https://github.com/TheElectronWill/night-config).
 */
@SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.AvoidDuplicateLiterals",
    "PMD.GodClass" })
public abstract class NightConfigStore extends ConfigurationStore {

    @SuppressWarnings("PMD.FieldNamingConventions")
    protected static final Logger logger
        = Logger.getLogger(NightConfigStore.class.getName());

    protected FileConfig config;
    protected Channel[] initialChannels;

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
    @Deprecated
    public NightConfigStore(Channel componentChannel, File file)
            throws IOException {
        this(componentChannel, file, true, true);
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
    @Deprecated
    @SuppressWarnings("PMD.ShortVariable")
    public NightConfigStore(Channel componentChannel, File file,
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
    public NightConfigStore(Channel componentChannel, File file,
            boolean update, boolean watch) throws IOException {
        super(componentChannel);
        if (update) {
            Handler.Evaluator.add(this, "onConfigurationUpdate",
                channel().defaultCriterion());
        }
        if (watch) {
            Handler.Evaluator.add(this, "onFileChanged",
                channel().defaultCriterion());
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    /**
     * If watching the configuration file is enabled, fire
     * a {@link ConfigurationUpdate} event with the complete
     * configuration when the file changes. The event is fired
     * on the same channel(s) as the {@link InitialConfiguration}
     * event.
     *
     * @param event the event
     */
    @Handler(dynamic = true)
    public void onFileChanged(FileChanged event) {
        if (config.getNioPath().equals(event.path())
            && event.change() == FileChanged.Kind.MODIFIED
            && initialChannels != null) {
            config.load();
            ConfigurationUpdate updEvt = new ConfigurationUpdate();
            addPrefs(updEvt, "/", config);
            fire(updEvt, initialChannels);
        }
    }

    @Override
    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition",
        "PMD.AvoidBranchingStatementAsLastInLoop", "PMD.CloseResource" })
    public Optional<Map<String, Object>> structured(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with \"/\".");
        }

        // Walk down to node.
        var segs = new StringTokenizer(path, "/");
        @SuppressWarnings("PMD.CloseResource")
        Config cur = config;
        while (segs.hasMoreTokens()) {
            var nextSeg = segs.nextToken();
            Object next = Optional.ofNullable(cur.get("_" + nextSeg))
                .orElse(cur.get("/" + nextSeg));
            if (next instanceof Config) {
                cur = (Config) next;
                continue;
            }
            return Optional.empty();
        }
        return Optional.of(toValueMap(cur));
    }

    private Map<String, Object> toValueMap(Config config) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> result = new HashMap<>();
        for (var entry : config.entrySet()) {
            if (isNode(entry.getKey())) {
                continue;
            }
            if (entry.getValue() instanceof Config) {
                result.put(entry.getKey(), toValueMap(entry.getValue()));
                continue;
            }
            if (entry.getValue() instanceof List<?> values) {
                result.put(entry.getKey(), convertList(values));
                continue;
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private List<Object> convertList(List<?> values) {
        List<Object> copy = new ArrayList<>();
        for (var element : values) {
            if (element instanceof Config cfg) {
                copy.add(toValueMap(cfg));
                continue;
            }
            copy.add(element);
        }
        return copy;
    }

    /**
     * Checks if the name is an entry for a node.
     *
     * @param name the name
     * @return true, if is node
     */
    protected boolean isNode(String name) {
        if (name == null || name.length() < 1) {
            return false;
        }
        char first = name.charAt(0);
        return first == '_' || first == '/';
    }

    /**
     * Intercepts the {@link Start} event and fires a
     * {@link ConfigurationUpdate} event.
     *
     * @param event the event
     * @throws BackingStoreException the backing store exception
     * @throws InterruptedException the interrupted exception
     */
    @Handler(priority = 999_999, channels = Channel.class)
    @SuppressWarnings("PMD.CognitiveComplexity")
    public void onStart(Start event)
            throws BackingStoreException, InterruptedException {
        ConfigurationUpdate updEvt = new InitialConfiguration();
        addPrefs(updEvt, "/", config);
        initialChannels = event.channels();
        newEventPipeline().fire(updEvt, initialChannels).get();
    }

    private void addPrefs(ConfigurationUpdate updEvt, String path,
            Config config) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> atPath = new HashMap<>();
        for (var e : config.entrySet()) {
            if (isNode(e.getKey()) && e.getValue() instanceof Config) {
                addPrefs(updEvt, ("/".equals(path) ? "" : path)
                    + "/" + e.getKey().substring(1), e.getValue());
                continue;
            }
            if (e.getValue() instanceof Config) {
                atPath.put(e.getKey(), toValueMap(e.getValue()));
            } else if (e.getValue() instanceof List<?> values) {
                atPath.put(e.getKey(), convertList(values));
            } else {
                atPath.put(e.getKey(), e.getValue());
            }
        }
        if (!atPath.isEmpty()) {
            updEvt.set(path, atPath);
        }
    }

    /**
     * Merges and saves configuration updates.
     *
     * @param event the event
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(dynamic = true)
    @SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity",
        "PMD.AvoidLiteralsInIfCondition",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    public void onConfigurationUpdate(ConfigurationUpdate event)
            throws IOException {
        if (event instanceof InitialConfiguration) {
            return;
        }

        boolean changed = false;
        for (String path : event.paths()) {
            if ("/".equals(path) && event.values(path).isEmpty()) {
                // Special case, "remove root", i.e. all configuration data
                config.clear();
                changed = true;
                continue;
            }
            if (handleSegment(config, new StringTokenizer(path, "/"),
                event.structured(path).map(ConfigurationStore::flatten))) {
                changed = true;
            }
        }
        if (changed) {
            config.save();
        }
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private boolean handleSegment(Config config,
            StringTokenizer tokenizer, Optional<Map<String, Object>> values) {
        if (!tokenizer.hasMoreTokens()) {
            // "Leaf" map
            return mergeValues(config, values.get());
        }
        boolean changed = false;
        var nextSeg = tokenizer.nextToken();
        var usSel = List.of("_" + nextSeg);
        var slashSel = List.of("/" + nextSeg);
        if (!tokenizer.hasMoreTokens() && values.isEmpty()) {
            // Selected is last segment from path and we must remove
            for (var sel : List.of(usSel, slashSel)) {
                if (config.get(sel) != null) {
                    // Delete sub-map.
                    config.remove(sel);
                    changed = true;
                }
            }
            return changed;
        }
        // Check if sub config exists
        Object subConfig = Optional.ofNullable(config.get(usSel))
            .orElse(config.get(slashSel));
        if (!(subConfig instanceof Config)) {
            // Doesn't exist or is of wrong type, new sub-map
            changed = true;
            subConfig = config.createSubConfig();
            config.set(usSel, subConfig);
        }
        // Continue with sub-map
        return handleSegment((Config) subConfig, tokenizer, values) || changed;
    }

    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
        "PMD.CognitiveComplexity" })
    private boolean mergeValues(Config config, Map<String, Object> values) {
        boolean changed = false;
        Map<String, Object> curValues = flatten(toValueMap(config));
        for (var e : values.entrySet()) {
            if (e.getValue() == null) {
                // Delete from map (and config)
                if (curValues.containsKey(e.getKey())) {
                    curValues.remove(e.getKey());
                    changed = true;
                }
                continue;
            }
            Object oldValue = curValues.get(e.getKey());
            if (oldValue == null || !e.getValue().equals(oldValue)) {
                curValues.put(e.getKey(), e.getValue());
                changed = true;
            }
        }
        if (changed) {
            for (var itr = config.entrySet().iterator(); itr.hasNext();) {
                if (!isNode(itr.next().getKey())) {
                    itr.remove();
                }
            }
            addToConfig(config, structure(curValues));
        }
        return changed;
    }

    @SuppressWarnings("unchecked")
    private void addToConfig(Config config, Map<String, Object> map) {
        for (var e : map.entrySet()) {
            var selector = List.of(e.getKey());
            if (e.getValue() instanceof Map) {
                Config subConfig = config.get(selector);
                if (subConfig == null) {
                    subConfig = config.createSubConfig();
                    config.set(selector, subConfig);
                }
                addToConfig(subConfig, (Map<String, Object>) e.getValue());
            } else if (e.getValue() instanceof Collection) {
                config.set(selector,
                    checkCollection((Collection<?>) e.getValue()));
            } else {
                config.set(selector, e.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> checkCollection(Collection<?> items) {
        var checked = new ArrayList<>();
        for (var item : items) {
            if (item instanceof Map) {
                Config subConfig = config.createSubConfig();
                addToConfig(subConfig, (Map<String, Object>) item);
                checked.add(subConfig);
            } else if (item instanceof Collection) {
                checked.add(checkCollection((Collection<?>) item));
            } else {
                checked.add(item);
            }
        }
        return checked;
    }

}
