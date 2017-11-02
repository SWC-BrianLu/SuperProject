package com.example.lubrian.superproject.thriftManager;
import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * 將所有發出protocol 以byte形式暫存在記憶體中
 * Holds all the data in memory.
 *
 * @author rwondratschek
 */
public class MemoryByteStore extends ByteStore {

    //Byte放置的載體
    protected final LazyByteArrayOutputStream mByteArrayOutputStream;
    //每次寫入的長度
    protected int mBytesWritten;
    //這個接口是否已經被關閉
    protected boolean mClosed;
    //Byte中介
    protected byte[] mData;

    //interface -> crate
    protected MemoryByteStore() {
        mByteArrayOutputStream = new LazyByteArrayOutputStream();
    }

    @Override
    public void write(@NonNull byte[] buffer, int offset, int count) throws IOException {
        checkNotClosed();

        mByteArrayOutputStream.write(buffer, offset, count);
        mBytesWritten += count;
    }

    @Override
    public void write(int oneByte) throws IOException {
        checkNotClosed();

        mByteArrayOutputStream.write(oneByte);
        mBytesWritten++;
    }

    //檢查是否已關閉
    private void checkNotClosed() throws IOException {
        if (mClosed) {
            throw new IOException("Already closed");
        }
    }

    @Override
    public void close() throws IOException {
        if (!mClosed) {
            mByteArrayOutputStream.reset();
            mClosed = true;
        }
    }

    @Override
    public int getBytesWritten() {
        return mBytesWritten;
    }

    @Override
    public byte[] getData() throws IOException {
        if (mData != null) {
            return mData;
        }

        close();

        mData = mByteArrayOutputStream.toByteArray();
        return mData;
    }

    @Override
    public void reset() throws IOException {
        try {
            close();

        } finally {
            mData = null;
            mBytesWritten = 0;
            mClosed = false;
        }
    }

    @SuppressWarnings("unused")
    public static class Factory implements ByteStore.Factory {

        @Override
        public MemoryByteStore create() {
            return new MemoryByteStore();
        }
    }
}
