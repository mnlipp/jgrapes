package org.jgrapes.util.test;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;

public class LinkTimeComponent extends Component {

    public LinkTimeComponent() {
    }

    public LinkTimeComponent(Channel componentChannel) {
        super(componentChannel);
    }

    public LinkTimeComponent(Channel componentChannel,
            ChannelReplacements channelReplacements) {
        super(componentChannel, channelReplacements);
    }

}
