package org.reactome.server.analysis.tools.components;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-config-test.xml")
public class SpringAppTests {
//    @Autowired
//    private TestService testService;

    @Test
    public void testSayHello() {
//        Assert.assertEquals("Hello world!", testService.g());
    }
}
