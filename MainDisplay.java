import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


public class MainDisplay {

	private static Scanner scan = new Scanner(System.in);
	private static int lineNumber = 0;
	public static String errorReport = "";
	public static String warningReport = "";
	public static ArrayList<ArrayList<String[]>> symbolTable = new ArrayList<ArrayList<String[]>>();
	public static ArrayList<Object[]> concatSyntaxTree = new ArrayList<Object[]>();
	private static String grammar_regex = "";
	private final static String[][] GRAMMAR_TABLE =
		{
		    {"\\(", " E_OPEN "},
			{"\\)", " E_CLOSE "},
			{"\\{", " B_OPEN "},
			{"\\}", " B_CLOSE "},
			{"print", " PRINT "},
			{"(?<![!<>=])=(?!=)", " SET "},
			{"==", " E_EQTO "},
			{"!=", " E_NOTEQ "},
			{"\\+", " E_PLUS "},
			{"string", " D_STR "},
			{"boolean", " D_BOOL "},
			{"int", " D_INT "},
			{"if", " C_BRANCH "},
			{"while", " C_LOOP "},
			{"true", " TRUE "},
			{"false", " FALSE "},
			{"\\$", " EOF "}
		};
	
	public static void main(String[] args) {
		//Get Input
		String practiceStatement = "{intxx = 2 + 3 if(x != 4){\nprint(x)print(2)\nprint(\"all printed\")\n}}$";
		System.out.println("Sample:\n" + practiceStatement + "\n\nPlease Enter Code Below:");
		String scanTest = scan.nextLine();
		while(!scanTest.endsWith("$"))
			scanTest += "\n" + scan.nextLine();
		if(!scanTest.startsWith("{")){
			System.out.println("\nPlease make sure your code is wrapped by {}$");
			return;
		}
		String[] practiceArray = scanTest.split("\n");
		
		//Lex
		initGrammar();
		String lexedCode = "";
		System.out.println("\n\n");
		for(int i = 0; i < practiceArray.length; i++){
			lineNumber = i+1;
			practiceArray[i] = lexateLine(practiceArray[i]).replaceFirst("^\\s+", "");
			lexedCode += "LN_" + lineNumber + " " + practiceArray[i];
			System.out.println(practiceArray[i]);
		}
		if(!warningReport.isEmpty()) {
			System.out.println("Lex Warnings:\n" + warningReport);
			warningReport = "";
		}
		if(!errorReport.isEmpty()) {
			System.out.println("Lex Errors:\n" + errorReport);
			return;
		}
		
		//Parse
		System.out.println("\n");
		LineParser.parseCode(lexedCode);
		if(!warningReport.isEmpty()) {
			System.out.println("Parse Warnings:\n" + warningReport);
			warningReport = "";
		}
		if(!errorReport.isEmpty()) {
			System.out.println("Parse Errors:\n" + errorReport);
			return;
		}
		
		//Samantic Analysis
		System.out.println("\n");
		SAnalyzer.createAST(lexedCode);
		if(!warningReport.isEmpty()) {
			System.out.println("AST Warnings:\n" + warningReport);
			warningReport = "";
		}
		if(!errorReport.isEmpty()) {
			System.out.println("AST Errors:\n" + errorReport);
			return;
		}
		
		System.out.println("\n");
		ArrayList<Object> ast = SAnalyzer.analyzeCode();
		if(!warningReport.isEmpty()) {
			System.out.println("Semantic Warnings:\n" + warningReport);
			warningReport = "";
		}
		if(!errorReport.isEmpty()) {
			System.out.println("Semantic Errors:\n" + errorReport);
			return;
		}
		
		System.out.println("\n");
		CodeGenerator.generateCode(ast);
		if(!warningReport.isEmpty()) {
			System.out.println("Generation Warnings:\n" + warningReport);
			warningReport = "";
		}
		if(!errorReport.isEmpty()) {
			System.out.println("Generation Errors:\n" + errorReport);
			return;
		}
		
		//Compiling Finished Notice
		System.out.println("All Good");
	}
	
	private static String lexateLine(String lexThis) {
		String[] lexThese = lexThis.split("\"");
		boolean endQuote = lexThis.endsWith("\"");
		lexThis = "";
		
		for(int i=0; i<lexThese.length; i++) {
			if(i%2==0) {
				lexThese[i] = lexStatement(lexThese[i]);
				lexThis += lexThese[i];
				wordCheck(lexThese[i]);
				numCheck(lexThese[i]);
			}
			else {
				lexThis += " Q_OPEN " + lexQuote(lexThese[i]) + " Q_CLOSE ";
				if(lexThese.length-1 == i && !endQuote) {
					errorReport += "[Line: " + lineNumber + "] End quote not detected on line. Remember, quotes can only be on a single line.\n";
				}
			}
		}
		
		return lexThis.replaceAll("(\\s)+", " ");
	}
	
	private static void wordCheck(String checkThis) {
		checkThis = checkThis.replaceAll(grammar_regex, " ");
		checkThis = checkThis.replaceAll("\\-?\\d+", " ");
		checkThis = checkThis.replaceAll("(\\s)+", " ");
		ArrayList<String> words = new ArrayList<String>(Arrays.asList(checkThis.split("\\s")));
		for(String test : words) {
			if(!test.isEmpty() && !test.matches("[a-z]"))
				errorReport += "[Line: " + lineNumber + "] The term \"" + test + "\" is an invalid command or id.\n";
		}
	}
	private static void numCheck(String checkThis) {
		if(checkThis.matches(".*\\d\\d+.*")) {
			errorReport += "[Line: " + lineNumber + "] The value \"" + checkThis.replaceFirst("(^.*?)(\\-?\\d\\d+)(.*?$)", "$2") + "\" is too large.\n";
		}
		if(checkThis.matches(".*\\-\\d+.*")) {
			errorReport += "[Line: " + lineNumber + "] Negative numbers are invalid.\n";
		}
	}
	
	private static String lexStatement (String statement) {
		for(String[] reglex : GRAMMAR_TABLE) {
			statement = statement.replaceAll(reglex[0], reglex[1]);
		}
		statement = statement.replaceAll("(\\-?)(0*)(\\d)", " $1$3 ");
		statement = statement.replaceAll("([a-z])", " $1 ");
		return statement;
	}
	private static String lexQuote(String quoteText) {
		if(quoteText.isEmpty())
			return "Q_EMPTY";
		if(quoteText.matches(".*[A-Z].*"))
			errorReport += "[Line: " + lineNumber + "] Uppercase letters detected in quote. Please use lowercase and spaces.\n";
		if(quoteText.matches(".*\\d.*"))
			errorReport += "[Line: " + lineNumber + "] Numbers detected in quote. Please use lowercase and spaces.\n";
		if(quoteText.matches(".*[^a-zA-Z\\s].*"))
			errorReport += "[Line: " + lineNumber + "] Invalid characters detected in quote. Please use lowercase and spaces.\n";
		quoteText = quoteText.toLowerCase();
		quoteText = quoteText.replaceAll("[^a-z\\s]", "");
		quoteText = quoteText.replaceAll("\\s", " Q_SPACE ");
		return quoteText;
	}
	
	private static void initGrammar() {
		for(String[] reglex : GRAMMAR_TABLE) {
			grammar_regex += reglex[1] + "|";
		}
		if(grammar_regex.isEmpty())
			return;
		grammar_regex = grammar_regex.substring(0, grammar_regex.length()-1);
	}
	
}
