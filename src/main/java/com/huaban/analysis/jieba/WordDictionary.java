package com.huaban.analysis.jieba;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WordDictionary {

    private static final Logger LOGGER = Logger.getLogger(WordDictionary.class.getName());

    private static WordDictionary INSTANCE = new WordDictionary();
    private static final String MAIN_DICT = "/dict.txt";
    private static String USER_DICT_SUFFIX = ".dict";

    public final Map<String, Double> freqs = new HashMap<>();
    private Double minFreq = Double.MAX_VALUE;
    private Double total = 0.0;
    private DictSegment _dict;

    private WordDictionary() {
        this.loadDict();
    }

    public static WordDictionary getInstance() {
        return INSTANCE;
    }


    /**
     * for ES to initialize the user dictionary.
     *
     * @param configFile
     */
    public void init(final Path configFile) {
        final String configPath = configFile.toAbsolutePath().toString();
        final Set<String> loadedPath = new HashSet<>();
        LOGGER.info("initialize user dictionary:" + configPath);
        synchronized (WordDictionary.class) {
            if (loadedPath.contains(configPath)) {
                return;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(configFile, String.format(Locale.getDefault(), "*%s", USER_DICT_SUFFIX))) {
                for (Path path : stream) {
                    LOGGER.info(String.format(Locale.getDefault(), "loading dict %s", path.toString()));
                    INSTANCE.loadUserDict(path);
                }
                loadedPath.add(configPath);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, String.format(Locale.getDefault(), "%s: load user dict failure!", configFile.toString()));
            }
        }
    }

    /**
     * let user just use their own dict instead of the default dict
     */
    public void resetDict() {
        _dict = new DictSegment((char) 0);
        freqs.clear();
    }

    protected void loadDict() {
        _dict = new DictSegment((char) 0);

        final long s = System.currentTimeMillis();
        try (InputStream is = this.getClass().getResourceAsStream(MAIN_DICT)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");

                if (tokens.length < 2) {
                    continue;
                }

                String word = tokens[0];
                double freq = Double.valueOf(tokens[1]);
                total += freq;
                word = addWord(word);
                freqs.put(word, freq);
            }
            // normalize
            for (Entry<String, Double> entry : freqs.entrySet()) {
                entry.setValue((Math.log(entry.getValue() / total)));
                minFreq = Math.min(entry.getValue(), minFreq);
            }

            LOGGER.info(String.format(Locale.getDefault(), "main dict load finished, time elapsed %d ms", System.currentTimeMillis() - s));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format(Locale.getDefault(), "%s load failure!", MAIN_DICT));
        }
    }


    private String addWord(String word) {
        if (null != word && !word.trim().isEmpty()) {
            String key = word.trim().toLowerCase(Locale.getDefault());
            _dict.fillSegment(key.toCharArray());
            return key;
        }

        return null;
    }

    protected void loadUserDict(Path userDict) {
        loadUserDict(userDict, StandardCharsets.UTF_8);
    }

    protected void loadUserDict(Path userDict, Charset charset) {
        try (BufferedReader br = Files.newBufferedReader(userDict, charset)) {
            long s = System.currentTimeMillis();
            int count = 0;
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");

                if (tokens.length < 1) {
                    // Ignore empty line
                    continue;
                }

                final String word = addWord(tokens[0]);
                final double freq = tokens.length == 2 ? Double.parseDouble(tokens[1]) : 3.0d;
                freqs.put(word, Math.log(freq / total));
                count++;
            }
            LOGGER.info(String.format(Locale.getDefault(), "user dict %s load finished, tot words:%d, time elapsed:%dms", userDict.toString(), count, System.currentTimeMillis() - s));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format(Locale.getDefault(), "%s: load user dict failure!", userDict.toString()));
        }
    }

    public DictSegment getTrie() {
        return this._dict;
    }

    public boolean containsWord(String word) {
        return freqs.containsKey(word);
    }

    public Double getFreq(String key) {
        return containsWord(key) ? freqs.get(key) : minFreq;
    }
}
