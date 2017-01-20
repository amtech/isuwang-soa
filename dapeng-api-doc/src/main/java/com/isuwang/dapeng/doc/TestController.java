package com.isuwang.dapeng.doc;

import com.isuwang.dapeng.core.metadata.Service;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.doc.cache.ServiceCache;
import com.isuwang.dapeng.remoting.fake.json.JSONPost;
import com.isuwang.dapeng.remoting.filter.LoadBalanceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * 测试Controller
 *
 * @author tangliu
 * @date 15/10/8
 */
@Controller
@RequestMapping("test")
public class TestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestController.class);

    @ModelAttribute
    public void populateModel(Model model) {
        model.addAttribute("tagName", "test");
    }

    @Autowired
    private ServiceCache serviceCache;

    private JSONPost jsonPost;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public String test(HttpServletRequest req) {

        String jsonParameter = req.getParameter("parameter");
        String serviceName = req.getParameter("serviceName");
        String versionName = req.getParameter("version");
        String methodName = req.getParameter("methodName");

        Service service = serviceCache.getService(serviceName, versionName);

        SoaHeader header = new SoaHeader();
        header.setServiceName(serviceName);
        header.setVersionName(versionName);
        header.setMethodName(methodName);
        header.setCallerFrom(Optional.of("TestController"));

        header.setAttachment("userId","1");
        header.setAttachment("userLevel","1");
        header.setAttachment("userScore","50");

        String callerInfo = LoadBalanceFilter.getCallerInfo(serviceName, versionName, methodName);

        if (callerInfo == null && SoaSystemEnvProperties.SOA_REMOTING_MODE.equals("local")) {
            jsonPost = new JSONPost(SoaSystemEnvProperties.SOA_SERVICE_IP, SoaSystemEnvProperties.SOA_SERVICE_PORT, true);
        } else if (callerInfo != null) {
            String[] infos = callerInfo.split(":");
            jsonPost = new JSONPost(infos[0], Integer.valueOf(infos[1]), true);
        } else {
            return "{\"message\":\"没找到可用服务\"}";
        }
        try {
            return jsonPost.callServiceMethod(header, jsonParameter, service);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}
