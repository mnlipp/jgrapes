package org.jdrupes.httpcodec.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.DatabaseMetaData;
import java.util.Random;

import org.jdrupes.httpcodec.util.ByteBufferOutputStream;
import org.junit.Test;

public class OutputStreamTests {

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
		while (dataSource.hasRemaining()) {
			os.write(dataSource.get());
		}
		assignedBuf.flip();
		sink.put(assignedBuf);
		
		assertEquals(dataSource, sink);
	}

}
