package simpledb.buffer;

import simpledb.file.Block;
import simpledb.file.FileMgr;

/**
 * CS 4432 Project 1
 *
 * We made this class extend our AbstractBufferMgr so we could modify the usage
 * in the BufferMgr class.
 *
 * Manages the pinning and unpinning of buffers to blocks.
 *
 * @author Edward Sciore
 *
 */
class BasicBufferMgr extends AbstractBufferMgr {
	private Buffer[] bufferpool;
	private int numAvailable;

	/**
	 * Creates a buffer manager having the specified number of buffer slots.
	 * This constructor depends on both the {@link FileMgr} and
	 * {@link simpledb.log.LogMgr LogMgr} objects that it gets from the class
	 * {@link simpledb.server.SimpleDB}. Those objects are created during system
	 * initialization. Thus this constructor cannot be called until
	 * {@link simpledb.server.SimpleDB#initFileAndLogMgr(String)} or is called
	 * first.
	 *
	 * @param numbuffs
	 *            the number of buffer slots to allocate
	 */
	BasicBufferMgr(int numbuffs) {
		super(numbuffs);
		bufferpool = new Buffer[numbuffs];
		for (int i = 0; i < numbuffs; i++)
			bufferpool[i] = new Buffer();
	}

	/**
	 * Returns the number of available (i.e. unpinned) buffers.
	 *
	 * @return the number of available buffers
	 */
	@Override
	public int available() {
		return numAvailable;
	}

	@Override
	protected Buffer chooseUnpinnedBuffer() {
		for (Buffer buff : bufferpool)
			if (!buff.isPinned())
				return buff;
		return null;
	}

	@Override
	protected Buffer findExistingBuffer(Block blk) {
		for (Buffer buff : bufferpool) {
			Block b = buff.block();
			if (b != null && b.equals(blk))
				return buff;
		}
		return null;
	}

	/**
	 * Flushes the dirty buffers modified by the specified transaction.
	 *
	 * @param txnum
	 *            the transaction's id number
	 */
	@Override
	protected synchronized void flushAll(int txnum) {
		for (Buffer buff : bufferpool)
			if (buff.isModifiedBy(txnum))
				buff.flush();
	}

	/**
	 * Pins a buffer to the specified block. If there is already a buffer
	 * assigned to that block then that buffer is used; otherwise, an unpinned
	 * buffer from the pool is chosen. Returns a null value if there are no
	 * available buffers.
	 *
	 * @param blk
	 *            a reference to a disk block
	 * @return the pinned buffer
	 */
	@Override
	protected synchronized Buffer pin(Block blk) {
		Buffer buff = findExistingBuffer(blk);
		if (buff == null) {
			buff = chooseUnpinnedBuffer();
			if (buff == null)
				return null;
			buff.assignToBlock(blk);
		}
		if (!buff.isPinned())
			numAvailable--;
		buff.pin();
		return buff;
	}

	/**
	 * Allocates a new block in the specified file, and pins a buffer to it.
	 * Returns null (without allocating the block) if there are no available
	 * buffers.
	 *
	 * @param filename
	 *            the name of the file
	 * @param fmtr
	 *            a pageformatter object, used to format the new block
	 * @return the pinned buffer
	 */
	@Override
	protected synchronized Buffer pinNew(String filename, PageFormatter fmtr) {
		Buffer buff = chooseUnpinnedBuffer();
		if (buff == null)
			return null;
		buff.assignToNew(filename, fmtr);
		numAvailable--;
		buff.pin();
		return buff;
	}

	/**
	 * Unpins the specified buffer.
	 *
	 * @param buff
	 *            the buffer to be unpinned
	 */
	@Override
	protected synchronized void unpin(Buffer buff) {
		buff.unpin();
		if (!buff.isPinned())
			numAvailable++;
	}
}
