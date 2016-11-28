package com.huaban.analysis.jieba;

public class Pair<K> {
    
    public K key;
    public double freq;

    public Pair(final K key, final double freq) {
        this.key = key;
        this.freq = freq;
    }

    @Override
    public String toString() {
        return "Candidate [key=" + key + ", freq=" + freq + "]";
    }

}
