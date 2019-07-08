package com.xxl.job.core.enums;

public enum SubTaskEnum {


    SUSPEND("2","暂停"),
    INVALID("0","失效"),
    RUNNING("1","运行");

    private String code;
    private String desc;

    private SubTaskEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
