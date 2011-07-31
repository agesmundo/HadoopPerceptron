import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Sentence {
	static public String defaultLabelSeparator = "_";

	List<Word> words = new ArrayList<Word>();

	public Sentence(String line) {
		StringTokenizer tokenizer = new StringTokenizer(line);
		while (tokenizer.hasMoreTokens()) {
			words.add(new Word(tokenizer.nextToken()));
		}
	}

	static class Word {
		String word;
		List<String> labels;

		Word(String w) {
			String[] tokens = w.split(defaultLabelSeparator);
			word = tokens[0];
			labels = new ArrayList<String>(tokens.length - 1);
			for (int i = 1; i < tokens.length; i++) {
				labels.add(tokens[i]);
			}
			// NB need to register label if missing
			if (labels.size() != 0)
				LabelLib.storeLabel(getGoldLabel());
		}

		String getGoldLabel() {
			if (labels.size() == 0)
				return null;
			return labels.get(getGoldLabelId());
		}

		int getGoldLabelId() {
			return labels.size() - 1;
		}
	}

	public int size() {
		return words.size();
	}

	public String getWord(int i) {
		if (i < words.size() && i >= 0)
			return words.get(i).word;
		return "";
	}

	public String getGoldLabel(int i) {
		if (i < words.size() && i >= 0)
			return words.get(i).getGoldLabel();
		return "";
	}

}
