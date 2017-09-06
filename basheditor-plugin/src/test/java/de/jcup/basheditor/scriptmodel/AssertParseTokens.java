package de.jcup.basheditor.scriptmodel;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssertParseTokens {

	private List<ParseToken> parseTokens;

	/**
	 * Ensures given list is not null and returns dedicated assert object
	 * @param parseTokens
	 * @return assert object
	 */
	public static AssertParseTokens assertThat(List<ParseToken> parseTokens){
		return new AssertParseTokens(parseTokens);
	}
	
	public AssertParseTokens(List<ParseToken> parseTokens) {
		assertNotNull("Parse tokens may not be null!", parseTokens);
		this.parseTokens=parseTokens;
	}
	
	public AssertParseTokens containsOneToken(String text){
		return containsToken(text,1);
	}
	
	public AssertParseTokens containsNotToken(String text){
		return containsToken(text,0);
	}
	
	public AssertParseTokens containsToken(String text, int expectedAmount){
		int count =0;
		for (ParseToken token: parseTokens){
			if (text.equals(token.text)){
				count++;
			}
		}
		if (expectedAmount!=count){
			assertEquals("The token amount for '"+text+"' is not as expected", expectedAmount,count);
		}
		return this;
	}
	
	public AssertParseTokens containsTokens(String ...tokens){
		List<String> found = new ArrayList<String>();
		for (ParseToken token: parseTokens){
			found.add(token.text);
		} 
		if (tokens.length != found.size()){
			fail("tokens length differ!\nexpected tokens:"+Arrays.asList(tokens)+"\nfound tokens:"+found);
		}
		assertArrayEquals("Tokens not as in expected way",tokens, found.toArray());
		return this;
	}
	
	
}