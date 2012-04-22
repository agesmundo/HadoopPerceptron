
public class Feature {

		/** Name of the feature */
		public String featstr;
		/** Weight of the feature */
		public double weight;

		public Feature(String str) {
			featstr = str;
			weight = 0;
		}

		public Feature(String str, double w) {
			featstr = str;
			weight = w;
		}

}
