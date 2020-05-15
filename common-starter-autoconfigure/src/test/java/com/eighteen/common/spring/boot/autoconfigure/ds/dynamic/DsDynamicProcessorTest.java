package com.eighteen.common.spring.boot.autoconfigure.ds.dynamic;

import lombok.var;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.when;

@SpringBootTest
public class DsDynamicProcessorTest {

    DsDynamicProcessor dynamicProcessor;

    @Mock
    MethodInvocation invocation;

    public DsDynamicProcessorTest() {
        dynamicProcessor = new DsDynamicProcessor();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetWxDatasource() {
        when(invocation.getArguments()).thenReturn(new Object[]{new DynamicDataSourceEntity("wx")});
        var ds= dynamicProcessor.doDetermineDatasource(invocation,"");
        Assert.assertEquals("wx",ds);
    }

    @Test
    public void testGetToutiaoDatasource() {
        when(invocation.getArguments()).thenReturn(new Object[]{new DynamicDataSourceEntity("toutiao")});
        var ds= dynamicProcessor.doDetermineDatasource(invocation,"");
        Assert.assertEquals("toutiao",ds);
    }
}