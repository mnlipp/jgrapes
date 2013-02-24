package org.jdrupes.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({org.jdrupes.test.basic.AllTests.class,
			   org.jdrupes.test.core.AllTests.class})
public class AllTests {

}
