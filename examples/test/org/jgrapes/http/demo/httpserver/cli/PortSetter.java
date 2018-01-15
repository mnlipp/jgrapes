/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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

package org.jgrapes.http.demo.httpserver.cli;

import java.util.prefs.Preferences;

import org.jgrapes.http.demo.httpserver.HttpServerDemo;

/**
 * 
 */
public class PortSetter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Preferences preferences = Preferences
			.userNodeForPackage(HttpServerDemo.class)
			.node("PreferencesStore")
			.node("HttpServerDemo")
			.node("HttpServer");
		preferences.putInt("port", Integer.parseInt(args[0]));
	}
}
