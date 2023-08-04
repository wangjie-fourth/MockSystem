package org.github.fourth.mocksystem.integrationtest.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AppTest {

    @Mock
    private App app;

    @Test
    public void test() {

        when(app.sayHello(anyString())).thenReturn("mock success");
        System.out.println(app.sayHello("world"));



        when(app.sayHello(anyString())).thenReturn("mock again success");
        System.out.println(app.sayHello("xxx"));
    }

}
