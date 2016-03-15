package uk.ac.kent;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Analyse a corpus to extract candidate grammar rules
 */
public class CorpusAnalyser {
    public static void main(String[] args) {
        String inputPath = "build/classes/main/sherlock.txt";
        String POSPatternsOutputPath = "POSTaggedSherlock";
        String sortedPOSPatternsOutputPath = POSPatternsOutputPath + "-sorted";
        String bigramOutputPath = "sherlockBigrams";


        writePOSTagsToFile(inputPath, POSPatternsOutputPath);
        LinkedList<String> sortedPOSPatterns = getSortedLinesFromFile(POSPatternsOutputPath);
        writeLineCollectionToFile(sortedPOSPatterns, sortedPOSPatternsOutputPath);
        LinkedList<Bigram> bigrams = getPOSBigramsFromPOSPatternsFile(POSPatternsOutputPath);
        writePOSBigramsToFile(bigrams, bigramOutputPath);
    }

    public static void writePOSTagsToFile(String inputPath, String outputPath) {

        try {
            PrintWriter output = IOUtils.getPrintWriter(outputPath);
            output.print(getPOSTagsForFile(inputPath));
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getPOSTagsForFile(String inputPath) {
        Annotation annotation = new Annotation(IOUtils.slurpFileNoExceptions(inputPath));
        StanfordCoreNLP pipeline = getPOSTaggingPipeline();
        pipeline.annotate(annotation);
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        StringBuilder stringBuilder = new StringBuilder();
        for (CoreMap sentence : sentences) {
            stringBuilder.append(getPOSTagsForSentence(sentence)).append("\n");
        }
        return stringBuilder.toString();
    }

    public static StanfordCoreNLP getPOSTaggingPipeline() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos");
        return new StanfordCoreNLP(props);
    }

    public static String getPOSTagsForSentence(CoreMap sentence) {
        StringBuilder stringBuilder = new StringBuilder();
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        for (CoreLabel token : tokens) {
            stringBuilder.append(token.tag()).append(" ");
        }
        return stringBuilder.toString();
    }

    public static LinkedList<String> getSortedLinesFromFile(String inputPath) {
        LinkedList<String> lines = new LinkedList<>();
        try {
            BufferedReader bufferedReader = IOUtils.readerFromString(inputPath);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lines.push(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        lines.sort(null);
        return lines;
    }

    public static void writeLineCollectionToFile(Collection<String> lines, String outputPath) {
        try {
            PrintWriter writer = IOUtils.getPrintWriter(outputPath);
            writer.print(String.join("\n", lines));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static LinkedList<Bigram> getPOSBigramsFromPOSPatternsFile(String inputPath) {
        String corpus = IOUtils.slurpFileNoExceptions(inputPath);
        LinkedList<Bigram> bigrams = getPOSBigrams(corpus);
        Collections.sort(bigrams,Collections.reverseOrder());
        return bigrams;
    }

    public static void writePOSBigramsToFile(LinkedList<Bigram> bigrams, String outputPath) {
        LinkedList<String> lines = new LinkedList<>();
        for (Bigram bigram : bigrams) {
            lines.push(bigram.getFirst() + " " + bigram.getSecond() + " " + bigram.getFrequency());
        }
        writeLineCollectionToFile(lines, outputPath);
    }

    public static LinkedList<Bigram> getPOSBigrams(String corpus) {
        HashMap<String, Bigram> bigrams = getStringBigramHashMap(corpus);
        return  new LinkedList<>(bigrams.values());
    }

    public static HashMap<String, Bigram> getStringBigramHashMap(String corpus) {
        HashMap<String, Bigram> bigrams = new HashMap<>();
        String[] posTags = corpus.split(" |\n");
        for (int i = 0; i < posTags.length-1; i++) {
            String firstTag = posTags[i];
            String secondTag = posTags[i + 1];
            String hashKey = firstTag + secondTag;
            if (bigrams.containsKey(hashKey)) {
                bigrams.get(hashKey).incrementfrequency();
            } else {
                bigrams.put(hashKey, new Bigram(firstTag, secondTag));
            }
        }
        return bigrams;
    }
}
