package cm;

import java.io.IOException;

import net.sourceforge.argparse4j.inf.Namespace;

public abstract class CM_Learner {
	protected cm_data dat						= null;
	protected String lprefix					= null;
	protected String rprefix					= null;
	
	
	
	protected void setArgs(Namespace nes){
		lprefix						= nes.getString("lprefix");
		rprefix						= nes.getString("rprefix");
	}
	
	public abstract void init();
	public abstract void run() throws IOException;
}

