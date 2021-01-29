package com.berrontech.weight.scale.commons;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Create By Levent8421
 * Create Time: 2020/6/24 14:44
 * Class Name: DataBuffer
 * Author: lastnika
 * Description:
 * Data Buffer
 *
 * @author Levent8421
 */
@SuppressWarnings("unused")
public class DataBuffer {
    private static final int READ_INTERVAL = 5;
    private static final int BUFFER_MAX_SIZE = 32 * 1024;
    private final byte[] buffer = new byte[BUFFER_MAX_SIZE];

    private int bufferOffset = 0;
    private final Lock lock = new ReentrantLock();
    private volatile int workingCounter = 0;

    public boolean isWorking() {
        lock.lock();
        try {
            return (workingCounter > 0);
        } finally {
            lock.unlock();
        }
    }

    public void setWorking(boolean val) {
        lock.lock();
        try {
            if (val) {
                incrementWorkingCounter(1);
            } else {
                incrementWorkingCounter(-1);
            }
        } finally {
            lock.unlock();
        }
    }

    private synchronized void incrementWorkingCounter(int val) {
        workingCounter += val;
    }

    public void resetWorkingCounter() {
        lock.lock();
        try {
            workingCounter = 0;
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        return getLength() == 0;
    }

    public int getLength() {
        return bufferOffset;
    }

    public void push(byte[] newBuf) {
        push(newBuf, 0, newBuf.length);
    }

    public void push(byte[] newBuf, int offset, int count) {
        lock.lock();
        try {
            if (count + bufferOffset > buffer.length) {
                count = buffer.length - bufferOffset;
            }
            if (count > 0) {
                System.arraycopy(newBuf, offset, buffer, bufferOffset, count);
                bufferOffset += count;
            }
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            bufferOffset = 0;
        } finally {
            lock.unlock();
        }
    }

    public void delete(int offset, int count) {
        if (offset >= getLength()) {
            return;
        }

        lock.lock();
        try {
            if (offset + count > getLength()) {
                count = getLength() - offset;
            }
            int len = getLength() - offset - count;
            if (len > 0) {
                System.arraycopy(buffer, offset + count, buffer, offset, len);
                bufferOffset -= count;
            } else {
                bufferOffset = offset;
            }
        } finally {
            lock.unlock();
        }
    }

    public byte[] pop(int count) {
        if (count <= 0) {
            return null;
        }

        int remain = getLength();
        count = Math.min(count, remain);
        byte[] buf;

        lock.lock();
        try {
            buf = new byte[count];
            System.arraycopy(buffer, 0, buf, 0, count);
            delete(0, count);
        } finally {
            lock.unlock();
        }

        return buf;
    }

    public int lookup(byte[] bts) {
        lock.lock();
        try {
            int end = getLength() - bts.length;
            boolean matched = false;
            for (int pos = 0; pos <= end; pos++) {
                for (int cmp = 0; cmp < bts.length; cmp++) {
                    matched = (buffer[pos + cmp] == bts[cmp]);
                    if (!matched) {
                        break;
                    }
                }
                if (matched) {
                    return pos;
                }
            }
            return -1;
        } finally {
            lock.unlock();
        }
    }

    public byte[] readLine(byte[] lineEnd) {
        lock.lock();
        try {
            int len = lookup(lineEnd);
            if (len > 0) {
                byte[] buf = pop(len);
                pop(lineEnd.length);
                return buf;
            } else if (len == 0) {
                pop(lineEnd.length);
                return new byte[]{};
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    private void trySleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(READ_INTERVAL);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // Ignore this Exception
        }
    }

    public byte[] readLine(byte[] lineEnd, long timeout) {
        try {
            long end = System.currentTimeMillis() + timeout;
            while (System.currentTimeMillis() <= end) {
                byte[] line = readLine(lineEnd);
                if (line != null) {
                    return line;
                }
                trySleep();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] readExisting() {
        lock.lock();
        try {
            return pop(getLength());
        } finally {
            lock.unlock();
        }
    }

    public byte[] readBytes(int count) {
        lock.lock();
        try {
            if (getLength() < count) {
                return null;
            }
            return pop(count);
        } finally {
            lock.unlock();
        }
    }

    public byte[] readBytes(int count, long timeout) {
        try {
            long end = System.currentTimeMillis() + timeout;
            while (System.currentTimeMillis() <= end) {
                byte[] buf = readBytes(count);
                if (buf != null) {
                    return buf;
                }
                trySleep();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean waitByte(byte bt, long timeout) {
        try {
            long end = System.currentTimeMillis() + timeout;
            while (System.currentTimeMillis() <= end) {
                byte[] buf = readBytes(1);
                if (buf != null && buf[0] == bt) {
                    return true;
                }
                trySleep();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
