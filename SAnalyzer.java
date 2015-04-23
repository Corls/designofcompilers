import java.util.ArrayList;
import java.util.Arrays;


public class SAnalyzer {
	
	private static String lineNumber = "0";
	private static ArrayList<Integer> scopeHistory = new ArrayList<Integer>();
	private static int nextScope = 0;
	private static ArrayList<Object> ast = new ArrayList<Object>();
	private static String[] parsed;
	private static int ts;
	
	public static void createAST(String treeThis) {
		parsed = treeThis.split("\\s");
		for(ts = 0; ts < parsed.length; ts++) {
			if(!isIgnored(parsed[ts]))
				ast.add(createBranch(parsed[ts]));
		}
		System.out.println(Arrays.deepToString(ast.toArray()));
	}
	
	private static Object[] createBranch(String token) {
		Object[] branch = null;
		if(token.matches("B_OPEN")) {
			branch = branchBlock();
		}
		else if(token.matches("C_[A-Z]+")) {
			token = token.substring(2);
			ts++;
			Object condExpr = branchEXPR();
			ts++;
			branch = new Object[]{token, condExpr, branchBlock()};
		}
		else if(token.matches("D_[A-Z]+")) {
			ts++;
			branch = new Object[]{"DECLARE", token.substring(2), parsed[ts]};
		}
		else if(token.equals("PRINT")) {
			ts++;
			branch = new Object[]{token, branchEXPR()};
		}
		else if(token.matches("[a-z]")) {
			ts += 2;
			branch = new Object[]{"SET", token, branchEXPR()};
		}
		else {
			MainDisplay.errorReport = "[Line: " + lineNumber + "] This message should never appear. (" + token + ")\n";
		}
		return branch;
	}
	private static boolean isIgnored(String token) {
		if(token.matches("LN_\\d+")) {
			lineNumber = token.substring(3);
			return true;
		}
		else if(token.equals("EOF")) {
			ts = parsed.length;
			return true;
		}
		
		return false;
	}
	
	//Branch Methods
	private static Object[] branchBlock() {
		ArrayList<Object> block = new ArrayList<Object>();
		block.add("BLOCK");
		
		for(ts++; ts<parsed.length; ts++) {
			if(parsed[ts].equals("B_CLOSE")) {
				return block.toArray();
			}
			if(!isIgnored(parsed[ts]))
				block.add(createBranch(parsed[ts]));
		}
		
		MainDisplay.errorReport = "[Line: " + lineNumber + "] This message should never appear. (BLOCK)\n";
		return block.toArray();
	}
	
	private static Object branchEXPR() {
		String root = "";
		Object branchOne;
		Object branchTwo;
		
		if(parsed[ts].equals("E_OPEN")) {
			ts++;
			branchOne = branchEXPR();
		}
		else if(parsed[ts].equals("Q_OPEN")) {
			branchOne = branchQuote();
		}
		else {
			branchOne = parsed[ts];
		}
		
		if(!parsed[ts+1].matches("E_[A-Z]+")) {
			return branchOne;
		}
		else {
			ts++;
		}
		
		if(parsed[ts].equals("E_CLOSE")) {
			return branchOne;
		}
		else {
			root = parsed[ts].substring(2);
		}
		
		ts++;
		branchTwo = branchEXPR();
		
		if(root.isEmpty()) {
			MainDisplay.errorReport = "[Line: " + lineNumber + "] This message should never appear. (EXPR)\n";
		}
		
		return new Object[]{root, branchOne, branchTwo};
	}
	
	private static Object[] branchQuote() {
		ArrayList<String> quote = new ArrayList<String>();
		quote.add("QUOTE");
		for(ts++; ts < parsed.length && !parsed[ts].matches("Q_CLOSE"); ts++) {
			if(parsed[ts].matches("Q_SPACE"))
				quote.add("SPACE");
			else
				quote.add(parsed[ts]);
		}
		return quote.toArray();
	}
	
	
	//
	public static void analyzeCode() {
		for(Object branch : ast) {
			if(branch instanceof Object[]) {
				analyzeBranch((Object[]) branch);
			}
		}
	}
	
	private static void analyzeBlock(Object[] block) {
		MainDisplay.symbolTable.add(new ArrayList<String[]>());
		scopeHistory.add(nextScope);
		nextScope++;
		for(Object branch : block) {
			if(branch instanceof Object[]) {
				analyzeBranch((Object[]) branch);
			}
		}
		scopeHistory.remove(scopeHistory.size()-1);
	}
	
	private static void analyzeBranch(Object[] branch) {
		//System.out.println(branch[0]);
		if(branch[0].equals("DECLARE")) {
			addToSymbolTable((String)branch[1], (String)branch[2]);
		}
		else if(branch[0].equals("SET")) {
			String idType = getVarTypeOf((String)branch[1]);
			if(idType.isEmpty()) {
				MainDisplay.errorReport += "[Line: " + lineNumber + "] " + branch[1] + " can not be found. Please declare it first.\n";
			}
		}
		else if(branch[0].equals("BLOCK")) {
			analyzeBlock(branch);
		}
		else if(branch[0].equals("VARDECL")) {
			//HERPADERP
		}
		else if(branch[0].equals("VARDECL")) {
			//HERPADERP
		}
	}
	
	private static void addToSymbolTable(String type, String id) {
		int curScope = scopeHistory.get(scopeHistory.size()-1);
		ArrayList<String[]> scopeTable = MainDisplay.symbolTable.get(curScope);
		for(String[] symbolInfo : scopeTable) {
			if(symbolInfo[0].equals(id)) {
				MainDisplay.errorReport += "[Line: " + lineNumber + "] " + id + " has already been delared in this scope.\n";
				return;
			}
		}
		String[] declareInfo = new String[4];
		declareInfo[0] = id; //Variable Name
		declareInfo[1] = type; //Type
		declareInfo[2] = lineNumber; //Line of Creation
		if(type.equals("STR"))
			declareInfo[3] = ""; //Default String
		else if(type.equals("INT"))
			declareInfo[3] = "0"; //Default Integer
		else if(type.equals("BOOL"))
			declareInfo[3] = "FALSE"; //Default Boolean
		else
			declareInfo[3] = null; //Default Value
		
		scopeTable.add(declareInfo);
		MainDisplay.symbolTable.set(curScope, scopeTable);
	}
	
	
	private static String getVarTypeOf(String id) {
		ArrayList<String[]> scopeTable;
		for(int h = scopeHistory.size(); h > 0; h--) {
			scopeTable = MainDisplay.symbolTable.get(scopeHistory.get(h-1));
			for(String[] idInfo : scopeTable) {
				if(idInfo[0].equals(id)) {
					return idInfo[1];
				}
			}
		}
		return "";
	}
	
}
