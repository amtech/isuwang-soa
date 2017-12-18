package com.isuwang.dapeng.doc;


import com.isuwang.dapeng.core.metadata.Method;
import com.isuwang.dapeng.core.metadata.Service;
import com.isuwang.dapeng.core.metadata.Struct;
import com.isuwang.dapeng.core.metadata.TEnum;
import com.isuwang.dapeng.doc.cache.ServiceCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Api服务Controller
 *
 * @author craneding
 * @date 15/9/29
 */
@Controller
@RequestMapping(value = "api")
public class ApiServiceController {
    @Autowired
    private ServiceCache serviceCache;

    @ModelAttribute
    public void populateModel(Model model) {
        model.addAttribute("tagName", "api");
    }

    @RequestMapping(value = "index", method = RequestMethod.GET)
    public String api(HttpServletRequest request) {
        Map<String, Service> services = serviceCache.getServices();

        request.setAttribute("services", services.values());

        return "api/api";
    }

    @RequestMapping(value = "service/{serviceName}/{version}", method = RequestMethod.GET)
    @Transactional(value = "isuwang_api", rollbackFor = Exception.class)
    public String service(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version) {
        request.setAttribute("service", serviceCache.getService(serviceName, version));
        request.setAttribute("services", serviceCache.getServices().values());
        return "api/service";
    }

    @RequestMapping(value = "method/{serviceName}/{version}/{methodName}", method = RequestMethod.GET)
    @Transactional(value = "isuwang_api", rollbackFor = Exception.class)
    public String method(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String methodName) {
        Service service = serviceCache.getService(serviceName, version);

        Method seleted = null;
        List<Method> methods = service.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                seleted = method;

                break;
            }
        }

        Collections.sort(methods, (arg0, arg1) -> arg0.getName().compareTo(arg1.getName()));

        request.setAttribute("service", service);
        request.setAttribute("methods", methods);
        request.setAttribute("method", seleted);
        return "api/method";
    }

    @RequestMapping(value = "findmethod/{serviceName}/{version}/{methodName}", method = RequestMethod.GET)
    @ResponseBody
    public Method findMethod(@PathVariable String serviceName, @PathVariable String version, @PathVariable String methodName) {
        Service service = serviceCache.getService(serviceName, version);

        List<Method> methods = service.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }

        return null;
    }

    @RequestMapping(value = "struct/{serviceName}/{version}/{ref}", method = RequestMethod.GET)
    @Transactional(value = "isuwang_api", rollbackFor = Exception.class)
    public String struct(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String ref) {
        Service service = serviceCache.getService(serviceName, version);

        List<Struct> structDefinitions = service.getStructDefinitions();

        for (Struct struct : structDefinitions) {
            String fullStructName = struct.getNamespace() + "." + struct.getName();

            if (fullStructName.equals(ref)) {
                request.setAttribute("struct", struct);
                break;
            }
        }

        request.setAttribute("service", service);
        request.setAttribute("structs", service.getStructDefinitions());
        return "api/struct";
    }

    @RequestMapping(value = "findstruct/{serviceName}/{version}/{fullStructName}", method = RequestMethod.GET)
    @ResponseBody
    public Struct findStruct(@PathVariable String serviceName, @PathVariable String version, @PathVariable String fullStructName) {
        Service service = serviceCache.getService(serviceName, version);

        List<Struct> structDefinitions = service.getStructDefinitions();

        for (Struct struct : structDefinitions) {
            String fsname = struct.getNamespace() + "." + struct.getName();

            if (fsname.equals(fullStructName)) {
                return struct;
            }
        }

        return null;
    }

    @RequestMapping(value = "enum/{serviceName}/{version}/{ref}", method = RequestMethod.GET)
    @Transactional(value = "isuwang_api", rollbackFor = Exception.class)
    public String anEnum(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String ref) {
        Service service = serviceCache.getService(serviceName, version);

        List<TEnum> enums = service.getEnumDefinitions();

        for (TEnum anEnum : enums) {
            String ename = anEnum.getNamespace() + "." + anEnum.getName();

            if (ename.equals(ref)) {
                request.setAttribute("anEnum", anEnum);

                break;
            }
        }

        request.setAttribute("service", service);
        request.setAttribute("enums", service.getEnumDefinitions());
        return "api/enum";
    }

    @RequestMapping(value = "test/{serviceName}/{version}/{methodName}", method = RequestMethod.GET)
    @Transactional(value = "isuwang_api", rollbackFor = Exception.class)
    public String goTest(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String methodName) {

        Service service = serviceCache.getService(serviceName, version);

        Method seleted = null;
        List<Method> methods = service.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                seleted = method;

                break;
            }
        }

        request.setAttribute("service", service);
        request.setAttribute("method", seleted);
        request.setAttribute("services", serviceCache.getServices().values());
        return "api/test";
    }

    @RequestMapping(value = "findService/{serviceName}/{version}", method = RequestMethod.GET)
    @ResponseBody
    public Service findService(@PathVariable String serviceName, @PathVariable String version) {
        Service service = serviceCache.getService(serviceName, version);
        return service;
    }

    @RequestMapping(value = "findServiceAfterRefresh/{serviceName}/{version}/{refresh}", method = RequestMethod.GET)
    @ResponseBody
    public Service findService(@PathVariable String serviceName, @PathVariable String version, @PathVariable boolean refresh) {
//        if (refresh) {
//            serviceCache.reloadServices();
//        }
        Service service = serviceCache.getService(serviceName, version);
        return service;
    }
}
