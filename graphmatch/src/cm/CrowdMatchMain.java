package cm;

import gm.data.DataLoader;

import java.util.Arrays;

public class CrowdMatchMain {
	public static void main(String[] args) {
		 String [] newargs = Arrays.copyOfRange(args, 1, args.length);
		
		 switch (args[0].toLowerCase()){
			 case "dataloader":
				 DataLoader.main(newargs);
				 break;
			 case "graphmatching":
				 CrowdMatch.main(newargs);
				 break;
//			 case "graphmatching_crowdsourcing":
//				 GraphMatchingCrowdsourcing.main(newargs);
//				 break;
		 }

	}

}
