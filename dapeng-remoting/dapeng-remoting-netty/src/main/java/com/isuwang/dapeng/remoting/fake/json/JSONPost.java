package com.isuwang.dapeng.remoting.fake.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.core.metadata.Service;
import com.isuwang.dapeng.remoting.BaseServiceClient;
import com.isuwang.dapeng.remoting.netty.SoaClient;
import com.isuwang.org.apache.thrift.TApplicationException;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TMessage;
import com.isuwang.org.apache.thrift.protocol.TMessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tangliu on 2016/4/13.
 */
public class JSONPost {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONPost.class);

    private JSONSerializer jsonSerializer = new JSONSerializer();

    private String host = "127.0.0.1";

    private Integer port = 9090;

    private boolean doNotThrowError = false;

    public JSONPost(String host, Integer port, boolean doNotThrowError) {
        this.host = host;
        this.port = port;
        this.doNotThrowError = doNotThrowError;
    }

    private static Map<String, SoaClient> connectionPool = new ConcurrentHashMap<>();

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

        if (null == jsonParameter || "".equals(jsonParameter.trim())) jsonParameter = "{}";

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
        final InvocationInfo invocationInfo = new InvocationInfo();
        final DataInfo request = new DataInfo();
        request.setConsumesType("JSON");
        request.setConsumesValue(out.toString());
        request.setServiceName(soaHeader.getServiceName());
        request.setVersion(soaHeader.getVersionName());
        request.setMethodName(soaHeader.getMethodName());
        invocationInfo.setDataInfo(request);

        final long beginTime = System.currentTimeMillis();

        LOGGER.info("soa-request: {}", out.toString());

        initContext(soaHeader);
        String jsonResponse = post(invocationInfo);

        LOGGER.info("soa-response: {} {}ms", jsonResponse, System.currentTimeMillis() - beginTime);

        return jsonResponse;
    }


    /**
     * 初始化上下文
     *
     * @param soaHeader
     */
    private static void initContext(SoaHeader soaHeader) {

        InvocationContext context = InvocationContext.Factory.getCurrentInstance();
        context.setSeqid(BaseServiceClient.seqid_.incrementAndGet());

        try {
            soaHeader.setCallerIp(Optional.of(InetAddress.getLocalHost().getHostAddress()));
        } catch (UnknownHostException e) {
            LOGGER.error(e.getMessage(), e);
        }

        context.setHeader(soaHeader);
        context.setCalleeTimeout(45000);
    }

    /**
     * 构建客户端，发送和接收请求
     *
     * @param invocationInfo
     * @return
     */
    private String post(InvocationInfo invocationInfo) throws Exception {

        String jsonResponse = "{}";

        SoaClient client = null;
        TSoaTransport inputSoaTransport = null;
        TSoaTransport outputSoaTransport = null;

        try {

            String connectionKey = this.host + ":" + this.port.toString();
            if (connectionPool.containsKey(connectionKey))
                client = connectionPool.get(connectionKey);
            else {
                client = new SoaClient(this.host, this.port);
                connectionPool.put(connectionKey, client);
            }

            InvocationContext context = InvocationContext.Factory.getCurrentInstance();
            SoaHeader soaHeader = context.getHeader();

            final ByteBuf requestBuf = Unpooled.buffer(8192);
            outputSoaTransport = new TSoaTransport(requestBuf);

            TSoaServiceProtocol outputProtocol;

            outputProtocol = new TSoaServiceProtocol(outputSoaTransport, true);
            outputProtocol.writeMessageBegin(new TMessage(invocationInfo.getDataInfo().getServiceName() + ":" + invocationInfo.getDataInfo().getMethodName(), TMessageType.CALL, context.getSeqid()));
            jsonSerializer.write(invocationInfo, outputProtocol);
            outputProtocol.writeMessageEnd();
            outputSoaTransport.flush();//在报文头部写入int,代表报文长度(不包括自己)

            ByteBuf responseBuf = client.send(context.getSeqid(), requestBuf); //发送请求，返回结果

            if (null != responseBuf) {

                inputSoaTransport = new TSoaTransport(responseBuf);
                TSoaServiceProtocol inputProtocol = new TSoaServiceProtocol(inputSoaTransport, true);

                TMessage msg = inputProtocol.readMessageBegin();
                soaHeader=InvocationContext.Factory.getCurrentInstance().getHeader();
                if (TMessageType.EXCEPTION == msg.type) {
                    TApplicationException x = TApplicationException.read(inputProtocol);
                    inputProtocol.readMessageEnd();
                    throw x;
                } else if (context.getSeqid() != msg.seqid) {
                    throw new TApplicationException(4, soaHeader.getMethodName() + " failed: out of sequence response");
                } else {
                    if ("0000".equals(soaHeader.getRespCode().get())) {
                        // 接收返回包
                        jsonSerializer.read(invocationInfo, inputProtocol);
                        inputProtocol.readMessageEnd();
                    } else {
                        throw new SoaException(soaHeader.getRespCode().get(), soaHeader.getRespMessage().get());
                    }

                    return invocationInfo.getResponseData();
                }

            } else {
                throw new SoaException(SoaBaseCode.TimeOut);
            }

        } catch (SoaException e) {

            LOGGER.error(e.getMsg());
            if (doNotThrowError)
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", e.getCode(), e.getMsg(), "{}");
            else
                throw e;

        } catch (TException e) {

            LOGGER.error(e.getMessage(), e);
            if (doNotThrowError)
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "9999", e.getMessage(), "{}");
            else
                throw e;

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);
            if (doNotThrowError)
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "9999", "系统繁忙，请稍后再试[9999]！", "{}");
            else
                throw e;

        } finally {
//            if (client != null) {
//                client.close();
//                client.shutdown();
//            }

            if (outputSoaTransport != null)
                outputSoaTransport.close();

            if (inputSoaTransport != null)
                inputSoaTransport.close();

            if (doNotThrowError)
                InvocationContext.Factory.removeCurrentInstance();
        }

        return jsonResponse;
    }
}
