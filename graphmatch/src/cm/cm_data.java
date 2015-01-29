package cm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class cm_data {

	public void read_data_file(String filename) {
		/* read sequences */
		try {
			int cnt = 0;
			String line;
			@SuppressWarnings("resource")
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			// XXX: txt message may be empty 
			while((line = reader.readLine()) != null) {
				cnt++;

				// System.out.println(line);
				String[] arr = line.trim().split("\t", 6);
				if (arr.length != 6) {
					System.err.println ("[warn] arr.length != 6: " + line);
					//System.exit(1);
				}

				int sid = Integer.parseInt(arr[0]);
				int len = Integer.parseInt(arr[1]);
				int idx = Integer.parseInt(arr[2]);
				double lon = Double.parseDouble(arr[3]);
				double lat = Double.parseDouble(arr[4]);
			}

			System.out.println("" + cnt + " tweets");
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
