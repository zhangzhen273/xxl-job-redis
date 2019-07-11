package com.xxl.job.admin.core.model;

import org.springframework.data.redis.core.ZSetOperations;

public class SubTaskZset implements ZSetOperations.TypedTuple<Object>{


    public SubTaskZset(Long value, double score) {
        this.value = value;
        this.score = score;
    }

    private Long value;
    private double score;


    @Override
    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
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

    public void setScore(double score) {
        this.score = score;
    }
}
