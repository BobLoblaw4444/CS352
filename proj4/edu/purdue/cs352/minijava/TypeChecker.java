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
			    
	try
	{
	    // Add class types to map
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
	    }

	    // Set field and method types 
	    for(SSAClass cl : prog.getClassesOrdered())
	    {
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
		  /*  for(SSAStatement statement : method.getBody())
		    {
			DetermineOp(statement);	
		    }
		  */
		}
	    }

	    // Check main statements
	    for (SSAStatement statement : prog.getMain().getBody())
		StatementCheck(statement, null, prog.getMain());

	    // Check method statements
	    for(SSAClass cl : prog.getClassesOrdered())
		for(SSAMethod method : cl.getMethodsOrdered())
		    for(SSAStatement statement : method.getBody())
			StatementCheck(statement, cl, method);

	    // DEBUG
	    printMap();
	}
	catch(TypeException e)
	{
	    System.out.println(e.message);
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    // Switch based on SSAStatement operation
    private void StatementCheck(SSAStatement statement, SSAClass cl, SSAMethod method) throws TypeException
    {
	switch(statement.getOp())
    	{
	    case Unify:
		break;

	    case Alias:
		statement.setType(statement.getRight().getType());
    		break;

	    case This:
		statement.setType(types.get(cl.getASTNode().getName()));
		break;

	    case Parameter:
		statement.setType(types.get("IntType"));
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
		String typeName = (String)statement.getSpecial();
		statement.setType(types.get(typeName));
    		break;

	    case NewIntArray:
		NewIntArrayCheck(statement);
		break;
			
	    // Control flow
	    case Label:
		statement.setType(types.get("VoidType"));
    		break;

	    case Goto:
		statement.setType(types.get("VoidType"));
    		break;
			    
	    case Branch:
	    case NBranch:
		BranchCheck(statement);
		break;

	    // Function call
	    case Call:
		CallCheck(statement);
		break;
			    
	    // Print
	    case Print:
		PrintCheck(statement); 
    		break;

	    case Return:
  		break;

	    // Member and index access
	    case Member:
				
    		break;

	    case Index:
		IndexCheck(statement);
    		break;

	    // Assignment
	    case VarAssg:
		VarAssgCheck(statement, cl, method);
 		break;

	    case IndexAssg:
	    	IndexAssgCheck(statement);
		break;
			    
	    // Not
	    case Not:
	    	NotCheck(statement);
		break;

	    // Binary Expressions	
	    case Lt:
	    case Le:
	    case Gt:
	    case Ge:
	    	CompareCheck(statement);  
		break;

	    case Eq:
	    case Ne:
    		statement.setType(types.get("BooleanType"));   			    
		break;

	    case And:
	    case Or:
	    	LogicCheck(statement);  
		break;

	    case Plus:
	    case Minus:
	    case Mul:
	    case Div:
	    case Mod:
	    	ArithmeticCheck(statement);  			    
    		break;
	}

    }

    // Unify
    private void UnifyCheck(SSAStatement statement)
    {

    }

    // NewIntArray
    private void NewIntArrayCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
				
	if(!leftType.equals(types.get("IntType")))
	{
	    throw new TypeException("Left operand to newIntArray must be int. Found: " + leftType);
	}
				
	statement.setType(types.get("IntTypeArray"));
    }

    // Branch, NBranch
    private void BranchCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();

	if(!leftType.equals(types.get("BooleanType")))
	{
	    throw new TypeException("Branch condition expected boolean but found " + leftType);
	}
	
	statement.setType(types.get("VoidType"));
    }

    // Function calls
    private void CallCheck(SSAStatement statement) throws TypeException
    {
	System.out.println("Name of Method:");	
	SSAClass clazz = prog.getClass(((ObjectType)statement.getLeft().getType()).getName());
	SSACall call = (SSACall)statement.getSpecial();
    
	SSAMethod meth = clazz.getMethod(prog, call.getMethod());
	
	if(meth == null)
	{
	    throw new TypeException("No method exists in class");
	}
	
	int i = 0;
	List<StaticType> paramType = meth.getParamTypes();
	List<StaticType> argType = new ArrayList<StaticType>();

	for(SSAStatement arg : call.getArgs())
	    argType.add(arg.getType());

	while(paramType.get(i) != null && argType.get(i) != null)
	{
	    if(!paramType.get(i).equals(argType.get(i)))
	    {
		throw new TypeException("Invalid arguments to " + call.getMethod());
	    }
	    i++;
        }
	
    }

    // Print
    private void PrintCheck(SSAStatement statement) throws TypeException
    {
	System.out.println("Print Check");
	StaticType leftType = statement.getLeft().getType();
			       
	if(!leftType.equals(types.get("IntType")))
	{
	    throw new TypeException("Print target must be int but found " + leftType);
	}
	
	statement.setType(types.get("VoidType"));
	System.out.println("Type: "+statement.getType());
    }

    // Index
    private void IndexCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
    	StaticType rightType = statement.getRight().getType();
			        
	if(!leftType.equals(types.get("IntTypeArray"))
	    || !rightType.equals(types.get("IntType")))
	{			    
	    throw new TypeException("Invalid operands for index expression: " + leftType + " " + rightType);
	}
	
	statement.setType(types.get("IntType"));
    }

    // VarAssg
    private void VarAssgCheck(SSAStatement statement, SSAClass cl, SSAMethod method) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	StaticType varType = null;
	String varName = (String)statement.getSpecial();

	SSAField var = cl.getField(prog, varName);

	if(var == null)
	{
	    // Search vardecls for the given variable
	    for(VarDecl varDecl : method.getMethod().getVarDecls())
	    {	
		System.out.println("Name: " + varDecl.getName());
		if(varDecl.getName().equals(varName))
		{
		    varType = GetStaticType(varDecl.getType());
		    break;
		}
	    }
	    if(varType == null)
	    {
		// Search parameters for the given variable
		for(Parameter param : method.getMethod().getParameters())
		{	
		    System.out.println("Name: " + param.getName());
		    if(param.getName().equals(varName))
		    {
			varType = GetStaticType(param.getType());
			break;
		    }
		}

		// Throw exception if variable not found
		if(varType == null)
	        {
		  throw new TypeException("Local variable " + varName + " does not exist on line " + statement.getIndex());
		}
	    }
    	}
	else
	{
	    varType = var.getType();
	}

	if(leftType.subtypeOf(varType))
	{
	    statement.setType(varType);    
	}
	else
	{
	    throw new TypeException(leftType + " is not a subtype of " + var.getType());
	}
    }

    // IndexAssg
    private void IndexAssgCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	StaticType rightType = statement.getRight().getType();

	if(!leftType.equals(types.get("IntTypeArray"))
	    || !rightType.equals(types.get("IntType")))
	{
	    throw new TypeException("Invalid index assignment operands: " + leftType + " " + rightType);
	}
	
	statement.setType(types.get("IntType"));
    }

    // Not
    private void NotCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();

	if(!leftType.equals(types.get("BooleanType")))
	{
	    throw new TypeException("Not value must be boolean but found " + leftType);
	}

	statement.setType(types.get("BooleanType"));
    }

    // Lt, Le, Gt, Ge
    private void CompareCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	StaticType rightType = statement.getRight().getType();
				
	if(statement.getLeft().getType() == null)
	    System.out.println("balls");

	if(!leftType.equals(types.get("IntType"))
	    || !rightType.equals(types.get("IntType")))
	{
	    throw new TypeException("Invalid operand type for comparitive operator "+ leftType + " " + statement.getOp() + " " + rightType);
	}
	
	statement.setType(types.get("BooleanType"));  
    }

    //And, Or
    private void LogicCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	StaticType rightType = statement.getRight().getType();

	if(!leftType.equals(types.get("BooleanType"))
	    || !rightType.equals(types.get("BooleanType")))
	{
	    throw new TypeException("Invalid operand for logic operator: "+ leftType + " " + statement.getOp() + " " + rightType);
	}
	
	statement.setType(types.get("BooleanType")); 
    }

    // Mul, Div, Add, Minus, Mod
    private void ArithmeticCheck(SSAStatement statement) throws TypeException
    {
	StaticType leftType = statement.getLeft().getType();
	StaticType rightType = statement.getRight().getType();

	if(!leftType.equals(types.get("IntType"))
	    || !rightType.equals(types.get("IntType")))
	{
	    throw new TypeException("Invalid operand for arithmetic operator: "+ leftType + " " + statement.getOp() + " " + rightType);
	}
	
	statement.setType(types.get("IntType")); 	
    }

    // Helper function to retrieve StaticType
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
