/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.http.events;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.stream.Stream;
import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;

/**
 * Represents the response (header).
 */
public class Response extends MessageReceived<Void> {

    @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
    private final MessageHeader response;

    /**
     * Instantiates a new response.
     *
     * @param response the response
     */
    public Response(MessageHeader response) {
        this.response = response;
    }

    /**
     * @return the response
     */
    public MessageHeader response() {
        return response;
    }

    /**
     * Convenience method for retrieving the {@link Charset}
     * from the response.
     *
     * @return the optional
     */
    public Optional<Charset> charset() {
        return ((HttpResponse) response())
            .findValue(HttpField.CONTENT_TYPE, Converters.MEDIA_TYPE)
            .map(mt -> mt.parameters().entrySet().stream())
            .orElse(Stream.empty())
            .filter(e -> "charset".equalsIgnoreCase(e.getKey()))
            .findFirst().map(e -> e.getValue()).map(csn -> {
                try {
                    return Charset.forName(csn);
                } catch (Exception e) {
                    return null;
                }
            });
    }

}
