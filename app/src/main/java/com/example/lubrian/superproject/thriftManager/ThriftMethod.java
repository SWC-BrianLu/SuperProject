package com.example.lubrian.superproject.thriftManager;

import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.meta_data.FieldMetaData;
import org.apache.thrift.meta_data.FieldValueMetaData;
import org.apache.thrift.protocol.TMessageType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


@SuppressWarnings("rawtypes")
class ThriftMethod {
    private final boolean oneWay;
    private final String name;
    final Class<?>[] declaredThrowableException;
    private final TBase<? extends TBase, TFieldIdEnum> argsObject;
    private final TFieldIdEnum[] argsFieldIdEnums;

    private final TBase<? extends TBase, TFieldIdEnum> resultObject;
    private final TFieldIdEnum successField;
    private final List<TFieldIdEnum> exceptionFields;

    @SuppressWarnings({ "unchecked", "SuspiciousArrayCast" })
    ThriftMethod(Class<?> clientClass, Method method, String thriftServiceName) {
        requireNonNull(clientClass);
        requireNonNull(method);
        requireNonNull(thriftServiceName);
        name = method.getName();
        declaredThrowableException = method.getExceptionTypes();

        boolean oneWay = false;
        try {
            clientClass.getMethod("recv_" + name);
        } catch (NoSuchMethodException ignore) {
            oneWay = true;
        }
        this.oneWay = oneWay;

        String argClassName = thriftServiceName + '$' + name + "_args";
        final Class<TBase<? extends TBase, TFieldIdEnum>> argClass;
        try {
            argClass = (Class<TBase<? extends TBase, TFieldIdEnum>>) Class.forName(argClassName);
            argsObject = argClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("fail to create a new instance: " + argClassName, e);
        }

        String argFieldEnumName = thriftServiceName + '$' + name + "_args$_Fields";
        try {
            Class<?> fieldIdEnumClass = Class.forName(argFieldEnumName);
            argsFieldIdEnums = (TFieldIdEnum[]) requireNonNull(fieldIdEnumClass.getEnumConstants(),
                    "field enum may not be empty");
        } catch (Exception e) {
            throw new IllegalArgumentException("fail to create a new instance : " + argFieldEnumName, e);
        }

        FieldValueMetaData successFieldMetadata = null;
        if (oneWay) {
            resultObject = null;
            successField = null;
            exceptionFields = Collections.emptyList();
        } else {
            String resultClassName = thriftServiceName + '$' + name + "_result";
            final Class resultClass;
            try {
                resultClass = Class.forName(resultClassName);
                resultObject = (TBase<? extends TBase, TFieldIdEnum>) resultClass.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("fail to create a new instance : " + resultClassName, e);
            }

            try {

                @SuppressWarnings("unchecked")
                final Map<TFieldIdEnum, FieldMetaData> resultMetaDataMap =
                        (Map<TFieldIdEnum, FieldMetaData>) FieldMetaData.getStructMetaDataMap(resultClass);

                TFieldIdEnum successField = null;
                List<TFieldIdEnum> exceptionFields = new ArrayList<>(resultMetaDataMap.size());
                for (Entry<TFieldIdEnum, FieldMetaData> e : resultMetaDataMap.entrySet()) {
                    final TFieldIdEnum key = e.getKey();
                    final String fieldName = key.getFieldName();
                    if ("success".equals(fieldName)) {
                        successField = key;
                        successFieldMetadata = e.getValue().valueMetaData;
                        continue;
                    }

                    Class<?> fieldType = resultClass.getField(fieldName).getType();
                    if (Throwable.class.isAssignableFrom(fieldType)) {
                        exceptionFields.add(key);
                    }
                }

                this.successField = successField;
                this.exceptionFields = Collections.unmodifiableList(exceptionFields);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "failed to find the success metaDataMap: " + resultClass.getName(),
                        e);
            }
        }
    }

    TBase createArgs() {
        return argsObject.deepCopy();
    }

    @SuppressWarnings("unchecked")
    TBase createArgs(boolean isAsync, Object[] args) {
        final TBase newArgs = createArgs();
        if (args != null) {
            final int toFillArgLength = args.length - (isAsync ? 1 : 0);
            for (int i = 0; i < toFillArgLength; i++) {
                newArgs.setFieldValue(argsFieldIdEnums[i], args[i]);
            }
        }
        return newArgs;
    }


    byte methodType() {
        return oneWay ? TMessageType.ONEWAY : TMessageType.CALL;
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
     * Returns {@code o} if non-null, or throws {@code NullPointerException}.
     */
    public static <T> T requireNonNull(T o) {
        if (o == null) {
            throw new NullPointerException();
        }
        return o;
    }

    /**
     * Returns {@code o} if non-null, or throws {@code NullPointerException}
     * with the given detail message.
     */
    public static <T> T requireNonNull(T o, String message) {
        if (o == null) {
            throw new NullPointerException(message);
        }
        return o;
    }


    Class<?>[] declaredThrowableException() {
        return declaredThrowableException;
    }

    TBase<? extends TBase, TFieldIdEnum> createResult() {
        return resultObject.deepCopy();
    }

    TFieldIdEnum successField() {
        return successField;
    }

    List<TFieldIdEnum> getExceptionFields() {
        return exceptionFields;
    }
}