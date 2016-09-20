package com.dlouvton.badger.test;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.dlouvton.badger.util.MongoManager;

public class MongoManagerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	DBCollection collection;

	MongoManager manager;

	/**
	 * the tests use a mock collection to avoid uploading to a real mongo host. 
	 * for tests to work, the collection.find().count() was stubbed to return 5
	 * (the value, 5, doesn't matter... could be any number so stub works) 
	 */
	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		manager = new MongoManager(collection);
		when(collection.find().count()).thenReturn(5);
	}

	/**
	 * The Manager is not throwing UnknownHostException when the mongo host isn't
	 * accessible, but instead prints a log message
	 * This test verifies that there are no throwables
	 */
	@Test
	public void testUnknownHost()  {
		manager = new MongoManager("dummy");
	}

	/**
	 * The testng results xml is being converted to DBObject on mongo
	 * @throws IOException 
	 * 
	 * @result a DBObject is being generated successfully
	 */
	@Test
	public void testConvertXml() throws IOException {
		DBObject dbObject = manager.convertXmlResultsToMongoDocument("src/resources/sample.xml", null);
		assertTrue(dbObject.containsField("_hostname"));
		assertEquals(dbObject.toMap().size(), 8);
	}

	/**
	 * The testng results xml string is being converted to json so it can be
	 * uploaded
	 * 
	 * @result a json string
	 */
	@Test
	public void testToJSON() {
		assertEquals(
				MongoManager.toJSON("<root><a b=\"1\" c=\"2\"></a></root>"),
				"{\"a\": {\n  _b: \"1\",\n  _c: \"2\"\n}}");
	}

}
