package edu.purdue.cs352.minijava.backend;

import java.util.*;

import edu.purdue.cs352.minijava.ssa.*;
import edu.purdue.cs352.minijava.types.*;

public class AsmMIPS {
    StringBuilder sb;

    int wordSize = 4;

    // registers for MIPS:
    private static final String[] registers = {
        "zero",
        "at",
        "v0", "v1",
        "a0", "a1", "a2", "a3",
        "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
        "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
        "t8", "t9",
        "k0", "k1",
        "gp", "sp", "fp", "ra"
    };

    // registers free for normal use
    private static final int[] freeRegisters = {
        4, 5, 6, 7, // a*
        8, 9, 10, 11, 12, 13, 14, 15, // t*
        16, 17, 18, 19, 20, 21, 22, 23, // s*
        24, 25 // t*
    };

    // pinned registers for arguments:
    private static final int[] argRegisters = {
        4, 5, 6, 7
    };

    // mapping of arg register indexes to free register indexes
    private static final int[] argFreeRegisters = {
        0, 1, 2, 3
    };

    // callee-saved registers, excluding stack/base
    private static final int[] calleeSavedRegisters = {
        16, 17, 18, 19, 20, 21, 22, 23, // s*
        28, // gp
        31, // ra
    };

    // caller-saved registers, excluding stack/base
    private static final int[] callerSavedRegisters = {
        2, 3, // v*
        4, 5, 6, 7, // a*
        8, 9, 10, 11, 12, 13, 14, 15, 24, 25 // t*
    };

    private AsmMIPS(StringBuilder sb) {
        this.sb = sb;
    }

    public static String compile(SSAProgram prog) {
        AsmMIPS compiler = new AsmMIPS(new StringBuilder());

        // SPIM stuff
        compiler.sb.append(
            "main:\n" +
            " jal mj_main\n" +
            " li $v0, 10\n" +
            " syscall\n\n" +

            "minijavaNew:\n" +
            " move $t0, $a0\n" +
            " mul $a0, $a1, 4\n" +
            " li $v0, 9\n" +
            " syscall\n" +
            " sw $t0, ($v0)\n" +
            " j $ra\n\n" +

            "minijavaNewArray:\n" +
            " move $t0, $a0\n" +
            " mul $a0, $a0, 4\n" +
            " add $a0, $a0, 4\n" +
            " li $v0, 9\n" +
            " syscall\n" +
            " sw $t0, ($v0)\n" +
            " j $ra\n\n" +

            ".data\n" +
            ".align 4\n" +
            "minijavaNewline:\n" +
            " .asciiz \"\\n\"\n\n" +

            ".text\n" +
            "minijavaPrint:\n" +
            " li $v0, 1\n" +
            " syscall\n" +
            " la $a0, minijavaNewline\n" +
            " li $v0, 4\n" +
            " syscall\n" +
            " j $ra\n\n"
        ); 

        // first compile main
        compiler.compile(prog, prog.getMain(), "mj_main");

        // then compile all the classes
        for (SSAClass cl : prog.getClassesOrdered()) {
            compiler.compile(prog, cl);
        }

        return compiler.toString();
    }

    // compile this class
    private void compile(SSAProgram prog, SSAClass cl) {
        // first make the vtable for this class
        sb.append(".data\n.align ");
        sb.append(wordSize);
        sb.append("\n");
        sb.append("mj__v_" + cl.getASTNode().getName() + ":\n");
        // FILLIN
		

        // now compile the actual methods
        sb.append(".text\n");
        for (SSAMethod m : cl.getMethodsOrdered()) {
            String name = "mj__m_" + cl.getASTNode().getName() + "_" + m.getMethod().getName();
            compile(prog, m, name);
        }
    }

