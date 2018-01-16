package com.isuwang.dapeng.core;

import com.isuwang.org.apache.thrift.TException;

/**
 * soa异常
 *
 * @author craneding
 * @date 15/9/10
 */
public class SoaException extends TException {

    private static final long serialVersionUID = -129682168859027730L;

    private String code;
    private String msg;

    public SoaException(TException err) {
        super(err);

        this.code = SoaCode.UnKnown.getCode();
        this.msg = err.getMessage();
    }

    public SoaException(SoaBaseCodeInterface soaCode) {
        this(soaCode.getCode(), soaCode.getMsg());
    }

    public SoaException(SoaBaseCodeInterface soaCode, String label) {
        this(soaCode.getCode(), label);
    }

    public SoaException(String code, String msg) {
        super(code + ":" + msg);

        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return code + ":" + msg;
    }

}
