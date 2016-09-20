package com.dlouvton.badger.test;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.dlouvton.badger.util.MongoManager;
import com.dlouvton.badger.util.Utils;

public class UtilTest {

	@Before
	public void setup() {
	}

	@Test
	public void testJsonConversion() throws IOException {
		String xml = Utils.fileToString("src/resources/sample.xml");
		assertTrue(MongoManager.toJSON(xml).contains("_timestamp"));
	}
	
	/**
	 * The testng results xml file is being parsed
	 * 
	 * @result a String containing the xml data
	 */
	@Test
	public void testXmlToString() throws IOException {
		assertEquals(Utils.fileToString("src/resources/simpleXml.xml"),
				"<root><a b=1 c=2/></root>");

	}
}
