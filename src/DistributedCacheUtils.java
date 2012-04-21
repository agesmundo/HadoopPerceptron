import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;



public class DistributedCacheUtils {

	static int loadParametersFolder(String paramFolder, JobConf conf){
		try {
			FileSystem fs = FileSystem.get(conf);
			Path paramFolderPath = new Path(paramFolder);
			if (!fs.exists(paramFolderPath)) throw new FileNotFoundException("Parameters folder does not exists: "+paramFolder);
			FileStatus[] status = fs.listStatus(paramFolderPath);
			if (status.length<=0) throw new IOException("Parameters folder is empty: "+paramFolder);
			int count=0;
			for (int j = 0; j < status.length; j++) {
				if (!skip(status[j])) {
					DistributedCache.addCacheFile(status[j].getPath().toUri(), conf);
					count++;
				}
			}
			if (count==0) throw new IOException("Parameters folder does not contain parameter files: "+paramFolder);
		} catch (Exception e) {
			System.err.println("\nError:\n"+e.getMessage()+"\n");
			return 1;
		}
		return 0;
	}
	

	/**
	 * this test if the file name does not start with the char '_'.
	 * is used to skip the _SUCCESS and _log files that are saved in the param folders by the Train.
	 * @param status	FileStatus representing the file in the dfs.
	 * @return	true if the file name does not start with the char '_'.
	 */
	public static boolean skip(FileStatus status) {
		String tokens[] = status.getPath().toString().split("/");
		return tokens[tokens.length - 1].startsWith("_");
	}


}
