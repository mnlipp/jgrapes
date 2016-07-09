/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
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
package org.jgrapes.io.test.file;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.File;
import org.jgrapes.io.events.FileOpened;
import org.jgrapes.io.events.OpenFile;
import org.jgrapes.io.util.ByteBufferOutputStream;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class FileWriteTests {

	public static class Producer extends AbstractComponent {
		
		@Handler
		public void opened(FileOpened event) 
				throws InterruptedException, IOException {
			EventPipeline pipeline = newEventPipeline();
			try (ByteBufferOutputStream out = new ByteBufferOutputStream
					(event.getConnection(), pipeline)) {
				for (int i = 1; i <= 10000; i++) {
					out.write(new String(i + ": Hello World!\n").getBytes());
				}
			}
		}
		
	}

	@Test
	public void testWrite() 
			throws IOException, InterruptedException, ExecutionException {
		Path filePath = Files.createTempFile("jgrapes-", ".txt");
		filePath.toFile().deleteOnExit();
		Producer producer = new Producer();
		File app = new File(producer, 512);
		app.attach(producer);
		Components.start(app);
		app.fire(new OpenFile(filePath, StandardOpenOption.WRITE), producer);
		Components.awaitExhaustion();
		try (BufferedReader br = new BufferedReader
				(new FileReader(filePath.toFile()))) {
			int expect  = 1;
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				int num = Integer.parseInt(line.split(":")[0]);
				assertEquals(expect, num);
				expect += 1;
			}
			assertEquals(expect, 10001);
		}
	}
}
