package com.dlouvton.badger.test;

import static org.junit.Assert.assertFalse;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.dlouvton.badger.provisioner.model.Component;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.provisioner.model.ModelException;

public class ModelTest {

	Model model;

	@Before
	public void setup() {
		model = new Model(new File("models/a-b-c-d.json"),1);
	}

	@Test
	public void testModel() {
		List<Component> comps = model.getComponents();
		assertEquals(model.getComponentsStr(), "[d__1, c__1, b__1, a__1]");
		assertEquals(comps.size(), 4);
		assertEquals(comps.get(0).name, "d__1");
		assertEquals(comps.get(0).getLocalizedName(), "d");
		assertEquals(comps.get(3).name, "a__1");
	}

	@Test(expected = ModelException.class)
	public void testFileNotFound() {
		model = new Model(new File("src/resources/notfound.json"),1);
	}

	@Test
	public void testProperties() {
		assertEquals(model.getModelParameters().get("name"), "utest");
	}

	@Test
	public void testCompProperties() {
		assertEquals(model.getComponent("c").getProvider(), "dummyManaged");
		assertEquals(model.getComponent("b").getProvider(), "dummy");
		assertEquals(model.getComponent("a").isStatic(), true);
		assertEquals(model.getComponent("c").getAttribute("not_found","not found"), "not found");
	}

	@Test
	public void testDynamicProperties() {
		assertEquals(model.getComponent("c").getProvider(), "dummyManaged");
		assertEquals(model.getComponent("b").getProvider(), "dummy");
		assertEquals(model.getComponent("b").isStatic(), false);
		assertFalse(model.getComponent("c").properties.get("test")
				.contains("RANDOM"));
	}
}
