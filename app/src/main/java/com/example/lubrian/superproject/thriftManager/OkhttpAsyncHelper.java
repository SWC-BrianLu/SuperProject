package com.example.lubrian.superproject.thriftManager;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 非同步管理器
 *
 * 閱讀及理解本篇你需要知道幾個關鍵字
 * "Proxy"
 * "InvocationHandler"
 * "BlockingQueue" by 超
 *
 */
public class OkhttpAsyncHelper implements InvocationHandler {

    private static final Map<Class<?>, Map<String, ThriftMethod>> methodMapCache = new HashMap<>();
    private static ExecutorService executorService;
    private Map<String, ThriftMethod> asyncMethodMap;
    private Map<String, ThriftMethod> syncMethodMap;
    private boolean isAsyncClient;
    private static final String SYNC_IFACE = "Iface";
    private static final String ASYNC_IFACE = "AsyncIface";

    private final AtomicInteger seq = new AtomicInteger();

    private String serverUrl;

    public TransportManager m_transportManager;


    public OkhttpAsyncHelper(String url)
    {
        int maxSize = 1;
        this.serverUrl = url;
        this.m_transportManager = new TransportManager(this.serverUrl,maxSize);
        executorService = Executors.newFixedThreadPool(maxSize);
    }

    public <T> T build(Class<T> interfaceClass,Class iface) {
        asyncMethodMap = getThriftMethodMapFromInterface(interfaceClass, true);
        syncMethodMap = getThriftMethodMapFromInterface(iface, false);

        //創建動態代理
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class[] { interfaceClass },
                this);
    }

    //將協定方法介面從FPService裡面抓出來放在Cache裡
    private static Map<String, ThriftMethod> getThriftMethodMapFromInterface(Class<?> interfaceClass,
                                                                             boolean isAsyncInterface) {
        Map<String, ThriftMethod> methodMap = methodMapCache.get(interfaceClass);
        if (methodMap != null) {
            return methodMap;
        }
        methodMap = new HashMap<>();

        String interfaceName = interfaceClass.getName();
        ClassLoader loader = interfaceClass.getClassLoader();

        int interfaceNameSuffixLength = isAsyncInterface ? ASYNC_IFACE.length() : SYNC_IFACE.length();

        final String thriftServiceName =
                interfaceName.substring(0, interfaceName.length() - interfaceNameSuffixLength - 1);

        final Class<?> clientClass;
        try {
            clientClass = Class.forName(thriftServiceName + "$Client", false, loader);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "Thrift Client Class not found. serviceName:" + thriftServiceName, e);
        }

        for (Method method : interfaceClass.getMethods()) {
            ThriftMethod thriftMethod = new ThriftMethod(clientClass, method, thriftServiceName);
            methodMap.put(method.getName(), thriftMethod);
        }

        //將其map映射出來放到cache裡，透過映射出來的map是無法修改的，修改會拋錯誤
        Map<String, ThriftMethod> resultMap = Collections.unmodifiableMap(methodMap);
        methodMapCache.put(interfaceClass, resultMap);

        return methodMap;

    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (args == null) {
            throw new IllegalArgumentException(
                    "not async call");
        }
        //非同步最後一個參數為callback
        AsyncMethodCallback callback = asyncCallback(args);
        try {
            //同步呼叫方法必須去掉AsyncMethodCallback的參數
            Object[] newArgs = new Object[args.length-1];
            //複製新的參數陣列
            System.arraycopy(args,0,newArgs,0,newArgs.length);
            //提交任務
            executorService.submit(new InvokeRunnable(method,newArgs,callback));
        } catch (Exception e){
            callback.onError(e);
        }
        return null;
    }

    static AsyncMethodCallback asyncCallback(Object[] args) {
        if (requireNonNull(args, "args").length == 0) {
            throw new IllegalArgumentException("args must contains objects");
        }
        final Object lastObj = args[args.length - 1];
        if (lastObj instanceof AsyncMethodCallback) {
            return (AsyncMethodCallback) lastObj;
        }
        if (lastObj == null) {
            return null;
        }
        throw new IllegalArgumentException(
                "the last element of args must be AsyncMethodCallback: " + lastObj.getClass().getName());
    }

    /**
     * 檢查null
     * Returns {@code o} if non-null, or throws {@code NullPointerException}
     * with the given detail message.
     */
    public static <T> T requireNonNull(T o, String message) {
        if (o == null) {
            throw new NullPointerException(message);
        }
        return o;
    }

    //實作反射的Runnable
    private class InvokeRunnable implements Runnable
    {
        private final String methodName;
        private final AsyncMethodCallback callback;
        private final Object[] args;
        private Method method;

        public InvokeRunnable( Method method,Object[] args,AsyncMethodCallback callback)
        {
            this.method = method;
            this.methodName = this.method.getName();
            this.args = args;
            this.callback = callback;
        }
        @Override
        public void run() {
            TAndroidTransport transport = null;
            try
            {
                //從車庫取得Protocol運輸車輛
                transport = m_transportManager.syncGet();
                //載裝method
                ThriftMethod thriftMethod = asyncMethodMap.get(this.methodName);
                //準備上路
                TProtocol tProtocol = new TBinaryProtocol(transport);
                //填寫出貨單
                TMessage tMessage = new TMessage(this.methodName, thriftMethod.methodType(),
                        seq.incrementAndGet());
                //安心上路
                tProtocol.writeMessageBegin(tMessage);
                TBase tArgs = thriftMethod.createArgs(isAsyncClient, args);
                tArgs.write(tProtocol);
                tProtocol.writeMessageEnd();
                transport.flush();

                //安安你好我回來了
                TMessage msg = tProtocol.readMessageBegin();
                if (msg.type == TMessageType.EXCEPTION) {
                    TApplicationException ex = TApplicationException.readFrom(tProtocol);
                    tProtocol.readMessageEnd();
                    throw ex;
                }

                if (thriftMethod == null) {
                    throw new TApplicationException(TApplicationException.WRONG_METHOD_NAME, msg.name);
                }
                TBase<? extends TBase, TFieldIdEnum> result = thriftMethod.createResult();
                result.read(tProtocol);
                tProtocol.readMessageEnd();

                for (TFieldIdEnum fieldIdEnum : thriftMethod.getExceptionFields()) {
                    if (result.isSet(fieldIdEnum)) {
                        throw (TException) result.getFieldValue(fieldIdEnum);
                    }
                }

                TFieldIdEnum successField = thriftMethod.successField();
                if (successField == null) { //void method
                    callback.onComplete(null);
                }
                if (result.isSet(successField)) {
                    callback.onComplete(result.getFieldValue(successField));
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
                callback.onError(e);
            }
            finally {
                try
                {
                    if(transport!=null)
                    {
                        m_transportManager.putReuse(transport);
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
