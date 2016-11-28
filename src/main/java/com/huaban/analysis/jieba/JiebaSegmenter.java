package com.huaban.analysis.jieba;

import com.huaban.analysis.jieba.viterbi.FinalSeg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class JiebaSegmenter {

    private static WordDictionary WORD_DICT = WordDictionary.getInstance();
    private static FinalSeg VITERBI_SEGMENT = FinalSeg.getInstance();

    public enum SegMode {
        INDEX,
        SEARCH
    }

    private Map<Integer, List<Integer>> createDAG(String sentence) {
        Map<Integer, List<Integer>> dag = new HashMap<>();
        DictSegment trie = WORD_DICT.getTrie();
        char[] chars = sentence.toCharArray();
        int N = chars.length;
        int i = 0, j = 0;
        while (i < N) {
            Hit hit = trie.match(chars, i, j - i + 1);
            if (hit.isPrefix() || hit.isMatch()) {
                if (hit.isMatch()) {
                    if (!dag.containsKey(i)) {
                        List<Integer> value = new ArrayList<>();
                        dag.put(i, value);
                        value.add(j);
                    } else {
                        dag.get(i).add(j);
                    }
                }
                j += 1;
                if (j >= N) {
                    i += 1;
                    j = i;
                }
            } else {
                i += 1;
                j = i;
            }
        }
        for (i = 0; i < N; ++i) {
            if (!dag.containsKey(i)) {
                List<Integer> value = new ArrayList<>();
                value.add(i);
                dag.put(i, value);
            }
        }
        return dag;
    }


    private Map<Integer, Pair<Integer>> calc(String sentence, Map<Integer, List<Integer>> dag) {
        int N = sentence.length();
        HashMap<Integer, Pair<Integer>> route = new HashMap<Integer, Pair<Integer>>();
        route.put(N, new Pair<>(0, 0.0));
        for (int i = N - 1; i > -1; i--) {
            Pair<Integer> candidate = null;
            for (Integer x : dag.get(i)) {
                double freq = WORD_DICT.getFreq(sentence.substring(i, x + 1)) + route.get(x + 1).freq;
                if (null == candidate) {
                    candidate = new Pair<>(x, freq);
                } else if (candidate.freq < freq) {
                    candidate.freq = freq;
                    candidate.key = x;
                }
            }
            route.put(i, candidate);
        }
        return route;
    }


    public List<SegToken> process(String paragraph, SegMode mode) {
        List<SegToken> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int offset = 0;
        for (int i = 0; i < paragraph.length(); ++i) {
            char ch = CharacterUtil.normalize(paragraph.charAt(i));
            if (CharacterUtil.ccFind(ch)) {
                sb.append(ch);
            } else {
                if (sb.length() > 0) {
                    // process
                    if (mode == SegMode.SEARCH) {
                        for (String word : sentenceProcess(sb.toString())) {
                            tokens.add(new SegToken(word, offset, offset += word.length()));
                        }
                    } else {
                        for (String token : sentenceProcess(sb.toString())) {
                            if (token.length() > 2) {
                                String gram2;
                                int j = 0;
                                for (; j < token.length() - 1; ++j) {
                                    gram2 = token.substring(j, j + 2);
                                    if (WORD_DICT.containsWord(gram2))
                                        tokens.add(new SegToken(gram2, offset + j, offset + j + 2));
                                }
                            }
                            if (token.length() > 3) {
                                String gram3;
                                int j = 0;
                                for (; j < token.length() - 2; ++j) {
                                    gram3 = token.substring(j, j + 3);
                                    if (WORD_DICT.containsWord(gram3))
                                        tokens.add(new SegToken(gram3, offset + j, offset + j + 3));
                                }
                            }
                            tokens.add(new SegToken(token, offset, offset += token.length()));
                        }
                    }
                    sb = new StringBuilder();
                    offset = i;
                }
                if (WORD_DICT.containsWord(paragraph.substring(i, i + 1)))
                    tokens.add(new SegToken(paragraph.substring(i, i + 1), offset, ++offset));
                else
                    tokens.add(new SegToken(paragraph.substring(i, i + 1), offset, ++offset));
            }
        }
        if (sb.length() > 0)
            if (mode == SegMode.SEARCH) {
                for (String token : sentenceProcess(sb.toString())) {
                    tokens.add(new SegToken(token, offset, offset += token.length()));
                }
            } else {
                for (String token : sentenceProcess(sb.toString())) {
                    if (token.length() > 2) {
                        String gram2;
                        int j = 0;
                        for (; j < token.length() - 1; ++j) {
                            gram2 = token.substring(j, j + 2);
                            if (WORD_DICT.containsWord(gram2))
                                tokens.add(new SegToken(gram2, offset + j, offset + j + 2));
                        }
                    }
                    if (token.length() > 3) {
                        String gram3;
                        int j = 0;
                        for (; j < token.length() - 2; ++j) {
                            gram3 = token.substring(j, j + 3);
                            if (WORD_DICT.containsWord(gram3))
                                tokens.add(new SegToken(gram3, offset + j, offset + j + 3));
                        }
                    }
                    tokens.add(new SegToken(token, offset, offset += token.length()));
                }
            }

        return tokens;
    }


    /*
     * 
     */
    public List<String> sentenceProcess(String sentence) {
        final int N = sentence.length();
        List<String> tokens = new ArrayList<>(N);
        Map<Integer, List<Integer>> dag = createDAG(sentence);
        Map<Integer, Pair<Integer>> route = calc(sentence, dag);

        int x = 0;
        int y = 0;
        String buf;
        StringBuilder sb = new StringBuilder();
        while (x < N) {
            y = route.get(x).key + 1;
            String lWord = sentence.substring(x, y);
            if (y - x == 1) {
                sb.append(lWord);
            } else {
                if (sb.length() > 0) {
                    buf = sb.toString();
                    sb = new StringBuilder();
                    if (buf.length() == 1) {
                        tokens.add(buf);
                    } else {
                        if (WORD_DICT.containsWord(buf)) {
                            tokens.add(buf);
                        } else {
                            VITERBI_SEGMENT.cut(buf, tokens);
                        }
                    }
                }
                tokens.add(lWord);
            }
            x = y;
        }
        buf = sb.toString();
        if (buf.length() > 0) {
            if (buf.length() == 1) {
                tokens.add(buf);
            } else {
                if (WORD_DICT.containsWord(buf)) {
                    tokens.add(buf);
                } else {
                    VITERBI_SEGMENT.cut(buf, tokens);
                }
            }

        }
        return tokens;
    }
}
