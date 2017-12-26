package com.isuwang.dapeng.metadata;

import com.isuwang.dapeng.client.netty.SoaConnectionImpl;
import com.isuwang.dapeng.core.SoaException;
import com.isuwang.org.apache.thrift.TException;

/**
 * Created by tangliu on 2016/3/3.
 */
public class MetadataClient {

    private final String serviceName;
    private final String version;
    private final String methodName = "getServiceMetadata";

    public MetadataClient(String serviceName, String version) {
        this.serviceName = serviceName;
        this.version = version;
    }

    /**
     * getServiceMetadata
     **/
    public String getServiceMetadata() throws Exception {
        getServiceMetadata_result result = new SoaConnectionImpl("127.0.0.1",9090)
                .send(serviceName,version,methodName, new getServiceMetadata_args(),
                        new GetServiceMetadata_argsSerializer(),new GetServiceMetadata_resultSerializer());

        return result.getSuccess();
    }


}
