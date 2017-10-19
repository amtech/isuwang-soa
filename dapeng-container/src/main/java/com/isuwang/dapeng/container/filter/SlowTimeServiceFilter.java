package com.isuwang.dapeng.container.filter;

import com.isuwang.dapeng.container.filter.slowtime.TaskManager;
import com.isuwang.dapeng.container.filter.slowtime.Task;
import com.isuwang.dapeng.core.TransactionContext;
import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.org.apache.thrift.TException;

/**
 * Created by tangliu on 2016/2/1.
 */
public class SlowTimeServiceFilter implements StatusFilter ,ContainerFilter{

    private final TaskManager taskManager = new TaskManager();

    @Override
    public void init() {
        if (!taskManager.hasStarted())
            taskManager.start();
    }

    @Override
    public void doFilter(FilterChain chain) throws TException {
        TransactionContext context = TransactionContext.Factory.getCurrentInstance();
        Task task = new Task(context);
        taskManager.addTask(task);

        try {
            chain.doFilter();
        } finally {
            taskManager.remove(task);
        }
    }

    @Override
    public void destory() {
        if (taskManager.hasStarted())
            taskManager.stop();
    }
}
