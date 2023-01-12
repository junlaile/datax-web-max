package com.wugui.datax.rpc.serialize.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.wugui.datax.rpc.serialize.Serializer;
import com.wugui.datax.rpc.util.XxlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * hessian serialize
 *
 * @author xuxueli 2015-9-26 02:53:29
 */
public class HessianSerializer extends Serializer {

    private static final Logger logger = LoggerFactory.getLogger(HessianSerializer.class);

    @Override
    public <T> byte[] serialize(T obj) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)){
            objectOutputStream.writeObject(obj);
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new XxlRpcException("序列化文件报错",e);
        }
    }

    @Override
    public <T> Object deserialize(byte[] bytes, Class<T> clazz) {

        try (ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(bytes);
             ObjectInputStream inputStream = new ObjectInputStream(arrayInputStream)){

            return inputStream.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            logger.error("反序列化报错！！！",e);
            throw new XxlRpcException("反序列化文件报错",e);
        }
    }

//    @Override
//    public <T> byte[] serialize(T obj) {
//        if (obj instanceof XxlRpcRequest ||
//                obj instanceof XxlRpcResponse) {
//            logger.info("写入对象{}", obj);
//            return JSONObject.toJSONBytes(obj);
//        }
//        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        Hessian2Output ho = new Hessian2Output(os);
//        try {
//            ho.writeObject(obj);
//            ho.flush();
//            return os.toByteArray();
//        } catch (IOException e) {
//            throw new XxlRpcException(e);
//        } finally {
//            try {
//                ho.close();
//                os.close();
//            } catch (IOException e) {
//                logger.error("序列化关闭流失败！！！");
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Override
//    public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
//        if (clazz.getName().equals(XxlRpcRequest.class.getName()) ||
//                clazz.getName().equals(XxlRpcResponse.class.getName())) {
//            logger.info("待解析对象{}", clazz.getSimpleName());
//            return JSONObject.parseObject(bytes, clazz);
//        }
//
//        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
//        Hessian2Input hi = new Hessian2Input(is);
//        try {
//            return hi.readObject(clazz);
//        } catch (IOException e) {
//            throw new XxlRpcException(e);
//        } finally {
//            try {
//                hi.close();
//                is.close();
//            } catch (Exception e) {
//                logger.error("反序列化关闭流失败！！！");
//                e.printStackTrace();
//            }
//        }
//    }



}
