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

package org.jgrapes.io.util;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper around a {@link Buffer} that maintains a lock count for that
 * buffer. All methods known from {@code Buffer} are provided and
 * delegate to the backing buffer. Managed buffers can be used to maintain
 * pools of buffers. Buffers are locked when retrieved from the pool
 * and can automatically be returned when the last lock is released.
 * 
 * Newly created managed buffer always have a lock count of 1 (you
 * create them for using them, don't you).
 */
@SuppressWarnings("PMD.TooManyMethods")
public class ManagedBuffer<T extends Buffer> {

    public static final ManagedBuffer<ByteBuffer> EMPTY_BYTE_BUFFER
        = wrap(ByteBuffer.allocate(0));

    public static final ManagedBuffer<CharBuffer> EMPTY_CHAR_BUFFER
        = wrap(CharBuffer.allocate(0));

    protected T backing;
    private ManagedBuffer<T> linkedTo;
    protected T savedBacking;
    private final BufferCollector<ManagedBuffer<T>> manager;
    private final AtomicInteger lockCount = new AtomicInteger(1);

    /**
     * Create a new Managed buffer, backed by the given buffer,
     * with a lock count of one.
     * 
     * @param buffer the backing buffer
     * @param manager used for restoring the buffer when the lock 
     * count reaches zero
     */
    public ManagedBuffer(T buffer, BufferCollector<ManagedBuffer<T>> manager) {
        this.backing = buffer;
        this.manager = manager;
    }

    /**
     * Convenience method for creating a {@link ManagedBuffer} with
     * a {@link BufferCollector#NOOP_COLLECTOR} from a NIO buffer.
     * Effectively, this creates an *unmanaged* buffer that
     * looks like a managed buffer from an existing NIO buffer that
     * does not belong to any pool.
     *
     * @param <B> the buffer type
     * @param buffer the buffer to wrap
     * @return the managed buffer
     */
    public static <B extends Buffer> ManagedBuffer<B> wrap(B buffer) {
        return new ManagedBuffer<B>(buffer, BufferCollector.noopCollector());
    }

    /**
     * Return the backing buffer.
     * 
     * @return the buffer
     */
    public T backingBuffer() {
        return backing;
    }

    /**
     * Replace the backing buffer.
     * 
     * @param buffer the new buffer
     * @return the managed buffer for easy chaining
     */
    public ManagedBuffer<T> replaceBackingBuffer(T buffer) {
        backing = buffer;
        return this;
    }

    /**
     * Links this instance's backing buffer (temporarily) to
     * the given buffer's backing buffer. Locks the given buffer.
     * The buffer is unlocked again and the original backing buffer
     * restored if this method is called with `null` or this
     * managed buffer is recollected (i.e. no longer used). 
     *
     * This method may be used to "assign" data that is already
     * available in a buffer to this buffer without copying it
     * over. 
     *
     * @param buffer the buffer
     * @return the managed buffer for easy chaining
     */
    public ManagedBuffer<T> linkBackingBuffer(ManagedBuffer<T> buffer) {
        if (linkedTo != null) {
            backing = savedBacking;
            linkedTo.unlockBuffer();
            linkedTo = null;
        }
        if (buffer == null) {
            return this;
        }
        buffer.lockBuffer();
        linkedTo = buffer;
        savedBacking = backing;
        backing = linkedTo.backing;
        return this;
    }

    /**
     * Return the buffer's manager.
     * 
     * @return the manager
     */
    public BufferCollector<?> manager() {
        return manager;
    }

    /**
     * Increases the buffer's lock count.
     * 
     * @return the managed buffer for easy chaining
     */
    public ManagedBuffer<T> lockBuffer() {
        lockCount.incrementAndGet();
        return this;
    }

    /**
     * Decreases the buffer's lock count. If the lock count reached
     * zero, the buffer collect's {@link BufferCollector#recollect}
     * method is invoked. 
     * 
     * @throws IllegalStateException if the buffer is not locked or 
     * has been released already
     */
    @SuppressWarnings("PMD.AvoidUncheckedExceptionsInSignatures")
    public void unlockBuffer() throws IllegalStateException {
        int locks = lockCount.decrementAndGet();
        if (locks < 0) {
            throw new IllegalStateException(
                "Buffer not locked or released already.");
        }
        if (locks == 0) {
            if (linkedTo != null) {
                backing = savedBacking;
                linkedTo.unlockBuffer();
                linkedTo = null;
            }
            manager.recollect(this);
        }
    }

    /**
     * Convenience method to fill the buffer from the channel.
     * Unlocks the buffer if an {@link IOException} occurs.
     * This method may only be invoked for {@link ManagedBuffer}s
     * backed by a {@link ByteBuffer}.
     *
     * @param channel the channel
     * @return the bytes read
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public int fillFromChannel(
            ReadableByteChannel channel) throws IOException {
        if (!(backing instanceof ByteBuffer)) {
            throw new IllegalArgumentException(
                "Backing buffer is not a ByteBuffer.");
        }
        try {
            return channel.read((ByteBuffer) backing);
        } catch (IOException e) {
            unlockBuffer();
            throw e;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(50);
        builder.append(getClass().getSimpleName())
            .append(" [");
        if (backing != null) {
            builder.append("buffer=").append(backing).append(", ");
        }
        if (lockCount != null) {
            builder.append("lockCount=").append(lockCount);
        }
        builder.append(']');
        return builder.toString();
    }

    /**
     * @return the backing array
     * @see java.nio.Buffer#array()
     */
    public Object array() {
        return backing.array();
    }

    /**
     * @return the backing array offset
     * @see java.nio.Buffer#arrayOffset()
     */
    public int arrayOffset() {
        return backing.arrayOffset();
    }

    /**
     * @return the capacity
     * @see java.nio.Buffer#capacity()
     */
    public final int capacity() {
        return backing.capacity();
    }

    /**
     * @return the buffer
     * @see java.nio.Buffer#clear()
     */
    public final Buffer clear() {
        return backing.clear();
    }

    /**
     * Duplicate the buffer.
     *
     * @return the t
     */
    @SuppressWarnings("unchecked")
    public final T duplicate() {
        if (backing instanceof ByteBuffer) {
            return (T) ((ByteBuffer) backing).duplicate();
        }
        if (backing instanceof CharBuffer) {
            return (T) ((CharBuffer) backing).duplicate();
        }
        throw new IllegalArgumentException("Backing buffer of unknown type.");
    }

    /**
     * @return the buffer
     * @see java.nio.Buffer#flip()
     */
    public final Buffer flip() {
        return backing.flip();
    }

    /**
     * @return the result
     * @see java.nio.Buffer#hasArray()
     */
    public boolean hasArray() {
        return backing.hasArray();
    }

    /**
     * @return the result
     * @see java.nio.Buffer#hasRemaining()
     */
    public final boolean hasRemaining() {
        return backing.hasRemaining();
    }

    /**
     * @return the result
     * @see java.nio.Buffer#isDirect()
     */
    public boolean isDirect() {
        return backing.isDirect();
    }

    /**
     * @return the result
     * @see java.nio.Buffer#isReadOnly()
     */
    public boolean isReadOnly() {
        return backing.isReadOnly();
    }

    /**
     * @return the result
     * @see java.nio.Buffer#limit()
     */
    public final int limit() {
        return backing.limit();
    }

    /**
     * @param newLimit the new limit
     * @return the result
     * @see java.nio.Buffer#limit(int)
     */
    public final Buffer limit(int newLimit) {
        return backing.limit(newLimit);
    }

    /**
     * @return the buffer
     * @see java.nio.Buffer#mark()
     */
    public final Buffer mark() {
        return backing.mark();
    }

    /**
     * @return the result
     * @see java.nio.Buffer#position()
     */
    public final int position() {
        return backing.position();
    }

    /**
     * @param newPosition the new position
     * @return the buffer
     * @see java.nio.Buffer#position(int)
     */
    public final Buffer position(int newPosition) {
        return backing.position(newPosition);
    }

    /**
     * @return the result
     * @see java.nio.Buffer#remaining()
     */
    public final int remaining() {
        return backing.remaining();
    }

    /**
     * @return the Buffer
     * @see java.nio.Buffer#reset()
     */
    public final Buffer reset() {
        return backing.reset();
    }

    /**
     * @return the Buffer
     * @see java.nio.Buffer#rewind()
     */
    public final Buffer rewind() {
        return backing.rewind();
    }

    /**
     * Creates a new {@link ByteBuffer} view.
     *
     * @return the byte buffer view
     */
    @SuppressWarnings("PMD.AccessorClassGeneration")
    public ByteBufferView newByteBufferView() {
        return new ByteBufferView();
    }

    /**
     * A read-only view of the managed buffer's content
     * (backing buffer) and a reference to the managed buffer.
     * Can be used if several consumers need the same content.
     */
    public final class ByteBufferView {
        private final ByteBuffer bufferView;

        private ByteBufferView() {
            if (!(backing instanceof ByteBuffer)) {
                throw new IllegalArgumentException("Not a managed ByteBuffer.");
            }
            bufferView = ((ByteBuffer) backing).asReadOnlyBuffer();
        }

        /**
         * Returns the {@link ByteBuffer} that represents this
         * view (position, mark, limit).
         * 
         * @return the `ByteBuffer` view
         */
        public ByteBuffer get() {
            return bufferView;
        }

        /**
         * Returns the managed buffer that this reader is a view of.
         * 
         * @return the managed buffer
         */
        @SuppressWarnings("unchecked")
        public ManagedBuffer<ByteBuffer> managedBuffer() {
            return (ManagedBuffer<ByteBuffer>) ManagedBuffer.this;
        }
    }

    /**
     * Creates a new {@link CharBuffer} view.
     *
     * @return the byte buffer view
     */
    @SuppressWarnings("PMD.AccessorClassGeneration")
    public CharBufferView newCharBufferView() {
        return new CharBufferView();
    }

    /**
     * A read-only view of the managed buffer's content
     * (backing buffer) and a reference to the managed buffer.
     * Can be used if several consumers need the same content.
     */
    public final class CharBufferView {
        private final CharBuffer bufferView;

        private CharBufferView() {
            if (!(backing instanceof CharBuffer)) {
                throw new IllegalArgumentException("Not a managed CharBuffer.");
            }
            bufferView = ((CharBuffer) backing).asReadOnlyBuffer();
        }

        /**
         * Returns the {@link ByteBuffer} that represents this
         * view (position, mark, limit).
         * 
         * @return the `ByteBuffer` view
         */
        public CharBuffer get() {
            return bufferView;
        }

        /**
         * Returns the managed buffer that this reader is a view of.
         * 
         * @return the managed buffer
         */
        @SuppressWarnings("unchecked")
        public ManagedBuffer<CharBuffer> managedBuffer() {
            return (ManagedBuffer<CharBuffer>) ManagedBuffer.this;
        }
    }
}
