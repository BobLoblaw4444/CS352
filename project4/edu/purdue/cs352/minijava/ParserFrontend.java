package edu.purdue.cs352.minijava;

import edu.purdue.cs352.minijava.ast.*;
import edu.purdue.cs352.minijava.parser.*;

public class ParserFrontend {
    public static void main(String[] args) throws ParseException {
        ParserAST parser;
        Program prog;
        ASTToSExp sexp;

        if (args.length != 1) {
            System.out.println("Use: mjparse-ast <input file>");
            return;
        }

        try {
            parser = new ParserAST(new java.io.FileInputStream(args[0]));
        } catch (java.io.FileNotFoundException ex) {
            System.out.println("File " + args[0] + " not found.");
            return;
        }

        prog = parser.Program();
        sexp = new ASTToSExp();

        System.out.println(prog.accept(sexp));
    }
}
