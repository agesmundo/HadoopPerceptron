JAR=HP.jar
ITERS=4
TRAIN_IN_DIR=/in3
PREDICT_IN_DIR=/in2
TRAIN_OUT_DIR=/outT
PREDICT_OUT_DIR=/outP
EVAL_OUT_DIR=/outE
WEIGHT_IN=/init

#assign to HADOOP_CORE the path to the hadoop core jar, e.g.:
#HADOOP_CORE=-cp /path/to/hadoop-core-VERSION.jar
HADOOP_CORE=./jars/hadoop-core-1.0.1.jar

#comman line parser:commons-cli-1.2.jar
CLI=./jars/commons-cli-1.2.jar

CP=-cp ${HADOOP_CORE}:${CLI}

jr:jc
	jar -cf jars/${JAR} -C class/ .

jc:
	javac ${CP} -d class/ src/*.java

train:jr clean
	hadoop jar jars/${JAR} Train -i ${TRAIN_IN_DIR} -o ${TRAIN_OUT_DIR} -N 2 -M 4 -R 4
#-w ${WEIGHT_IN}

predict:jr
	hadoop jar jars/${JAR} Predict ${PREDICT_IN_DIR} ${PREDICT_OUT_DIR} ${TRAIN_OUT_DIR}_${ITERS} 

eval:jr
	hadoop jar jars/${JAR} Evaluate ${TRAIN_IN_DIR} ${EVAL_OUT_DIR} ${TRAIN_OUT_DIR}_${ITERS} 
	hadoop fs -cat ${EVAL_OUT_DIR}/*
clean:
	hadoop fs -rmr ${TRAIN_OUT_DIR}*
