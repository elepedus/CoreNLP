package uk.ac.kent;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

/**
 * Analyse a corpus to extract candidate grammar rules
 */
public class CorpusAnalyser {
    public static void main(String[] args) {
        savePOSTagsToFile("build/classes/main/sherlock.txt", "POSTaggedSherlock");
    }

    public static void savePOSTagsToFile(String inputPath, String outputPath) {

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

}
