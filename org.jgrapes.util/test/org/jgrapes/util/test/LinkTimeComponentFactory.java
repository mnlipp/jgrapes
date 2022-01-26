package org.jgrapes.util.test;

import java.util.Map;
import java.util.Optional;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentFactory;
import org.jgrapes.core.ComponentType;

public class LinkTimeComponentFactory implements ComponentFactory {

    @Override
    public Class<? extends ComponentType> componentType() {
        return LinkTimeComponent.class;
    }

    @Override
    public Optional<ComponentType> create(Channel componentChannel,
            Map<?, ?> properties) {
        return Optional.of(new LinkTimeComponent(componentChannel));
    }

}
