import java.util.ArrayList;

public class LineParser {
	
	private static String[] tokens;
	private static String lineNumber = "0";
	private static int validType = 0; //0 = start, 1 = normal, 2 = condition until ") {", 3 = condition before ==/!=/true/false
	private static int bracketsIn = 0;
	private static int parensIn = 0;
	private static int ts = 0;
	private static final String[][] startTokens =
		{
		  //{"Start Token", "Following Token", "Function", "Found", "Expected"},
			{"LN_\\d+", "", "SC_LINE", "New Line", "Not That"},
			{"B_OPEN", "", "SC_SBLOCK", "{", "Not That"},
			{"B_CLOSE", "", "SC_EBLOCK", "}", "Not That"},
			{"C_[A-Z]+", "E_OPEN", "SC_COND", "if/while", "("},
			{"D_[A-Z]+", "[a-z]", "SC_DECL", "int/string/boolean", "id"},
			{"PRINT", "E_OPEN", "", "print", "("},
			{"[a-z]", "SET", "", "id", "="},
			{"EOF", "", "SC_END", "$", "Not That"}
		};
	private static final String[][] tokenTree =
		{
		  //{"Start Token", "Following Token", "Function", "Found", "Expected"},
		    {"SET", "E_OPEN", "", "=", "Expression"},
		    {"SET", "TK_EXPR", "", "=", "Expression"},
		    {"E_OPEN", "TK_EXPR", "SC_SEXP", "(", "Expression"},
			{"E_PLUS", "TK_EXPR", "", "+", "id/number"},
			{"TK_EXPR", "Q_OPEN|[a-z]|\\d|TRUE|FALSE|E_OPEN", "", "Expression", "Not That"},
			//Can't seem to logic the TK_EXPOP shortcut properly...
			//I mean, I know why, but I haven't gotten around to it yet...
			//{"TK_EXPOP", "E_EQTO|E_NOTEQ|E_PLUS|E_CLOSE", ""},
			{"E_CLOSE", "E_EQTO|E_NOTEQ|E_CLOSE", "SC_DEXP", ")", "Comparison/New Statement"},
			{"E_CLOSE", ".*", "SC_DEXP", ")", "Comparison/New Statement"},
		    
			{"Q_OPEN", ".*", "EXT_STR", "\"", "Comparison/New Statement"},
			{"Q_CLOSE", "E_EQTO|E_NOTEQ|E_CLOSE", "", "\"", "Not That"},
			{"Q_CLOSE", "", "", "\"", "Not That"},
			{"[a-z]", "E_EQTO|E_NOTEQ|E_CLOSE", "", "id", "Comparison/New Statement"},
			{"[a-z]", "", "", "id", "Comparison/New Statement"},
			{"\\d", "E_EQTO|E_NOTEQ|E_PLUS|E_CLOSE", "", "number", "+/Comparison/New Statement"},
			{"\\d", "", "", "number", "+/Comparison/New Statement"},
			{"TRUE|FALSE", "E_EQTO|E_NOTEQ|E_CLOSE", "EXT_BOOL", "true/false", "+/Comparison/New Statement"},
			{"TRUE|FALSE", "", "EXT_BOOL", "true/false", "+/Comparison/New Statement"},
			{"E_EQTO|E_NOTEQ", "TK_EXPR", "EXT_BOOL", "==/!==", "Expression"}
		};
	
	public static void parseCode(String parseThis) {
		tokens = parseThis.split("\\s");
		for(ts=1; ts<tokens.length; ts++) {
			tokenMatch(tokens[ts-1], tokens[ts]);
		}
		if(ts == tokens.length){
			if(!tokens[tokens.length-1].equals("EOF")) {
				MainDisplay.warningReport += "You are missing an end of file ($).\n";
			}
			parseSC("SC_END", ts-1);
		}
	}
	
	public static void tokenMatch(String token, String lookahead) {
		String[][] validTokens = tokenTree;
		String expected = "";
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
				else {
					expected = "Expected: \"" + valToken[4] + "\" after \"" + valToken[3] + "\"";
				}
			}
		}
		MainDisplay.errorReport += "[Line: " + lineNumber + "] Invalid token found.";
		if(lookError) {
			MainDisplay.errorReport += " " + expected + ". Found: \"" + lookahead + "\"\n";
		}
		else {
			MainDisplay.errorReport += "\n";
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
				validType = 0;
			}
			else if(parensIn == 0) {
				if(validType == 3) {
					MainDisplay.errorReport += "[Line: " + lineNumber + "] Please make the conditional statement a boolean.\n";
					validType = 2;
				}
				if(validType == 2 && !tokens[loc].equals("B_OPEN")) {
					System.out.println(tokens[loc]);
					MainDisplay.errorReport += "[Line: " + lineNumber + "] Expected { after conditional statement.\n";
					errorFound();
				}
				validType = 0;
			}
		}
		else if(type.equals("COND")) {
			validType = 3;
		}
		else if(type.equals("DECL")) {
			ts++;
			validType = 0;
		}
		else if(type.equals("END")) {
			if(ts < tokens.length) {
				MainDisplay.warningReport += "[Line: " + lineNumber + "] Extra code found after end of file ($).\n";
			}
			if(bracketsIn > 0 && MainDisplay.errorReport.isEmpty()) {
				MainDisplay.errorReport += "[End of File] You are missing " + bracketsIn + " }.\n";
			}
			ts = tokens.length;
		}
	}
	private static void parseEXPR(String type, int loc) {
		type = type.substring(4);
		//Because Mac Yosemite doesn't allow anything > 1.6 and you need 1.7 or > to switch-case Strings...
		if(type.equals("STR")) {
			parseQuote(loc);
		}
		else if(type.equals("BOOL")) {
			if(validType == 3) {
				validType = 2;
			}
		}
	}
	
	private static void parseQuote(int loc) {
		for(;loc<tokens.length; loc++) {
			if(tokens[loc].equals("Q_CLOSE")) {
				ts = loc;
				return;
			}
		}
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
