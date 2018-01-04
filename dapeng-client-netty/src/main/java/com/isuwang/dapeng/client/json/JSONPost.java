package com.isuwang.dapeng.client.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isuwang.dapeng.client.netty.SoaConnectionImpl;
import com.isuwang.dapeng.client.netty.TSoaTransport;
import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.dapeng.core.SoaException;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.core.metadata.Method;
import com.isuwang.dapeng.core.metadata.Service;
import com.isuwang.dapeng.util.SoaMessageBuilder;
import com.isuwang.dapeng.util.SoaSystemEnvProperties;
import com.isuwang.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tangliu on 2016/4/13.
 */
public class JSONPost {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONPost.class);

    private JSONSerializer jsonSerializer = new JSONSerializer();

    private String host = "127.0.0.1";

    private Integer port = SoaSystemEnvProperties.SOA_CONTAINER_PORT;

    private boolean doNotThrowError = false;

    public JSONPost(String host, Integer port, boolean doNotThrowError) {
        this.host = host;
        this.port = port;
        this.doNotThrowError = doNotThrowError;
    }

    /**
     * 调用远程服务
     *
     * @param soaHeader
     * @param jsonParameter
     * @param service
     * @return
     * @throws Exception
     */
    public String callServiceMethod(SoaHeader soaHeader, String jsonParameter, Service service) throws Exception {

        if (null == jsonParameter || "".equals(jsonParameter.trim())) {
            jsonParameter = "{}";
        }

        jsonSerializer.setService(service);

        ObjectMapper objectMapper = new ObjectMapper();
        StringWriter out = new StringWriter();

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> params = objectMapper.readValue(jsonParameter, Map.class);

        Map<String, Object> map = new HashMap<>();
        map.put("serviceName", soaHeader.getServiceName());
        map.put("version", soaHeader.getVersionName());
        map.put("methodName", soaHeader.getMethodName());
        map.put("params", params);

        objectMapper.writeValue(out, map);

        //发起请求
        final DataInfo request = new DataInfo();
        request.setConsumesType("JSON");
        request.setConsumesValue(out.toString());
        request.setServiceName(soaHeader.getServiceName());
        request.setVersion(soaHeader.getVersionName());
        request.setMethodName(soaHeader.getMethodName());
        for (Method method: service.getMethods()) {
            if (method.getName().equals(soaHeader.getMethodName())) {
                request.setMethod(method);
                break;
            }
        }

        jsonSerializer.setDataInfo(request);

        final long beginTime = System.currentTimeMillis();

        LOGGER.info("soa-request: {}", out.toString());

        String jsonResponse = post(soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), out.toString());

        LOGGER.info("soa-response: {} {}ms", jsonResponse, System.currentTimeMillis() - beginTime);

        return jsonResponse;
    }


    /**
     * 构建客户端，发送和接收请求
     *
     * @return
     */
    private String post(String serviceName, String version, String methodName, String requestJson) throws Exception {

        String jsonResponse = "{}";

        TSoaTransport inputSoaTransport = null;
        TSoaTransport outputSoaTransport = null;

        try {
            //TODO: need serialize jsonMap to RequestObj

            Object result = new SoaConnectionImpl(host,port).send(serviceName,version,methodName,requestJson,jsonSerializer,jsonSerializer);

            jsonResponse = (String)result;

        } catch (SoaException e) {

            LOGGER.error(e.getMsg());
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", e.getCode(), e.getMsg(), "{}");
            } else {
                throw e;
            }

        } catch (TException e) {

            LOGGER.error(e.getMessage(), e);
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "9999", e.getMessage(), "{}");
            } else {
                throw e;
            }

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "9999", "系统繁忙，请稍后再试[9999]！", "{}");
            }
            else {
                throw e;
            }

        } finally {
            if (outputSoaTransport != null) {
                outputSoaTransport.close();
            }

            if (inputSoaTransport != null) {
                inputSoaTransport.close();
            }
        }

        return jsonResponse;
    }


    private <REQ> ByteBuf buildRequestBuf(String service, String version, String method,int seqid, REQ request, BeanSerializer<REQ> requestSerializer) throws TException {
        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);//Unpooled.directBuffer(8192);  // TODO Pooled
        final TSoaTransport reqSoaTransport = new TSoaTransport(requestBuf);

        SoaMessageBuilder<REQ> builder = new SoaMessageBuilder<>();

        // TODO set protocol
        SoaHeader header = buildHeader(service, version, method);
        ByteBuf buf = builder.buffer(requestBuf)
                .header(header)
                .body(request, requestSerializer)
                .seqid(seqid)
                .build();
        return buf;
    }

    private SoaHeader buildHeader(String service, String version, String method) {
        SoaHeader header = new SoaHeader();
        header.setServiceName(service);
        header.setVersionName(version);
        header.setMethodName(method);

        // TODO fill header from InvocationContext

        return header;
    }
}
