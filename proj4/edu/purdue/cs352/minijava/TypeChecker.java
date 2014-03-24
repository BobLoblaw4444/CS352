package edu.purdue.cs352.minijava;

import java.util.*;

import edu.purdue.cs352.minijava.ast.*;
import edu.purdue.cs352.minijava.ssa.*;
import edu.purdue.cs352.minijava.types.*;

public class TypeChecker {
    SSAProgram prog;
    Map<String, StaticType> types;

    public TypeChecker(SSAProgram prog) 
    {
        this.prog = prog;
        types = new HashMap<String, StaticType>();
        types.put("Object", new ObjectType("Object", null));
    }

    public void typeCheck() 
    {
        // FILLIN
        // (Hint: Create types, then assign types to SSAFields/SSAMethods, then check types in SSAStatements)
	
	// Add Primitive types
	types.put("IntType", new PrimitiveType.IntType());
	types.put("BooleanType", new PrimitiveType.BooleanType());
	types.put("VoidType", new VoidType());
	types.put("IntTypeArray", new ObjectType("IntTypeArray", (ObjectType)types.get("Object")));
			    
	// Add class types to map
	try
	{
	    for(SSAClass cl : prog.getClassesOrdered())
	    {
		String className = cl.getASTNode().getName();

		if(types.get(className) == null)
		{
    		    SSAClass supercl = cl.superclass(prog);

		    // Add class type to map
		    if(supercl != null)
			types.put(className, new ObjectType(className, (ObjectType)types.get(supercl.getASTNode().getName())));
		    else
			types.put(className, (ObjectType)types.get("Object"));
		    
		}
	//	else
	//	    throw new TypeException();

		// Set field types 
		for(SSAField field : cl.getFieldsOrdered())
		{
		    field.setType(GetStaticType(field.getField().getType()));
	    	}

		// Set method return types and parameter types
		for(SSAMethod method : cl.getMethodsOrdered())
		{
		    method.setRetType(GetStaticType(method.getMethod().getType()));

		    ArrayList<StaticType> params = new ArrayList<StaticType>();
		    for(Parameter parameter : method.getMethod().getParameters())
			params.add(GetStaticType(parameter.getType()));

		    method.setParamTypes(params);
		
		    // Type check SSAStatements in method body
		    for(SSAStatement statement : method.getBody())
		    {
			StaticType leftType, rightType; 
			
			switch(statement.getOp())
			{
			    case Unify:
				break;
			    case Alias:
				statement.setType(statement.getRight().getType());
				break;
			    case This:
				statement.setType(types.get(className));
    				break;
			    case Parameter:
    				break;
			    case Arg:
				statement.setType(statement.getLeft().getType());
    				break;
			    case Null:
    				break;
			    
			    // Primitive types
			    case Int:
				statement.setType(types.get("IntType"));
    				break;
			    case Boolean:
				statement.setType(types.get("BooleanType"));
    				break;

			    // New objects
			    case NewObj:
    				break;
			    case NewIntArray:
				leftType = statement.getLeft().getType();
				
				if(!leftType.equals(types.get("IntType")))
				    throw new TypeException("Left operand to newIntArray must be int. Found: " + leftType);
				statement.setType(types.get("IntTypeArray"));
    				break;
			
			    // Control flow
			    case Label:
				statement.setType(types.get("VoidType"));
    				break;
			    case Goto:
				statement.setType(types.get("VoidType"));
    				break;
			    case Branch:
			    	leftType = statement.getLeft().getType();
			
				if(!leftType.equals(types.get("BooleanType")))
				    throw new TypeException("Branch condition expected boolean but found " + leftType);
				statement.setType(types.get("VoidType"));
    				break;
			    case NBranch:
			    	leftType = statement.getLeft().getType();

				if(!leftType.equals(types.get("BooleanType")))
				    throw new TypeException("NBranch condition expected boolean but found " + leftType);
			        statement.setType(types.get("VoidType"));
    				break;

			    // Function call
			    case Call:
    				break;
			    case Print:
			    	leftType = statement.getLeft().getType();
			       
			       if(!leftType.equals(types.get("IntType")))
				    throw new TypeException("Print target must be int but found " + leftType);
			        statement.setType(types.get("VoidType"));
    				break;
			    case Return:
    				break;

			    // Member and index access
			    case Member:
				
    				break;

			    case Index:
			    	leftType = statement.getLeft().getType();
				rightType = statement.getRight().getType();
			        
				if(!leftType.equals(types.get("IntTypeArray"))
				    || !rightType.equals(types.get("IntType")))
				    throw new TypeException("Invalid operands for index expression: " + leftType + " " + rightType);
			        statement.setType(types.get("IntType"));
    				break;

			    // Assignment
			    case VarAssg:
    				break;
			    case IndexAssg:
			    	leftType = statement.getLeft().getType();
				rightType = statement.getRight().getType();

			        if(!leftType.equals(types.get("IntTypeArray"))
				    || !rightType.equals(types.get("IntType")))
				    throw new TypeException("Invalid index assignment operands: " + leftType + " " + rightType);
			        statement.setType(types.get("IntType"));
    				break;
			    
			    // Not
			    case Not:
			    	leftType = statement.getLeft().getType();

			        if(!leftType.equals(types.get("BooleanType")))
				    throw new TypeException("Not value must be boolean but found " + leftType);
			        statement.setType(types.get("BooleanType"));
    				break;

			    // Binary Expressions	
			    case Lt:
			    case Le:
			    case Gt:
			    case Ge:
			    	leftType = statement.getLeft().getType();
				rightType = statement.getRight().getType();
				
				if(statement.getLeft().getType() == null)
				    System.out.println("balls");

				if(!leftType.equals(types.get("IntType"))
				    || !rightType.equals(types.get("IntType")))
				    throw new TypeException("Invalid operand type for comparitive operator "+ leftType + " " + statement.getOp() + " " + rightType);
				statement.setType(types.get("BooleanType"));    
				break;
			    case Eq:
			    case Ne:
    				statement.setType(types.get("BooleanType"));   			    
				break;
    			    case And:
			    case Or:
			    	leftType = statement.getLeft().getType();
				rightType = statement.getRight().getType();

			    	if(!leftType.equals(types.get("BooleanType"))
				    || !rightType.equals(types.get("BooleanType")))
				    throw new TypeException("Invalid operand for logic operator: "+ leftType + " " + statement.getOp() + " " + rightType);
				statement.setType(types.get("BooleanType"));   
				break;
			    case Plus:
			    case Minus:
			    case Mul:
			    case Div:
			    case Mod:
			    	leftType = statement.getLeft().getType();
				rightType = statement.getRight().getType();

				if(!leftType.equals(types.get("IntType"))
				    || !rightType.equals(types.get("IntType")))
				    throw new TypeException("Invalid operand for arithmetic operator: "+ leftType + " " + statement.getOp() + " " + rightType);
				statement.setType(types.get("IntType"));   			    
    				break;
			}
		    }
		}
	    }

	    

	    // DEBUG
	    printMap();
	}
	catch(TypeException e)
	{
	    System.out.println(e.message);
	    System.exit(1);
	}
    }

    private StaticType GetStaticType(Type type)
    {
	// Int
        if(type instanceof TypeInt)
	    return types.get("IntType");
		    
        // Boolean
        else if(type instanceof TypeBoolean)
	    return types.get("BooleanType");
		    
	// Int Array
	else if(type instanceof TypeIntArray)
	    return types.get("IntTypeArray");
	
	// Void
	else
	    return types.get("VoidType");
    }

    // Debug method to print out the type names in map
    public void printMap()
    {
	System.out.println("Type Table:");
	System.out.println("***************************");

	for(Map.Entry<String, StaticType> type : types.entrySet())
	    System.out.println(type.getKey());
    
	System.out.println("***************************");
    }
}

class TypeException extends Exception
{
    String message;
    TypeException(String message)
    {
	this.message = message;
    }
};
