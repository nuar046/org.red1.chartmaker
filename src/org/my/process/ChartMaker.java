/**

import org.compiere.process.SvrProcess;
import org.compiere.util.DB;







		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
				else if(name.equals("ChartName")){
					ChartName = (String)p.getParameter();
			}
				else if(name.equals("Time")){
					Time = (String)p.getParameter();
			}
				else if(name.equals("AD_Table_ID")){
					AD_Table_ID = p.getParameterAsInt();
			}
				else if(name.equals("CategoryColumn")){
					CategoryColumn = (String)p.getParameter();
			}
				else if(name.equals("ValueBreak")){
					ValueBreak = (String)p.getParameter();
			}
				else if(name.equals("WhereClause")){
					WhereClause = (String)p.getParameter();
			}
		}
	}

		MChart chart = new Query(Env.getCtx(),MChart.Table_Name,MChart.COLUMNNAME_Name+"=?",get_TrxName())
}