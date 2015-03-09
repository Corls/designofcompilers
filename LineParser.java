import java.util.ArrayList;

public class LineParser {
	
	private static String[] tokens;
	private static String lineNumber = "0";
	private static int validType = 0; //0 = start, 1 = normal, 2 = condition until ") {", 3 = condition before ==/!=
	private static int bracketsIn = 0;
	private static int parensIn = 0;
	private static int ts = 0;
	private static final String[][] startTokens =
		{
		  //{"Start Token", "Following Token", "Function"},
			{"LN_\\d+", "", "SC_LINE"},
			{"B_OPEN", "", "SC_SBLOCK"},
			{"B_CLOSE", "", "SC_EBLOCK"},
			{"C_[A-Z]+", "E_OPEN", "SC_COND"},
			{"D_[A-Z]+", "[a-z]", "SC_DECL"},
			{"PRINT", "E_OPEN", ""},
			{"[a-z]", "SET", ""},
			{"EOF", "", "SC_END"}
		};
	private static final String startTokenErrorReference = "{, if, while, string, int, boolean, print, id, or }";
	private static final String[][] tokenTree =
		{
		  //{"Start Token", "Following Token", "Function"},
		    {"SET", "E_OPEN", ""},
		    {"SET", "TK_EXPR", ""},
		    {"E_OPEN", "TK_EXPR", "SC_SEXP"},
			{"E_PLUS", "TK_EXPR", "EXT_INT"},
			{"TK_EXPR", "Q_OPEN|[a-z]|\\d|TRUE|FALSE|E_OPEN", ""},
			//Can't seem to logic the TK_EXPOP shortcut properly...
			//I mean, I know why, but I haven't gotten around to it yet...
			//{"TK_EXPOP", "E_EQTO|E_NOTEQ|E_PLUS|E_CLOSE", ""},
			{"E_CLOSE", "E_EQTO|E_NOTEQ|E_CLOSE", "SC_DEXP"},
			{"E_CLOSE", ".*", "SC_DEXP"},
		    
			{"Q_OPEN", "[a-z]+", "EXT_STR"},
			{"Q_CLOSE", "E_EQTO|E_NOTEQ|E_PLUS|E_CLOSE", ""},
			{"Q_CLOSE", "", ""},
			{"[a-z]", "E_EQTO|E_NOTEQ|E_PLUS|E_CLOSE", "EXT_ID"},
			{"[a-z]", "", "EXT_ID"},
			{"\\d", "E_EQTO|E_NOTEQ|E_PLUS|E_CLOSE", "EXT_INT"},
			{"\\d", "", "EXT_INT"},
			{"TRUE|FALSE", "E_EQTO|E_NOTEQ|E_PLUS|E_CLOSE", "EXT_BOOL"},
			{"TRUE|FALSE", "", "EXT_BOOL"},
			{"E_EQTO|E_NOTEQ", "TK_EXPR", "EXT_BOOL"}
		};
	
	public static void parseCode(String parseThis) {
		tokens = parseThis.split("\\s");
		for(ts=1; ts<tokens.length; ts++) {
			tokenMatch(tokens[ts-1], tokens[ts]);
		}
	}
	
	public static void tokenMatch(String token, String lookahead) {
		String[][] validTokens = tokenTree;
		boolean lookError = false;
		if(validType == 0) {
			validTokens = startTokens;
			validType = 1;
		}
		for(String[] valToken : validTokens) {
			if(token.matches(valToken[0])) {
				lookError = true;
				if(valToken[1].isEmpty()) {
					validType = 0;
				}
				if(valToken[1].isEmpty() || lookahead.matches(valToken[1]) || valToken[1].startsWith("TK_")) {
					if(valToken[2].startsWith("SC_")) {
						parseSC(valToken[2], ts);
					}
					else if(valToken[2].startsWith("EXT_")) {
						parseEXPR(valToken[2], ts);
					}
					if(valToken[1].startsWith("TK_")) {
						tokenMatch(valToken[1], tokens[ts]);
						return;
					}
					return;
				}
			}
		}
		MainDisplay.errorReport += "[Line: " + lineNumber + "] Invalid token found. ";
		if(validType == 0) {
			MainDisplay.errorReport += "\nExpected a(n) " + startTokenErrorReference;
		}
		System.out.println(lookError);
		if(lookError) {
			MainDisplay.errorReport += "\nBefore: " + token + "\n";
			MainDisplay.errorReport += "\nFound: " + lookahead + "\n";
		}
		else {
			MainDisplay.errorReport += "\nValid: " + validType + "\n";
			MainDisplay.errorReport += "\nFound: " + token + "\n";
		}
		errorFound();
	}
	
