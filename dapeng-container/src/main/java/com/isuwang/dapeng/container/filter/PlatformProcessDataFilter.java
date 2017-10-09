package com.isuwang.dapeng.container.filter;

import com.isuwang.dapeng.container.util.PlatformProcessDataFactory;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.core.TransactionContext;
import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.dapeng.core.filter.container.ContainerFilterChain;
import com.isuwang.dapeng.monitor.api.MonitorServiceClient;
import com.isuwang.dapeng.monitor.api.domain.PlatformProcessData;
import com.isuwang.org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by tangliu on 2016/3/9.
 */
public class PlatformProcessDataFilter implements StatusFilter ,ContainerFilter{

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformProcessDataFilter.class);

    private static final long period = 1 * 60 * 1000L;// 1分钟间隔

    private final Timer timer = new Timer("PlatformProcessDataFilter-Timer");

    @Override
    public void init() {
        ContainerSoaHeader.setup();

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        LOGGER.info("PlatformProcessDataFilter 定时时间:{} 上送间隔:{}ms", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(calendar.getTime()), period);

        timer.schedule(new MyTimerTask(), calendar.getTime(), period);
    }

    @Override
    public void doFilter(FilterChain chain) throws TException {
        final SoaHeader soaHeader = TransactionContext.Factory.getCurrentInstance().getHeader();

        try {
            chain.doFilter();
        } finally {
            PlatformProcessDataFactory.update(soaHeader, cacheProcessData -> {
                final Long totalTime = (Long) chain.getAttribute(ContainerFilterChain.ATTR_KEY_I_PROCESSTIME);
                cacheProcessData.setITotalTime(cacheProcessData.getITotalTime() + totalTime);

                if (cacheProcessData.getIMinTime() == 0 || totalTime < cacheProcessData.getIMinTime())
                    cacheProcessData.setIMinTime(totalTime);
                if (cacheProcessData.getIMaxTime() == 0 || totalTime > cacheProcessData.getIMaxTime())
                    cacheProcessData.setIMaxTime(totalTime);
            });
        }
    }

    @Override
    public void destory() {
        timer.cancel();

        PlatformProcessDataFactory.clearDataMap();
    }

    private class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            try {
                final Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                final long timeMillis = calendar.getTimeInMillis();

                final List<PlatformProcessData> dataList = new ArrayList<>();

                Map<String, PlatformProcessData> dataMap = PlatformProcessDataFactory.getDataMap();

                synchronized (dataMap) {
                    final Set<String> keySet = dataMap.keySet();

                    dataList.addAll(keySet.stream().map(key -> dataMap.get(key)).collect(Collectors.toList()));

                    dataMap.clear();
                }

                for (PlatformProcessData data : dataList) {
                    data.setPeriod((int) (period / 60 / 1000));
                    data.setAnalysisTime(timeMillis);
                    data.setIAverageTime(data.getITotalTime() / data.getTotalCalls());
                    data.setPAverageTime(data.getPTotalTime() / data.getTotalCalls());
                }

                new MonitorServiceClient().uploadPlatformProcessData(dataList);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
