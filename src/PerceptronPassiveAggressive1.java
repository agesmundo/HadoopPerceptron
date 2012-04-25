
public class PerceptronPassiveAggressive1 extends Perceptron{
	
	float slack=1;//slak variable
	
	PerceptronPassiveAggressive1(float s){
		slack=s;
	}
	
	void update(Candidate promote, Candidate penalize){
		double margin = 1;//TODO move this
		double loss = penalize.score-promote.score +margin;
		double normSquare=promote.features.size()+penalize.features.size();//this holds for binary features
		double alpha = loss/normSquare;
		if(alpha>slack)alpha=slack;
		updateFeat(promote.features, +1*alpha);
		updateFeat(penalize.features, -1*alpha);

	}

}
