package com.isuwang.dapeng.impl.filters;

import com.isuwang.dapeng.api.filters.FilterChain;
import com.isuwang.dapeng.api.filters.FilterContext;
import com.isuwang.dapeng.api.filters.HandlerFilter;
import com.isuwang.org.apache.thrift.TException;

/**
 * Created by lihuimin on 2017/12/11.
 */
public class SharedChain implements FilterChain {

    private HandlerFilter head;
    private HandlerFilter[]shared; // log->a->b->c
    private HandlerFilter tail;
    private int index;  // 0 -> n+2

    public HandlerFilter getHead() {
        return head;
    }

    public HandlerFilter[] getShared() {
        return shared;
    }

    public HandlerFilter getTail() {
        return tail;
    }

    public void setHead(HandlerFilter head) {
        this.head = head;
    }

    public void setShared(HandlerFilter[] shared) {
        this.shared = shared;
    }

    public void setTail(HandlerFilter tail) {
        this.tail = tail;
    }

    public int getCurrentIndex(){
        return index;
    }

    public SharedChain(HandlerFilter head, HandlerFilter[] shared, HandlerFilter tail, int index){
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
        if(index  <= 1 + shared.length)
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
        index++;
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
        index--;
    }
}
