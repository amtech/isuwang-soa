//package com.isuwang.dapeng.core.filter.container;
//
//import com.isuwang.dapeng.core.filter.FilterChain;
//import com.isuwang.org.apache.thrift.TException;
//
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//
///**
// * Container Filter Chain
// *
// * @author craneding
// * @date 16/1/21
// */
//public class ContainerFilterChain implements FilterChain {
//
//    protected static final List<Filter> filters = new LinkedList<>();
//    protected final Map<String, Object> attributes = new HashMap<>();
//    protected int index = -1;
//    protected Filter lastFilter;
//
//    public static final String ATTR_KEY_CONTEXT = "context";
//    public static final String ATTR_KEY_HEADER = "header";
//    public static final String ATTR_KEY_LOGID = "logid";
//    public static final String ATTR_KEY_I_PROCESSTIME = "iTime";
//    public static final String ATTR_KEY_P_PROCESSTIME = "pTime";
//    public static final String ATTR_KEY_IFACE = "iface";
//    public static final String ATTR_KEY_FUTURE = "future";
//
//    @Override
//    public void doFilter() throws TException {
//        next().doFilter(this);
//    }
//
//    public Filter next() {
//        Filter filter = nextFilter();
//        return filter == null ? lastFilter : filter;
//    }
//
//    protected Filter nextFilter() {
//        if (filters.size() <= ++index)
//            return null;
//        else {
//            Filter filter = null;
//            try {
//                filter = filters.get(index);
//            } catch (IndexOutOfBoundsException e) {
//            }
//
//            return filter;
//        }
//    }
//
//    @Override
//    public Object getAttribute(String name) {
//        synchronized (attributes) {
//            return attributes.get(name);
//        }
//    }
//
//    @Override
//    public void setAttribute(String name, Object value) {
//        synchronized (attributes) {
//            attributes.put(name, value);
//        }
//    }
//
//    public void setLastFilter(Filter lastFilter) {
//        this.lastFilter = lastFilter;
//    }
//
//    public static void addFilter(Filter filter) {
//        synchronized (filters) {
//            filters.add(filter);
//        }
//    }
//
//    public static boolean removeFilter(Filter filter) {
//        synchronized (filters) {
//            return filters.remove(filter);
//        }
//    }
//
//    public static void clearFilter() {
//        synchronized (filters) {
//            filters.clear();
//        }
//    }
//
//}
