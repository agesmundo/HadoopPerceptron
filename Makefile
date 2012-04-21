JAR=HP.jar
ITERS=1
TRAIN_IN_DIR=/in1
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

train:jr clean_tr
	hadoop jar jars/${JAR} Train -i ${TRAIN_IN_DIR} -o ${TRAIN_OUT_DIR} -N ${ITERS} -M 2 -R 1 -p /inParams

predict:jr clean_pr
	hadoop jar jars/${JAR} Predict ${PREDICT_IN_DIR} ${PREDICT_OUT_DIR} ${TRAIN_OUT_DIR}_${ITERS}

eval:jr
	hadoop jar jars/${JAR} Evaluate ${TRAIN_IN_DIR} ${EVAL_OUT_DIR} ${TRAIN_OUT_DIR}_${ITERS} 
	hadoop fs -cat ${EVAL_OUT_DIR}/*

clean_tr:
	hadoop fs -rmr ${TRAIN_OUT_DIR}*

clean_pr:
	hadoop fs -rmr ${PREDICT_OUT_DIR}*
