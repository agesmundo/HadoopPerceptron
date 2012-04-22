
public class PerceptronStandard extends Perceptron{
	
	void update(Candidate promote, Candidate penalize){
		updateFeat(promote.features, +1);
		updateFeat(penalize.features, -1);
	}

}
