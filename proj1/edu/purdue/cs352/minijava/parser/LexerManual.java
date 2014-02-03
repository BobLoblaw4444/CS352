/* LexerManual.java */
    package edu.purdue.cs352.minijava.parser;

    import java.util.*;
    import java.lang.String;

    class MyToken
    {
        public String id;
        public String lexeme;

        MyToken(String inID, String inLexeme)
        {
            this.id = inID;
            this.lexeme = inLexeme;
        }

    };

    class LexerException extends Exception
    {
        void printError(int lineCount)
        {
            System.out.println("Invalid symbol found on line " + lineCount + " .");
        }
    };

    public class LexerManual
    {
        public static List<MyToken> tokenList = new ArrayList<MyToken>();
        public static int lineCount = 1;
        
        public static void main(String[] args)
        {
            
            java.io.FileInputStream input;
            
            if (args.length != 1)
            {
                System.out.println("Use: mjlex <input file>");
                return;
            }

            try
            {
                input = new java.io.FileInputStream(args[0]);

                String temp = "";
                String sym = "";
                int fileIn;
                boolean seenSymbol = false;
                boolean seenSlash = false;
                boolean seenSingle = false;
                boolean seenMulti = false;
                char lastChar = '1';
                
                while((fileIn = input.read()) != -1)
                {
                    char cur = (char)fileIn;
                
                    if(cur == '\n')
                        lineCount++;
                    
                    // We are in comment mode. Ignore anything that doesn't end a comment
                    if(seenSingle || seenMulti)
                    {
                        // End of single comment, reset booleans
                        if(seenSingle && cur == '\n')
                        {
                            seenSingle = false;
                            seenSlash = false;
                        }
                        // End of multi-line comment, reset booleans
                        else if(seenMulti && lastChar == '*' && cur == '/')
                        {
                            seenMulti = false;
                            seenSlash = false;
                        }
                        // Save current char to check for */ during next iteration
                        lastChar = cur;
                    }
                    // This is not a comment. Proceed with tokenizing
                    else
                    {
                        // Check for the start of a comment
                        if(cur == '/')
                        {
                            if(!seenSlash)
                            {
                                seenSlash = true;
                            }
                            // Check for single line comment
                            else
                            {
                                seenSingle = true;
                            }
                        }
                        // Check for start of multi line comment
                        else if(cur == '*')
                        {
                            // If it is a comment enter comment mode
                            if(seenSlash)
                            {
                                seenMulti = true;
                            }
                            // Just a multiply symbol. Tokenize it.
                            else
                            {
                                Tokenize(temp);
                                temp="";
                                Tokenize("*");
                            }
                        }
                        // Check for whitespace
                        else if(cur == ' ' || cur == '\t' || cur == '\n' || cur == '\r' || cur == '\f')
                        {
                            // Found a whitespace separated thing. Tokenize
                            if(temp.length() != 0)
                            {
                                Tokenize(temp);
                                temp = "";
                            }
                        }
                        // Check for symbols in current word
                        else if(!String.valueOf(cur).matches("([a-z]|[A-Z]|_)([a-z]|[A-Z]|[0-9]|_)*") && !String.valueOf(cur).matches("([0-9])+"))
                        {
                            // Handle System.out.println in a special way
                            if(temp.equals("System") || temp.equals("System.out"))
                            {
                                temp += cur;
                            }
                            // Check for start of possible 2 char symbol
                            else if(!seenSymbol && (cur == '&' || cur == '|' || cur == '<' ||
                                                    cur == '=' || cur == '!' || cur == '>'))
                            {
                                seenSymbol = true;
                                Tokenize(temp);
                                temp = "";
                                sym += cur;
                                continue;
                            }
                            // Check for second char of 2 char symbol
                            else if(seenSymbol)
                            {
                                // If it is a 2 char symbol tokenize it
                                if((sym.equals("&") && cur == '&') ||
                                   (sym.equals("|") && cur == '|') ||
                                   (!sym.equals("&") && !sym.equals("|") && cur == '='))
                                {
                                    sym += cur;
                                    Tokenize(sym);
                                }
                                else
                                {
                                    Tokenize(sym);
                                    Tokenize(String.valueOf(cur));
                                }
                                sym = "";
                            }
                            // Single char symbol found. Tokenize it
                            else
                            {
                                Tokenize(temp);
                                temp = "";
                                Tokenize(String.valueOf(cur));
                            }
                        }
                        // Cur is just a character in a word or number, add to temp String
                        else
                        {
                            temp += cur;
                        }
                        
                        // We encountered a symbol at some point
                        if(seenSymbol)
                        {
                            // If it was a single char comment instead of a 2 char like we thought, tokenize it now
                            if(sym.length() == 1)
                            {
                                Tokenize(sym);
                            }
                            
                            // Reset symbol variables
                            seenSymbol = false;
                            sym = "";
                        }
                    }
                }
            
                if(seenMulti)
                {
                    throw new LexerException();
                }
                
                // Iterate through token list and print lexemes
                Iterator i = tokenList.iterator();
                while (i.hasNext())
                {
                    System.out.println(((MyToken)(i.next())).lexeme);
                }
                
            }
            catch (java.io.IOException ex)
            {
                System.out.println("File " + args[0] + " not found.");
                return;
            }
            catch (LexerException ex)
            {
                System.out.println("Error: Unclosed comment.");
                System.exit(1);
            }
        }
        
        public static void Tokenize(String token)
        {
            try
            {
                // Check for keywords
                if(token.equals("class"))
                {
                    tokenList.add(new MyToken("KEYWORD","class"));
                }
                else if(token.equals("public"))
                {
                    tokenList.add(new MyToken("KEYWORD","public"));
                }
                else if(token.equals("static"))
                {
                    tokenList.add(new MyToken("KEYWORD","static"));
                }
                else if(token.equals("void"))
                {
                    tokenList.add(new MyToken("KEYWORD","void"));
                }
                else if(token.equals("main"))
                {
                    tokenList.add(new MyToken("KEYWORD","main"));
                }
                else if(token.equals("String"))
                {
                    tokenList.add(new MyToken("KEYWORD","String"));
                }
                else if(token.equals("extends"))
                {
                    tokenList.add(new MyToken("KEYWORD","extends"));
                }
                else if(token.equals("return"))
                {
                    tokenList.add(new MyToken("KEYWORD","return"));
                }
                else if(token.equals("int"))
                {
                    tokenList.add(new MyToken("KEYWORD","int"));
                }
                else if(token.equals("boolean"))
                {
                    tokenList.add(new MyToken("KEYWORD","boolean"));
                }
                else if(token.equals("if"))
                {
                    tokenList.add(new MyToken("KEYWORD","if"));
                }
                else if(token.equals("else"))
                {
                    tokenList.add(new MyToken("KEYWORD","else"));
                }
                else if(token.equals("while"))
                {
                    tokenList.add(new MyToken("KEYWORD","while"));
                }
                else if(token.equals("true"))
                {
                    tokenList.add(new MyToken("KEYWORD","true"));
                }
                else if(token.equals("false"))
                {
                    tokenList.add(new MyToken("KEYWORD","false"));
                }
                else if(token.equals("this"))
                {
                    tokenList.add(new MyToken("KEYWORD","this"));
                }
                else if(token.equals("new"))
                {
                    tokenList.add(new MyToken("KEYWORD","new"));
                }
                else if(token.equals("System.out.println"))
                {
                    tokenList.add(new MyToken("KEYWORD","System.out.println"));
                }
                
                // Symbols
                else if(token.equals("{"))
                {
                    tokenList.add(new MyToken("SYMBOL","{"));
                }
                else if(token.equals("}"))
                {
                    tokenList.add(new MyToken("SYMBOL","}"));
                }
                else if(token.equals("("))
                {
                    tokenList.add(new MyToken("SYMBOL","("));
                }
                else if(token.equals(")"))
                {
                    tokenList.add(new MyToken("SYMBOL",")"));
                }
                else if(token.equals("["))
                {
                    tokenList.add(new MyToken("SYMBOL","["));
                }
                else if(token.equals("]"))
                {
                    tokenList.add(new MyToken("SYMBOL","]"));
                }
                else if(token.equals(";"))
                {
                    tokenList.add(new MyToken("SYMBOL",";"));
                }
                else if(token.equals("="))
                {
                    tokenList.add(new MyToken("SYMBOL","="));
                }
                else if(token.equals("&&"))
                {
                    tokenList.add(new MyToken("SYMBOL","&&"));
                }
                else if(token.equals("||"))
                {
                    tokenList.add(new MyToken("SYMBOL","||"));
                }
                else if(token.equals("<"))
                {
                    tokenList.add(new MyToken("SYMBOL","<"));
                }
                else if(token.equals("<="))
                {
                    tokenList.add(new MyToken("SYMBOL","<="));
                }
                else if(token.equals("=="))
                {
                    tokenList.add(new MyToken("SYMBOL","=="));
                }
                else if(token.equals("!="))
                {
                    tokenList.add(new MyToken("SYMBOL","!="));
                }
                else if(token.equals(">"))
                {
                    tokenList.add(new MyToken("SYMBOL",">"));
                }
                else if(token.equals(">="))
                {
                    tokenList.add(new MyToken("SYMBOL",">="));
                }else if(token.equals("+"))
                {
                    tokenList.add(new MyToken("SYMBOL","+"));
                }
                else if(token.equals("-"))
                {
                    tokenList.add(new MyToken("SYMBOL","-"));
                }
                else if(token.equals("*"))
                {
                    tokenList.add(new MyToken("SYMBOL","*"));
                }
                else if(token.equals("/"))
                {
                    tokenList.add(new MyToken("SYMBOL","/"));
                }
                else if(token.equals("%"))
                {
                    tokenList.add(new MyToken("SYMBOL","%"));
                }
                else if(token.equals("!"))
                {
                    tokenList.add(new MyToken("SYMBOL","!"));
                }
                else if(token.equals("."))
                {
                    tokenList.add(new MyToken("SYMBOL","."));
                }
                else if(token.equals(","))
                {
                    tokenList.add(new MyToken("SYMBOL",","));
                }
            
            
                // Check for identifiers and numerals
                else if(token.matches("([a-z]|[A-Z]|_)([a-z]|[A-Z]|[0-9]|_)*"))
                {
                    tokenList.add(new MyToken("IDENTIFIER",token));
                }
                else if(token.matches("([0-9])+"))
                {
                    tokenList.add(new MyToken("INT_LITERAL",token));
                }
            
                else if(token.equals(""))
                {
                    // Ignore
                }
                else
                {
                    throw new LexerException();
                }
            }
            catch(LexerException ex)
            {
                ex.printError(lineCount);
                System.exit(1);
            }
        }
    }
