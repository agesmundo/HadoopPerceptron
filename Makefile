JAR=HP.jar
ITERS=4
TRAIN_IN_DIR=/in1
PREDICT_IN_DIR=/in2
TRAIN_OUT_DIR=/outT
PREDICT_OUT_DIR=/outP
EVAL_OUT_DIR=/outE
WEIGHT_IN=/init

jr:jc
	jar -cf jars/${JAR} -C class/ .

jc:
	javac -d class/ src/*.java

train:jr
	hadoop jar jars/${JAR} Train ${ITERS} ${TRAIN_IN_DIR} ${TRAIN_OUT_DIR} ${WEIGHT_IN}

predict:jr
	hadoop jar jars/${JAR} Predict ${PREDICT_IN_DIR} ${PREDICT_OUT_DIR} ${TRAIN_OUT_DIR}_${ITERS} 

eval:jr
	hadoop jar jars/${JAR} Evaluate ${TRAIN_IN_DIR} ${EVAL_OUT_DIR} ${TRAIN_OUT_DIR}_${ITERS} 
	hadoop fs -cat ${EVAL_OUT_DIR}/*

