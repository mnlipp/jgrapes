package org.jgrapes.core.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({org.jgrapes.core.test.basic.AllTests.class,
			   org.jgrapes.core.test.core.AllTests.class})
public class AllTests {

}
