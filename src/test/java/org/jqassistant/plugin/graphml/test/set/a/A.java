package org.jqassistant.plugin.graphml.test.set.a;

import org.jqassistant.plugin.graphml.test.set.b.B;

public class A {

    private B b;

    public void methodA1() {
        b = new B();
    }

    public void methodA2() {
        b.methodB1();
    }

}
