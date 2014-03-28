package org.jdrupes.core.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({org.jdrupes.core.test.basic.AllTests.class,
			   org.jdrupes.core.test.core.AllTests.class})
public class AllTests {

}
