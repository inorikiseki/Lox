package com.interpreter.lox;

import static com.interpreter.lox.TokenType.*;
// static import makes it no need to type 'TokenType' anymore.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0 ;
	private int current = 0;
	private int line =1;
	private static final Map<String,TokenType> keywords;
	static {
		keywords= new HashMap<>();
		keywords.put("and", AND);
		keywords.put("class", CLASS);
		keywords.put("else", ELSE);
		keywords.put("false", FALSE);
		keywords.put("for", FOR);
		keywords.put("fun", FUN);
		keywords.put("if", IF);
		keywords.put("nil", NIL);
		keywords.put("or", OR);
		keywords.put("print", PRINT);
		keywords.put("return", RETURN);
		keywords.put("super", SUPER);
		keywords.put("this", THIS);
		keywords.put("true", TRUE);
		keywords.put("var", VAR);
		keywords.put("while", WHILE);
		keywords.put("break", BREAK);
	}
	
	Scanner(String source){
		this.source = source;
	}
	
	public List<Token> scanTokens(){
		while(!isAtEnd()) {
			// we are at the beginning of the next lexeme.
			start = current ;
			scanToken();
		}
		tokens.add(new Token(TokenType.EOF,"",null,line));
		return tokens;
	}
	
	private void scanToken() {
		char c = advance();
		switch(c) {
		case '(': addToken(LEFT_PAREN); break;
		case ')': addToken(RIGHT_PAREN); break;
		case '{': addToken(LEFT_BRACE); break;
		case '}': addToken(RIGHT_BRACE); break;
		case ',': addToken(COMMA); break;
		case '.': addToken(DOT); break;
		case '-': addToken(MINUS); break;
		case '+': addToken(PLUS); break;
		case ';': addToken(SEMICOLON); break;
		case '*': addToken(STAR); break;
		case '!':
			addToken(match('=')?BANG_EQUAL:BANG);
			break;
		case '=': 
			addToken(match('=')?EQUAL_EQUAL:EQUAL);
			break;
		case '<':
			addToken(match('=')?LESS_EQUAL:LESS);
			break;
		case '>':
			addToken(match('=')?GREATER_EQUAL:GREATER);
			break;
		case '/':
			if (match('/')) {
				while(peek() != '\n' && !isAtEnd()) advance();
			}else if(match('*')){
				multiComment();
			}else
			{
				addToken(SLASH);
			}
			break;
		case ' ':
		case '\r':
		case '\t':
			break;
		case '\n':
			line++;
			break;
		case '"': 
			if(isMultiString()) multiString();
			else string();
			break;
		default:
			if (isDigit(c)) {
				number();
			}else if(isAlpha(c)) {
				identifier();
			}else{
				Lox.error(line, "Unexpected character.");				
			}
			break;
		}
	}

	private boolean isMultiString() {
		if(peek()!='"')return false;
		if(peekNext()=='"') {
			advance();
			advance();
			return true;
		}
		return false;
	}

	private void multiString() {
		while(!endMultiString() && !isAtEnd()) {
			if (peek()=='\n') line++;
			advance();
		}
		
		if (isAtEnd()) {
			Lox.error(line, "Unterminated Multi-String");
		}
		
		boolean isMSCompleted = true;
		for(int i = 0; i<3; i++) {
			if (isAtEnd()) {
				isMSCompleted=false;
				break;
			}
			advance();
		}
		
		if(isMSCompleted){
			String value = source.substring(start+3,current-3);
			addToken(MULTISTRING,value);
		}else {
			addToken(MULTISTRING);
		}
		
	}

	private boolean endMultiString() {
		if(peek()!='"')return false;
		if(peekNext()!='"')return false;
		if(source.charAt(current+2)!='"') return false;
		return true;
	}

	private void multiComment() {
		while(!endMultiComment() && !isAtEnd()) {
			if (peek()=='\n') line++;
			advance();
		}
		
		if (isAtEnd()) {
			Lox.error(line, "Unterminated Multi-Comment");
		}
		for(int i = 0; i<2; i++) {
			if (isAtEnd()) break;
			advance();
		}
		
		addToken(MULTICOMMENT);
	}

	private boolean endMultiComment() {
		if (peek()!= '*') return false;
		if (peekNext()=='/') return true;
		else return false;
	}

	private void identifier() {
		while(isAlphaNumeric(peek())) advance();
		String text = source.substring(start,current);
		TokenType type = keywords.get(text);
		if (type == null ) type = IDENTIFIER;
		addToken(type);
		
	}

	private boolean isAlphaNumeric(char c) {
		return isAlpha(c)||isDigit(c);
	}

	private boolean isAlpha(char c) {
		return (c>='a' && c <= 'z')||(c >= 'A' && c <= 'Z')|| c == '_';
	}

	private void number() {
		while(isDigit(peek())) advance();
		if (peek()=='.' && isDigit(peekNext())){
			advance();
			
			while(isDigit(peek())) advance();
		}
		addToken(NUMBER,Double.parseDouble(
				source.substring(start,current)));
		
	}

	private char peekNext() {
		if(current + 1>= source.length()) return '\0';
		return source.charAt(current+1);
	}

	private boolean isDigit(char c) {
		return c>='0' && c<='9';
	}

	private void string() {
		while(peek()!='"' && !isAtEnd()) {
			if(peek()== '\n') line++;
			advance();
		}
		
		if(isAtEnd()) {
			Lox.error(line,"Unterminated string.");
			return;
		}
		
		advance();
		String value = source.substring(start+1,current-1);//omit ""
		addToken(STRING,value);
	}

	//lookahead
	private char peek() {
		if (isAtEnd()) return '\0';
		return source.charAt(current);
	}

	private boolean match(char expected) {
		if (isAtEnd()) return false;
		if (source.charAt(current)!=expected)return false;
		current++;
		return true;
	}

	private void addToken(TokenType type) {
		addToken(type,null);
	}

	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start,current);
		tokens.add(new Token(type,text,literal,line));	
	}

	private char advance() {
		return source.charAt(current++);
	}

	private boolean isAtEnd() {
		return current >= source.length();
	}
	
}
