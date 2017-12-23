package com.isuwang.dapeng.remoting.fake.metadata;

import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.*;

/**
 * Created by tangliu on 2016/3/3.
 */
public class GetServiceMetadata_argsSerializer implements BeanSerializer<getServiceMetadata_args> {

    @Override
    public getServiceMetadata_args read( TProtocol iprot) throws TException {

        getServiceMetadata_args bean =new getServiceMetadata_args();
        TField schemeField;
        iprot.readStructBegin();

        while (true) {
            schemeField = iprot.readFieldBegin();
            if (schemeField.type == TType.STOP) {
                break;
            }
            switch (schemeField.id) {
                default:
                    TProtocolUtil.skip(iprot, schemeField.type);

            }
            iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        validate(bean);
        return bean;
    }


    @Override
    public void write(getServiceMetadata_args bean, TProtocol oprot) throws TException {

        validate(bean);
        oprot.writeStructBegin(new TStruct("getServiceMetadata_args"));
        oprot.writeFieldStop();
        oprot.writeStructEnd();
    }

    public void validate(getServiceMetadata_args bean) throws TException {
    }

    @Override
    public String toString(getServiceMetadata_args bean) {
        return bean == null ? "null" : bean.toString();
    }

}

