package com.huaban.analysis.jieba;

import java.util.HashMap;
import java.util.Map;


/**
 * 词典树分段，表示词典树的一个分枝
 */
class DictSegment implements Comparable<DictSegment> {

    // 公用字典表，存储汉字
    private static final Map<Character, Character> charMap = new HashMap<Character, Character>(16, 0.95f);

    // Map存储结构
    private Map<Character, DictSegment> segments = new HashMap<>(4);

    // 当前节点上存储的字符
    private final char nodeChar;

    // 当前DictSegment状态 ,默认 0 , 1表示从根节点到当前节点的路径表示一个词
    private int nodeState = 0;

    DictSegment(final char nodeChar) {
        this.nodeChar = nodeChar;
    }

    protected Character getNodeChar() {
        return nodeChar;
    }

    /**
     * 匹配词段
     *
     * @param charArray
     * @return Hit
     */
    Hit match(char[] charArray) {
        return this.match(charArray, 0, charArray.length);
    }


    /**
     * 匹配词段
     *
     * @param charArray
     * @param begin
     * @param length
     * @return Hit
     */
    Hit match(char[] charArray, int begin, int length) {
        return this.match(charArray, begin, begin, length);
    }


    /**
     * 匹配词段
     *
     * @param charArray
     * @param begin
     * @param length
     * @param searchHit
     * @return Hit
     */
    Hit match(char[] charArray, int begin, int pos, int length) {

        if (length == 0) {
            final Hit possibleHit = new Hit(begin, pos);
            // STEP3 没有找到DictSegment， 将HIT设置为不匹配
            if (nodeState == 1) {
                possibleHit.setMatch();
            }

            return possibleHit;
        }

        final Character keyChar = charArray[pos];
        final DictSegment ds = segments.get(keyChar);

        // STEP2 找到DictSegment，判断词的匹配状态，是否继续递归，还是返回结果
        if (ds != null) {
            return ds.match(charArray, begin, pos + 1, length - 1);
        }

        return new Hit(begin, pos);
    }

    /**
     * 加载填充词典片段
     *
     * @param charArray
     */
    void fillSegment(char[] charArray) {
        this.fillSegment(charArray, 0, charArray.length, 1);
    }

    /**
     * 加载填充词典片段
     *
     * @param charArray
     * @param begin
     * @param length
     * @param enabled
     */
    private void fillSegment(char[] charArray, int begin, int length, int enabled) {
        // 获取字典表中的汉字对象
        Character beginChar = charArray[begin];
        Character keyChar = charMap.get(beginChar);
        // 字典中没有该字，则将其添加入字典
        if (keyChar == null) {
            charMap.put(beginChar, beginChar);
            keyChar = beginChar;
        }

        // 搜索当前节点的存储，查询对应keyChar的keyChar，如果没有则创建
        DictSegment ds = lookForSegment(keyChar, enabled);
        if (ds != null) {
            // 处理keyChar对应的segment
            if (length > 1) {
                // 词元还没有完全加入词典树
                ds.fillSegment(charArray, begin + 1, length - 1, enabled);
            } else if (length == 1) {
                // 已经是词元的最后一个char,设置当前节点状态为enabled，
                // enabled=1表明一个完整的词，enabled=0表示从词典中屏蔽当前词
                ds.nodeState = enabled;
            }
        }
    }


    /**
     * 查找本节点下对应的keyChar的segment *
     *
     * @param keyChar
     * @param create  =1如果没有找到，则创建新的segment ; =0如果没有找到，不创建，返回null
     * @return
     */
    private DictSegment lookForSegment(final Character keyChar, int create) {
        DictSegment ds = segments.get(keyChar);
        if (ds == null && create == 1) {
            // 构造新的segment
            ds = new DictSegment(keyChar);
            segments.put(keyChar, ds);
        }

        return ds;
    }

    /**
     * 实现Comparable接口
     *
     * @param o
     * @return int
     */
    public int compareTo(final DictSegment o) {
        // 对当前节点存储的char进行比较
        return this.nodeChar - o.nodeChar;
    }
}