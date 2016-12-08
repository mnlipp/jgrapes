/*******************************************************************************
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2016  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.jdrupes.httpcodec.test.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.jdrupes.httpcodec.util.ByteBufferOutputStream;
import org.jdrupes.httpcodec.util.ByteBufferUtils;
import org.junit.Test;

public class OutputStreamTests {

	@Test
	public void testPutAsMuch() {
		StringBuilder s = new StringBuilder();
		
		ByteBuffer src = ByteBuffer.allocate(1000);
		src.put("Hello World!".getBytes());
		src.flip();
		ByteBuffer dest = ByteBuffer.allocate(5);
		ByteBufferUtils.putAsMuchAsPossible(dest, src);
		assertEquals(5, dest.position());
		assertEquals(7, src.remaining());
		dest.flip();
		byte[] b = new byte[dest.remaining()];
		dest.get(b);
		s.append(new String(b));
		dest = ByteBuffer.allocate(100);
		ByteBufferUtils.putAsMuchAsPossible(dest, src);
		dest.flip();
		b = new byte[dest.remaining()];
		dest.get(b);
		s.append(new String(b));
		assertEquals("Hello World!", s.toString());
	}
	
	@Test
	public void testOverflow() throws IOException {
		ByteBuffer dataSource = ByteBuffer.allocate(2048);
		ByteBuffer sink = ByteBuffer.allocate(dataSource.capacity());
		Random rand = new Random();
		byte[] bytes = new byte[dataSource.remaining()];
		rand.nextBytes(bytes);
		dataSource.put(bytes);
		dataSource.flip();
		
		ByteBufferOutputStream os = new ByteBufferOutputStream();
		ByteBuffer assignedBuf = ByteBuffer.allocate(256);
		os.assignBuffer(assignedBuf);
		assertEquals(256, os.remaining());
		// write 100 + 400; 256 fit and then we have 3+1 overflow buffers
		bytes = new byte[100];
		dataSource.get(bytes);
		os.write(bytes);
		assertEquals(156, os.remaining());
		bytes = new byte[400];
		dataSource.get(bytes);
		os.write(bytes);
		assertEquals(500, os.bytesWritten());
		assignedBuf.flip();
		sink.put(assignedBuf); // 256/500 sinked
		assertTrue(os.remaining() < 0);
		// Now get 32 (only part of overflow buffer fits)
		assignedBuf = ByteBuffer.allocate(32);
		os.assignBuffer(assignedBuf);
		// Everything is transfered immediately
		assertEquals(32, assignedBuf.position());
		assertTrue(os.remaining() < 0);
		assignedBuf.flip();
		sink.put(assignedBuf); // 288/500 sinked
		assertEquals(288, sink.position());
		// Leave 10 bytes in partially filled overflow buffer
		assignedBuf = ByteBuffer.allocate(202);
		os.assignBuffer(assignedBuf);
		assertEquals(202, assignedBuf.position());
		assertEquals(500, os.bytesWritten());
		assertTrue(os.remaining() < 0);
		assignedBuf.flip();
		sink.put(assignedBuf);
		assertEquals(490, sink.position());
		// Use partially written overflow buffer (and more) for writing
		bytes = new byte[600];
		dataSource.get(bytes);
		assertEquals(1100, dataSource.position());
		os.write(bytes);
		assertTrue(os.remaining() < 0);
		// Get rest from overflows
		assignedBuf = ByteBuffer.allocate(dataSource.capacity());
		os.assignBuffer(assignedBuf);
		assertEquals(610, assignedBuf.position());
		assertEquals(sink.position() + assignedBuf.position(), 
				dataSource.position());
		assertTrue(os.remaining() > dataSource.remaining());
		// Single write, immediate transfer
		os.write(dataSource.get());
		assertEquals(sink.position() + assignedBuf.position(), 
				dataSource.position());
		os.write(dataSource);
		assignedBuf.flip();
		sink.put(assignedBuf);
		os.close();
		
		assertEquals(dataSource, sink);
	}

	
}
