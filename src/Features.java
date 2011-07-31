import java.util.*;

public class Features {
	public static List<String> getFeatures(String previousWord,
			String currentWord, String nextWord, String previousPredictedLabel) {
		List<String> features = new LinkedList<String>();
		// features.add(currentWord);
		features.add("length_pre_" + String.valueOf(previousWord.length()));
		features.add("length_cur_" + String.valueOf(currentWord.length()));
		if (previousWord.length() > 0
				&& Character.isUpperCase(previousWord.charAt(0))) {
			features.add("uppercase_pre");
		}
		if (nextWord.length() > 0 && Character.isUpperCase(nextWord.charAt(0))) {
			features.add("uppercase_next");
		}
		// Prefix
		if (currentWord.length() > 2) {
			features.add("pre_" + currentWord.substring(0, 2).toLowerCase());
		} else {
			features.add("pre_" + currentWord.toLowerCase());
		}
		// Suffix
		if (currentWord.length() > 2) {
			features.add("suf_"
					+ currentWord.substring(currentWord.length() - 2,
							currentWord.length()).toLowerCase());
		} else {
			features.add("suf_" + currentWord.toLowerCase());
		}
		if (previousPredictedLabel.length() > 0) {
			features.add("previous_label=" + previousPredictedLabel);
		}
		return features;
	}

	public static String getLabel(String featString) {
		int pos = featString.indexOf(defaultFeatureSeparator);
		if (pos < 1)
			return null;
		return featString.substring(0, pos);
	}

	public static String defaultFeatureSeparator = "|";
}
