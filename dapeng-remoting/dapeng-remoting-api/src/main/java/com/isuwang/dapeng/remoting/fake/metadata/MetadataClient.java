package com.isuwang.dapeng.remoting.fake.metadata;

import com.isuwang.dapeng.core.SoaException;
import com.isuwang.dapeng.remoting.BaseCommonServiceClient;
import com.isuwang.org.apache.thrift.TException;

/**
 * Created by tangliu on 2016/3/3.
 */
public class MetadataClient extends BaseCommonServiceClient {

    public MetadataClient(String serviceName, String versionName) {
        super(serviceName, versionName);
    }

    /**
     * getServiceMetadata
     **/
    public String getServiceMetadata() throws SoaException {

        initContext("getServiceMetadata");
        try {
            getServiceMetadata_args getServiceMetadata_args = new getServiceMetadata_args();
            getServiceMetadata_result response = sendBase(getServiceMetadata_args, new GetServiceMetadata_argsSerializer(), new GetServiceMetadata_resultSerializer());
            return response.getSuccess();
        } catch (SoaException e) {
            throw e;
        } catch (TException e) {
            throw new SoaException(e);
        } finally {
            destoryContext();
        }
    }


}