    // compile this method with this name
    private void compile(SSAProgram prog, SSAMethod m, String name) {
        // beginning of the prologue
        sb.append(name);
        sb.append(":\n");
        sb.append(" add $sp, $sp, -");
        sb.append(wordSize);
        sb.append("\n");
        sb.append(" sw $fp, ($sp)\n");
        sb.append(" move $fp, $sp\n");

        // pin registers
        for (SSAStatement s : m.getBody()) {
            if (s.getOp() == SSAStatement.Op.Parameter) {
                int paramNum = ((Integer) s.getSpecial()).intValue();
                if (paramNum < argRegisters.length)
                    s.pinRegister(argFreeRegisters[paramNum]);
            }

            if (s.getOp() == SSAStatement.Op.Arg) {
                int argNum = ((Integer) s.getSpecial()).intValue();
                if (argNum < argRegisters.length)
                    s.pinRegister(argFreeRegisters[argNum]);
                else
                    s.pinRegister(-1); // pin to -1 to do this at Call time
            }
        }
		
		int originalSize = m.getBody().size();

        // FILLIN: perform register allocation
		RegisterAllocator.alloc(m, freeRegisters.length);

        // FILLIN: figure out how much space we need to reserve for spills
        int spillSpace = m.getBody().size() - originalSize;

        // FILLIN: and perhaps any other space we need to reserve (saved registers?)

        // FILLIN: reserve space
        sb.append(" add $sp, $sp, -");
        sb.append(wordSize*(spillSpace));
        sb.append("\n");

        // FILLIN: save the callee-saved registers, anything else that needs to be saved
		int[] calleeReg = new int[calleeSavedRegisters.length];
		System.arraycopy( calleeSavedRegisters, 0, calleeReg, 0, calleeSavedRegisters.length );
        
		// now write the code
        for (SSAStatement s : m.getBody()) {
            compile(prog, name, s);
        }

        // the epilogue starts here
        sb.append(" .ret_");
        sb.append(name);
        sb.append(":\n");

        // FILLIN: restore the callee-saved registers (anything else?)
		System.arraycopy( calleeReg, 0, calleeSavedRegisters, 0, calleeSavedRegisters.length );
        
        // and the rest of the epilogue
        sb.append(" move $sp, $fp\n");
        sb.append(" lw $fp, ($sp)\n");
        sb.append(" add $sp, $sp, ");
        sb.append(wordSize);
        sb.append("\n");
        sb.append(" j $ra\n");
    }

    // compile this statement (FILLIN: might need more registers, coming from above method)
    private void compile(SSAProgram prog, String methodName, SSAStatement s) {
        // recommended for debuggability:
        sb.append(" # ");
        if (s.getRegister() >= 0)
            sb.append(reg(s));
        sb.append(": ");
        sb.append(s.toString());
        sb.append("\n");

		boolean newLine = true;

        switch (s.getOp()) 
		{
            // FILLIN (this is the actual code generator!)
			
	        case Unify:
				newLine = false;
	        	break;

	        case Alias:
	          break;

	        case This:
	          break;

	        case Parameter:
				newLine = false;
	        	break;

	        case Arg:
				newLine = false;
	        	break;

	        case Null:
				ssaGen("move", s, false, false);
				sb.append("$zero");
	        	break;

	          // Primitive types
	        case Int:
				ssaGen("li", s, false, false);
				sb.append(s.getSpecial());
	        	break;

	        case Boolean:
	          break;

	          // New objects
	        case NewObj:

	          break;

	        case NewIntArray:
	          break;

	          // Control flow
	        case Label:
				sb.append(".");
				sb.append(s.getSpecial());
	        	sb.append(":");
				break;

	        case Goto:
				sb.append("j ");
				sb.append(".");
	        	sb.append(s.getSpecial());
				break;

	        case Branch:
	        case NBranch:
				ssaGen("beq", s, true, false);
				sb.append(".");
				sb.append(s.getSpecial());
	        	break;

	          // Function call
	        case Call:
	          break;

	          // Print
	        case Print:
	          break;

	        case Return:
				sb.append("move ");
				sb.append("$");
				sb.append("v0, ");
				printReg(s);
	        	break;

	          // Member and index access
	        case Member:
	          break;

	        case Index:
	          break;

	          // Assignment
	        case VarAssg:
				newLine = false;
				break;

	        case MemberAssg:
	          break;

	        case IndexAssg:
	          
	          break;

	          // Not
	        case Not:
	          break;

	          // Binary Expressions
	        case Lt:
				ssaGen("slt", s, true, true);
				break;
	        case Le:
	        case Gt:
	        case Ge:
	        	break;

	        case Eq:
	        case Ne:
	          break;

	        case And:
	        case Or:
	          break;

	        case Plus:
				ssaGen("add", s, true, true);
				break;
	        case Minus:
				ssaGen("sub", s, true, true);
				break;
	        case Mul:
				ssaGen("mul", s, true, true);
				break;
	        case Div:
				ssaGen("div", s, true, true);
				break;
	        case Mod:
				ssaGen("mod", s, true, true);
				break;	      
			
            default:
                throw new Error("Implement MIPS compiler for " + s.getOp() + "!");
        }
		
		if(newLine)
		{
			sb.append("\n");
		}
    }

	private void printReg(SSAStatement s)
	{
		sb.append("$");
		sb.append(reg(s));
	}

	private void ssaGen(String command, SSAStatement s, boolean printLeft, boolean printRight)
	{
		sb.append(command);
		sb.append(" ");
		printReg(s);
		sb.append(", ");
		
		if(printLeft)
		{
			printReg(s.getLeft());
			sb.append(", ");
		}
		
		if(printRight)
		{
			printReg(s.getRight());
			//sb.append(", ");
		}
	}

	private String reg(SSAStatement s)
	{
		return registers[freeRegisters[s.getRegister()]];
	}

    // get the actual code generated
    public String toString() {
        return sb.toString();
    }
}
