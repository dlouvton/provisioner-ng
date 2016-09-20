package com.dlouvton.badger.itest.common;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

public class SeleniumTest extends BaseTest {
	protected WebDriver driver;

	public SeleniumTest() {
		DesiredCapabilities desiredCapabilities = DesiredCapabilities
				.htmlUnit();
		desiredCapabilities.setCapability(HtmlUnitDriver.INVALIDSELECTIONERROR,
				true);
		desiredCapabilities.setCapability(HtmlUnitDriver.INVALIDXPATHERROR,
				false);
		desiredCapabilities.setJavascriptEnabled(true);
		driver = new HtmlUnitDriver(desiredCapabilities);
	}

	public WebDriver getWebDriver(String url) {
		driver.get(url);
		return driver;
	}

	public WebDriver getWebDriver() {
		return driver;
	}
}