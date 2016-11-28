package com.huaban.analysis.jieba;

public class SegToken {

    private final String word;
    private final int startOffset;
    private final int endOffset;

    public SegToken(final String word,
                    final int startOffset,
                    final int endOffset) {
        this.word = word;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public String getWord() {
        return word;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    @Override
    public String toString() {
        return "[" + word + ", " + startOffset + ", " + endOffset + "]";
    }

}
