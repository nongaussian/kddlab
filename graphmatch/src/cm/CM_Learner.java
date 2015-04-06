package cm;

import java.io.IOException;

import net.sourceforge.argparse4j.inf.Namespace;

public abstract class CM_Learner {
	protected cm_data dat						= null;

	
	public void set_dat (cm_data dat) {
		this.dat = dat;
	}
	
	protected void setArgs(Namespace nes){
		
	}
	
	public abstract void run() throws IOException;
}

