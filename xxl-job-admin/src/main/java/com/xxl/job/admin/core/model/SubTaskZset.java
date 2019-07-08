package com.xxl.job.admin.core.model;

import org.springframework.data.redis.core.ZSetOperations;

public class SubTaskZset implements ZSetOperations.TypedTuple<Object>{


    public SubTaskZset(String value, double score) {
        this.value = value;
        this.score = score;
    }

    private String value;
    private double score;

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public Double getScore() {
        return score;
    }

    @Override
    public int compareTo(ZSetOperations.TypedTuple<Object> o) {
        SubTaskZset o1 = (SubTaskZset) o;
        return new Double(this.score).compareTo(o1.getScore());
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
