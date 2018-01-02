package com.isuwang.dapeng.doc;

import com.isuwang.dapeng.client.json.JSONPost;
import com.isuwang.dapeng.core.metadata.Service;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.doc.cache.ServiceCache;
import com.isuwang.dapeng.util.SoaSystemEnvProperties;
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
        try {
            jsonPost = new JSONPost(SoaSystemEnvProperties.SOA_SERVICE_IP, SoaSystemEnvProperties.SOA_SERVICE_PORT, true);
            return jsonPost.callServiceMethod(header, jsonParameter, service);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}
