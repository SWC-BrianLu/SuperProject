package com.example.lubrian.superproject.thriftManager;


import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.LinkedBlockingQueue;

public class TransportManager {

    private LinkedBlockingQueue<TAndroidTransport> m_queue;
    /**
     * Protocol運輸車輛數量上限
     */
    private final int m_iMaxSize;
    /**
     * http元件
     */
    private OkHttpClient m_OkHttpClient;

    /**
     * 當前Protocol運輸車輛數量
     */
    private int m_iNowTransportInstanceNum;
    private String m_sServerUrl;

    /**
     * 建立一個運輸Protocol的車庫
     *
     * @param serverUrl 運輸地點
     * @param maxSize   車輛上限
     */
    public TransportManager(String serverUrl, int maxSize) {
        this.m_iMaxSize = maxSize;
        this.m_sServerUrl = serverUrl;
        this.m_OkHttpClient = new OkHttpClient();
        this.m_queue = new LinkedBlockingQueue<>(this.m_iMaxSize);
        this.m_iNowTransportInstanceNum = 0;
    }

    /**
     * call一台Protocol運輸車輛跑Protocol
     *
     * @return 一台有空的運輸車
     * @throws InterruptedException
     */
    public TAndroidTransport syncGet() throws InterruptedException {
        synchronized (this) {
            //跑protocol的運輸車輛還沒到限制上限時 就叫工廠做出來跑運輸
            if (this.m_queue.size() == 0 && this.m_iNowTransportInstanceNum < this.m_iMaxSize) {
                MemoryByteStore mb = new MemoryByteStore.Factory().create();
                TAndroidTransport transport = new TAndroidTransport(m_OkHttpClient, mb, this.m_sServerUrl);
                //將車輛放進車庫 如果車客還有位置 則返回true 若無位置則會拋出錯誤
                this.m_queue.add(transport);
                this.m_iNowTransportInstanceNum++;
            }
            //工廠會排班叫運輸車輛去送Protocol 如果當前沒有排班車輛 會等到有車輛回來再運送
            TAndroidTransport transport = this.m_queue.take();
            return transport;
        }
    }

    //把運輸完畢的車輛丟回排班列表
    public void putReuse(TAndroidTransport transport) throws InterruptedException {
        //將車輛停回車庫 如果車庫是滿的 會等待車庫有位置再停進去
        this.m_queue.put(transport);
    }

}
