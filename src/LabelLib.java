import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Library of labels
 * 
 * @author Andrea Gesmundo
 * 
 */
public class LabelLib {
	private static Map<String, Integer> label2id = new HashMap<String, Integer>();

	private static List<String> id2label = new ArrayList<String>();

	// ///////////////////////////////////////////////////////////////

	/**
	 * get the Label ID with a string new label would be inserted into LabelLib
	 */
	public static void storeLabel(String label) {
		if (label == null)
			return;

		Integer id = (Integer) label2id.get(label);
		if (id != null) {
			return;
		}

		id = new Integer(id2label.size());
		label2id.put(label, id);
		id2label.add(label);
		return;
	}

	public static List<String> getCandidateLabels() {
		return id2label;
	}

}
