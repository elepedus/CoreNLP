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
import java.nio.Buffer;
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
        HashMap<String, Integer> bigrams = getPOSBigramsFromFile(POSPatternsOutputPath);
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

    public static void writePOSBigramsToFile(HashMap<String, Integer> bigrams, String outputPath) {
        LinkedList<String> lines = new LinkedList<>();
        for (String bigram : bigrams.keySet()) {
            lines.push(bigrams.get(bigram) + " " + bigram);
        }
        lines.sort(null);
        writeLineCollectionToFile(lines, outputPath);
    }

    public static HashMap<String, Integer> getPOSBigramsFromFile(String inputPath) {
        String corpus = IOUtils.slurpFileNoExceptions(inputPath);
        HashMap<String, Integer> bigrams = getPOSBigrams(corpus);
        return bigrams;
    }

    public static HashMap<String, Integer> getPOSBigrams(String corpus) {
        HashMap<String, Integer> bigrams = new HashMap<>();
        LinkedList<String> tags = new LinkedList<>(Arrays.asList(corpus.split(" |\n")));
        while (tags.size() > 1) {
            String currentTag = tags.pop();
            String nextTag = tags.peek();
            String bigram = currentTag + " " + nextTag;
            if (bigrams.containsKey(bigram)) {
                bigrams.replace(bigram, bigrams.get(bigram) + 1);
            } else {
                bigrams.put(bigram, 1);
            }
        }
        return bigrams;
    }
}