	private static void parseSC(String type, int loc) {
		type = type.substring(3);
		//Because Mac Yosemite doesn't allow anything > 1.6 and you need 1.7 or > to switch-case Strings...
		if(type.equals("LINE")) {
			lineNumber = tokens[loc-1].substring(3);
		}
		else if(type.equals("SBLOCK")) {
			bracketsIn++;
		}
		else if(type.equals("EBLOCK")) {
			bracketsIn--;
			if(bracketsIn < 0) {
				MainDisplay.errorReport += "[Line: " + lineNumber + "] Found an unexpected }\n";
			}
			validType = 0;
		}
		else if(type.equals("SEXP")) {
			//For Tree
			parensIn++;
		}
		else if(type.equals("DEXP")) {
			//For Tree
			parensIn--;
			if(parensIn < 0) {
				MainDisplay.errorReport += "[Line: " + lineNumber + "] Found an unexpected )\n";
				parensIn++;
			}
			if(validType == 2 && !tokens[loc].equals("B_OPEN")) {
				MainDisplay.errorReport += "[Line: " + lineNumber + "] Expected { after conditional statement.\n";
				errorFound();
			}
			else if(validType == 3) {
				MainDisplay.errorReport += "[Line: " + lineNumber + "] Please make the conditional statement a boolean.\n";
			}
			validType = 0;
		}
		else if(type.equals("COND")) {
			validType = 3;
		}
		else if(type.equals("DECL")) {
			addToSymbolTable(tokens[loc-1],tokens[loc]);
		}
		else if(type.equals("END")) {
			if(ts < tokens.length) {
				MainDisplay.warningReport += "[Line: " + lineNumber + "] Extra code found after end of file ($).\n";
			}
			ts = tokens.length;
		}
	}
	private static void parseEXPR(String type, int loc) {
		type = type.substring(4);
		//Because Mac Yosemite doesn't allow anything > 1.6 and you need 1.7 or > to switch-case Strings...
		if(type.equals("STR")) {
			parseQuote(loc);
			//For Tree?
		}
		else if(type.equals("ID")) {
			//For Tree?
		}
		else if(type.equals("INT")) {
			//For Tree?
		}
		else if(type.equals("BOOL")) {
			//For Tree?
			if(validType == 3) {
				validType = 2;
			}
		}
	}
	
	private static void parseQuote(int loc) {
		for(;loc<tokens.length; loc++) {
			if(tokens[loc].startsWith("LN_"))
				lineNumber = tokens[loc].substring(3);
			else if(tokens[loc].matches("Q_CLOSE")) {
				ts = loc;
				return;
			}
		}
	}
	private static void addToSymbolTable(String type, String id) {
		if(!id.matches("[a-z]")) {
			MainDisplay.errorReport += "[Line: " + lineNumber + "] Expected an id after " + type + " delaration.\nFound: " + id + "\n";
			errorFound();
			return;
		}
		for(String[] symbolInfo : MainDisplay.symbolTable) {
			if(symbolInfo[0].equals(id)) {
				return;
			}
		}
		
		String[] declareInfo = new String[4];
		declareInfo[0] = id; //Variable Name
		declareInfo[1] = type; //Type
		declareInfo[2] = "null"; //Value
		declareInfo[3] = lineNumber; //Line of Creation
		//declareInfo[4] = 0; //Scope (Ignored on Parse)
		
		MainDisplay.symbolTable.add(declareInfo);
	}
	
	private static void errorFound() {
		for(;ts<tokens.length; ts++) {
			if(tokens[ts].startsWith("LN_")) {
				validType = 0;
				return;
			}
		}
	}
}
