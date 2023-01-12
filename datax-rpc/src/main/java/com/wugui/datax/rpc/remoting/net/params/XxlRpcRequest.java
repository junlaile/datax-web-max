package com.wugui.datax.rpc.remoting.net.params;

import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;

/**
 * request
 *
 * @author xuxueli 2015-10-29 19:39:12
 */
@Data
public class XxlRpcRequest implements Serializable {
	private static final long serialVersionUID = 459L;
	
	private String requestId;
	private long createMillisTime;
	private String accessToken;

    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;

	private String version;


	@Override
	public String toString() {
		return "XxlRpcRequest{" +
				"requestId='" + requestId + '\'' +
				", createMillisTime=" + createMillisTime +
				", accessToken='" + accessToken + '\'' +
				", className='" + className + '\'' +
				", methodName='" + methodName + '\'' +
				", parameterTypes=" + Arrays.toString(parameterTypes) +
				", parameters=" + Arrays.toString(parameters) +
				", version='" + version + '\'' +
				'}';
	}

}
