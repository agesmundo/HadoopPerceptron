
public class PerceptronPassiveAggressive2 extends Perceptron{
	
	double slack=1;//slak variable
	
	PerceptronPassiveAggressive2(float s){
		slack=s;
	}
	
	void update(Candidate promote, Candidate penalize){
		double margin = 1;//TODO move this
		double loss = penalize.score-promote.score +margin;
		double normSquare=promote.features.size()+penalize.features.size();//this holds for binary features
		double alpha = loss/(normSquare+ 1/(2*slack));
		updateFeat(promote.features, +1*alpha);
		updateFeat(penalize.features, -1*alpha);

	}

}
