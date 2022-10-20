/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.mail;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.security.Principal;

/**
 * A variant of {@link InternetAddress} that can be used as a 
 * {@link Principal}.
 */
public class InternetAddressPrincipal extends InternetAddress
        implements Principal {

    private static final long serialVersionUID = -6900967855203936680L;

    /**
     * See {@link InternetAddress#InternetAddress()}.
     */
    public InternetAddressPrincipal() {
        super();
    }

    /**
     * See {@link InternetAddress#InternetAddress(String, boolean))}.
     *
     * @param address the address
     * @param strict the strict
     * @throws AddressException the address exception
     */
    public InternetAddressPrincipal(String address, boolean strict)
            throws AddressException {
        super(address, strict);
    }

    /**
     * See {@link InternetAddress#InternetAddress(String, String, String)))}.
     *
     * @param address the address
     * @param personal the personal
     * @param charset the charset
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public InternetAddressPrincipal(String address, String personal,
            String charset) throws UnsupportedEncodingException {
        super(address, personal, charset);
    }

    /**
     * See {@link InternetAddress#InternetAddress(String, String)))}.
     *
     * @param address the address
     * @param personal the personal
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public InternetAddressPrincipal(String address, String personal)
            throws UnsupportedEncodingException {
        super(address, personal);
    }

    /**
     * See {@link InternetAddress#InternetAddress(String)))}.
     *
     * @param address the address
     * @throws AddressException the address exception
     */
    public InternetAddressPrincipal(String address) throws AddressException {
        super(address);
    }

    @Override
    public String getName() {
        return getAddress();
    }

}
