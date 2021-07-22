/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.my.process;
import java.math.BigDecimal;import java.util.List;import org.adempiere.exceptions.AdempiereException;import org.compiere.model.MChart;import org.compiere.model.MChartDatasource;import org.compiere.model.MEntityType;import org.compiere.model.MTable;import org.compiere.model.Query;import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;import org.compiere.util.Env;import org.compiere.util.KeyNamePair;	public class ChartMaker extends SvrProcess {
	private String ChartName = "";
	private String Time = "";
	private int AD_Table_ID = 0;
	private String CategoryColumn = "";
	private String ValueBreak = "";
	private String WhereClause = "";		//	private StringBuilder FromClause = new StringBuilder(); 	private BigDecimal ValueColumn = Env.ZERO;	private String DateColumn = ""; 	private String Name = "";
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)					;
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
	protected String doIt() {
		MChart chart = new Query(Env.getCtx(),MChart.Table_Name,MChart.COLUMNNAME_Name+"=?",get_TrxName())		.setParameters(ChartName).first();		if (chart==null){			chart = new MChart(Env.getCtx(),0,get_TrxName());			chart.setName(ChartName);			chart.setChartType(MChart.CHARTTYPE_3DStackedBarChart);			chart.setWinHeight(300);			chart.setEntityType(MEntityType.ENTITYTYPE_UserMaintained);			chart.setChartOrientation(MChart.CHARTORIENTATION_Horizontal);					chart.saveEx(get_TrxName());		}		chart.setIsDisplayLegend(true);		chart.setChartType(MChart.CHARTTYPE_3DStackedBarChart);		chart.setRangeLabel("Value");		if (Time!=null){			chart.setIsTimeSeries(true);			if (Time.startsWith("M")){				chart.setDomainLabel("Month");				chart.setTimeUnit(MChart.TIMEUNIT_Month);				}			else if (Time.startsWith("D")){				chart.setDomainLabel("Day");				chart.setTimeUnit(MChart.TIMEUNIT_Day);				}			else if (Time.startsWith("W")){				chart.setDomainLabel("Week");				chart.setTimeUnit(MChart.TIMEUNIT_Week);				}			else if (Time.startsWith("Y")){				chart.setDomainLabel("Year");				chart.setTimeUnit(MChart.TIMEUNIT_Year);				}			else {				chart.setIsTimeSeries(false);				chart.setDomainLabel("");				chart.setTimeUnit("Y");			}		}		if (!ValueBreak.contains(".") || !ValueBreak.contains(">"))			throw new AdempiereException("Example: LineAmt>C_Order.C_BPartner.C_BPartnerLocation_ID");		MTable table = new MTable(Env.getCtx(),AD_Table_ID,null);		String breakIDfield = "";		//forming FROM_CLAUSE		FromClause = new StringBuilder(table.getTableName()+" a");		//analyse ValueBreak for FromClause		String[] valuebreak = ValueBreak.split(">");		int dsline=0;		if (valuebreak.length>0){			char alias=(char)'b';			KeyNamePair[] keyIDs = null;			String valuecolumn = valuebreak[0];			String[] tablesjoin = valuebreak[1].split("\\.");			if (tablesjoin.length==0)				throw new AdempiereException("SPECIFY JOIN TABLES '.' Example 'C_Order.C_BPartner'");			else {				for (int i=0;i<tablesjoin.length;i++){					if (tablesjoin[i].endsWith("_ID")){						breakIDfield = tablesjoin[i];						String breakIDTable = tablesjoin[i].substring(0,breakIDfield.indexOf("_ID"));						if (MTable.getTable_ID(breakIDTable)<1)							throw new AdempiereException("ValueBreak last ID not existing.");						//form SQL FromClause with key ID criteria to obtain values for DS details						String sql4IDs = "SELECT "+tablesjoin[i]+",Name"+" FROM "+tablesjoin[i].substring(0,breakIDfield.indexOf("_ID"));						keyIDs = DB.getKeyNamePairs(sql4IDs,false);					} else {						MTable tablejoin = new Query(Env.getCtx(),MTable.Table_Name,MTable.COLUMNNAME_TableName+"=?",get_TrxName())						.setParameters(tablesjoin[i]).first();						if (tablejoin==null)							throw new AdempiereException("No such table in ValueBreak: "+tablesjoin[i]);						char prealias = getCorrectAlias(tablesjoin);						alias = (char)('b'+i);												String tablename = tablejoin.getTableName();						String tableID = tablename+"_ID";						FromClause.append(" INNER JOIN "+tablename+" "+alias								+" ON ("+alias+"."+tableID+"="+(prealias)+"."+tableID+")");					}				}				//delete previous ChartDatasources				 List<MChartDatasource> dss = new Query(Env.getCtx(),MChartDatasource.Table_Name,MChartDatasource.COLUMNNAME_AD_Chart_ID+"=?",get_TrxName())				 .setParameters(chart.get_ID()).list();				 for (MChartDatasource datasource:dss){					 datasource.delete(true);				 }				 for (KeyNamePair keyID:keyIDs){					String sql = "SELECT COUNT(a.AD_Client_ID) FROM "+FromClause+" WHERE "+alias+"."+breakIDfield+"=?";					int count = DB.getSQLValueEx(get_TrxName(), sql, keyID.getKey());					if (count==0)						continue;					MChartDatasource ds = new MChartDatasource(Env.getCtx(),0,null);					ds.setAD_Chart_ID(chart.get_ID());					ds.setFromClause(FromClause.toString());					ds.setValueColumn("sum(a."+valuecolumn+")");					if (!Time.isEmpty())						ds.setDateColumn(CategoryColumn.contains(".")?CategoryColumn:"a."+CategoryColumn);					else						ds.setCategoryColumn(CategoryColumn.contains(".")?CategoryColumn:"a."+CategoryColumn);					ds.setWhereClause(alias+"."+breakIDfield+"="+keyID.getKey()+(WhereClause.length()>0?" AND "+WhereClause:""));					ds.setName(keyID.getName());					ds.setKeyColumn("a."+table.getTableName()+"_ID");					ds.setAD_Table_ID(AD_Table_ID);					ds.saveEx(get_TrxName());					dsline++;					}				chart.saveEx(get_TrxName());							}		} else			throw new AdempiereException("SPECIFY BREAK '>'. Example 'LineAmt>M_Product_ID'");			return "DataSource Lines created:"+dsline;	}	private char getCorrectAlias(String[] tablesjoin) {		char a = (char)'a';		for (int i=0;i<tablesjoin.length;i++){			MTable table = new Query(Env.getCtx(),MTable.Table_Name,MTable.COLUMNNAME_TableName+"=?",get_TrxName())			.setParameters(tablesjoin[i]).first();			if (table!=null)				return (char)(a+i);		}		return 0;	}
}
