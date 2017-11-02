package com.example.lubrian.superproject.thriftManager;


import com.example.lubrian.superproject.protocol.IdolService;

public class ThriftUtils {

    private static IdolService.AsyncIface asyncIface = null;

    public static IdolService.AsyncIface getAsyncClient(String url)
    {
        if(asyncIface==null){
            asyncIface = new OkhttpAsyncHelper(url).build(IdolService.AsyncIface.class,IdolService.Iface.class);
        }
        return asyncIface;
    }
}
