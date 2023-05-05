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

/**
 * Components for handling mail. The components are based on the 
 * [Jakarta Mail API](https://github.com/jakartaee/mail-api).
 *
 * All components handle 
 * {@link org.jgrapes.util.events.ConfigurationUpdate} events. They
 * process properties from three paths:
 * 
 *  1. `/org.jgrapes.mail/Component` 
 *  2. `/org_jgrapes_mail/Component` 
 *  3. `/org.jgrapes.mail/` + component class name 
 *  4. `/org_jgrapes_mail/` + component class name 
 *  5. The path return by {@link org.jgrapes.core.Manager#componentPath()}
 *  
 * With the properties from the later sources taking precedence.
 *  
 * In each source, there can be a sub-section "`.../mail`". The valid keys 
 * in this sub-section are all properties defined for 
 * [Jakarta Mail](https://jakarta.ee/specifications/mail/2.1/apidocs/jakarta.mail/jakarta/mail/package-summary.html)
 * with the prefix "`mail.`" removed to avoid unnecessary redundancy.
 * 
 * The additional key/value defined with the paths above are used to
 * call the respective setter methods of the component.
 * 
 * Example configuration using 
 * {@link org.jgrapes.util.JsonConfigurationStore}:
 * ```json
 * {
 *     "/org.jgrapes.mail": {
 *         "/Component": {
 *             "/mail": {
 *                 "user": "..."
 *             },
 *             "password": "..."
 *         },
 *         "/MailMonitor": {
 *             "/mail": {
 *                 "host": "...",
 *                 "store.protocol": "imap",
 *                 "imap.ssl.enable": "true",
 *                 "imap.port": 993
 *             }
 *         },
 *         "/MailSender": {
 *             "/mail": {
 *                 "host": "...",
 *                 "transport.protocol": "smtp",
 *                 "smtp.ssl.enable": "true",
 *                 "smtp.port": 465,
 *                 "smtp.auth": true
 *             }
 *         }
 *     }
 * }
 * ```
 */
package org.jgrapes.mail;
