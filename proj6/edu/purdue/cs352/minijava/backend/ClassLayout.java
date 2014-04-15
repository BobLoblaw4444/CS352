package edu.purdue.cs352.minijava.backend;

import java.util.*;

import edu.purdue.cs352.minijava.ssa.*;

public class ClassLayout {
		
    // get the number of fields in an instance of this object
    public static int objectFields(SSAProgram prog, SSAClass cl) {
        // FILLIN
		Set<String> totalFieldSet = new HashSet<String>();
		
		while(cl != null)
		{			
			for(SSAField field : cl.getFieldsOrdered())
			{
				/*if(!totalFieldList.contains(field.getKey()))
				{
					totalFieldList.add(field.getKey());	
				}*/
				totalFieldSet.add(field.getName());				
			}
			
			cl = cl.superclass(prog);
		}
		
		return totalFieldSet.size();
    }

    // get the size of an object (its number of fields plus one for the vtable)
    public static int objectSize(SSAProgram prog, SSAClass cl) {
        // FILLIN
		return objectFields(prog, cl) + 1;
    }

    // get the offset of a field within an object
    public static int fieldOffset(SSAProgram prog, SSAClass cl, String field) {
        // FILLIN
		// Account for Vtable
		int offset = 1;
		
		List<SSAClass> classList = new ArrayList<SSAClass>();
		
		while(cl != null)
		{
			classList.add(cl);
			cl = cl.superclass(prog);
		}
		
		Collections.reverse(classList);
		
		for(SSAClass clazz : classList)
		{
			for(SSAField classField : clazz.getFieldsOrdered())
			{
				if(classField.getName().equals(field))	
				{
					return offset;
				}
				offset++;			
			}	
		}
		return -1;
    }

    // a vtable
    public static class Vtable {
        public final List<String> methods;
        public final Map<String, Integer> methodOffsets;

        public Vtable(List<String> methods) {
            this.methods = methods;

            methodOffsets = new HashMap<String, Integer>();
            int off = 0;
            for (String m : methods)
                methodOffsets.put(m, off++);
        }
    }

    // get the complete vtable layout for this class
    public static Vtable getVtable(SSAProgram prog, SSAClass cl) {
        // FILLIN
		return new Vtable(new ArrayList<String>());
    }

    // get the size of the vtable for a class
    public static int vtableSize(SSAProgram prog, SSAClass cl) {
        // FILLIN
		return 1;
    }

    // for a given method, get the implementing class
    public SSAClass getImplementor(SSAProgram prog, SSAClass cl, String method) {
        // FILLIN
		return cl;
    }
}
