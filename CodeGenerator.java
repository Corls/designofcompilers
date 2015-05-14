import java.util.ArrayList;
import java.util.Arrays;

public class CodeGenerator {

	private static String lineNumber = "0";
	private static String mechCode = "";
	private static String staticCode = "00 ";
	private static String heapCode = "";
	private static int jumpTemp = 1;
	
	private static ArrayList<String> varTable = new ArrayList<String>();
	//private static ArrayList<String[]> strTable = new ArrayList<String[]>();
	private static ArrayList<Object[]> jumpTable = new ArrayList<Object[]>();
	
	public static void generateCode(ArrayList<Object> ast) {
		varTable.add("T0");
		for(Object branch : ast) {
			if(branch instanceof Object[]) {
				generateBranch((Object[])branch);
			}
		}
		System.out.println(mechCode);
	}
	
	private static void generateBranch(Object[] branch) {
		String temp = "";
		if(branch[0].equals("LINE")) {
			lineNumber = (String) branch[1];
		}
		else if(branch[0].equals("BLOCK")) {
			for(int i = 1; i < branch.length; i++) {
				generateBranch((Object[])branch[i]);
			}
		}
		else if(branch[0].equals("DECLARE")) {
			if(branch[1].equals("INT") || branch[1].equals("BOOL")) {
				varTable.add("T" + branch[2]);
				staticCode += "00 ";
			}
			else if(branch[1].equals("STR")) {
				varTable.add("T" + branch[2]);
				staticCode += "FF ";
				//strTable.add(new String[]{("S" + branch[2]), Integer.toHexString(255 - Math.round(heapCode.length()/3)*4)});
			}
		}
		else if(branch[0].equals("SET")) {
			if(branch[2] instanceof Object[]) {
				temp = generateExpr((Object[])branch[2], branch[1]);
			}
			else if(branch[2] instanceof String) {
				temp = generateExpr((String)branch[2]);
			}
			else {
				MainDisplay.errorReport = "[Line: " + lineNumber + "]You should never get this message.\n";
			}
			mechCode += temp + " 8D T" + branch[1] + " 00 ";
		}
		else if(branch[0].equals("PRINT")) {
			if(branch[1] instanceof Object[]) {
				temp = generateExpr((Object[])branch[1], "0");
			}
			else if(branch[2] instanceof String) {
				temp = generateExpr((String)branch[2]);
			}
			else {
				MainDisplay.errorReport = "[Line: " + lineNumber + "]You should never get this message.\n";
			}
			mechCode += temp + " 8D T0 00 AC T0 00 A2 01 FF ";
		}
		else if(branch[0].equals("BRANCH") || branch[0].equals("LOOP")) {
			
		}
		else if(branch[0].equals("")) {
			
		}
	}
	
	private static String generateExpr(Object[] expr, Object id) {
		String temp = "";
		if(expr[0].equals("PLUS")) {
			if(expr[1] instanceof Object[]) {
				temp = generateExpr((Object[])expr[1], id);
			}
			else if (expr[1] instanceof String) {
				temp = generateExpr((String)expr[1]);
			}
			else {
				MainDisplay.errorReport = "[Line: " + lineNumber + "] You should never get this message.\n";
			}
			temp += " 8D T" + id + " 00 ";
			
			if(expr[2] instanceof Object[]) {
				temp += generateExpr((Object[])expr[2], id);
			}
			else if (expr[2] instanceof String) {
				temp += generateExpr((String)expr[2]);
			}
			else {
				MainDisplay.errorReport = "[Line: " + lineNumber + "] You should never get this message.\n";
			}
			temp += " 6D T" + id + " 00";
		}
		else if(expr[0].equals("EQTO") || expr[0].equals("NOTEQ")) {
			if(expr[1] instanceof Object[]) {
				temp = generateExpr((Object[])expr[1], id);
			}
			else if (expr[1] instanceof String) {
				temp = generateExpr((String)expr[1]);
			}
			else {
				MainDisplay.errorReport = "[Line: " + lineNumber + "] You should never get this message.\n";
			}
			temp += " 8D T" + id + " 00 AE T" + id + " 00";
			
			if(expr[2] instanceof Object[]) {
				temp += generateExpr((Object[])expr[2], id);
			}
			else if (expr[2] instanceof String) {
				temp += generateExpr((String)expr[2]);
			}
			else {
				MainDisplay.errorReport = "[Line: " + lineNumber + "] You should never get this message.\n";
			}
			temp += " 8D T" + id + " 00 EC T" + id + " 00";
			
			if(expr[0].equals("EQTO")) {
				temp += " A9 01 D0 08 A9 00";
			}
			else {
				temp += " A9 00 D0 08 A9 01";
			}
		}
		else if(expr[0].equals("QUOTE")) {
			String quote = "";
			for(int i = 1; i < expr.length; i++) {
				temp = (String) expr[i];
				if(temp.equals("SPACE")) {
					quote += "20 ";
				}
				else {
					char[] test = temp.toCharArray();
					for(char tst : test) {
						quote += Integer.toHexString(tst);
					}
				}
			}
			quote += " 00";
			return "Q_" + quote;
		}
		else {
			MainDisplay.errorReport = "[Line: " + lineNumber + "] You should never get this message.\n";
		}
		return temp;
	}
	private static String generateExpr(String expr) {
		if(expr.matches("\\d")) {
			expr = Integer.toHexString(Integer.parseInt(expr));
			if(expr.length() < 2) {
				expr = "0" + expr;
			}
		}
		else if(expr.equals("TRUE")) {
			expr = "01";
		}
		else if(expr.equals("FALSE")) {
			expr = "00";
		}
		else if(expr.matches("[a-z]")) {
			return "AD T" + expr + " 00";
		}
		else {
			MainDisplay.errorReport = "[Line: " + lineNumber + "] You should never get this message.\n";
		}
		
		return "A9 " + expr;
	}
	
	private static String generateCondition(Object something) {
		String temp = "";
		
		return temp;
	}
	
	private static String generatePrint(Object[] expr) {
		String temp = "";
		
		return temp;
	}
	
	private static String generateX(Object something) {
		String temp = "";
		
		return temp;
	}

}
