package uk.ac.kent;

/**
 * Utility class to encapsulate a bigram and associated data
 */
public class Bigram implements Comparable {
    private String first;
    private String second;
    private int frequency;

    /**
     * Initialise a bigram. Frequency defaults to 1.
     *
     * @param firstItem First item of the bigram
     * @param secondItem Second item of the bigram
     */
    public Bigram(String firstItem, String secondItem) {
        this(firstItem, secondItem, 1);
    }

    /**
     * Initialise a bigram specifying the frequency
     *
     * @param firstItem First item of the bigram
     * @param secondItem Second item of the bigram
     * @param frequency The observed frequency of the bigram
     */
    public Bigram(String firstItem, String secondItem, int frequency) {
        this.first = firstItem;
        this.second = secondItem;
        this.frequency = frequency;
    }

    public void incrementfrequency() {
        frequency++;
    }

    public String getFirst() {
        return first;
    }

    public String getSecond() {
        return second;
    }

    public int getFrequency() {
        return frequency;
    }

    @Override
    public int compareTo(Object o) {
        Bigram bigram = (Bigram) o;
        if (bigram.getFrequency() < this.getFrequency()) {
            return -1;
        }
        if (bigram.getFrequency() > this.getFrequency()) {
            return 1;
        }
        return 0;
    }
}
