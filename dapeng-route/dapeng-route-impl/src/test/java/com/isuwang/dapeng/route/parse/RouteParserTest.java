package com.isuwang.dapeng.route.parse;

import com.isuwang.dapeng.core.InvocationContext;
import com.isuwang.dapeng.core.InvocationContextImpl;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.route.Route;
import com.isuwang.dapeng.route.RouteExecutor;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Eric on 2016-06-21
 * @date
 */
public class RouteParserTest {

    public static void main(String args[]) {
        String source = "      operatorId match ~%'1024n+0..9' => ~ip'1.2.3.4'\n" +
                "      operatorId match n'10|11|12|13' => ~ip'1.2.3.4'\n" +
                "      operatorId match ~n'10..20' => ~ip'1.2.3.4'\n" +
                "      callerFrom match s'app|oss' => ~ip'1.2.3/24'\n" +
                "      ip match ~ip'1.2.3.0/24|192.168.3.39' => ~ip'1.2.3.4|192.168.1.39/32'\n";

//        String str = "operatorId match %'1024n+0..9' and ip match ip'192.168.3/24' => ~ip'1.2.3.4|192.168.1.1/24'";
//        String str = "service match s'ArticleService' => ~ip'1.2.3.4|192.168.1.1/24'";
        String str = "service match s'com.isuwang.soa.user.service.UserService' and version match s'1.0.0' => ~ip'192.168.3.39'";
        RouteParser parser = new RouteParser();

        List routes = new ArrayList<Route>();
        parser.parseAll(routes, str);

        List<String> servers = new ArrayList<>();
        servers.add("1.2.3.4");
        servers.add("1.2.3.5");

        InvocationContext ctx = new InvocationContextImpl();
        SoaHeader soaHeader = new SoaHeader();
        soaHeader.setOperatorId(Optional.of(1024));
        soaHeader.setCallerIp(Optional.of("192.168.3.39"));
        soaHeader.setCallerFrom(Optional.of("app"));

        soaHeader.setServiceName("com.isuwang.soa.user.service.UserService");
        soaHeader.setVersionName("1.0.0");
        soaHeader.setMethodName("getArticleDetail");

        ctx.setHeader(soaHeader);

        Set<InetAddress> serverResult = RouteExecutor.execute(ctx, routes, servers);

        System.out.println(serverResult);

    }
}
