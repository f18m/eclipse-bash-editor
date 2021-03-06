package de.jcup.basheditor.script;

import static org.junit.Assert.*;

public class AssertVariable{

    private BashVariable variable;

    AssertVariable(BashVariable variable) {
        this.variable=variable; 
    }

    public AssertVariable withValue(String value) {
        assertEquals("Variable has not expected content", value,variable.getInitialValue());
        return this;
    }
    public AssertVariable hasAssignments(int amount) {
        assertEquals("Variable has not expected assignments",amount,variable.getAssignments().size());
        return this;
    }

    public AssertVariable islocal() {
        assertTrue("Is not local!",variable.isLocal());
        return this;
    }
    
    public AssertVariable isGlobal() {
        assertFalse("Is not global!",variable.isLocal());
        return this;
    }
    
}