package org.jgrapes.http.test;

import java.io.IOException;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.util.CharBufferWriter;

public class ReflectProvider extends Component {

    public int invocations = 0;

    public ReflectProvider(Channel componentChannel) {
        super(componentChannel);
        PostTest.contentProvider = this;
    }

    @RequestHandler(patterns = "/reflect")
    public void onPost(Request.In.Post event, IOSubchannel channel)
            throws ParseException {
        invocations += 1;

        channel.setAssociated(ReflectProvider.class, "reflect");
        final HttpResponse response = event.httpRequest().response().get();
        response.setStatus(HttpStatus.OK);
        response.setHasPayload(true);
        response.setField(HttpField.CONTENT_TYPE,
            MediaType.builder().setType("text", "plain")
                .setParameter("charset", "utf-8").build());
        channel.respond(new Response(response));
        event.setResult(true);
        event.stop();
    }

    @Handler
    public void onInput(Input<CharBuffer> event, IOSubchannel channel)
            throws InterruptedException, IOException {
        Optional<String> marker
            = channel.associated(ReflectProvider.class, String.class);
        if (!marker.isPresent() || !marker.get().equals("reflect")) {
            return;
        }
        CharBufferWriter out = new CharBufferWriter(channel);
        out.write("->");
        out.write(event.buffer().backingBuffer().toString());
        if (event.isEndOfRecord()) {
            out.suppressClose();
        } else {
            out.suppressEndOfRecord().suppressClose();
        }
        out.close();
    }
}