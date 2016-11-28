package com.huaban.analysis.jieba.viterbi;

import com.huaban.analysis.jieba.CharacterUtil;
import com.huaban.analysis.jieba.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class FinalSeg {

    private static final Logger LOGGER = Logger.getLogger(FinalSeg.class.getName());

    private static FinalSeg INSTANCE = new FinalSeg();
    private static final String PROB_EMIT = "/prob_emit.txt";
    private static char[] STATES = new char[]{'B', 'M', 'E', 'S'};
    private static Double MIN_FLOAT = -3.14e100;

    private Map<Character, Map<Character, Double>> emit;
    private Map<Character, Double> starts;
    private Map<Character, Map<Character, Double>> trans;
    private Map<Character, char[]> prevStates;

    private FinalSeg() {
        loadModel();
    }

    public static FinalSeg getInstance() {
        return INSTANCE;
    }

    private void loadModel() {

        final long s = System.currentTimeMillis();

        prevStates = new HashMap<>();
        prevStates.put('B', new char[]{'E', 'S'});
        prevStates.put('M', new char[]{'M', 'B'});
        prevStates.put('S', new char[]{'S', 'E'});
        prevStates.put('E', new char[]{'B', 'M'});

        starts = new HashMap<>(4);
        starts.put('B', -0.26268660809250016);
        starts.put('E', -3.14e+100);
        starts.put('M', -3.14e+100);
        starts.put('S', -1.4652633398537678);

        trans = new HashMap<>(4);
        final Map<Character, Double> transB = new HashMap<>(2);
        transB.put('E', -0.510825623765990);
        transB.put('M', -0.916290731874155);
        trans.put('B', transB);
        final Map<Character, Double> transE = new HashMap<>(2);
        transE.put('B', -0.5897149736854513);
        transE.put('S', -0.8085250474669937);
        trans.put('E', transE);
        final Map<Character, Double> transM = new HashMap<>(2);
        transM.put('E', -0.33344856811948514);
        transM.put('M', -1.2603623820268226);
        trans.put('M', transM);
        final Map<Character, Double> transS = new HashMap<>(2);
        transS.put('B', -0.7211965654669841);
        transS.put('S', -0.6658631448798212);
        trans.put('S', transS);

        try (InputStream is = this.getClass().getResourceAsStream(PROB_EMIT)) {
            final BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            emit = new HashMap<>();
            Map<Character, Double> values = new HashMap<>();
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("\t");
                if (tokens.length == 1) {
                    values = new HashMap<>();
                    emit.put(tokens[0].charAt(0), values);
                } else {
                    values.put(tokens[0].charAt(0), Double.valueOf(tokens[1]));
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format(Locale.getDefault(), "%s: load model failure!", PROB_EMIT));
        }

        LOGGER.info(String.format(Locale.getDefault(), "model load finished, time elapsed %d ms.", System.currentTimeMillis() - s));
    }

    public void cut(final String sentence, final List<String> cuts) {
        StringBuilder chinese = new StringBuilder();
        StringBuilder unknown = new StringBuilder();
        for (int i = 0, len = sentence.length(); i < len; i += 1) {
            final char ch = sentence.charAt(i);
            if (CharacterUtil.isChineseLetter(ch)) {
                if (unknown.length() > 0) {
                    processOtherUnknownWords(unknown, cuts);
                    unknown = new StringBuilder();
                }
                chinese.append(ch);
            } else {
                if (chinese.length() > 0) {
                    viterbi(chinese, cuts);
                    chinese = new StringBuilder();
                }
                unknown.append(ch);
            }

        }
        if (chinese.length() > 0)
            viterbi(chinese.toString(), cuts);
        else {
            processOtherUnknownWords(unknown, cuts);
        }
    }


    public void viterbi(final CharSequence sentence, final List<String> cuts) {

        final int length = sentence.length();
        final List<Map<Character, Double>> v = new LinkedList<>();

        Map<Character, Node> path = new HashMap<>();
        v.add(new HashMap<>());
        for (char state : STATES) {
            Double emP = emit.get(state).getOrDefault(sentence.charAt(0), MIN_FLOAT);
            v.get(0).put(state, starts.get(state) + emP);
            path.put(state, new Node(state, null));
        }

        for (int i = 1; i < length; ++i) {
            final Map<Character, Double> vv = new HashMap<>();
            v.add(vv);
            Map<Character, Node> newPath = new HashMap<>();
            for (char y : STATES) {
                Double emp = emit.get(y).getOrDefault(sentence.charAt(i), MIN_FLOAT);
                Pair<Character> candidate = null;
                for (char y0 : prevStates.get(y)) {
                    Double tranp = trans.get(y0).getOrDefault(y, MIN_FLOAT);
                    tranp += (emp + v.get(i - 1).get(y0));
                    if (null == candidate) {
                        candidate = new Pair<>(y0, tranp);
                    } else if (candidate.freq <= tranp) {
                        candidate.freq = tranp;
                        candidate.key = y0;
                    }
                }
                vv.put(y, candidate.freq);
                newPath.put(y, new Node(y, path.get(candidate.key)));
            }
            path = newPath;
        }

        double probE = v.get(sentence.length() - 1).get('E');
        double probS = v.get(sentence.length() - 1).get('S');

        List<Character> posList = new ArrayList<>(length);
        Node win = probE < probS ? path.get('S') : path.get('E');
        while (win != null) {
            posList.add(win.value);
            win = win.parent;
        }
        Collections.reverse(posList);

        int begin = 0, next = 0;
        for (int i = 0, len = sentence.length(); i < len; i += 1) {
            char pos = posList.get(i);
            if (pos == 'B') {
                begin = i;
            } else if (pos == 'E') {
                cuts.add(sentence.subSequence(begin, i + 1).toString());
                next = i + 1;
            } else if (pos == 'S') {
                cuts.add(sentence.subSequence(i, i + 1).toString());
                next = i + 1;
            }
        }

        if (next < length) {
            cuts.add(sentence.subSequence(next, length).toString());
        }
    }

    private void processOtherUnknownWords(final CharSequence other, List<String> tokens) {
        final Matcher mat = CharacterUtil.reSkip.matcher(other);
        int offset = 0;
        while (mat.find()) {
            if (mat.start() > offset) {
                tokens.add(other.subSequence(offset, mat.start()).toString());
            }
            tokens.add(mat.group());
            offset = mat.end();
        }
        if (offset < other.length()) {
            tokens.add(other.subSequence(offset, other.length()).toString());
        }
    }

    protected static class Node {

        protected char value;
        protected Node parent;

        public Node(char value, Node parent) {
            this.value = value;
            this.parent = parent;
        }
    }
}
