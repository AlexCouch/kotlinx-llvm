// Generated from C:/projects/kotlinx-llvm/toylang/src/main/antlr\ToylangLexer.g4 by ANTLR 4.8
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ToylangLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		NEWLINE=1, WS=2, LET=3, MUT=4, FN=5, RETURN=6, IF=7, ELSE=8, FOR=9, FOREACH=10, 
		LOOP=11, WHILE=12, BREAK=13, CONTINUE=14, TRAIT=15, STRUCT=16, RECORDS=17, 
		IDENT=18, INTLITERAL=19, DECIMALLITERAL=20, PLUS=21, MINUS=22, ASTERISK=23, 
		FORWORD_SLASH=24, ASSIGN=25, LPAREN=26, RPAREN=27, COLON=28, SEMICOLON=29, 
		COMMA=30, LCURLBRACE=31, RCURLBRACE=32, STRING_OPEN=33, DOC_COMMENT=34, 
		LINE_COMMENT=35, UNMATCHED=36, ESCAPE_STRING_DELIMITER=37, ESCAPE_SLASH=38, 
		ESCAPE_NEWLINE=39, STRING_CLOSE=40, INTERPOLATION_OPEN=41, STRING_CONTENT=42, 
		INTERPOLATION_CLOSE=43;
	public static final int
		WHITESPACE=2, COMMENT=3;
	public static final int
		MODE_IN_STRING=1, MODE_IN_INTERPOLATION=2;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN", "WHITESPACE", "COMMENT"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "MODE_IN_STRING", "MODE_IN_INTERPOLATION"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"NEWLINE", "WS", "LET", "MUT", "FN", "RETURN", "IF", "ELSE", "FOR", "FOREACH", 
			"LOOP", "WHILE", "BREAK", "CONTINUE", "TRAIT", "STRUCT", "RECORDS", "IDENT", 
			"INTLITERAL", "DECIMALLITERAL", "PLUS", "MINUS", "ASTERISK", "FORWORD_SLASH", 
			"ASSIGN", "LPAREN", "RPAREN", "COLON", "SEMICOLON", "COMMA", "LCURLBRACE", 
			"RCURLBRACE", "STRING_OPEN", "DOC_COMMENT", "LINE_COMMENT", "UNMATCHED", 
			"ESCAPE_STRING_DELIMITER", "ESCAPE_SLASH", "ESCAPE_NEWLINE", "STRING_CLOSE", 
			"INTERPOLATION_OPEN", "STRING_CONTENT", "STR_UNMATCHED", "INTERPOLATION_CLOSE", 
			"INTERP_WS", "INTERP_LET", "INTERP_MUT", "INTERP_INTLIT", "INTERP_DECI_LIT", 
			"INTERP_PLUS", "INTERP_MINUS", "INTERP_ASTERISK", "INTERP_DIVISION", 
			"INTERP_LPAREN", "INTERP_RPAREN", "INTERP_IDENT", "INTERP_STRING_OPEN", 
			"INTERP_UNMATCHED"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, "'fn'", "'return'", "'if'", "'else'", "'for'", 
			"'foreach'", "'loop'", "'while'", "'break'", "'continue'", "'trait'", 
			"'struct'", "'record'", null, null, null, null, null, null, null, "'='", 
			null, null, "':'", "';'", "','", "'{'", null, null, null, null, null, 
			"'\\\"'", "'\\\\'", "'\\n'", null, "'\\$\\{'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "NEWLINE", "WS", "LET", "MUT", "FN", "RETURN", "IF", "ELSE", "FOR", 
			"FOREACH", "LOOP", "WHILE", "BREAK", "CONTINUE", "TRAIT", "STRUCT", "RECORDS", 
			"IDENT", "INTLITERAL", "DECIMALLITERAL", "PLUS", "MINUS", "ASTERISK", 
			"FORWORD_SLASH", "ASSIGN", "LPAREN", "RPAREN", "COLON", "SEMICOLON", 
			"COMMA", "LCURLBRACE", "RCURLBRACE", "STRING_OPEN", "DOC_COMMENT", "LINE_COMMENT", 
			"UNMATCHED", "ESCAPE_STRING_DELIMITER", "ESCAPE_SLASH", "ESCAPE_NEWLINE", 
			"STRING_CLOSE", "INTERPOLATION_OPEN", "STRING_CONTENT", "INTERPOLATION_CLOSE"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public ToylangLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "ToylangLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2-\u01ae\b\1\b\1\b"+
		"\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n"+
		"\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21"+
		"\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30"+
		"\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37"+
		"\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t"+
		"*\4+\t+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63"+
		"\4\64\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\3\2\3"+
		"\2\3\2\5\2}\n\2\3\2\3\2\3\3\6\3\u0082\n\3\r\3\16\3\u0083\3\3\3\3\3\4\3"+
		"\4\3\4\3\4\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\b"+
		"\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\22\3\22"+
		"\3\22\3\22\3\22\3\22\3\22\3\23\3\23\7\23\u00de\n\23\f\23\16\23\u00e1\13"+
		"\23\3\24\6\24\u00e4\n\24\r\24\16\24\u00e5\3\25\6\25\u00e9\n\25\r\25\16"+
		"\25\u00ea\3\25\3\25\6\25\u00ef\n\25\r\25\16\25\u00f0\3\25\3\25\6\25\u00f5"+
		"\n\25\r\25\16\25\u00f6\5\25\u00f9\n\25\3\26\3\26\3\27\3\27\3\30\3\30\3"+
		"\31\3\31\3\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37\3"+
		" \3 \3!\3!\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\7#\u011c\n#\f#\16#\u011f\13"+
		"#\3$\3$\3$\3$\7$\u0125\n$\f$\16$\u0128\13$\3$\3$\3%\3%\3&\3&\3&\3\'\3"+
		"\'\3\'\3(\3(\3(\3)\3)\3)\3)\3*\3*\3*\3*\3*\3*\3*\3+\6+\u0143\n+\r+\16"+
		"+\u0144\3,\3,\3,\3,\3-\3-\3-\3-\3.\6.\u0150\n.\r.\16.\u0151\3.\3.\3.\3"+
		"/\3/\3/\3/\3/\3/\3\60\3\60\3\60\3\60\3\60\3\60\3\61\6\61\u0164\n\61\r"+
		"\61\16\61\u0165\3\61\3\61\3\62\6\62\u016b\n\62\r\62\16\62\u016c\3\62\3"+
		"\62\6\62\u0171\n\62\r\62\16\62\u0172\5\62\u0175\n\62\3\62\3\62\6\62\u0179"+
		"\n\62\r\62\16\62\u017a\3\62\3\62\3\63\3\63\3\63\3\63\3\64\3\64\3\64\3"+
		"\64\3\65\3\65\3\65\3\65\3\66\3\66\3\66\3\66\3\67\3\67\3\67\3\67\38\38"+
		"\38\38\39\79\u0198\n9\f9\169\u019b\139\39\39\79\u019f\n9\f9\169\u01a2"+
		"\139\39\39\3:\3:\3:\3:\3:\3;\3;\3;\3;\2\2<\5\3\7\4\t\5\13\6\r\7\17\b\21"+
		"\t\23\n\25\13\27\f\31\r\33\16\35\17\37\20!\21#\22%\23\'\24)\25+\26-\27"+
		"/\30\61\31\63\32\65\33\67\349\35;\36=\37? A!C\"E#G$I%K&M\'O(Q)S*U+W,Y"+
		"\2[-]\2_\2a\2c\2e\2g\2i\2k\2m\2o\2q\2s\2u\2w\2\5\2\3\4\n\4\2\f\f\17\17"+
		"\4\2\13\13\"\"\4\2C\\c|\6\2\62;C\\aac|\3\2\62;\5\2\13\f\17\17$$\3\2aa"+
		"\3\2c|\2\u01be\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3"+
		"\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2"+
		"\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3"+
		"\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2"+
		"\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2"+
		";\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3"+
		"\2\2\2\2I\3\2\2\2\2K\3\2\2\2\3M\3\2\2\2\3O\3\2\2\2\3Q\3\2\2\2\3S\3\2\2"+
		"\2\3U\3\2\2\2\3W\3\2\2\2\3Y\3\2\2\2\4[\3\2\2\2\4]\3\2\2\2\4_\3\2\2\2\4"+
		"a\3\2\2\2\4c\3\2\2\2\4e\3\2\2\2\4g\3\2\2\2\4i\3\2\2\2\4k\3\2\2\2\4m\3"+
		"\2\2\2\4o\3\2\2\2\4q\3\2\2\2\4s\3\2\2\2\4u\3\2\2\2\4w\3\2\2\2\5|\3\2\2"+
		"\2\7\u0081\3\2\2\2\t\u0087\3\2\2\2\13\u008b\3\2\2\2\r\u008f\3\2\2\2\17"+
		"\u0092\3\2\2\2\21\u0099\3\2\2\2\23\u009c\3\2\2\2\25\u00a1\3\2\2\2\27\u00a5"+
		"\3\2\2\2\31\u00ad\3\2\2\2\33\u00b2\3\2\2\2\35\u00b8\3\2\2\2\37\u00be\3"+
		"\2\2\2!\u00c7\3\2\2\2#\u00cd\3\2\2\2%\u00d4\3\2\2\2\'\u00db\3\2\2\2)\u00e3"+
		"\3\2\2\2+\u00f8\3\2\2\2-\u00fa\3\2\2\2/\u00fc\3\2\2\2\61\u00fe\3\2\2\2"+
		"\63\u0100\3\2\2\2\65\u0102\3\2\2\2\67\u0104\3\2\2\29\u0106\3\2\2\2;\u0108"+
		"\3\2\2\2=\u010a\3\2\2\2?\u010c\3\2\2\2A\u010e\3\2\2\2C\u0110\3\2\2\2E"+
		"\u0112\3\2\2\2G\u0116\3\2\2\2I\u0120\3\2\2\2K\u012b\3\2\2\2M\u012d\3\2"+
		"\2\2O\u0130\3\2\2\2Q\u0133\3\2\2\2S\u0136\3\2\2\2U\u013a\3\2\2\2W\u0142"+
		"\3\2\2\2Y\u0146\3\2\2\2[\u014a\3\2\2\2]\u014f\3\2\2\2_\u0156\3\2\2\2a"+
		"\u015c\3\2\2\2c\u0163\3\2\2\2e\u0174\3\2\2\2g\u017e\3\2\2\2i\u0182\3\2"+
		"\2\2k\u0186\3\2\2\2m\u018a\3\2\2\2o\u018e\3\2\2\2q\u0192\3\2\2\2s\u0199"+
		"\3\2\2\2u\u01a5\3\2\2\2w\u01aa\3\2\2\2yz\7\17\2\2z}\7\f\2\2{}\t\2\2\2"+
		"|y\3\2\2\2|{\3\2\2\2}~\3\2\2\2~\177\b\2\2\2\177\6\3\2\2\2\u0080\u0082"+
		"\t\3\2\2\u0081\u0080\3\2\2\2\u0082\u0083\3\2\2\2\u0083\u0081\3\2\2\2\u0083"+
		"\u0084\3\2\2\2\u0084\u0085\3\2\2\2\u0085\u0086\b\3\2\2\u0086\b\3\2\2\2"+
		"\u0087\u0088\7n\2\2\u0088\u0089\7g\2\2\u0089\u008a\7v\2\2\u008a\n\3\2"+
		"\2\2\u008b\u008c\7o\2\2\u008c\u008d\7w\2\2\u008d\u008e\7v\2\2\u008e\f"+
		"\3\2\2\2\u008f\u0090\7h\2\2\u0090\u0091\7p\2\2\u0091\16\3\2\2\2\u0092"+
		"\u0093\7t\2\2\u0093\u0094\7g\2\2\u0094\u0095\7v\2\2\u0095\u0096\7w\2\2"+
		"\u0096\u0097\7t\2\2\u0097\u0098\7p\2\2\u0098\20\3\2\2\2\u0099\u009a\7"+
		"k\2\2\u009a\u009b\7h\2\2\u009b\22\3\2\2\2\u009c\u009d\7g\2\2\u009d\u009e"+
		"\7n\2\2\u009e\u009f\7u\2\2\u009f\u00a0\7g\2\2\u00a0\24\3\2\2\2\u00a1\u00a2"+
		"\7h\2\2\u00a2\u00a3\7q\2\2\u00a3\u00a4\7t\2\2\u00a4\26\3\2\2\2\u00a5\u00a6"+
		"\7h\2\2\u00a6\u00a7\7q\2\2\u00a7\u00a8\7t\2\2\u00a8\u00a9\7g\2\2\u00a9"+
		"\u00aa\7c\2\2\u00aa\u00ab\7e\2\2\u00ab\u00ac\7j\2\2\u00ac\30\3\2\2\2\u00ad"+
		"\u00ae\7n\2\2\u00ae\u00af\7q\2\2\u00af\u00b0\7q\2\2\u00b0\u00b1\7r\2\2"+
		"\u00b1\32\3\2\2\2\u00b2\u00b3\7y\2\2\u00b3\u00b4\7j\2\2\u00b4\u00b5\7"+
		"k\2\2\u00b5\u00b6\7n\2\2\u00b6\u00b7\7g\2\2\u00b7\34\3\2\2\2\u00b8\u00b9"+
		"\7d\2\2\u00b9\u00ba\7t\2\2\u00ba\u00bb\7g\2\2\u00bb\u00bc\7c\2\2\u00bc"+
		"\u00bd\7m\2\2\u00bd\36\3\2\2\2\u00be\u00bf\7e\2\2\u00bf\u00c0\7q\2\2\u00c0"+
		"\u00c1\7p\2\2\u00c1\u00c2\7v\2\2\u00c2\u00c3\7k\2\2\u00c3\u00c4\7p\2\2"+
		"\u00c4\u00c5\7w\2\2\u00c5\u00c6\7g\2\2\u00c6 \3\2\2\2\u00c7\u00c8\7v\2"+
		"\2\u00c8\u00c9\7t\2\2\u00c9\u00ca\7c\2\2\u00ca\u00cb\7k\2\2\u00cb\u00cc"+
		"\7v\2\2\u00cc\"\3\2\2\2\u00cd\u00ce\7u\2\2\u00ce\u00cf\7v\2\2\u00cf\u00d0"+
		"\7t\2\2\u00d0\u00d1\7w\2\2\u00d1\u00d2\7e\2\2\u00d2\u00d3\7v\2\2\u00d3"+
		"$\3\2\2\2\u00d4\u00d5\7t\2\2\u00d5\u00d6\7g\2\2\u00d6\u00d7\7e\2\2\u00d7"+
		"\u00d8\7q\2\2\u00d8\u00d9\7t\2\2\u00d9\u00da\7f\2\2\u00da&\3\2\2\2\u00db"+
		"\u00df\t\4\2\2\u00dc\u00de\t\5\2\2\u00dd\u00dc\3\2\2\2\u00de\u00e1\3\2"+
		"\2\2\u00df\u00dd\3\2\2\2\u00df\u00e0\3\2\2\2\u00e0(\3\2\2\2\u00e1\u00df"+
		"\3\2\2\2\u00e2\u00e4\t\6\2\2\u00e3\u00e2\3\2\2\2\u00e4\u00e5\3\2\2\2\u00e5"+
		"\u00e3\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6*\3\2\2\2\u00e7\u00e9\t\6\2\2"+
		"\u00e8\u00e7\3\2\2\2\u00e9\u00ea\3\2\2\2\u00ea\u00e8\3\2\2\2\u00ea\u00eb"+
		"\3\2\2\2\u00eb\u00ec\3\2\2\2\u00ec\u00f9\7h\2\2\u00ed\u00ef\t\6\2\2\u00ee"+
		"\u00ed\3\2\2\2\u00ef\u00f0\3\2\2\2\u00f0\u00ee\3\2\2\2\u00f0\u00f1\3\2"+
		"\2\2\u00f1\u00f2\3\2\2\2\u00f2\u00f4\7\60\2\2\u00f3\u00f5\t\6\2\2\u00f4"+
		"\u00f3\3\2\2\2\u00f5\u00f6\3\2\2\2\u00f6\u00f4\3\2\2\2\u00f6\u00f7\3\2"+
		"\2\2\u00f7\u00f9\3\2\2\2\u00f8\u00e8\3\2\2\2\u00f8\u00ee\3\2\2\2\u00f9"+
		",\3\2\2\2\u00fa\u00fb\7-\2\2\u00fb.\3\2\2\2\u00fc\u00fd\7/\2\2\u00fd\60"+
		"\3\2\2\2\u00fe\u00ff\7,\2\2\u00ff\62\3\2\2\2\u0100\u0101\7\61\2\2\u0101"+
		"\64\3\2\2\2\u0102\u0103\7?\2\2\u0103\66\3\2\2\2\u0104\u0105\7*\2\2\u0105"+
		"8\3\2\2\2\u0106\u0107\7+\2\2\u0107:\3\2\2\2\u0108\u0109\7<\2\2\u0109<"+
		"\3\2\2\2\u010a\u010b\7=\2\2\u010b>\3\2\2\2\u010c\u010d\7.\2\2\u010d@\3"+
		"\2\2\2\u010e\u010f\7}\2\2\u010fB\3\2\2\2\u0110\u0111\7\177\2\2\u0111D"+
		"\3\2\2\2\u0112\u0113\7$\2\2\u0113\u0114\3\2\2\2\u0114\u0115\b\"\3\2\u0115"+
		"F\3\2\2\2\u0116\u0117\7\61\2\2\u0117\u0118\7\61\2\2\u0118\u0119\7\61\2"+
		"\2\u0119\u011d\3\2\2\2\u011a\u011c\n\2\2\2\u011b\u011a\3\2\2\2\u011c\u011f"+
		"\3\2\2\2\u011d\u011b\3\2\2\2\u011d\u011e\3\2\2\2\u011eH\3\2\2\2\u011f"+
		"\u011d\3\2\2\2\u0120\u0121\7\61\2\2\u0121\u0122\7\61\2\2\u0122\u0126\3"+
		"\2\2\2\u0123\u0125\n\2\2\2\u0124\u0123\3\2\2\2\u0125\u0128\3\2\2\2\u0126"+
		"\u0124\3\2\2\2\u0126\u0127\3\2\2\2\u0127\u0129\3\2\2\2\u0128\u0126\3\2"+
		"\2\2\u0129\u012a\b$\4\2\u012aJ\3\2\2\2\u012b\u012c\13\2\2\2\u012cL\3\2"+
		"\2\2\u012d\u012e\7^\2\2\u012e\u012f\7$\2\2\u012fN\3\2\2\2\u0130\u0131"+
		"\7^\2\2\u0131\u0132\7^\2\2\u0132P\3\2\2\2\u0133\u0134\7^\2\2\u0134\u0135"+
		"\7p\2\2\u0135R\3\2\2\2\u0136\u0137\7$\2\2\u0137\u0138\3\2\2\2\u0138\u0139"+
		"\b)\5\2\u0139T\3\2\2\2\u013a\u013b\7^\2\2\u013b\u013c\7&\2\2\u013c\u013d"+
		"\7^\2\2\u013d\u013e\7}\2\2\u013e\u013f\3\2\2\2\u013f\u0140\b*\6\2\u0140"+
		"V\3\2\2\2\u0141\u0143\n\7\2\2\u0142\u0141\3\2\2\2\u0143\u0144\3\2\2\2"+
		"\u0144\u0142\3\2\2\2\u0144\u0145\3\2\2\2\u0145X\3\2\2\2\u0146\u0147\13"+
		"\2\2\2\u0147\u0148\3\2\2\2\u0148\u0149\b,\7\2\u0149Z\3\2\2\2\u014a\u014b"+
		"\7\177\2\2\u014b\u014c\3\2\2\2\u014c\u014d\b-\5\2\u014d\\\3\2\2\2\u014e"+
		"\u0150\t\3\2\2\u014f\u014e\3\2\2\2\u0150\u0151\3\2\2\2\u0151\u014f\3\2"+
		"\2\2\u0151\u0152\3\2\2\2\u0152\u0153\3\2\2\2\u0153\u0154\b.\2\2\u0154"+
		"\u0155\b.\b\2\u0155^\3\2\2\2\u0156\u0157\7n\2\2\u0157\u0158\7g\2\2\u0158"+
		"\u0159\7v\2\2\u0159\u015a\3\2\2\2\u015a\u015b\b/\t\2\u015b`\3\2\2\2\u015c"+
		"\u015d\7o\2\2\u015d\u015e\7w\2\2\u015e\u015f\7v\2\2\u015f\u0160\3\2\2"+
		"\2\u0160\u0161\b\60\n\2\u0161b\3\2\2\2\u0162\u0164\t\6\2\2\u0163\u0162"+
		"\3\2\2\2\u0164\u0165\3\2\2\2\u0165\u0163\3\2\2\2\u0165\u0166\3\2\2\2\u0166"+
		"\u0167\3\2\2\2\u0167\u0168\b\61\13\2\u0168d\3\2\2\2\u0169\u016b\t\6\2"+
		"\2\u016a\u0169\3\2\2\2\u016b\u016c\3\2\2\2\u016c\u016a\3\2\2\2\u016c\u016d"+
		"\3\2\2\2\u016d\u016e\3\2\2\2\u016e\u0175\7h\2\2\u016f\u0171\t\6\2\2\u0170"+
		"\u016f\3\2\2\2\u0171\u0172\3\2\2\2\u0172\u0170\3\2\2\2\u0172\u0173\3\2"+
		"\2\2\u0173\u0175\3\2\2\2\u0174\u016a\3\2\2\2\u0174\u0170\3\2\2\2\u0175"+
		"\u0176\3\2\2\2\u0176\u0178\7\60\2\2\u0177\u0179\t\6\2\2\u0178\u0177\3"+
		"\2\2\2\u0179\u017a\3\2\2\2\u017a\u0178\3\2\2\2\u017a\u017b\3\2\2\2\u017b"+
		"\u017c\3\2\2\2\u017c\u017d\b\62\f\2\u017df\3\2\2\2\u017e\u017f\7-\2\2"+
		"\u017f\u0180\3\2\2\2\u0180\u0181\b\63\r\2\u0181h\3\2\2\2\u0182\u0183\7"+
		"/\2\2\u0183\u0184\3\2\2\2\u0184\u0185\b\64\16\2\u0185j\3\2\2\2\u0186\u0187"+
		"\7,\2\2\u0187\u0188\3\2\2\2\u0188\u0189\b\65\17\2\u0189l\3\2\2\2\u018a"+
		"\u018b\7\61\2\2\u018b\u018c\3\2\2\2\u018c\u018d\b\66\20\2\u018dn\3\2\2"+
		"\2\u018e\u018f\7*\2\2\u018f\u0190\3\2\2\2\u0190\u0191\b\67\21\2\u0191"+
		"p\3\2\2\2\u0192\u0193\7+\2\2\u0193\u0194\3\2\2\2\u0194\u0195\b8\r\2\u0195"+
		"r\3\2\2\2\u0196\u0198\t\b\2\2\u0197\u0196\3\2\2\2\u0198\u019b\3\2\2\2"+
		"\u0199\u0197\3\2\2\2\u0199\u019a\3\2\2\2\u019a\u019c\3\2\2\2\u019b\u0199"+
		"\3\2\2\2\u019c\u01a0\t\t\2\2\u019d\u019f\t\5\2\2\u019e\u019d\3\2\2\2\u019f"+
		"\u01a2\3\2\2\2\u01a0\u019e\3\2\2\2\u01a0\u01a1\3\2\2\2\u01a1\u01a3\3\2"+
		"\2\2\u01a2\u01a0\3\2\2\2\u01a3\u01a4\b9\22\2\u01a4t\3\2\2\2\u01a5\u01a6"+
		"\7$\2\2\u01a6\u01a7\3\2\2\2\u01a7\u01a8\b:\23\2\u01a8\u01a9\b:\3\2\u01a9"+
		"v\3\2\2\2\u01aa\u01ab\13\2\2\2\u01ab\u01ac\3\2\2\2\u01ac\u01ad\b;\7\2"+
		"\u01adx\3\2\2\2\30\2\3\4|\u0083\u00df\u00e5\u00ea\u00f0\u00f6\u00f8\u011d"+
		"\u0126\u0144\u0151\u0165\u016c\u0172\u0174\u017a\u0199\u01a0\24\2\4\2"+
		"\7\3\2\2\5\2\6\2\2\7\4\2\t&\2\t\4\2\t\5\2\t\6\2\t\25\2\t\26\2\t\27\2\t"+
		"\30\2\t\31\2\t\32\2\t\34\2\t\24\2\t#\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}