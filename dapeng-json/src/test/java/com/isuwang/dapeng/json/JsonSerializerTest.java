package com.isuwang.dapeng.json;

import com.isuwang.dapeng.client.netty.TSoaTransport;
import com.isuwang.dapeng.core.metadata.Method;
import com.isuwang.dapeng.core.metadata.Service;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TCompactProtocol;
import com.isuwang.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Collectors;

/**
 * Unit test for simple App.
 */
public class JsonSerializerTest {
    private static Service getService(final String xmlFilePath) throws IOException {
        String xmlContent =  IOUtils.toString(JsonSerializerTest.class.getResource(xmlFilePath), "UTF-8");
        return JAXB.unmarshal(new StringReader(xmlContent), com.isuwang.dapeng.core.metadata.Service.class);
    }

    private static String loadJson(final String jsonPath) throws IOException {
        return IOUtils.toString(JsonSerializerTest.class.getResource(jsonPath), "UTF-8");
    }

    public static void main(String[] args) throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml";
        final String orderDescriptorXmlPath = "/order.xml";

        Service crmService = getService(crmDescriptorXmlPath);

        Service orderService = getService(orderDescriptorXmlPath);

        Method orderServicePayNotify = orderService.methods.stream().filter(method->{return method.name.equals("payNotify");}).collect(Collectors.toList()).get(0);
        String payNotifyJson = loadJson("/orderService_payNotify.json");


        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);

        JsonSerializer jsonSerializer = new JsonSerializer(orderServicePayNotify.request, requestBuf, orderService, orderServicePayNotify);

        TProtocol outProtocol = new TCompactProtocol(new TSoaTransport(requestBuf));
        jsonSerializer.write(payNotifyJson, outProtocol);

        TProtocol inProtocol = new TCompactProtocol(new TSoaTransport(requestBuf));

        System.out.println("origJson:\n" + payNotifyJson);

        System.out.println("after enCode and decode:\n" + jsonSerializer.read(inProtocol));
    }
}
