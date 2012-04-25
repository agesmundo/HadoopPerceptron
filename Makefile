JAR=HP.jar
ITERS=2
PRE=/test
TRAIN_IN_DIR=${PRE}/train_folder
PREDICT_IN_DIR=${PRE}/test_folder
TRAIN_OUT_DIR=${PRE}/train_out
PREDICT_OUT_DIR=${PRE}/predict_out
EVAL_OUT_DIR=${PRE}/evaluate_out

#assign to HADOOP_CORE the path to the hadoop core jar, e.g.:
HADOOP_CORE=./jars/hadoop-core-1.0.1.jar

#comman line parser: http://commons.apache.org/cli
CLI=./jars/commons-cli-1.2.jar

CP=-cp ${HADOOP_CORE}:${CLI}

jr:jc
	jar -cf jars/${JAR} -C class/ .
jc:
	javac ${CP} -d class/ src/*.java
put:
	hadoop fs -put test/ /
train:jr clean_tr
	hadoop jar jars/${JAR} Train -i ${TRAIN_IN_DIR} -o ${TRAIN_OUT_DIR} -N ${ITERS} -P PA2
predict:jr clean_pr
	hadoop jar jars/${JAR} Predict -i ${PREDICT_IN_DIR} -o ${PREDICT_OUT_DIR} -p ${TRAIN_OUT_DIR}_${ITERS}
evaluate:jr clean_ev
	hadoop jar jars/${JAR} Evaluate -i ${TRAIN_IN_DIR} -o ${EVAL_OUT_DIR} -p ${TRAIN_OUT_DIR}_${ITERS} 
	hadoop fs -cat ${EVAL_OUT_DIR}/[^_]*
clean_tr:
	hadoop fs -rmr ${TRAIN_OUT_DIR}*
clean_pr:
	hadoop fs -rmr ${PREDICT_OUT_DIR}*
clean_ev:
	hadoop fs -rmr ${EVAL_OUT_DIR}*
all:train evaluate
