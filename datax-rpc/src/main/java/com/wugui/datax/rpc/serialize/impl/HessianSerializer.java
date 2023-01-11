package com.wugui.datax.rpc.serialize.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.wugui.datax.rpc.serialize.Serializer;
import com.wugui.datax.rpc.util.XxlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * hessian serialize
 * @author xuxueli 2015-9-26 02:53:29
 */
public class HessianSerializer extends Serializer {

	private static final Logger logger = LoggerFactory.getLogger(HessianSerializer.class);

	@Override
	public <T> byte[] serialize(T obj){
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Hessian2Output ho = new Hessian2Output(os);
		try {
			ho.writeObject(obj);
			ho.flush();
			return os.toByteArray();
		} catch (IOException e) {
			throw new XxlRpcException(e);
		} finally {
			try {
				ho.close();
				os.close();
			} catch (IOException e) {
				logger.error("序列化关闭流失败！！！");
				e.printStackTrace();
			}
		}

	}

	@Override
	public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		Hessian2Input hi = new Hessian2Input(is);
		try {
			return hi.readObject(clazz);
		} catch (IOException e) {
			throw new XxlRpcException(e);
		} finally {
			try {
				hi.close();
				is.close();
			} catch (Exception e) {
				logger.error("反序列化关闭流失败！！！");
				e.printStackTrace();
			}
		}
	}
	
}
