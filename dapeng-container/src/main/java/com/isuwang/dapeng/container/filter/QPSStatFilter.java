package com.isuwang.dapeng.container.filter;

import com.isuwang.dapeng.core.IPUtils;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.core.TransactionContext;
import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.dapeng.monitor.api.MonitorServiceClient;
import com.isuwang.dapeng.monitor.api.domain.QPSStat;
import com.isuwang.org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

/**
 * QPS Stat Filter
 *
 * @author craneding
 * @date 16/3/7
 */
public class QPSStatFilter implements StatusFilter ,ContainerFilter{

    private static final Logger LOGGER = LoggerFactory.getLogger(QPSStatFilter.class);

    private final long period = 5 * 1000L;
    private Map<String, AtomicInteger> methodCallCount = new ConcurrentHashMap<>();
    private final Timer timer = new Timer("QPSStatFilter-Timer");

    @Override
    public void init() {
        ContainerSoaHeader.setup();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        LOGGER.info("QPSStatFilter 定时时间:{} 上送间隔:{}ms", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(calendar.getTime()), period);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    final long timeMillis = System.currentTimeMillis() / 1000 * 1000;

                    Map<String, AtomicInteger> tmp = methodCallCount;
                    methodCallCount = new ConcurrentHashMap<>();

                    List<QPSStat> qpsStats = tmp.keySet().stream().map(key -> {
                        String[] infos = key.split(":");
                        QPSStat qpsStat = new QPSStat();
                        qpsStat.setPeriod((int) (period / 1000));
                        qpsStat.setAnalysisTime(timeMillis);
                        qpsStat.setServerIP(IPUtils.localIp());
                        qpsStat.setServerPort(SoaSystemEnvProperties.SOA_CONTAINER_PORT);

                        qpsStat.setServiceName(infos[0]);
                        qpsStat.setMethodName(infos[1]);
                        qpsStat.setVersionName(infos[2]);
                        qpsStat.setCallCount(tmp.get(key).get());

                        return qpsStat;
                    }).collect(toList());

                    new MonitorServiceClient().uploadQPSStat(qpsStats);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }, calendar.getTime(), period);
    }

    @Override
    public void doFilter(FilterChain chain) throws TException {

        final SoaHeader soaHeader = TransactionContext.Factory.getCurrentInstance().getHeader();
        final String key = generateKey(soaHeader);

        try {
            chain.doFilter();
        } finally {
            synchronized (methodCallCount) {
                if (methodCallCount.containsKey(key))
                    methodCallCount.get(key).incrementAndGet();
                else
                    methodCallCount.put(key, new AtomicInteger(1));
            }
        }
    }

    @Override
    public void destory() {
        timer.cancel();
    }

    private String generateKey(SoaHeader header) {
        return header.getServiceName() + ":" + header.getMethodName() + ":" + header.getVersionName();
    }
}
