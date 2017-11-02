package com.example.lubrian.superproject.thriftManager;

import java.io.ByteArrayOutputStream;

/**
 * @author rwondratschek
 */
/*package*/ class LazyByteArrayOutputStream extends ByteArrayOutputStream {

    @Override
    public synchronized byte[] toByteArray() {
        //原方法是new一個byte[]拷貝原有數據再返回，但是在這邊我們在外層給與final，所以這邊優化流程直接返回主要byte[]
        return buf;
    }
}