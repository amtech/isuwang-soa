package com.isuwang.dapeng.api;

import com.isuwang.org.apache.thrift.TException;

/**
 * Created by lihuimin on 2017/12/11.
 */
public class SharedChain implements FilterChain {

    private Filter head;
    private Filter[]shared; // log->a->b->c
    private Filter tail;
    private int index;  // 0 -> n+2

    public Filter getHead() {
        return head;
    }

    public Filter[] getShared() {
        return shared;
    }

    public Filter getTail() {
        return tail;
    }

    public void setHead(Filter head) {
        this.head = head;
    }

    public void setShared(Filter[] shared) {
        this.shared = shared;
    }

    public void setTail(Filter tail) {
        this.tail = tail;
    }

    public int getCurrentIndex(){
        return index;
    }

    public SharedChain(Filter head, Filter[] shared, Filter tail, int index){
        if(index >= 2 + shared.length)
            throw new IllegalArgumentException();
        this.head = head;
        this.shared = shared;
        this.tail = tail;
        this.index = index;

    }

    @Override
    public void onEntry(FilterContext ctx) throws TException {
        SharedChain next = null;
        if(index  < 1 + shared.length)
                next = new SharedChain(head, shared, tail, index+1);
        else next = null;

        if(index == 0) {
            head.onEntry(ctx, next);
        }
        else if(index > 0 && index < shared.length + 1) {
            shared[index-1].onEntry(ctx, next);
        }
        else if(index == shared.length+1) {
            tail.onEntry(ctx, next);
        }
    }

    @Override
    public void onExit(FilterContext ctx) throws TException{
        SharedChain prev = null;
        if(index >= 1)
            prev = new SharedChain(head, shared, tail, index - 1);
        else prev = null;

        if(index == 0) {
            head.onExit(ctx, null);
        }
        else if(index > 0 && index < shared.length + 1) {
            shared[index-1].onExit(ctx, prev);
        }
        else if(index == shared.length+1) {
            tail.onEntry(ctx, prev);
        }
    }
}
